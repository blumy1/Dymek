package sb.blumek.thermometer_controller_app.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.UUID;

import sb.blumek.thermometer_controller_app.Commands;
import sb.blumek.thermometer_controller_app.R;
import sb.blumek.thermometer_controller_app.SampleGattAttributes;
import sb.blumek.thermometer_controller_app.utils.StorageManager;
import sb.blumek.thermometer_controller_app.services.BluetoothLeService;
import sb.blumek.thermometer_controller_app.services.BluetoothLeService.LocalBinder;

public class DeviceControllerActivity extends AppCompatActivity {
    private final static String TAG = DeviceControllerActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private TextView mConnectionState;
    private Button changeStateButton;
    private String mDeviceName;
    private String mDeviceAddress;
    private ActionBar actionBar;
    private TextView temp1TV;
    private TextView temp2TV;
    private TextView temp1NameTV;
    private TextView temp2NameTV;
    private View separatorV;
    private BluetoothLeService mBluetoothLeService;
    private StorageManager storageManager;
    private boolean isFirstTimeLoading;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mBluetoothLeService = binder.getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                new AlertDialog.Builder(DeviceControllerActivity.this)
                        .setTitle("Not compatible")
                        .setMessage("Your phone does not support Bluetooth")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                finish();
            } else
                Log.i(TAG, "Bluetooth is initialized.");

            if (!mBluetoothLeService.isDeviceAddressSet()) {
                mBluetoothLeService.setDeviceAddress(mDeviceAddress);
            }

            if (isFirstTimeLoading){
                mBluetoothLeService.connect();
                isFirstTimeLoading = false;
            }

            setUpUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_CONNECTED.equals(action)) {
                Log.i(TAG, "Broadcast - Connected");
                showUI();
                changeConnectionStateToConnected();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_CONNECTING.equals(action)) {
                Log.i(TAG, "Broadcast - Connecting");
                changeConnectionStateToConnecting();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_AUTHORIZING.equals(action)) {
                Log.i(TAG, "Broadcast - Authorizing");
                changeConnectionStateToAuthorizing();
                hideUI();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_DISCONNECTED.equals(action)) {
                Log.i(TAG, "Broadcast - Disconnected");
                hideUI();
                changeConnectionStateToDisconnect();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "Broadcast - Services discovered");

                if (!storageManager.contains(StorageManager.MAC_ADDRESS)) {
                    Log.i(TAG, "Saving the device");
                    storageManager.saveString(StorageManager.DEVICE_NAME, mDeviceName);
                    storageManager.saveString(StorageManager.MAC_ADDRESS, mDeviceAddress);
                }
            } else if (BluetoothLeService.ACTION_ALARM_RINGING.equals(action)) {
                Log.i(TAG, "Broadcast - Alarm ringing");
                enableAlarmButton();
            } else if (BluetoothLeService.ACTION_TEMP1_VALUE_AVAILABLE.equals(action)) {
                Log.i(TAG, "Broadcast - Temp1 value available");
                String value = intent.getStringExtra(action);
                Log.i(TAG, "Temp1 value - " + value);
                temp1TV.setText(value);
            } else if (BluetoothLeService.ACTION_TEMP2_VALUE_AVAILABLE.equals(action)) {
                Log.i(TAG, "Broadcast - Temp2 value available");
                String value = intent.getStringExtra(action);
                Log.i(TAG, "Temp1 value - " + value);
                temp2TV.setText(value);
            } else if (BluetoothLeService.ACTION_TEMP1_NAME_AVAILABLE.equals(action)) {
                Log.i(TAG, "Broadcast - Temp1 name available");
                String name = intent.getStringExtra(action);
                Log.i(TAG, "Temp1 name - " + name);
                temp1NameTV.setText(name);
            } else if (BluetoothLeService.ACTION_TEMP2_NAME_AVAILABLE.equals(action)) {
                Log.i(TAG, "Broadcast - Temp2 name available");
                String name = intent.getStringExtra(action);
                Log.i(TAG, "Temp2 name - " + name);
                temp2NameTV.setText(name);
            }
        }
    };

    private void checkForStorageChanges() {
        if (storageManager.contains(StorageManager.TEMP_1_NAME)) {
            String temp1Name = storageManager.getString(StorageManager.TEMP_1_NAME);
            if (!temp1Name.equals(temp1NameTV.getText().toString())) {
                temp1NameTV.setText(temp1Name);
            }
        }

        if (storageManager.contains(StorageManager.TEMP_2_NAME)) {
            String temp2Name = storageManager.getString(StorageManager.TEMP_2_NAME);
            if (!temp2Name.equals(temp2NameTV.getText().toString())) {
                temp2NameTV.setText(temp2Name);
            }
        }
    }

    private void hideUI() {
        temp1TV.setVisibility(View.INVISIBLE);
        temp1NameTV.setVisibility(View.INVISIBLE);
        temp2TV.setVisibility(View.INVISIBLE);
        temp2NameTV.setVisibility(View.INVISIBLE);
        separatorV.setVisibility(View.INVISIBLE);
        changeStateButton.setVisibility(View.INVISIBLE);
    }

    private void showUI() {
        temp1TV.setVisibility(View.VISIBLE);
        temp1NameTV.setVisibility(View.VISIBLE);
        temp2TV.setVisibility(View.VISIBLE);
        temp2NameTV.setVisibility(View.VISIBLE);
        separatorV.setVisibility(View.VISIBLE);
        changeStateButton.setVisibility(View.VISIBLE);
    }

    private void changeConnectionStateTextView(String state) {
        mConnectionState.setText(state);
    }

    private void changeConnectionStateToConnected() {
        changeConnectionStateTextView(getResources().getString(R.string.connected));
    }

    private void changeConnectionStateToConnecting() {
        changeConnectionStateTextView(getResources().getString(R.string.connecting));
    }

    private void changeConnectionStateToDisconnect() {
        changeConnectionStateTextView(getResources().getString(R.string.disconnected));
    }

    private void changeConnectionStateToAuthorizing() {
        changeConnectionStateTextView(getResources().getString(R.string.authorizing));
    }

    private void setUpUI() {
        if (false) {

        } else if (mBluetoothLeService.isConnected()) {
            showUI();
            changeConnectionStateToConnected();
        } else if (mBluetoothLeService.isDisconnected()) {
            hideUI();
            changeConnectionStateToDisconnect();
        }
        else if (mBluetoothLeService.isConnecting()) {
            hideUI();
            changeConnectionStateToConnecting();
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_controller);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setTitle(null);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(getResources()
                    .getDrawable(R.drawable.ic_settings_black));
        }

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        storageManager = new StorageManager(getApplicationContext().getSharedPreferences(StorageManager.FILE_NAME, 0));

        mConnectionState = findViewById(R.id.connection_state);
        temp1TV = findViewById(R.id.temp1_tv);
        temp2TV = findViewById(R.id.temp2_tv);
        temp1NameTV = findViewById(R.id.temp1Name_tv);
        temp2NameTV = findViewById(R.id.temp2Name_tv);
        separatorV = findViewById(R.id.separator_v);
        changeStateButton = findViewById(R.id.change_state_btn);
        changeStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.sendMessage(Commands.OFF_ALARM);
                mBluetoothLeService.stopAlarm();
                disableAlarmButton();
            }
        });
        disableAlarmButton();

        isFirstTimeLoading = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForStorageChanges();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, getIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.controller_menu, menu);
        if (mBluetoothLeService != null &&
                mBluetoothLeService.isConnected()) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else if (mBluetoothLeService != null &&
                mBluetoothLeService.isConnecting()) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect();
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                Intent settingsControllerIntent =
                        new Intent(DeviceControllerActivity.this,
                                ControllerSettingsActivity.class);
                startActivity(settingsControllerIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    public void disableAlarmButton() {
        Log.i(TAG, "Disabling alarm button");
        changeStateButton.setEnabled(false);
        changeStateButton.setBackground(getResources().getDrawable(R.drawable.rounded_outline_button_disabled));
        changeStateButton.setTextColor(getResources().getColor(R.color.colorPrimaryLight));
    }

    public void enableAlarmButton() {
        Log.i(TAG, "Enabling alarm button");
        changeStateButton.setEnabled(true);
        changeStateButton.setBackground(getResources().getDrawable(R.drawable.rounded_outline_button));
        changeStateButton.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    private static IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_CONNECTING);
        intentFilter.addAction(BluetoothLeService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_ALARM_RINGING);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP1_VALUE_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP2_VALUE_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP1_NAME_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP2_NAME_AVAILABLE);
        return intentFilter;
    }
}
