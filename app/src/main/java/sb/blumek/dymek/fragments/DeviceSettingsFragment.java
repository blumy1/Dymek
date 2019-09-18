package sb.blumek.dymek.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import sb.blumek.dymek.R;
import sb.blumek.dymek.activities.MainActivity;
import sb.blumek.dymek.observables.Observable;
import sb.blumek.dymek.observables.Observer;
import sb.blumek.dymek.services.TemperatureService;
import sb.blumek.dymek.shared.Commands;
import sb.blumek.dymek.shared.Temperature;

public class DeviceSettingsFragment extends Fragment implements ServiceConnection, Observer {
    public final static String TAG = DeviceSettingsFragment.class.getSimpleName();

    private TemperatureService service;

    private Button setBTN;
    private EditText temp1MinET;
    private EditText temp1MaxET;
    private EditText temp2MinET;
    private EditText temp2MaxET;
    private EditText temp1NameET;
    private EditText temp2NameET;
    private LinearLayout disconnectLL;

    public DeviceSettingsFragment() {
    }

    @Override
    public void update(Observable observable) {
        if (observable instanceof TemperatureService) {
            TemperatureService service = (TemperatureService) observable;

            Temperature temp1 = service.getTemperature1();
            Temperature temp2 = service.getTemperature2();
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
        if(service == null)
            getActivity().startService(new Intent(getActivity(), TemperatureService.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        showBackButton();
    }

    @Override
    public void onPause() {
        super.onPause();
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

        setBTN = view.findViewById(R.id.set_btn);
        setBTN.setOnClickListener(view12 -> sendSettings());

        disconnectLL = view.findViewById(R.id.disconnect_ll);
        disconnectLL.setOnClickListener(view1 ->
                new AlertDialog.Builder(view1.getContext(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Czy na pewno?")
                .setMessage("Próbujesz odłączyć urządzenie od aplikacji. Czy chcesz to na pewno zrobić?")
                .setPositiveButton("Tak", (dialog, which) -> {

                })
                .setNegativeButton("Nie", (dialog, which) -> {

                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show());

        temp1MinET = view.findViewById(R.id.temp1Min_et);
        temp1MaxET = view.findViewById(R.id.temp1Max_et);
        temp2MinET = view.findViewById(R.id.temp2Min_et);
        temp2MaxET = view.findViewById(R.id.temp2Max_et);
        temp1NameET = view.findViewById(R.id.temp1Name_et);
        temp2NameET = view.findViewById(R.id.temp2Name_et);
    }

    private void sendSettings() {
        service.send(String.format(Commands.SET_TEMP_2_NAME, temp2NameET.getText().toString()));
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
