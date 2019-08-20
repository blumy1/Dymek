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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.Locale;

import sb.blumek.thermometer_controller_app.Commands;
import sb.blumek.thermometer_controller_app.R;
import sb.blumek.thermometer_controller_app.services.BluetoothLeService;
import sb.blumek.thermometer_controller_app.utils.CommandUtils;
import sb.blumek.thermometer_controller_app.utils.StorageManager;

public class ControllerSettingsActivity extends AppCompatActivity {
    private final static String TAG = DeviceControllerActivity.class.getSimpleName();
    private final static int MAX_NAME_LENGTH = 8;
    private final static int TEMP_MIN = -127;
    private final static int TEMP_MAX = 127;

    private String lastTemp1Name;
    private Double lastTemp1Min;
    private Double lastTemp1Max;
    private String lastTemp2Name;
    private Double lastTemp2Min;
    private Double lastTemp2Max;

    private Button setBTN;
    private ActionBar actionBar;
    private EditText temp1MinET;
    private EditText temp1MaxET;
    private EditText temp2MinET;
    private EditText temp2MaxET;
    private EditText temp1NameET;
    private EditText temp2NameET;
    private LinearLayout disconnectLL;
    private BluetoothLeService mBluetoothLeService;
    private StorageManager storageManager;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
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
            if (BluetoothLeService.ACTION_TEMP1_NAME_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_TEMP1_MIN_VALUE_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_TEMP1_MAX_VALUE_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_TEMP2_NAME_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_TEMP2_MIN_VALUE_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_TEMP2_MAX_VALUE_AVAILABLE.equals(action)) {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setTitle(null);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(getResources()
                    .getDrawable(R.drawable.ic_arrow_back_black));
        }

