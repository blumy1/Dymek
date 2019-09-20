package sb.blumek.dymek.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import sb.blumek.dymek.domain.Device;

import static sb.blumek.dymek.storage.DymekContract.*;

public class DymekDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Dymek.db";

    public DymekDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DeviceSchema.SQL_CREATE_ENTRY);
        db.execSQL(TemperaturesProfileSchema.SQL_CREATE_ENTRY);
        db.execSQL(TemperatureCacheSchema.SQL_CREATE_ENTRY);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DeviceSchema.SQL_DELETE_ENTRY);
        db.execSQL(TemperaturesProfileSchema.SQL_DELETE_ENTRY);
        db.execSQL(TemperatureCacheSchema.SQL_DELETE_ENTRY);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
