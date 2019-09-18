package sb.blumek.dymek.sockets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import sb.blumek.dymek.R;
import sb.blumek.dymek.listeners.BluetoothListener;
import sb.blumek.dymek.shared.Constants;

/**
 * wrap BLE communication into socket like class
 *   - connect, disconnect and write as methods,
 *   - read + status is returned by SerialListener
 */
public class BluetoothSocket extends BluetoothGattCallback {

    private static final UUID BLUETOOTH_LE_CCCD           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_CC254X_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_CC254X_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_NRF_SERVICE    = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID BLUETOOTH_LE_NRF_CHAR_RW2   = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // read on microbit, write on adafruit
    private static final UUID BLUETOOTH_LE_NRF_CHAR_RW3   = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID BLUETOOTH_LE_RN4870_SERVICE = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID BLUETOOTH_LE_RN4870_CHAR_RW = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");

    private static final int MAX_MTU = 512; // BLE standard does not limit, some BLE 4.2 devices support 251, various source say that Android has max 512
    private static final int DEFAULT_MTU = 23;
    private static final String TAG = "SerialSocket";

    private final ArrayList<byte[]> writeBuffer;
    private final IntentFilter pairingIntentFilter;
    private final BroadcastReceiver pairingBroadcastReceiver;
    private final BroadcastReceiver disconnectBroadcastReceiver;

    private Context context;
    private BluetoothListener listener;
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic readCharacteristic, writeCharacteristic;

    private boolean writePending;
    private boolean canceled;
    private boolean connected;
    private int payloadSize = DEFAULT_MTU-3;