        setBTN = findViewById(R.id.set_btn);
        setBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSettings();
            }
        });

        disconnectLL = findViewById(R.id.disconnect_ll);
        disconnectLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(view.getContext(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle("Czy na pewno?")
                        .setMessage("Próbujesz odłączyć urządzenie od aplikacji. Czy chcesz to na pewno zrobić?")
                        .setPositiveButton("Tak", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mBluetoothLeService.disconnect();
                                disconnectDevice();
                                openScanDevices();
                            }
                        })
                        .setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        temp1MinET = findViewById(R.id.temp1Min_et);
        temp1MaxET = findViewById(R.id.temp1Max_et);
        temp2MinET = findViewById(R.id.temp2Min_et);
        temp2MaxET = findViewById(R.id.temp2Max_et);
        temp1NameET = findViewById(R.id.temp1Name_et);
        temp2NameET = findViewById(R.id.temp2Name_et);

        storageManager = new StorageManager(getApplicationContext().getSharedPreferences(StorageManager.FILE_NAME, 0));

        setLastValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP1_NAME_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP1_MIN_VALUE_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP1_MAX_VALUE_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP2_NAME_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP2_MIN_VALUE_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMP2_MAX_VALUE_AVAILABLE);
        return intentFilter;
    }

    private void showEditTextError(EditText editText) {
        if (editText == null)
            return;

        editText.setBackground(getResources()
                .getDrawable(R.drawable
                        .rounded_dark_grey_text_error_bg));
    }

    private void showEditTextSuccess(EditText editText) {
        if (editText == null)
            return;

        editText.setBackground(getResources()
                .getDrawable(R.drawable
                        .rounded_dark_grey_text_success_bg));
    }

    private void showEditTextDefault(EditText editText) {
        if (editText == null)
            return;

        editText.setBackground(getResources()
                .getDrawable(R.drawable
                        .rounded_dark_grey_text_bg));
    }

    private void resetAllEditTexts() {
        showEditTextDefault(temp1NameET);
        showEditTextDefault(temp1MinET);
        showEditTextDefault(temp1MaxET);

        showEditTextDefault(temp2NameET);
        showEditTextDefault(temp2MinET);
        showEditTextDefault(temp2MaxET);
    }

    private void setLastValues() {
        lastTemp1Name = String.valueOf(temp1NameET.getText());
        lastTemp1Min = Double.valueOf(String.valueOf(temp1MinET.getText()));
        lastTemp1Max = Double.valueOf(String.valueOf(temp1MaxET.getText()));

        lastTemp2Name = String.valueOf(temp2NameET.getText());
        lastTemp2Min = Double.valueOf(String.valueOf(temp2MinET.getText()));
        lastTemp2Max = Double.valueOf(String.valueOf(temp2MaxET.getText()));
    }

    private void sendSettings() {
        boolean isCorrect = true;
        resetAllEditTexts();

        String temp1Name = String.valueOf(temp1NameET.getText());
        Double temp1Min = Double.valueOf(String.valueOf(temp1MinET.getText()));
        Double temp1Max = Double.valueOf(String.valueOf(temp1MaxET.getText()));

        String temp2Name = String.valueOf(temp2NameET.getText());
        Double temp2Min = Double.valueOf(String.valueOf(temp2MinET.getText()));
        Double temp2Max = Double.valueOf(String.valueOf(temp2MaxET.getText()));

        // Checking for temp1Name wrong values
        if (temp1Name == null || temp1Name.isEmpty()) {
            isCorrect = false;
            showEditTextError(temp1NameET);
        }

        if (temp1Name != null && temp1Name.length() > MAX_NAME_LENGTH) {
            isCorrect = false;
            showEditTextError(temp1NameET);
        }

        if (!CommandUtils.isValidName(temp1Name)) {
            isCorrect = false;
            showEditTextError(temp1NameET);
        }

        // Checking for temp1Min wrong values
        if (temp1Min == null) {
            isCorrect = false;
            showEditTextError(temp1MinET);
        }

        if (temp1Min != null && temp1Min < TEMP_MIN) {
            isCorrect = false;
            showEditTextError(temp1MinET);
        }

        if (temp1Min != null && temp1Min >= TEMP_MAX) {
            isCorrect = false;
            showEditTextError(temp1MinET);
        }

        //Checking for temp1Max wrong values
        if (temp1Max == null) {
            isCorrect = false;
            showEditTextError(temp1MaxET);
        }

        if (temp1Max != null && temp1Max < TEMP_MIN) {
            isCorrect = false;
            showEditTextError(temp1MaxET);
        }

        if (temp1Max != null && temp1Max >= TEMP_MAX) {
            isCorrect = false;
            showEditTextError(temp1MaxET);
        }

        // Checking for temp2Name wrong values
        if (temp2Name == null || temp2Name.isEmpty()) {
            isCorrect = false;
            showEditTextError(temp2NameET);
        }

        if (temp2Name != null && temp2Name.length() > MAX_NAME_LENGTH) {
            isCorrect = false;
            showEditTextError(temp2NameET);
        }

        if (!CommandUtils.isValidName(temp2Name)) {
            isCorrect = false;
            showEditTextError(temp2NameET);
        }

        // Checking for temp2Min wrong values
        if (temp2Min == null) {
            isCorrect = false;
            showEditTextError(temp2MinET);
        }

        if (temp2Min != null && temp2Min < TEMP_MIN) {
            isCorrect = false;
            showEditTextError(temp2MinET);
        }

        if (temp2Min != null && temp2Min >= TEMP_MAX) {
            isCorrect = false;
            showEditTextError(temp2MinET);
        }

        //Checking for temp2Max wrong values
        if (temp2Max == null) {
            isCorrect = false;
            showEditTextError(temp2MaxET);
        }

        if (temp2Max != null && temp2Max < TEMP_MIN) {
            isCorrect = false;
            showEditTextError(temp2MaxET);
        }

        if (temp2Max != null && temp2Max >= TEMP_MAX) {
            isCorrect = false;
            showEditTextError(temp2MaxET);
        }

        // Checking if temp1Max is greater or equal to temp1Min
        if (temp1Min != null && temp1Max != null && temp1Min >= temp1Max) {
            isCorrect = false;
            showEditTextError(temp1MinET);
            showEditTextError(temp1MinET);
        }

        // Checking if temp2Max is greater or equal to temp2Min
        if (temp2Min != null && temp2Max != null && temp2Min >= temp2Max) {
            isCorrect = false;
            showEditTextError(temp2MinET);
            showEditTextError(temp2MinET);
        }

        if (!isCorrect) {
            return;
        }

        if (!temp1Name.equals(lastTemp1Name)) {
            showEditTextSuccess(temp1NameET);
            mBluetoothLeService.sendMessage(String
                    .format(Locale.ENGLISH, Commands.SET_TEMP_1_NAME, temp1Name));
        }

        if (!temp1Min.equals(lastTemp1Min)) {
            showEditTextSuccess(temp1MinET);
            mBluetoothLeService.sendMessage(String
                    .format(Locale.ENGLISH, Commands.SET_TEMP_1_MIN, temp1Min));
        }

        if (!temp1Max.equals(lastTemp1Max)) {
            showEditTextSuccess(temp1MaxET);
            mBluetoothLeService.sendMessage(String
                    .format(Locale.ENGLISH, Commands.SET_TEMP_1_MAX, temp1Max));
        }

        if (!temp2Name.equals(lastTemp2Name)) {
            showEditTextSuccess(temp2NameET);
            mBluetoothLeService.sendMessage(String
                    .format(Locale.ENGLISH, Commands.SET_TEMP_2_NAME, temp2Name));
        }

        if (!temp2Min.equals(lastTemp2Min)) {
            showEditTextSuccess(temp2MinET);
            mBluetoothLeService.sendMessage(String
                    .format(Locale.ENGLISH, Commands.SET_TEMP_2_MIN, temp2Min));
        }

        if (!temp2Max.equals(lastTemp2Max)) {
            showEditTextSuccess(temp2MaxET);
            mBluetoothLeService.sendMessage(String
                    .format(Locale.ENGLISH, Commands.SET_TEMP_2_MAX, temp2Max));
        }
    }

    void disconnectDevice() {
        storageManager.removePref(StorageManager.DEVICE_NAME);
        storageManager.removePref(StorageManager.MAC_ADDRESS);
    }

    private void openScanDevices() {
        Intent intent = new Intent(this, DeviceScanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d(TAG, "Opening scan devices activity.");

        startActivity(intent);
    }
}
