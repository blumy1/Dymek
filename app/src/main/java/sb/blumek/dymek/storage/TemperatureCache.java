package sb.blumek.dymek.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import sb.blumek.dymek.shared.Temperature;

import static sb.blumek.dymek.storage.DymekContract.TemperatureCacheSchema;

public class TemperatureCache {

    private DymekDBHelper dbHelper;

    public TemperatureCache(Context context) {
        this.dbHelper = new DymekDBHelper(context);
    }

    public void saveTemperatures(Temperature firstTemperature, Temperature secondTemperature) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(TemperatureCacheSchema.COLUMN_NAME_TEMP_1_NAME, firstTemperature.getName());
        values.put(TemperatureCacheSchema.COLUMN_NAME_TEMP_1_MIN_VAL, firstTemperature.getTempMin());
        values.put(TemperatureCacheSchema.COLUMN_NAME_TEMP_1_MAX_VAL, firstTemperature.getTempMax());

        values.put(TemperatureCacheSchema.COLUMN_NAME_TEMP_2_NAME, secondTemperature.getName());
        values.put(TemperatureCacheSchema.COLUMN_NAME_TEMP_2_MIN_VAL, secondTemperature.getTempMin());
        values.put(TemperatureCacheSchema.COLUMN_NAME_TEMP_2_MAX_VAL, secondTemperature.getTempMax());

        db.insert(TemperatureCacheSchema.TABLE_NAME, null, values);
    }

    public Temperature getFirstTemperature() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(TemperatureCacheSchema.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TemperatureCacheSchema.COLUMN_NAME_TEMP_1_NAME));
            double tempMin = cursor.getDouble(cursor.getColumnIndexOrThrow(TemperatureCacheSchema.COLUMN_NAME_TEMP_1_MIN_VAL));
            double tempMax = cursor.getDouble(cursor.getColumnIndexOrThrow(TemperatureCacheSchema.COLUMN_NAME_TEMP_1_MAX_VAL));
            Temperature temperature = new Temperature();
            temperature.setName(name);
            temperature.setTempMin(tempMin);
            temperature.setTempMax(tempMax);
            cursor.close();

            return temperature;
        }

        return null;
    }

    public Temperature getSecondTemperature() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(TemperatureCacheSchema.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TemperatureCacheSchema.COLUMN_NAME_TEMP_2_NAME));
            double tempMin = cursor.getDouble(cursor.getColumnIndexOrThrow(TemperatureCacheSchema.COLUMN_NAME_TEMP_2_MIN_VAL));
            double tempMax = cursor.getDouble(cursor.getColumnIndexOrThrow(TemperatureCacheSchema.COLUMN_NAME_TEMP_2_MAX_VAL));
            Temperature temperature = new Temperature();
            temperature.setName(name);
            temperature.setTempMin(tempMin);
            temperature.setTempMax(tempMax);
            cursor.close();

            return temperature;
        }

        return null;
    }

    public void deleteTemperatures() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        db.delete(TemperatureCacheSchema.TABLE_NAME, null, null);
    }
}
