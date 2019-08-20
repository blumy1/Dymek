package sb.blumek.thermometer_controller_app.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

import sb.blumek.thermometer_controller_app.R;
import sb.blumek.thermometer_controller_app.adapters.DevicesAdapter;

public class DeviceScanActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {
    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    private ActionBar actionBar;
    private RecyclerView recyclerView;
    private DevicesAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private BluetoothAdapter mBluetoothAdapter;

    private Button scanButton;

    private boolean mScanning;
    private Handler mHandler;

    private final static int REQUEST_ACCESS_COARSE_LOCATION = 99;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10 * 1000;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (mAdapter == null)
                    return;

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mAdapter.addDevice(device);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        Log.d(TAG, "Creating an activity.");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setTitle(null);
        }
        mHandler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        scanButton = findViewById(R.id.scan_btn);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mScanning) {
                    scanButton.setText(R.string.stop);
                    mAdapter.clear();
                    startScanningDevices();
                } else {
                    scanButton.setText(R.string.scan);
                    stopScanningDevices();
                }
            }
        });

        recyclerView = findViewById(R.id.devices_RV);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DevicesAdapter(new ArrayList<BluetoothDevice>());
        mAdapter.setOnItemClickListener(DeviceScanActivity.this);
        recyclerView.setAdapter(mAdapter);
    }

    void startScanningDevices() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "A bluetooth adapter is disabled.");
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanningDevices();
            }
        }, SCAN_PERIOD);

        Log.d(TAG, "Checking for required permissions.");
        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Need additional permissions.");
            Log.d(TAG, "Asking for permissions.");
            ActivityCompat.requestPermissions(DeviceScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION);
        }

        Log.d(TAG, "Starting devices discovery.");
        mScanning = true;
        mBluetoothAdapter.startDiscovery();
        configureScanButton();
        invalidateOptionsMenu();
    }

    void stopScanningDevices() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "A bluetooth adapter is disabled.");
            return;
        }

        Log.d(TAG, "Canceling devices discovery.");
        mScanning = false;
        mBluetoothAdapter.cancelDiscovery();
        configureScanButton();
        invalidateOptionsMenu();
    }

    public void configureScanButton() {
        Log.d(TAG, "Configuring a scan button.");
        if (mScanning) {
            Log.d(TAG, "Configuring the scan button to stop scanning.");
            scanButton.setText(R.string.stop);
        } else {
            Log.d(TAG, "Configuring the scan button to start scanning.");
            scanButton.setText(R.string.scan);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Pausing the activity");

        stopScanningDevices();
        mAdapter.clear();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming the activity,");

        Log.d(TAG, "Registering a receiver.");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        if (mBluetoothAdapter != null) {
            Log.d(TAG, "Checking if a bluetooth adapter is enabled.");
            if (!mBluetoothAdapter.isEnabled()) {
                Log.w(TAG, "The bluetooth adapter is not enabled.");
                Log.d(TAG, "Asking for enabling the bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        startScanningDevices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            Log.d(TAG, "Turning off a progress bar.");
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            Log.d(TAG, "Turning on a progress bar.");
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public void onItemClick(final BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "An user taps on device.");
        if (bluetoothDevice != null && !bluetoothDevice
                .getName()
                .toLowerCase()
                .trim()
                .equals(getResources()
                        .getString(R.string.app_name)
                        .toLowerCase()
                        .trim())) {

            Log.w(TAG, "The user may have selected wrong device.");

            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Czy na pewno?")
                    .setMessage("Wybrane urządzenie ma inną nazwę niż dedykowany sprzęt do aplikacji - \""
                            + getResources().getString(R.string.app_name)
                            + "\", możesz potem odłączyć zatwierdzone urządzenie w ustawieniach kontrolera.")
                    .setPositiveButton("Wiem co robię", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, "The user confirms the selection.");
                            openDeviceController(bluetoothDevice);
                        }
                    })
                    .setNegativeButton("Chcę zmienić wybór", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, "The user gives up the selection.");
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        } else if (bluetoothDevice != null) {
            Log.d(TAG, "The user selects a device.");
            openDeviceController(bluetoothDevice);
        }

    }

    private void openDeviceController(BluetoothDevice bluetoothDevice) {
        if (mBluetoothAdapter.isDiscovering()) {
            stopScanningDevices();
        }

        Intent intent = new Intent(this, DeviceControllerActivity.class);
        intent.putExtra(DeviceControllerActivity.EXTRAS_DEVICE_NAME, bluetoothDevice.getName());
        intent.putExtra(DeviceControllerActivity.EXTRAS_DEVICE_ADDRESS, bluetoothDevice.getAddress());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d(TAG, "Opening device controller activity.");
        Log.d(TAG, "Attached bluetooth device - " + bluetoothDevice.getName() + " - " + bluetoothDevice.getAddress());

        startActivity(intent);
    }
}
