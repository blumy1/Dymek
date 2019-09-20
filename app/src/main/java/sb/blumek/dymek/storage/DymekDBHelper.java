package sb.blumek.dymek.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static sb.blumek.dymek.storage.DymekContract.*;

public class DymekDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Dymek.db";

    public DymekDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private static final String SQL_CREATE_ENTRIES = DeviceSchema.SQL_CREATE_ENTRY +
            TemperaturesProfileSchema.SQL_CREATE_ENTRY +
            TemperatureCacheSchema.SQL_CREATE_ENTRY;

    private static final String SQL_DELETE_ENTRIES = DeviceSchema.SQL_DELETE_ENTRY +
            TemperaturesProfileSchema.SQL_DELETE_ENTRY +
            TemperatureCacheSchema.SQL_DELETE_ENTRY;
}
