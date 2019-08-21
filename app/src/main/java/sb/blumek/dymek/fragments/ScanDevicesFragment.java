package sb.blumek.dymek.fragments;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import sb.blumek.dymek.R;
import sb.blumek.dymek.adapters.DevicesAdapter;

public class ScanDevicesFragment extends Fragment implements DevicesAdapter.OnItemClickListener {
    private final static String TAG = ScanDevicesFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private DevicesAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private Button scanButton;

    private static final int REQUEST_ACCESS_COARSE_LOCATION = 99;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10 * 1000;

    private boolean isScanning;
    private Handler mHandler;
    private BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver receiver;

    public ScanDevicesFragment() {
        mHandler = new Handler();

        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    if (mAdapter == null)
                        return;

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        Log.i(TAG, device.getName());
                    }
                    mAdapter.addDevice(device);
                }
            }
        };

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan_devices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", (dialog, which) -> System.exit(0))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        scanButton = view.findViewById(R.id.scan_btn);
        scanButton.setOnClickListener(v -> {
            if (!isScanning) {
                scanButton.setText(R.string.stop);
                mAdapter.clear();
                startScanningDevices();
            } else {
                scanButton.setText(R.string.scan);
                stopScanningDevices();
            }
        });

        recyclerView = view.findViewById(R.id.devices_RV);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DevicesAdapter();
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);
    }

    void startScanningDevices() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "A bluetooth adapter is disabled.");
            return;
        }

        mHandler.postDelayed(this::stopScanningDevices, SCAN_PERIOD);

        Log.d(TAG, "Checking for required permissions.");
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.w(TAG, "Need additional permissions.");
            Log.d(TAG, "Asking for permissions.");
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION);
        }

        Log.d(TAG, "Starting devices discovery.");
        isScanning = true;
        bluetoothAdapter.startDiscovery();
        configureScanButton();
        getActivity().invalidateOptionsMenu();
    }

    void stopScanningDevices() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "A bluetooth adapter is disabled.");
            return;
        }

        Log.d(TAG, "Canceling devices discovery.");
        isScanning = false;
        bluetoothAdapter.cancelDiscovery();
        configureScanButton();
        getActivity().invalidateOptionsMenu();
    }

    public void configureScanButton() {
        Log.d(TAG, "Configuring a scan button.");
        if (isScanning) {
            Log.d(TAG, "Configuring the scan button to stop scanning.");
            scanButton.setText(R.string.stop);
        } else {
            Log.d(TAG, "Configuring the scan button to start scanning.");
            scanButton.setText(R.string.scan);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopScanningDevices();
        mAdapter.clear();
        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming the activity,");

        Log.d(TAG, "Registering a receiver.");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, filter);

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "The bluetooth adapter is not enabled.");
            Log.d(TAG, "Asking for enabling the bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        startScanningDevices();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scan_menu, menu);
        if (!isScanning) {
            Log.d(TAG, "Turning off a progress bar.");
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            Log.d(TAG, "Turning on a progress bar.");
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_progress_bar);
        }
    }

    @Override
    public void onItemClick(BluetoothDevice bluetoothDevice) {
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

            new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Czy na pewno?")
                    .setMessage("Wybrane urządzenie ma inną nazwę niż dedykowany sprzęt do aplikacji - \""
                            + getResources().getString(R.string.app_name)
                            + "\", możesz potem odłączyć zatwierdzone urządzenie w ustawieniach kontrolera.")
                    .setPositiveButton("Wiem co robię", (dialog, which) -> {
                        Log.i(TAG, "The user confirms the selection.");
                        openDeviceController(bluetoothDevice);
                    })
                    .setNegativeButton("Chcę zmienić wybór", (dialog, which) ->
                            Log.i(TAG, "The user gives up the selection."))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        } else if (bluetoothDevice != null) {
            Log.d(TAG, "The user selects a device.");
            openDeviceController(bluetoothDevice);
        }
    }

    private void openDeviceController(BluetoothDevice bluetoothDevice) {
        if (bluetoothAdapter.isDiscovering()) {
            stopScanningDevices();
        }

        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, new DeviceControllerFragment(), "controller")
                .commit();
    }
}
