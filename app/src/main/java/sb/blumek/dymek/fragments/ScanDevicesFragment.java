package sb.blumek.dymek.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import sb.blumek.dymek.domain.Device;
import sb.blumek.dymek.storage.DeviceStorage;

public class ScanDevicesFragment extends Fragment implements DevicesAdapter.OnItemClickListener {
    public final static String TAG = ScanDevicesFragment.class.getSimpleName();

    private Menu menu;
    private DevicesAdapter mAdapter;
    private Button scanButton;

    private static final int REQUEST_ACCESS_COARSE_LOCATION = 99;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10 * 1000;
    private static final String CORRECT_DEVICE_WARN = "Wybrane urządzenie ma inną nazwę niż " +
            "dedykowany sprzęt do aplikacji - %s, możesz potem odłączyć zatwierdzone urządzenie " +
            "w ustawieniach kontrolera.";

    private boolean isScanning;
    private Handler mHandler;
    private BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver receiver;

    public ScanDevicesFragment() {
        mHandler = new Handler();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    if (mAdapter == null)
                        return;

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mAdapter.addDeviceAndNotify(device);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

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

        RecyclerView recyclerView = view.findViewById(R.id.devices_RV);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DevicesAdapter(this);
        recyclerView.setAdapter(mAdapter);
    }

    void startScanningDevices() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "A bluetooth adapter is disabled.");
            return;
        }

        mHandler.postDelayed(this::stopScanningDevices, SCAN_PERIOD);

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.w(TAG, "Need additional permissions.");
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION);
        }

        isScanning = true;
        bluetoothAdapter.startDiscovery();
        configureScanButton();
        showProgressBar();
    }

    private void stopScanningDevices() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "A bluetooth adapter is disabled.");
            return;
        }

        isScanning = false;
        bluetoothAdapter.cancelDiscovery();
        configureScanButton();
        hideProgressBar();
    }

    private void configureScanButton() {
        if (isScanning)
            scanButton.setText(R.string.stop);
        else
            scanButton.setText(R.string.scan);
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

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, filter);

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "The bluetooth adapter is not enabled.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        startScanningDevices();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scan_menu, menu);
        this.menu = menu;
        if (!isScanning) {
            hideProgressBar();
        } else {
            showProgressBar();
        }
    }

    private void showProgressBar() {
        if (getActivity() != null && menu != null)
            menu.findItem(R.id.menu_refresh).setActionView(
                R.layout.actionbar_progress_bar);
    }

    private void hideProgressBar() {
        if (getActivity() != null && menu != null)
            menu.findItem(R.id.menu_refresh).setActionView(null);
    }

    @Override
    public void onItemClick(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice != null && !bluetoothDevice
                .getName()
                .toLowerCase()
                .trim()
                .equals(getResources()
                        .getString(R.string.app_name)
                        .toLowerCase()
                        .trim())) {

            new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle(getResources()
                            .getString(R.string.are_you_sure))
                    .setMessage(String.format(CORRECT_DEVICE_WARN, getResources()
                            .getString(R.string.app_name)))
                    .setPositiveButton(getResources()
                            .getString(R.string.i_know_what_im_doing), (dialog, which) -> {
                        openDeviceController(bluetoothDevice);
                    })
                    .setNegativeButton(getResources()
                            .getString(R.string.i_want_to_change_selection), (dialog, which) ->
                            Log.w(TAG, "The user gives up the selection."))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        } else if (bluetoothDevice != null)
            openDeviceController(bluetoothDevice);
    }

    private void openDeviceController(BluetoothDevice bluetoothDevice) {
        if (bluetoothAdapter.isDiscovering()) {
            stopScanningDevices();
        }

        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .replace(R.id.fragment, new DeviceControllerFragment(bluetoothDevice.getAddress()),
                        DeviceControllerFragment.TAG)
                .commit();

        DeviceStorage deviceStorage = new DeviceStorage(getContext());
        deviceStorage.saveDevice(new Device(bluetoothDevice.getName(), bluetoothDevice.getAddress()));
    }
}
