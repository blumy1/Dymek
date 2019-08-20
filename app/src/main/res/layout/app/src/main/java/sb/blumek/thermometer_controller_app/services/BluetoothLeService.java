/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sb.blumek.thermometer_controller_app.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import sb.blumek.thermometer_controller_app.Commands;
import sb.blumek.thermometer_controller_app.R;
import sb.blumek.thermometer_controller_app.SampleGattAttributes;
import sb.blumek.thermometer_controller_app.utils.CommandUtils;
import sb.blumek.thermometer_controller_app.utils.StorageManager;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final long DELAY_SECONDS = 10 * 1000;

    private Handler mHandler;

    private boolean isAfterDelay;
    private MediaPlayer mediaPlayer;
    private StorageManager storageManager;
    private StringBuilder commandsCache;
    private BluetoothAdapter bluetoothAdapter;
    private String deviceAddress;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    private int mConnectionState = STATE_DISCONNECTED;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public final static String ACTION_CONNECTED =
            "sb.blumek.thermometer_controller_app.ACTION_CONNECTED";
    public final static String ACTION_CONNECTING =
            "sb.blumek.thermometer_controller_app.ACTION_CONNECTING";
    public final static String ACTION_AUTHORIZING =
            "sb.blumek.thermometer_controller_app.ACTION_AUTHORIZING";
    public final static String ACTION_DISCONNECTED =
            "sb.blumek.thermometer_controller_app.ACTION_DISCONNECTED";
    public final static String ACTION_SERVICES_DISCOVERED =
            "sb.blumek.thermometer_controller_app.ACTION_SERVICES_DISCOVERED";
    public final static String ACTION_ALARM_RINGING =
            "sb.blumek.thermometer_controller_app.ACTION_ALARM_RINGING";
    public final static String ACTION_TEMP1_VALUE_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP1_VALUE_AVAILABLE";
    public final static String ACTION_TEMP2_VALUE_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP2_VALUE_AVAILABLE";
    public final static String ACTION_TEMP1_NAME_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP1_NAME_AVAILABLE";
    public final static String ACTION_TEMP2_NAME_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP2_NAME_AVAILABLE";
    public final static String ACTION_TEMP1_MIN_VALUE_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP1_MIN_VALUE_AVAILABLE";
    public final static String ACTION_TEMP1_MAX_VALUE_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP1_MAX_VALUE_AVAILABLE";
    public final static String ACTION_TEMP2_MIN_VALUE_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP2_MIN_VALUE_AVAILABLE";
    public final static String ACTION_TEMP2_MAX_VALUE_AVAILABLE =
            "sb.blumek.thermometer_controller_app.ACTION_TEMP2_MAX_VALUE_AVAILABLE";
    public final static UUID UUID_HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                stopAlarm();
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_SERVICES_DISCOVERED);
                setService(getSupportedGattServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "Cache: " + commandsCache);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendMessage(characteristic);
                anyAvailableCommand();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Cache: " + commandsCache);
            appendMessage(characteristic);
            anyAvailableCommand();
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String command) {
        final Intent intent = new Intent(action);
        intent.putExtra(action, command);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        Log.d(TAG, "Trying to initialize bluetooth adapter.");
        if (bluetoothAdapter != null) {
            Log.w(TAG, "Bluetooth adapter is already initialized.");
            return true;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null;
    }

    public boolean connect() {
        Log.d(TAG, "Trying to connect...");

        if (isConnected()) {
            Log.w(TAG, "There is already an existing connection.");
            return true;
        }

        if (isConnecting()) {
            Log.w(TAG, "There is already a try to connect.");
            return true;
        }

        if (bluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (deviceAddress == null) {
            Log.w(TAG, "Device address not set.");
            return false;
        }

        if (bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing bluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                broadcastUpdate(ACTION_CONNECTING);
                return true;
            } else {
                final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                bluetoothGatt = device.connectGatt(this, false, mGattCallback);
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        broadcastUpdate(ACTION_CONNECTING);
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

	public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		bluetoothGatt.writeCharacteristic(characteristic);
	}   

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (characteristic == null) {
            Log.w(TAG, "Passed null characteristic.");
            return;
        }

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (UUID_HM_RX_TX.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null)
            return Collections.emptyList();

        return bluetoothGatt.getServices();
    }

    public boolean isConnected() {
        return mConnectionState == BluetoothLeService.STATE_CONNECTED;
    }

    public boolean isConnecting() {
        return mConnectionState == BluetoothLeService.STATE_CONNECTING;
    }

    public boolean isDisconnected() {
        return mConnectionState == BluetoothLeService.STATE_DISCONNECTED;
    }

    private void setService(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;

        for (BluetoothGattService gattService : gattServices) {
            if (!UUID.fromString(SampleGattAttributes.HM_10_SERIAL).equals(gattService.getUuid()))
                continue;

            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            setCharacteristicNotification(characteristicRX, true);
            return;
        }
    }

    public void sendMessage(String message) {
        Log.d(TAG, "Trying to send a message.");
        if (message == null) {
            Log.w(TAG, "The message is null.");
            return;
        }

        if (isConnected() && characteristicTX != null) {
            Log.i(TAG, String.format("Message: %s sent.", message));
            message += '\n';
            final byte[] onBytes = message.getBytes();
            characteristicTX.setValue(onBytes);
            writeCharacteristic(characteristicTX);
        } else {
            Log.w(TAG, "Couldn't send the message, device is disconnected.");
        }
    }

    private void handleCommands(String command) {
        if (command == null)
            return;

        Log.d(TAG, "Handling a command.");

        String welcomeCommand = CommandUtils.getStringFromExp(command, Commands.DEVICE_HI, 0);
        if (Commands.DEVICE_HI_CLR.equals(welcomeCommand)) {
            sendMessage(Commands.APP_HI);
            broadcastUpdate(ACTION_AUTHORIZING);
        }

        Double temp1 = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_VALUE, 1);
        if (temp1 != null) {
            broadcastUpdate(ACTION_TEMP1_VALUE_AVAILABLE, String.valueOf(temp1));
        }

        Double temp2 = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_VALUE, 1);
        if (temp2 != null) {
            broadcastUpdate(ACTION_TEMP2_VALUE_AVAILABLE, String.valueOf(temp2));
        }

        String alarmUp = CommandUtils.getStringFromExp(command, Commands.ALARM_UP, 0);
        if (alarmUp != null && isAfterDelay) {
            startAlarm();
            broadcastUpdate(ACTION_ALARM_RINGING);
        }

        Double t1Min = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_MIN_VALUE, 1);
        if (t1Min != null) {
            storageManager.saveString(StorageManager.TEMP_1_MIN, String.valueOf(t1Min));
            broadcastUpdate(ACTION_TEMP1_MIN_VALUE_AVAILABLE, String.valueOf(t1Min));
        }

        Double t1Max = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_MAX_VALUE, 1);
        if (t1Max != null) {
            storageManager.saveString(StorageManager.TEMP_1_MAX, String.valueOf(t1Max));
            broadcastUpdate(ACTION_TEMP1_MAX_VALUE_AVAILABLE, String.valueOf(t1Max));
        }

        Double t2Min = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_MIN_VALUE, 1);
        if (t2Min != null) {
            storageManager.saveString(StorageManager.TEMP_2_MIN, String.valueOf(t2Min));
            broadcastUpdate(ACTION_TEMP2_MIN_VALUE_AVAILABLE, String.valueOf(t2Min));
        }

        Double t2Max = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_MAX_VALUE, 1);
        if (t2Max != null) {
            storageManager.saveString(StorageManager.TEMP_2_MAX, String.valueOf(t2Max));
            broadcastUpdate(ACTION_TEMP2_MAX_VALUE_AVAILABLE, String.valueOf(t2Max));
        }

        String t1Name = CommandUtils.getStringFromExp(command, Commands.TEMP_1_NAME, 1);
        if (t1Name != null) {
            broadcastUpdate(ACTION_TEMP1_NAME_AVAILABLE, t1Name);
            storageManager.saveString(StorageManager.TEMP_1_NAME, t1Name);
        }

        String t2Name = CommandUtils.getStringFromExp(command, Commands.TEMP_2_NAME, 1);
        if (t2Name != null) {
            broadcastUpdate(ACTION_TEMP2_NAME_AVAILABLE, t2Name);
            storageManager.saveString(StorageManager.TEMP_2_NAME, t2Name);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating an activity.");

        setUpAlarm();
        mHandler = new Handler();
        isAfterDelay = true;
        commandsCache = new StringBuilder();
        storageManager =
                new StorageManager(getApplicationContext().getSharedPreferences(StorageManager.FILE_NAME, 0));
    }

    public void setUpAlarm() {
        Log.d(TAG, "Setting up an alarm.");
        mediaPlayer = new MediaPlayer();
        try {
            Log.d(TAG, "Setting up a data source for the alarm.");
            Uri mediaPath = Uri.parse(String.format(Locale.ENGLISH ,"android.resource://%s/%d", getPackageName(), R.raw.alarm));
            mediaPlayer.setDataSource(getApplicationContext(), mediaPath);

            Log.d(TAG, "Setting up audio attributes for the alarm.");
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            mediaPlayer.prepare();
        } catch (Exception e) {
            Log.e(TAG, "An error occurred while setting up the alarm.");
            e.printStackTrace();
        }
    }

    public void startAlarm() {
        if (mediaPlayer.isPlaying()) {
            Log.w(TAG, "The alarm is already playing.");
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Removing the delay.");
                isAfterDelay = true;
            }
        }, DELAY_SECONDS);

        Log.d(TAG, "Playing the alarm.");
        Log.d(TAG, "Setting a delay.");
        isAfterDelay = false;
        mediaPlayer.start();
    }

    public void stopAlarm() {
        if (!mediaPlayer.isPlaying()) {
            Log.w(TAG, "The alarm is not playing.");
            return;
        }

        Log.d(TAG, "Stopping the alarm.");
        mediaPlayer.pause();
        mediaPlayer.seekTo(0);
    }

    public void setDeviceAddress(String mBluetoothDeviceAddress) {
        this.deviceAddress = mBluetoothDeviceAddress;
    }

    public boolean isDeviceAddressSet() {
        return deviceAddress != null;
    }

    private void appendMessage(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            Log.d(TAG, "Appending the incoming message to the command cache.");
            commandsCache.append(new String(data));
        }
    }

    private void anyAvailableCommand() {
        Log.d(TAG, "Checking if there is any available command.");
        if (CommandUtils.isCommand(commandsCache.toString())) {
            Log.d(TAG, "There is an available command.");
            String command = CommandUtils.getCommand(commandsCache.toString());
            handleCommands(command);

            Log.d(TAG, "Clearing handled commands from the commands cache.");
            commandsCache = new StringBuilder(CommandUtils.removeCommandFromExp(commandsCache.toString()));
        }
    }
}
