package sb.blumek.dymek.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import sb.blumek.dymek.R;
import sb.blumek.dymek.listeners.BluetoothListener;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.shared.Constants;
import sb.blumek.dymek.sockets.BluetoothSocket;

public class BluetoothService extends Service implements BluetoothListener, Observable {

    public class ServiceBinder extends Binder {
        public BluetoothService getService() { return BluetoothService.this; }
    }

    private enum ConnectionState {NotConnected, Pending, Connected}
    private final Handler mainLooper;
    private final IBinder binder;

    private boolean connected;
    private StringBuilder commandsCache;
    private ConnectionState connectionState;
    private String deviceAddress;
    private BluetoothSocket socket;
    private String notificationMsg;
    private String newline = "\r\n";

    public BluetoothService() {
        mainLooper = new Handler(Looper.getMainLooper());
        binder = new ServiceBinder();
        commandsCache = new StringBuilder();
    }

    @Override
    public void onDestroy() {
        cancelNotification();
        disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connectionState = ConnectionState.Pending;
            socket = new BluetoothSocket();
            connected = true;
            socket.connect(getApplicationContext(), this, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    public void disconnect() {
        connectionState = ConnectionState.NotConnected;
        socket.disconnect();
        socket = null;
        connected = false;
        notificationMsg = null;
    }

    public void send(String str) {
        if(!isConnected()) {
            Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.Connected;
    }

    public boolean isDisconnected() {
        return connectionState == ConnectionState.NotConnected;
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(Constants.NOTIFICATION_CHANNEL, "Background service", NotificationManager.IMPORTANCE_LOW);
            nc.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(nc);
        }
        Intent disconnectIntent = new Intent()
                .setAction(Constants.INTENT_ACTION_DISCONNECT);
        Intent restartIntent = new Intent()
                .setClassName(this, Constants.INTENT_CLASS_MAIN_ACTIVITY)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(this, 1, disconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent restartPendingIntent = PendingIntent.getActivity(this, 1, restartIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_icon)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(notificationMsg)
                .setContentIntent(restartPendingIntent)
                .setOngoing(true)
                .addAction(new NotificationCompat.Action(R.drawable.ic_icon, "Disconnect", disconnectPendingIntent));
        Notification notification = builder.build();
        startForeground(Constants.NOTIFY_MANAGER_START_FOREGROUND_SERVICE, notification);
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    @Override
    public void onSerialConnect() {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> {

                });
            }
        }
    }

    @Override
    public void onSerialConnectError(Exception e) {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> {
                    cancelNotification();
                    disconnect();
                });
            }
        }
    }

    @Override
    public void onSerialRead(byte[] data) {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> receive(data));
                notifyObservers();
            }
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> {
                    cancelNotification();
                    disconnect();
                });
            }
        }
    }

    private void receive(byte[] data) {
//        Log.i("TAG", new String(data));
        appendMessage(new String(data));
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        Log.i("TAG", spn.toString());
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    private void appendMessage(String message) {
        if (message != null && !message.isEmpty()) {
            commandsCache.append(message);
        }
        Log.i("TAG", commandsCache.toString());
    }
}
