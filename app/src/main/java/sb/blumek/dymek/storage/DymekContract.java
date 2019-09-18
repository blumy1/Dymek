package sb.blumek.dymek.storage;

import android.provider.BaseColumns;

public final class DymekContract {
    private DymekContract() {}

    static class DeviceSchema implements BaseColumns {
        static final String TABLE_NAME = "device";
        static final String COLUMN_NAME_DEVICE_NAME = "name";
        static final String COLUMN_NAME_DEVICE_ADDRESS = "address";

        static final String SQL_CREATE_ENTRY =
                "CREATE TABLE " + DeviceSchema.TABLE_NAME + " (" +
                        DeviceSchema._ID + " INTEGER PRIMARY KEY," +
                        DeviceSchema.COLUMN_NAME_DEVICE_NAME + " TEXT," +
                        DeviceSchema.COLUMN_NAME_DEVICE_ADDRESS + " TEXT);";

        static final String SQL_DELETE_ENTRY =
                "DROP TABLE IF EXISTS " + DeviceSchema.TABLE_NAME + ";";
    }

    static class TemperaturesProfileSchema implements BaseColumns {
        static final String TABLE_NAME = "temperatures_profile";
        static final String COLUMN_NAME_TEMP_1_NAME = "temp_1_name";
        static final String COLUMN_NAME_TEMP_1_MIN_VAL = "temp_1_min_val";
        static final String COLUMN_NAME_TEMP_1_MAX_VAL = "temp_1_max_val";
        static final String COLUMN_NAME_TEMP_2_NAME = "temp_2_name";
        static final String COLUMN_NAME_TEMP_2_MIN_VAL = "temp_2_min_val";
        static final String COLUMN_NAME_TEMP_2_MAX_VAL = "temp_2_max_val";

        static final String SQL_CREATE_ENTRY =
                "CREATE TABLE " + TemperaturesProfileSchema.TABLE_NAME + " (" +
                        TemperaturesProfileSchema._ID + " INTEGER PRIMARY KEY," +
                        TemperaturesProfileSchema.COLUMN_NAME_TEMP_1_NAME + " TEXT," +
                        TemperaturesProfileSchema.COLUMN_NAME_TEMP_1_MIN_VAL + "REAL," +
                        TemperaturesProfileSchema.COLUMN_NAME_TEMP_1_MAX_VAL + "REAL," +
                        TemperaturesProfileSchema.COLUMN_NAME_TEMP_2_NAME + " TEXT," +
                        TemperaturesProfileSchema.COLUMN_NAME_TEMP_2_MIN_VAL + "REAL," +
                        TemperaturesProfileSchema.COLUMN_NAME_TEMP_2_MAX_VAL + "REAL);";

        static final String SQL_DELETE_ENTRY =
                "DROP TABLE IF EXISTS " + TemperaturesProfileSchema.TABLE_NAME + ";";
    }
}