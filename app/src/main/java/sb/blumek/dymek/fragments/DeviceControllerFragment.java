package sb.blumek.dymek.fragments;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import sb.blumek.dymek.R;
import sb.blumek.dymek.activities.MainActivity;
import sb.blumek.dymek.listeners.ConnectionListener;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.observables.Observer;
import sb.blumek.dymek.services.TemperatureService;
import sb.blumek.dymek.shared.Commands;
import sb.blumek.dymek.shared.Temperature;

public class DeviceControllerFragment extends Fragment implements ServiceConnection, Observer, ConnectionListener {
    public final static String TAG = ScanDevicesFragment.class.getSimpleName();

    private String deviceAddress;

    private TextView temp1TV;
    private TextView temp2TV;
    private TextView temp1NameTV;
    private TextView temp2NameTV;
    private View separatorV;
    private Button alarmBTN;
    private TextView connectionStateTV;
    private Menu menu;

    private TemperatureService service;
    private boolean initialStart = true;

    DeviceControllerFragment(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void update(Observable observable) {
        if (observable instanceof TemperatureService) {
            TemperatureService service = (TemperatureService) observable;

            Temperature temp1 = service.getTemperature1();
            Temperature temp2 = service.getTemperature2();

            temp1NameTV.setText(temp1.getName());
            temp1TV.setText(String.valueOf(temp1.getTemp()));

            temp2NameTV.setText(temp2.getName());
            temp2TV.setText(String.valueOf(temp2.getTemp()));

            refreshUI();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    private void showSettingsButton() {
        if (getActivity() instanceof AppCompatActivity) {
            ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            ((MainActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(getResources()
                    .getDrawable(R.drawable.ic_settings_black));
        }
    }

    private void hideSettingsButton() {
        if (getActivity() instanceof AppCompatActivity) {
            ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        if (service != null && !service.isDisconnected())
            disconnect();
        getActivity().stopService(new Intent(getActivity(), TemperatureService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        showSettingsButton();
        if(service == null)
            getActivity().startService(new Intent(getActivity(), TemperatureService.class));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), TemperatureService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "ON RESUME");
        showSettingsButton();
        refreshUI();

        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    private void refreshUI() {
        setUpUI();
        setConnectionState();
        setUpMenu();
    }

    private void setConnectionState() {
        if (isServiceConnected()) {
            setStateAsConnected();
        } else if (isServiceConnecting()) {
            setStateAsConnecting();
        } else {
            setStateAsDisconnected();
        }
    }

    private void setStateAsDisconnected() {
        connectionStateTV.setText(R.string.disconnected);
    }

    private void setStateAsConnecting() {
        connectionStateTV.setText(R.string.connecting);
    }

    private void setStateAsConnected() {
        connectionStateTV.setText(R.string.connected);
    }

    private void setUpUI() {
        if (isServiceConnected())
            setIsConnectedUI();
        else if (isServiceConnecting())
            setIsConnectingUI();
        else
            setIsConnectedUI();
    }

    private boolean isServiceConnected() {
        return service != null && service.isConnected();
    }

    private boolean isServiceConnecting() {
        return service != null && service.isConnecting();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((TemperatureService.ServiceBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            service.registerObserver(this);
            service.setDeviceAddress(deviceAddress);
            service.setConnectionListener(this);
            getActivity().runOnUiThread(this::connect);
            refreshUI();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service.setConnectionListener(null);
        service = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_controller, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        temp1TV = view.findViewById(R.id.temp1_tv);
        temp2TV = view.findViewById(R.id.temp2_tv);
        temp1NameTV = view.findViewById(R.id.temp1Name_tv);
        temp2NameTV = view.findViewById(R.id.temp2Name_tv);
        separatorV = view.findViewById(R.id.separator_v);
        alarmBTN = view.findViewById(R.id.change_state_btn);
        connectionStateTV = view.findViewById(R.id.connection_state);

        temp1TV.setOnClickListener(v -> {
            service.send(String.format(Commands.SET_TEMP_1_NAME, temp1TV.getText().toString()));
            Toast.makeText(getActivity(), "SENT", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.controller_menu, menu);
        this.menu = menu;
        setMenuOptions();
    }

    private void setMenuOptions() {
        if (isServiceConnected()) {
            showDisconnectButton();
        } else if (isServiceConnecting()){
            showConnectingProgress();
        } else {
            showConnectButton();
        }
    }

    private void showConnectingProgress() {
        if (menu != null) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_progress_bar);
        }
    }

    private void showConnectButton() {
        if (menu != null) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
    }

    private void showDisconnectButton() {
        if (menu != null) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }
    }

    private void connect() {
        service.connect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                connect();
                return true;
            case R.id.menu_disconnect:
                disconnect();
                return true;
            case android.R.id.home:
                openSettings();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment, new DeviceSettingsFragment(), DeviceSettingsFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    private void disconnect() {
        service.disconnect();
    }

    private void setUpMenu() {
        if (getActivity() != null)
            getActivity().invalidateOptionsMenu();
    }

    private void hideUI() {
        temp1TV.setVisibility(View.INVISIBLE);
        temp1NameTV.setVisibility(View.INVISIBLE);
        temp2TV.setVisibility(View.INVISIBLE);
        temp2NameTV.setVisibility(View.INVISIBLE);
        separatorV.setVisibility(View.INVISIBLE);
        alarmBTN.setVisibility(View.INVISIBLE);
    }

    private void showUI() {
        temp1TV.setVisibility(View.VISIBLE);
        temp1NameTV.setVisibility(View.VISIBLE);
        temp2TV.setVisibility(View.VISIBLE);
        temp2NameTV.setVisibility(View.VISIBLE);
        separatorV.setVisibility(View.VISIBLE);
        alarmBTN.setVisibility(View.VISIBLE);
    }

    private void disableAlarmButton() {
        alarmBTN.setEnabled(false);
        alarmBTN.setBackground(getResources().getDrawable(R.drawable.rounded_outline_button_disabled));
        alarmBTN.setTextColor(getResources().getColor(R.color.colorPrimaryLight));
    }

    private void enableAlarmButton() {
        alarmBTN.setEnabled(true);
        alarmBTN.setBackground(getResources().getDrawable(R.drawable.rounded_outline_button));
        alarmBTN.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    private void setIsConnectedUI() {
        setStateAsConnected();
        showUI();
        showDisconnectButton();
    }

    private void setIsConnectingUI() {
        setStateAsConnecting();
        hideUI();
        showConnectingProgress();
    }

    private void setIsDisconnectedUI() {
        setStateAsDisconnected();
        hideUI();
        showConnectButton();
    }

    @Override
    public void onConnect() {
        setIsConnectedUI();
    }

    @Override
    public void onConnecting() {
        setIsConnectingUI();
    }

    @Override
    public void onDisconnect() {
        setIsDisconnectedUI();
    }
}
