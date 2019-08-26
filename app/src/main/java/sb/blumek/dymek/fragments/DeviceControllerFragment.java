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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import sb.blumek.dymek.R;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.observables.Observer;
import sb.blumek.dymek.services.BluetoothService;
import sb.blumek.dymek.shared.Temperature;

public class DeviceControllerFragment extends Fragment implements ServiceConnection, Observer {
    public final static String TAG = ScanDevicesFragment.class.getSimpleName();

    private String deviceName;
    private String deviceAddress;

    private TextView temp1TV;
    private TextView temp2TV;
    private TextView temp1NameTV;
    private TextView temp2NameTV;
    private View separatorV;

    private BluetoothService service;
    private boolean initialStart = true;

    public DeviceControllerFragment(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.controller_menu, menu);
    }

    private void connect() {
        service.connect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    private void disconnect() {
        service.disconnect();
    }

    private void send(String str) {
        service.send(str);
    }
}
