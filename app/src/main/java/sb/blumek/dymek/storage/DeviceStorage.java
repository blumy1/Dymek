package sb.blumek.dymek.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import sb.blumek.dymek.domain.Device;

import static sb.blumek.dymek.storage.DymekContract.DeviceSchema;

public class DeviceStorage {
    private DymekDBHelper dbHelper;

    public DeviceStorage(Context context) {
        this.dbHelper = new DymekDBHelper(context);
    }

    public void saveDevice(Device device) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DeviceSchema.COLUMN_NAME_DEVICE_NAME, device.getName());
        values.put(DeviceSchema.COLUMN_NAME_DEVICE_ADDRESS, device.getAddress());

        db.insert(DeviceSchema.TABLE_NAME, null, values);
    }

    public Device getDevice() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(DeviceSchema.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DeviceSchema.COLUMN_NAME_DEVICE_NAME));
            String address = cursor.getString(cursor.getColumnIndexOrThrow(DeviceSchema.COLUMN_NAME_DEVICE_ADDRESS));
            Device device = new Device(name, address);
            cursor.close();

            return device;
        }

        return null;
    }

    public void deleteDevice() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        db.delete(DeviceSchema.TABLE_NAME, null, null);
    }
}
