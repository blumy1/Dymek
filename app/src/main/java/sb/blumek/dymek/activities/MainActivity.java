package sb.blumek.dymek.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import sb.blumek.dymek.R;
import sb.blumek.dymek.fragments.DeviceControllerFragment;
import sb.blumek.dymek.fragments.ScanDevicesFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment, new ScanDevicesFragment(), ScanDevicesFragment.TAG)
                .commit();
    }
}
