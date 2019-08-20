package sb.blumek.thermometer_controller_app.utils;

import android.content.SharedPreferences;

public class StorageManager {
    public static final String FILE_NAME = "Preferences";
    public static final String MAC_ADDRESS = "MAC_ADDRESS";
    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String TEMP_1_NAME = "TEMP_1_NAME";
    public static final String TEMP_2_NAME = "TEMP_2_NAME";
    public static final String TEMP_1_MIN = "TEMP_1_MIN";
    public static final String TEMP_1_MAX = "TEMP_1_MAX";
    public static final String TEMP_2_MIN = "TEMP_2_MIN";
    public static final String TEMP_2_MAX = "TEMP_2_MAX";

    private SharedPreferences sharedPreferences;

    public StorageManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void saveString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void removePref(String key) {
        if (!contains(key))
            return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }
}