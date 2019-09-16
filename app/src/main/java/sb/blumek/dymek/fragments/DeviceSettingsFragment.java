package sb.blumek.dymek.fragments;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sb.blumek.dymek.R;
import sb.blumek.dymek.activities.MainActivity;

public class DeviceSettingsFragment extends Fragment {
    public final static String TAG = DeviceSettingsFragment.class.getSimpleName();

    public DeviceSettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_settings, container, false);
    }

}