    public BluetoothSocket() {
        writeBuffer = new ArrayList<>();
        pairingIntentFilter = new IntentFilter();
        pairingIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        pairingIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        pairingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onPairingBroadcastReceive(context, intent);
            }
        };
        disconnectBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(listener != null)
                    listener.onIoError(new IOException("background disconnect"));
                disconnect(); // disconnect now, else would be queued until UI re-attached
            }
        };
    }

    public void disconnect() {
        Log.d(TAG, "disconnect");
        listener = null; // ignore remaining data and errors
        device = null;
        canceled = true;
        synchronized (writeBuffer) {
            writePending = false;
            writeBuffer.clear();
        }
        readCharacteristic = null;
        writeCharacteristic = null;
        if (gatt != null) {
            Log.d(TAG, "gatt.disconnect");
            gatt.disconnect();
            Log.d(TAG, "gatt.close");
            try {
                gatt.close();
            } catch (Exception ignored) {}
            gatt = null;
            connected = false;
        }
        try {
            context.unregisterReceiver(pairingBroadcastReceiver);
        } catch (Exception ignored) {
        }
        try {
            context.unregisterReceiver(disconnectBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    public void connect(Context context, BluetoothListener listener, BluetoothDevice device) throws IOException {
        if(connected || gatt != null)
            throw new IOException("already connected");
        canceled = false;
        this.context = context;
        this.listener = listener;
        this.device = device;
        context.registerReceiver(disconnectBroadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_DISCONNECT));
        Log.d(TAG, "connect "+device);
        context.registerReceiver(pairingBroadcastReceiver, pairingIntentFilter);
        if (Build.VERSION.SDK_INT < 23) {
            Log.d(TAG, "connectGatt");
            gatt = device.connectGatt(context, false, this);
        } else {
            Log.d(TAG, "connectGatt,LE");
            gatt = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE);
        }
        if (gatt == null)
            throw new IOException("connectGatt failed");
        // continues asynchronously in onPairingBroadcastReceive() and onConnectionStateChange()
    }

    private void onPairingBroadcastReceive(Context context, Intent intent) {
        // for ARM Mbed, Microbit, ... use pairing from Android bluetooth settings
        // for HM10-clone, ... pairing is initiated here
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(device==null || !device.equals(this.device))
            return;
        switch (intent.getAction()) {
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                final int pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1);
                Log.d(TAG, "pairing request " + pairingVariant);
                onConnectError(new IOException(context.getString(R.string.app_name)));
                // pairing dialog brings app to background (onPause), but it is still partly visible (no onStop), so there is no automatic disconnect()
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                Log.d(TAG, "bond state " + previousBondState + "->" + bondState);
                break;
            default:
                Log.d(TAG, "unknown broadcast " + intent.getAction());
                break;
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        // status directly taken from gat_api.h, e.g. 133=0x85=GATT_ERROR ~= timeout
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG,"connect status "+status+", discoverServices");
            if (!gatt.discoverServices())
                onConnectError(new IOException("discoverServices failed"));
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (connected)
                onIoError(new IOException("gatt status " + status));
            else
                onConnectError(new IOException("gatt status " + status));
        } else {
            Log.d(TAG, "unknown connect state "+newState+" "+status);
        }
        // continues asynchronously in onServicesDiscovered()
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "servicesDiscovered, status " + status);
        if (canceled)
            return;
        connectCharacteristics1(gatt);
    }

    private void connectCharacteristics1(BluetoothGatt gatt) {
        writePending = false;
        for (BluetoothGattService gattService : gatt.getServices()) {
            if (gattService.getUuid().equals(BLUETOOTH_LE_CC254X_SERVICE)) {
                Log.d(TAG, "service cc254x uart");
                //for(BluetoothGattCharacteristic characteristic : gattService.getCharacteristics())
                //    Log.d(TAG, "characteristic "+characteristic.getUuid());
                readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
                writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
            }
            if (gattService.getUuid().equals(BLUETOOTH_LE_RN4870_SERVICE)) {
                Log.d(TAG, "service rn4870 uart");
                //for(BluetoothGattCharacteristic characteristic : gattService.getCharacteristics())
                //    Log.d(TAG, "characteristic "+characteristic.getUuid());
                readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_RN4870_CHAR_RW);
                writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_RN4870_CHAR_RW);
            }
            if (gattService.getUuid().equals(BLUETOOTH_LE_NRF_SERVICE)) {
                Log.d(TAG, "service nrf uart");
                //for(BluetoothGattCharacteristic characteristic : gattService.getCharacteristics())
                //    Log.d(TAG, "characteristic "+characteristic.getUuid());
                BluetoothGattCharacteristic rw2 = gattService.getCharacteristic(BLUETOOTH_LE_NRF_CHAR_RW2);
                BluetoothGattCharacteristic rw3 = gattService.getCharacteristic(BLUETOOTH_LE_NRF_CHAR_RW3);
                if (rw2 != null && rw3 != null) {
                    int rw2prop = rw2.getProperties();
                    int rw3prop = rw3.getProperties();
                    boolean rw2write = (rw2prop & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                    boolean rw3write = (rw3prop & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                    Log.d(TAG, "characteristic properties " + rw2prop + "/" + rw3prop);
                    if (rw2write && rw3write) {
                        onConnectError(new IOException("multiple write characteristics (" + rw2prop + "/" + rw3prop + ")"));
                        return;
                    } else if (rw2write) { // some devices use this ...
                        writeCharacteristic = rw2;
                        readCharacteristic = rw3;
                    } else if (rw3write) { // ... and other devices use this characteristic
                        writeCharacteristic = rw3;
                        readCharacteristic = rw2;
                    } else {
                        onConnectError(new IOException("no write characteristic (" + rw2prop + "/" + rw3prop));
                        return;
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "request max MTU");
            if (!gatt.requestMtu(MAX_MTU))
                onConnectError(new IOException("request MTU failed"));
            // continues asynchronously in onMtuChanged
        } else {
            connectCharacteristics2(gatt);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Log.d(TAG,"mtu size "+mtu+", status="+status);
        super.onMtuChanged(gatt, mtu, status);
        if(status ==  BluetoothGatt.GATT_SUCCESS) {
            payloadSize = mtu - 3;
            Log.d(TAG, "payload size "+payloadSize);
        }
        connectCharacteristics2(gatt);
    }

    private void connectCharacteristics2(BluetoothGatt gatt) {
        if(readCharacteristic==null || writeCharacteristic==null) {
            for (BluetoothGattService gattService : gatt.getServices()) {
                Log.d(TAG, "service "+gattService.getUuid());
            }
            onConnectError(new IOException("no serial profile found"));
            return;
        }
        int writeProperties = writeCharacteristic.getProperties();
        if((writeProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE +     // Microbit,HM10-clone have WRITE
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) ==0) { // HM10,TI uart have only WRITE_NO_RESPONSE
            onConnectError(new IOException("write characteristic not writable"));
            return;
        }
        if(!gatt.setCharacteristicNotification(readCharacteristic,true)) {
            onConnectError(new IOException("no notification for read characteristic"));
            return;
        }
        BluetoothGattDescriptor readDescriptor = readCharacteristic.getDescriptor(BLUETOOTH_LE_CCCD);
        if(readDescriptor == null) {
            onConnectError(new IOException("no CCCD descriptor for read characteristic"));
            return;
        }
        int readProperties = readCharacteristic.getProperties();
        if((readProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            Log.d(TAG, "enable read indication");
            readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }else if((readProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            Log.d(TAG, "enable read notification");
            readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            onConnectError(new IOException("no indication/notification for read characteristic ("+readProperties+")"));
            return;
        }
        Log.d(TAG,"writing read characterictic descriptor");
        if(!gatt.writeDescriptor(readDescriptor)) {
            onConnectError(new IOException("read characteristic CCCD descriptor not writable"));
        }
        // continues asynchronously in onDescriptorWrite()
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if(descriptor.getCharacteristic() == readCharacteristic) {
            Log.d(TAG,"writing read characteristic descriptor finished, status="+status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onConnectError(new IOException("write descriptor failed"));
            } else {
                // onCharacteristicChanged with incoming data can happen after writeDescriptor(ENABLE_INDICATION/NOTIFICATION)
                // before confirmed by this method, so receive data can be shown before device is shown as 'Connected'.
                onConnect();
                connected = true;
                Log.d(TAG, "connected");
            }
        } else {
            Log.d(TAG,"unknown write descriptor finished, status="+status);
        }
    }

    /*
     * read
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(canceled)
            return;
        if(characteristic == readCharacteristic) {
            byte[] data = readCharacteristic.getValue();
            onRead(data);
//            Log.d(TAG,"read, len="+data.length);
        }
    }

    /*
     * write
     */
    public void write(byte[] data) throws IOException {
        if(canceled || !connected || writeCharacteristic == null)
            throw new IOException("not connected");
        byte[] data0;
        synchronized (writeBuffer) {
            if(data.length <= payloadSize) {
                data0 = data;
            } else {
                data0 = Arrays.copyOfRange(data, 0, payloadSize);
            }
            if(!writePending && writeBuffer.isEmpty()) {
                writePending = true;
            } else {
                writeBuffer.add(data0);
                Log.d(TAG,"write queued, len="+data0.length);
                data0 = null;
            }
            if(data.length > payloadSize) {
                for(int i=1; i<(data.length+payloadSize-1)/payloadSize; i++) {
                    int from = i*payloadSize;
                    int to = Math.min(from+payloadSize, data.length);
                    writeBuffer.add(Arrays.copyOfRange(data, from, to));
                    Log.d(TAG,"write queued, len="+(to-from));
                }
            }
        }
        if(data0 != null) {
            writeCharacteristic.setValue(data0);
            if (!gatt.writeCharacteristic(writeCharacteristic)) {
                onIoError(new IOException("write failed"));
            } else {
                Log.d(TAG,"write started, len="+data0.length);
            }
        }
        // continues asynchronously in onCharacteristicWrite()
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if(canceled || !connected || writeCharacteristic == null)
            return;
        if(status != BluetoothGatt.GATT_SUCCESS) {
            onIoError(new IOException("write failed"));
            return;
        }
        if(characteristic == writeCharacteristic) {
            Log.d(TAG,"write finished, status="+status);
            writeNext();
        }
    }

    private void writeNext() {
        final byte[] data;
        synchronized (writeBuffer) {
            if (!writeBuffer.isEmpty()) {
                writePending = true;
                data = writeBuffer.remove(0);
            } else {
                writePending = false;
                data = null;
            }
        }
        if(data != null) {
            writeCharacteristic.setValue(data);
            if (!gatt.writeCharacteristic(writeCharacteristic)) {
                onIoError(new IOException("write failed"));
            } else {
                Log.d(TAG,"write started, len="+data.length);
            }
        }
    }

    /**
     * SerialListener
     */
    private void onConnect() {
        if (listener != null)
            listener.onConnect();
    }

    private void onConnectError(Exception e) {
        canceled = true;
        if (listener != null)
            listener.onConnectError(e);
    }

    private void onRead(byte[] data) {
        if (listener != null)
            listener.onRead(data);
    }

    private void onIoError(Exception e) {
        writePending = false;
        canceled = true;
        if (listener != null)
            listener.onIoError(e);
    }

}
