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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import sb.blumek.dymek.R;
import sb.blumek.dymek.activities.MainActivity;
import sb.blumek.dymek.services.TemperatureService;
import sb.blumek.dymek.shared.Commands;
import sb.blumek.dymek.shared.Temperature;
import sb.blumek.dymek.storage.DeviceStorage;
import sb.blumek.dymek.storage.TemperatureCache;
import sb.blumek.dymek.validators.TempRangeValidator;
import sb.blumek.dymek.validators.TempNameValidator;
import sb.blumek.dymek.validators.Validator;

public class DeviceSettingsFragment extends Fragment implements ServiceConnection {
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
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((TemperatureService.ServiceBinder) binder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
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
        resetAllStatus();

        TemperatureCache temperatureCache = new TemperatureCache(getContext());
        Temperature firsTemperature = temperatureCache.getFirstTemperature();
        Temperature secondTemperature = temperatureCache.getSecondTemperature();

        setTempName(firsTemperature.getName(), temp1NameET);
        setTempValue(firsTemperature.getTempMin(), temp1MinET);
        setTempValue(firsTemperature.getTempMax(), temp1MaxET);

        setTempName(secondTemperature.getName(), temp2NameET);
        setTempValue(secondTemperature.getTempMin(), temp2MinET);
        setTempValue(secondTemperature.getTempMax(), temp2MaxET);

        if(service == null)
            getActivity().startService(new Intent(getActivity(), TemperatureService.class));
    }

    private void setTempName(String name, EditText editText) {
        if (name != null) {
            editText.setText(name);
        }
    }

    private void setTempValue(Double value, EditText editText) {
        if (value != null) {
            editText.setText(String.valueOf(value.intValue()));
        }
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
        sendSettingsButton.setOnClickListener(button -> {
            if (service != null && service.isConnected()) {
                sendSettings();
            } else
                Toast.makeText(getContext(), "Brak połączenia", Toast.LENGTH_SHORT).show();
        });

        disconnectButton = view.findViewById(R.id.disconnect_ll);
        disconnectButton.setOnClickListener(button -> {
            if (service != null)
                confirmDeviceDisconnect();
        });

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
        resetAllStatus();

        TemperatureCache temperatureCache = new TemperatureCache(getContext());
        Temperature firstTemp = temperatureCache.getFirstTemperature();
        Temperature secondTemp = temperatureCache.getSecondTemperature();

        new Thread(() -> {
            int delay = 500;
            boolean anythingSent = false;
            try {
                send("[]");

                if (!temp1NameET.getText().toString().isEmpty() && !temp1NameET.getText().toString().equals(firstTemp.getName())) {
                    Thread.sleep(delay);
                    sendTempName(temp1NameET, Commands.SET_TEMP_1_NAME);
                    anythingSent = true;
                }

                if (!temp1MinET.getText().toString().isEmpty() && !Double.valueOf(temp1MinET.getText().toString()).equals(firstTemp.getTempMin())) {
                    Thread.sleep(delay);
                    sendTempRangeValue(temp1MinET, Commands.SET_TEMP_1_MIN);
                    anythingSent = true;
                }

                if (!temp1MaxET.getText().toString().isEmpty() && !Double.valueOf(temp1MaxET.getText().toString()).equals(firstTemp.getTempMax())) {
                    Thread.sleep(delay);
                    sendTempRangeValue(temp1MaxET, Commands.SET_TEMP_1_MAX);
                    anythingSent = true;
                }

                if (!temp2NameET.getText().toString().isEmpty() && !temp2NameET.getText().toString().equals(secondTemp.getName())) {
                    Thread.sleep(delay);
                    sendTempName(temp2NameET, Commands.SET_TEMP_2_NAME);
                    anythingSent = true;
                }

                if (!temp2MinET.getText().toString().isEmpty() && !Double.valueOf(temp2MinET.getText().toString()).equals(secondTemp.getTempMin())) {
                    Thread.sleep(delay);
                    sendTempRangeValue(temp2MinET, Commands.SET_TEMP_2_MIN);
                    anythingSent = true;
                }

                if (!temp2MaxET.getText().toString().isEmpty() && !Double.valueOf(temp2MaxET.getText().toString()).equals(secondTemp.getTempMax())) {
                    Thread.sleep(delay);
                    sendTempRangeValue(temp2MaxET, Commands.SET_TEMP_2_MAX);
                    anythingSent = true;
                }

                Thread.sleep(delay);
                send("[]");

                if (anythingSent)
                    service.sendSettingsRequest();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void sendTempName(EditText editText, String template) {
        String name = editText.getText().toString();
        Validator validator = new TempNameValidator(name);
        sendValue(editText, template, validator);
    }

    private void sendTempRangeValue(EditText editText, String template) {
        double minTemp = Double.parseDouble(editText.getText().toString());
        Validator validator = new TempRangeValidator(minTemp);
        sendValue(editText, template, validator);
    }

    private void sendValue(EditText editText, String template, Validator validator) {
        if (validator.isValid() && !editText.toString().isEmpty()) {
            String value = editText.getText().toString();
            String command = interpolate(template, value);
            send(command);
            getActivity().runOnUiThread(() -> showAsCorrect(editText));
        } else
            getActivity().runOnUiThread(() -> showAsIncorrect(editText));
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

    public void showAsCorrect(EditText editText) {
        editText.setBackground(getResources().getDrawable(R.drawable.rounded_dark_grey_text_success_bg));
    }

    public void showAsIncorrect(EditText editText) {
        editText.setBackground(getResources().getDrawable(R.drawable.rounded_dark_grey_text_error_bg));
    }

    public void showAsDefault(EditText editText) {
        editText.setBackground(getResources().getDrawable(R.drawable.rounded_dark_grey_bg));
    }

    public void resetAllStatus() {
        showAsDefault(temp1NameET);
        showAsDefault(temp1MinET);
        showAsDefault(temp1MaxET);

        showAsDefault(temp2NameET);
        showAsDefault(temp2MinET);
        showAsDefault(temp2MaxET);
    }
}
