package sb.blumek.dymek.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
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

import java.io.IOException;
import java.util.Locale;

import sb.blumek.dymek.R;
import sb.blumek.dymek.listeners.AlarmListener;
import sb.blumek.dymek.listeners.BluetoothListener;
import sb.blumek.dymek.listeners.ConnectionListener;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.shared.Commands;
import sb.blumek.dymek.shared.Temperature;
import sb.blumek.dymek.sockets.BluetoothSocket;
import sb.blumek.dymek.storage.TemperatureCache;
import sb.blumek.dymek.utils.CommandUtils;

public class TemperatureService extends Service implements BluetoothListener, Observable {
    private static final String TAG = TemperatureService.class.getSimpleName();
    private static final long ALARM_DELAY_SECONDS = 10 * 1000;

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

    @Override
    public void onCreate() {
        super.onCreate();
        setUpAlarm();
        isAfterAlarmDelay = true;
        temperatureCache = new TemperatureCache(getApplicationContext());
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

    private MediaPlayer mediaPlayer;
    private boolean isAfterAlarmDelay;

    private TemperatureCache temperatureCache;

    private final int authDelay = 5000;
    private boolean afterAuthDelay = true;

    private boolean isInitialStart = true;

    Temperature firstTemperature = new Temperature();
    Temperature secondTemperature = new Temperature();

    public TemperatureService() {
        mainLooper = new Handler(Looper.getMainLooper());
        binder = new ServiceBinder();
        commandsCache = new StringBuilder();
        firstTemperature.setName("Temp 1");
        secondTemperature.setName("Temp 2");
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
        if (socket != null)
            socket.disconnect();

        socket = null;
        connected = false;

        if (isAvailableConnectionListener())
            connectionListener.onDisconnect();
    }

    public void send(String message) {
        if(!isConnected()) {
            Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        SpannableStringBuilder spn = new SpannableStringBuilder(message + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        String newline = "\r\n";
        byte[] data = (message + newline).getBytes();
        Log.d(TAG, "WRITING...");

        try {
            socket.write(data);
        } catch (IOException e) {
            e.printStackTrace();
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
    }

    private void handleCommands(String command) {
        if (command == null)
            return;

        Log.i(TAG, "Command - " + command);

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
            if (isAfterAlarmDelay)
                startAlarm();
        }
    }

    private void handleAlarmDownCommand(String command) {
        String alarmDown = CommandUtils.getStringFromExp(command, Commands.ALARM_DOWN, 0);
        if (alarmDown != null && isAvailableAlarmListener()) {
            alarmListener.alarmDeactivated();
            stopAlarm();
        }
    }

    private boolean isAvailableAlarmListener() {
        return alarmListener != null;
    }

    private void handleTemp2MaxCommand(String command) {
        Double t2Max = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_MAX_VALUE, 1);
        if (t2Max != null && !t2Max.equals(secondTemperature.getTempMax())) {
            secondTemperature.setTempMax(t2Max);
            updateTemperatures();
        }
    }

    private void updateTemperatures() {
        temperatureCache.updateTemperatures(firstTemperature, secondTemperature);
    }

    private void handleTemp2MinCommand(String command) {
        Double t2Min = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_MIN_VALUE, 1);
        if (t2Min != null) {
            secondTemperature.setTempMin(t2Min);
            updateTemperatures();
        }
    }

    private void handleTemp2ValueCommand(String command) {
        Double temp2 = CommandUtils.getDoubleFromExp(command, Commands.TEMP_2_VALUE, 1);
        if (temp2 != null) {
            secondTemperature.setTemp(temp2);
        }
    }

    private void handleTemp2NameCommand(String command) {
        String t2Name = CommandUtils.getStringFromExp(command, Commands.TEMP_2_NAME, 1);
        if (t2Name != null) {
            secondTemperature.setName(t2Name);
            updateTemperatures();
        }
    }

    private void handleTemp1MaxCommand(String command) {
        Double t1Max = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_MAX_VALUE, 1);
        if (t1Max != null) {
            firstTemperature.setTempMax(t1Max);
            updateTemperatures();
        }
    }

    private void handleTemp1MinCommand(String command) {
        Double t1Min = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_MIN_VALUE, 1);
        if (t1Min != null) {
            firstTemperature.setTempMin(t1Min);
            updateTemperatures();
        }
    }

    private void handleTemp1Value(String command) {
        if (isInitialStart) {
            send(Commands.GET_ALL_TEMP_SETTINGS);
            isInitialStart = false;
        }

        Double temp1 = CommandUtils.getDoubleFromExp(command, Commands.TEMP_1_VALUE, 1);
        if (temp1 != null) {
            firstTemperature.setTemp(temp1);
        }
    }

    private void handleTemp1NameCommand(String command) {
        String t1Name = CommandUtils.getStringFromExp(command, Commands.TEMP_1_NAME, 1);
        if (t1Name != null) {
            firstTemperature.setName(t1Name);
            updateTemperatures();
        }
    }

    private void handleAuthorizationCommand(String command) {
        String welcomeCommand = CommandUtils.getStringFromExp(command, Commands.DEVICE_HI, 0);
        if (Commands.DEVICE_HI_CLR.equals(welcomeCommand) && afterAuthDelay) {
            send(Commands.APP_HI);
            afterAuthDelay = false;

            new Handler().postDelayed(() -> afterAuthDelay = true, authDelay);
        }
    }

    private void handleAvailableCommands() {
        if (CommandUtils.isCommand(commandsCache.toString())) {
            String command = CommandUtils.getCommand(commandsCache.toString());
            handleCommands(command);

            String cache = CommandUtils.removeCommandFromExp(commandsCache.toString());
            commandsCache = new StringBuilder(cache == null ? "" : cache);
        }
    }

    public Temperature getFirstTemperature() {
        return firstTemperature;
    }

    public Temperature getSecondTemperature() {
        return secondTemperature;
    }

    public void sendSettingsRequest() {
        send(Commands.GET_ALL_TEMP_SETTINGS);
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
            return;
        }

        new Handler().postDelayed(() -> {
            isAfterAlarmDelay = true;
        }, ALARM_DELAY_SECONDS);

        isAfterAlarmDelay = false;
        mediaPlayer.start();
    }

    public void stopAlarm() {
        if (!mediaPlayer.isPlaying()) {
            return;
        }

        mediaPlayer.pause();
        mediaPlayer.seekTo(0);
    }
}
