package sb.blumek.dymek.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import sb.blumek.dymek.R;
import sb.blumek.dymek.activities.MainActivity;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.observables.Observer;
import sb.blumek.dymek.services.TemperatureService;
import sb.blumek.dymek.shared.Commands;
import sb.blumek.dymek.shared.Temperature;
import sb.blumek.dymek.storage.DeviceStorage;
import sb.blumek.dymek.validators.TempMaxValueValidator;
import sb.blumek.dymek.validators.TempMinValueValidator;
import sb.blumek.dymek.validators.TempNameValidator;
import sb.blumek.dymek.validators.Validator;

public class DeviceSettingsFragment extends Fragment implements ServiceConnection, Observer {
    public final static String TAG = DeviceSettingsFragment.class.getSimpleName();

    private TemperatureService service;

    private Button sendSettingsButton;
    private EditText temp1MinET;
    private EditText temp1MaxET;
    private EditText temp2MinET;
    private EditText temp2MaxET;
    private EditText temp1NameET;
    private EditText temp2NameET;
    private LinearLayout disconnectButton;

    public DeviceSettingsFragment() {
    }

    @Override
    public void update(Observable observable) {
        if (observable instanceof TemperatureService) {
            TemperatureService service = (TemperatureService) observable;

            Temperature temp1 = service.getFirstTemperature();
            Temperature temp2 = service.getSecondTemperature();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((TemperatureService.ServiceBinder) binder).getService();
        service.registerObserver(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service.unregisterObserver(this);
        service = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    public void onStart() {
        super.onStart();
        showBackButton();
        if(service == null)
            getActivity().startService(new Intent(getActivity(), TemperatureService.class));
    }

    @Override
    public void onStop() {
        super.onStop();
        hideBackButton();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sendSettingsButton = view.findViewById(R.id.set_btn);
        sendSettingsButton.setOnClickListener(button -> sendSettings());

        disconnectButton = view.findViewById(R.id.disconnect_ll);
        disconnectButton.setOnClickListener(button -> confirmDeviceDisconnect());

        temp1MinET = view.findViewById(R.id.temp1Min_et);
        temp1MaxET = view.findViewById(R.id.temp1Max_et);
        temp2MinET = view.findViewById(R.id.temp2Min_et);
        temp2MaxET = view.findViewById(R.id.temp2Max_et);
        temp1NameET = view.findViewById(R.id.temp1Name_et);
        temp2NameET = view.findViewById(R.id.temp2Name_et);
    }

    public void confirmDeviceDisconnect() {
        new AlertDialog.Builder(disconnectButton.getContext(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.you_re_trying_to_disconnect_device)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    disconnectDevice();
                    disconnectFromService();
                    clearBackStack();
                    openScanDevices();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {

                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void clearBackStack() {
        ((MainActivity) getActivity()).clearBackStack();
    }

    private void disconnectFromService() {
        if (service != null)
            service.disconnect();
    }

    private void openScanDevices() {
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.fragment, new ScanDevicesFragment(), ScanDevicesFragment.TAG)
                .commit();
    }

    private void disconnectDevice() {
        DeviceStorage deviceStorage = new DeviceStorage(getContext());
        deviceStorage.deleteDevice();
    }

    private void sendSettings() {
        new Thread(() -> {
            int delay = 500;
            try {
                send("[]");
                Thread.sleep(delay);
                sendTempName(temp1NameET.getText().toString(), Commands.SET_TEMP_1_NAME);
                Thread.sleep(delay);
                sendTempMinValue(Double.valueOf(temp1MinET.getText().toString()), Commands.SET_TEMP_1_MIN);
                Thread.sleep(delay);
                sendTempMaxValue(Double.valueOf(temp1MaxET.getText().toString()), Commands.SET_TEMP_1_MAX);
                Thread.sleep(delay);

                sendTempName(temp2NameET.getText().toString(), Commands.SET_TEMP_2_NAME);
                Thread.sleep(delay);
                sendTempMinValue(Double.valueOf(temp2MinET.getText().toString()), Commands.SET_TEMP_2_MIN);
                Thread.sleep(delay);
                sendTempMaxValue(Double.valueOf(temp2MaxET.getText().toString()), Commands.SET_TEMP_2_MAX);
                send("[]");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void sendTempName(String tempName, String template) {
        Validator validator = new TempNameValidator(tempName);
        String command = interpolate(template, tempName);
        sendValue(command, validator);
    }

    private void sendTempMinValue(double tempMin, String template) {
        Validator validator = new TempMinValueValidator(tempMin);
        String command = interpolate(template, String.valueOf(tempMin));
        sendValue(command, validator);
    }

    private void sendTempMaxValue(double tempMax, String template) {
        Validator validator = new TempMaxValueValidator(tempMax);
        String command = interpolate(template, String.valueOf(tempMax));
        sendValue(command, validator);
    }

    private void sendValue(String command, Validator validator) {
        if (validator.isValid()) {
            send(command);
        }
    }

    private String interpolate(String template, String value) {
        return String.format(template, value);
    }

    private void send(String message) {
        if (isServiceConnected())
            service.send(message);
    }

    private boolean isServiceConnected() {
        return service != null && service.isConnected();
    }

    private void showBackButton() {
        ActionBar actionBar = getActionBar();
        if (!isAvailableActionBar(actionBar))
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(getResources()
                .getDrawable(R.drawable.ic_arrow_back_black));
    }

    private boolean isAvailableActionBar(ActionBar actionBar) {
        return actionBar != null;
    }

    private ActionBar getActionBar() {
        if (getActivity() == null)
            return null;
        return ((MainActivity) getActivity()).getSupportActionBar();
    }

    private void hideBackButton() {
        ActionBar actionBar = getActionBar();
        if (!isAvailableActionBar(actionBar))
            return;

        getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().getSupportFragmentManager().popBackStack();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
