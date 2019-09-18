package sb.blumek.dymek.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import sb.blumek.dymek.R;
import sb.blumek.dymek.domain.Device;
import sb.blumek.dymek.fragments.DeviceControllerFragment;
import sb.blumek.dymek.fragments.ScanDevicesFragment;
import sb.blumek.dymek.storage.DeviceStorage;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        Device device = getSavedDevice();

        Fragment fragment = createFragment(device);
        openFragment(fragment);
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragment, fragment, fragment.getTag())
                .commit();
    }

    public void clearBackStack() {
        while (getSupportFragmentManager().getBackStackEntryCount() > 0){
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    private Device getSavedDevice() {
        DeviceStorage deviceStorage = new DeviceStorage(getApplicationContext());
        return deviceStorage.getDevice();
    }

    Fragment createFragment(Device device) {
        Fragment fragment;
        if (device == null)
            fragment = new ScanDevicesFragment();
        else
            fragment = new DeviceControllerFragment(device.getAddress());

        return fragment;
    }
}
