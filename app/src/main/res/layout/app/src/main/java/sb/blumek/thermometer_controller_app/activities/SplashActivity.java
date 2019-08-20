package sb.blumek.thermometer_controller_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import sb.blumek.thermometer_controller_app.services.BluetoothLeService;
import sb.blumek.thermometer_controller_app.utils.StorageManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, BluetoothLeService.class));
        StorageManager storageManager = new StorageManager(getApplicationContext().getSharedPreferences(StorageManager.FILE_NAME, 0));
        Intent intent;
        if (storageManager.contains(StorageManager.MAC_ADDRESS)) {
            intent = new Intent(this, DeviceControllerActivity.class);
            intent.putExtra(DeviceControllerActivity.EXTRAS_DEVICE_NAME, storageManager.getString(StorageManager.DEVICE_NAME));
            intent.putExtra(DeviceControllerActivity.EXTRAS_DEVICE_ADDRESS, storageManager.getString(StorageManager.MAC_ADDRESS));
        } else {
            intent = new Intent(this, DeviceScanActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
