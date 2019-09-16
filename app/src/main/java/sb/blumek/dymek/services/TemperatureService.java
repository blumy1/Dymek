package sb.blumek.dymek.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Objects;

import sb.blumek.dymek.R;
import sb.blumek.dymek.listeners.AlarmListener;
import sb.blumek.dymek.listeners.BluetoothListener;
import sb.blumek.dymek.listeners.ConnectionListener;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.shared.Commands;
import sb.blumek.dymek.shared.Temperature;
import sb.blumek.dymek.sockets.BluetoothSocket;
import sb.blumek.dymek.utils.CommandUtils;

public class TemperatureService extends Service implements BluetoothListener, Observable {

    public class ServiceBinder extends Binder {
        public TemperatureService getService() {
            return TemperatureService.this;
        }
    }

    private enum ConnectionState {
        NotConnected,
        Pending,
        Connected
    }

    private final Handler mainLooper;
    private final IBinder binder;
    private ConnectionListener connectionListener;
    private AlarmListener alarmListener;

    private boolean connected;
    private StringBuilder commandsCache;
    private ConnectionState connectionState;
    private String deviceAddress;
    private BluetoothSocket socket;

    Temperature temperature1 = new Temperature();
    Temperature temperature2 = new Temperature();

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public TemperatureService() {
        mainLooper = new Handler(Looper.getMainLooper());
        binder = new ServiceBinder();
        commandsCache = new StringBuilder();
        temperature1.setName("Temp 1");
        temperature2.setName("Temp 2");
    }

    @Override
    public void onDestroy() {
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
            if (isAvailableConnectionListener()) {
                connectionListener.onConnecting();
            }
            socket = new BluetoothSocket();
            connected = true;
            connectionState = ConnectionState.Connected;
            socket.connect(getApplicationContext(), this, device);
        } catch (Exception e) {
            onConnectError(e);
        }
    }

    public void disconnect() {
        connectionState = ConnectionState.NotConnected;
        socket.disconnect();
        socket = null;
        connected = false;

        if (isAvailableConnectionListener())
            connectionListener.onDisconnect();
    }

    public void send(String str) {
        if(!isConnected()) {
            Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            String newline = "\r\n";
            byte[] data = (str + newline).getBytes();
            System.out.println("WRITING...");
            socket.write(data);
        } catch (Exception e) {
            onIoError(e);
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.Connected;
    }

    public boolean isConnecting() {
        return connectionState == ConnectionState.Pending;
    }

    public boolean isDisconnected() {
        return connectionState == ConnectionState.NotConnected;
    }

    @Override
    public void onConnect() {
        if(connected) {
            synchronized (this) {
                if (isAvailableConnectionListener()) {
                    mainLooper.post(() -> connectionListener.onConnect());
                }
            }
        }
    }

    private boolean isAvailableConnectionListener() {
        return connectionListener != null;
    }

    @Override
    public void onConnectError(Exception e) {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> {
                    disconnect();

                    if (isAvailableConnectionListener()) {
                        connectionListener.onDisconnect();
                    }
                });
            }
        }
    }

    @Override
    public void onRead(byte[] data) {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> {
                    receive(data);
                    handleAvailableCommands();
                    notifyObservers();
                });
            }
        }
    }

    @Override
    public void onIoError(Exception e) {
        if(connected) {
            synchronized (this) {
                mainLooper.post(() -> {
                    disconnect();

                    if (isAvailableConnectionListener()) {
                        connectionListener.onDisconnect();
                    }
                });
            }
        }
    }

    private void receive(byte[] data) {
        appendMessage(new String(data));
    }

    private void status(String status) {
        SpannableStringBuilder spn = new SpannableStringBuilder(status + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        Log.i("TAG", spn.toString());
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void setAlarmListener(AlarmListener alarmListener) {
        this.alarmListener = alarmListener;
    }

    private void appendMessage(String message) {
        if (message != null && !message.isEmpty()) {
            commandsCache.append(message);
        }
        Log.i("TAG", commandsCache.toString());
    }

    private void handleCommands(String command) {
        if (command == null)
            return;

        handleAuthorizationCommand(command);
        handleTemp1NameCommand(command);
        handleTemp1Value(command);
        handleTemp1MinCommand(command);
        handleTemp1MaxCommand(command);

        handleTemp2NameCommand(command);
        handleTemp2ValueCommand(command);
        handleTemp2MinCommand(command);
        handleTemp2MaxCommand(command);

        handleAlarmUpCommand(command);
        handleAlarmDownCommand(command);
    }

    private void handleAlarmUpCommand(String command) {
        String alarmUp = CommandUtils.getStringFromExp(command, Commands.ALARM_UP, 0);
        if (alarmUp != null && isAvailableAlarmListener()) {
            alarmListener.alarmActivated();
        }
    }

    private void handleAlarmDownCommand(String command) {
        String alarmDown = CommandUtils.getStringFromExp(command, Commands.ALARM_DOWN, 0);
        if (alarmDown != null && isAvailableAlarmListener()) {
            alarmListener.alarmDeactivated();
        }
    }

    private boolean isAvailableAlarmListener() {
        return alarmListener != null;
    }

    private void handleTemp2MaxCommand(String command) {
        Double t2Max = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_MAX_VALUE, 1);
        if (t2Max != null) {
            temperature2.setTempMax(t2Max);
        }
    }

    private void handleTemp2MinCommand(String command) {
        Double t2Min = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_MIN_VALUE, 1);
        if (t2Min != null) {
            temperature2.setTempMin(t2Min);
        }
    }

    private void handleTemp2ValueCommand(String command) {
        Double temp2 = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_VALUE, 1);
        if (temp2 != null) {
            temperature2.setTemp(temp2);
        }
    }

    private void handleTemp2NameCommand(String command) {
        String t2Name = CommandUtils.getStringFromExp(command, Commands.TEMP_2_NAME, 1);
        if (t2Name != null) {
            temperature2.setName(t2Name);
        }
    }

    private void handleTemp1MaxCommand(String command) {
        Double t1Max = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_MAX_VALUE, 1);
        if (t1Max != null) {
            temperature1.setTempMax(t1Max);
        }
    }

    private void handleTemp1MinCommand(String command) {
        Double t1Min = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_MIN_VALUE, 1);
        if (t1Min != null) {
            temperature1.setTempMin(t1Min);
        }
    }

    private void handleTemp1Value(String command) {
        Double temp1 = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_VALUE, 1);
        if (temp1 != null) {
            temperature1.setTemp(temp1);
        }
    }

    private void handleTemp1NameCommand(String command) {
        String t1Name = CommandUtils.getStringFromExp(command, Commands.TEMP_1_NAME, 1);
        if (t1Name != null) {
            temperature1.setName(t1Name);
        }
    }

    private void handleAuthorizationCommand(String command) {
        String welcomeCommand = CommandUtils.getStringFromExp(command, Commands.DEVICE_HI, 0);
        if (Commands.DEVICE_HI_CLR.equals(welcomeCommand)) {
            send(Commands.APP_HI);
        }
    }

    private void handleAvailableCommands() {
        if (CommandUtils.isCommand(commandsCache.toString())) {
            String command = CommandUtils.getCommand(commandsCache.toString());
            handleCommands(command);

            commandsCache = new StringBuilder(Objects.requireNonNull(CommandUtils.removeCommandFromExp(commandsCache.toString())));
        }
    }

    public Temperature getTemperature1() {
        return temperature1;
    }

    public Temperature getTemperature2() {
        return temperature2;
    }
}
