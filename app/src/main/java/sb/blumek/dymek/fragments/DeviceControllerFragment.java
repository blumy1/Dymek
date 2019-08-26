package sb.blumek.dymek.fragments;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import sb.blumek.dymek.R;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.observables.Observer;
import sb.blumek.dymek.services.BluetoothService;
import sb.blumek.dymek.shared.Temperature;

public class DeviceControllerFragment extends Fragment implements ServiceConnection, Observer {
    public final static String TAG = ScanDevicesFragment.class.getSimpleName();

    private String deviceAddress;

    private TextView temp1TV;
    private TextView temp2TV;
    private TextView temp1NameTV;
    private TextView temp2NameTV;
    private View separatorV;
    private Button alarmBTN;
    private TextView connectionStateTV;

    private BluetoothService service;
    private boolean initialStart = true;

    public DeviceControllerFragment(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void update(Observable observable) {
        if (observable instanceof BluetoothService) {
            BluetoothService service = (BluetoothService) observable;

            Temperature temp1 = service.getTemperature1();
            Temperature temp2 = service.getTemperature2();

            temp1NameTV.setText(temp1.getName());
            temp1TV.setText(String.valueOf(temp1.getTemp()));

            temp2NameTV.setText(temp2.getName());
            temp2TV.setText(String.valueOf(temp2.getTemp()));

            setUpUI();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (service != null && !service.isDisconnected())
            disconnect();
        getActivity().stopService(new Intent(getActivity(), BluetoothService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service == null)
            getActivity().startService(new Intent(getActivity(), BluetoothService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
//        if(service != null && !getActivity().isChangingConfigurations())
//            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), BluetoothService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service !=null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((BluetoothService.ServiceBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            service.registerObserver(this);
            service.setDeviceAddress(deviceAddress);
            getActivity().runOnUiThread(this::connect);
            setUpUI();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.controller_menu, menu);
        if (service != null &&
                service.isConnected()) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void disconnect() {
        service.disconnect();
        setUpUI();
        refreshActionBarMenu();
    }

    private void refreshActionBarMenu() {
        if (getActivity() != null)
            getActivity().invalidateOptionsMenu();
    }

    private void send(String message) {
        service.send(message);
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

    public void disableAlarmButton() {
        alarmBTN.setEnabled(false);
        alarmBTN.setBackground(getResources().getDrawable(R.drawable.rounded_outline_button_disabled));
        alarmBTN.setTextColor(getResources().getColor(R.color.colorPrimaryLight));
    }

    public void enableAlarmButton() {
        alarmBTN.setEnabled(true);
        alarmBTN.setBackground(getResources().getDrawable(R.drawable.rounded_outline_button));
        alarmBTN.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    private void setUpUI() {
        refreshActionBarMenu();
        if (service.isConnected()) {
            showUI();
            connectionStateTV.setText(R.string.connected);
        } else if (service.isDisconnected()) {
            hideUI();
            connectionStateTV.setText(R.string.disconnected);
        }
    }
}
