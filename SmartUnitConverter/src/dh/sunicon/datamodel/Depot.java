package dh.sunicon.datamodel;

import java.text.ParseException;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Depot {

	private static final String GET_QUERY = "SELECT * FROM depot WHERE key=?";

	public static Long getLong(DatabaseHelper dbHelper, String key) {
		Cursor cur = dbHelper.getReadableDatabase().rawQuery(GET_QUERY, new String[] { key });
		try {
			if (cur.moveToNext())
				return cur.getLong(cur.getColumnIndex("i"));
			return null;
		} finally {
			cur.close();
		}
	}
	
	public static Integer getInt(DatabaseHelper dbHelper, String key) {
		Cursor cur = dbHelper.getReadableDatabase().rawQuery(GET_QUERY, new String[] { key });
		try {
			if (cur.moveToNext())
				return cur.getInt(cur.getColumnIndex("i"));
			return null;
		} finally {
			cur.close();
		}
	}
	
	public static Double getDouble(DatabaseHelper dbHelper, String key) {
		Cursor cur = dbHelper.getReadableDatabase().rawQuery(GET_QUERY, new String[] { key });
		try {
			if (cur.moveToNext())
				return cur.getDouble(cur.getColumnIndex("f"));
			return null;
		} finally {
			cur.close();
		}
	}
	
	public static Date getDateTime(DatabaseHelper dbHelper, String key) throws ParseException {
		Cursor cur = dbHelper.getReadableDatabase().rawQuery(GET_QUERY, new String[] { key });
		try {
			if (cur.moveToNext())
				return DatabaseHelper.DateFormat.parse(cur.getString(cur.getColumnIndex("d")));
			return null;
		} finally {
			cur.close();
		}
	}
	
	public static String getString(DatabaseHelper dbHelper, String key) {
		Cursor cur = dbHelper.getReadableDatabase().rawQuery(GET_QUERY, new String[] { key });
		try {
			if (cur.moveToNext())
				return cur.getString(cur.getColumnIndex("s"));
			return null;
		} finally {
			cur.close();
		}
	}
	
	public static void putLong(DatabaseHelper dbHelper, String key, long value) {
		ContentValues cv = new ContentValues(1);
		cv.put("key", key);
		cv.put("i", value);
		dbHelper.getWritableDatabase().insertWithOnConflict("depot", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public static void putInt(DatabaseHelper dbHelper, String key, int value) {
		ContentValues cv = new ContentValues(1);
		cv.put("key", key);
		cv.put("i", value);
		dbHelper.getWritableDatabase().insertWithOnConflict("depot", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public static void putDouble(DatabaseHelper dbHelper, String key, double value) {
		ContentValues cv = new ContentValues(1);
		cv.put("key", key);
		cv.put("f", value);
		dbHelper.getWritableDatabase().insertWithOnConflict("depot", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public static void putDateTime(DatabaseHelper dbHelper, String key, Date value) {
		ContentValues cv = new ContentValues(1);
		cv.put("key", key);
		cv.put("d", DatabaseHelper.DateFormat.format(value));
		dbHelper.getWritableDatabase().insertWithOnConflict("depot", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public static void putString(DatabaseHelper dbHelper, String key, String value) {
		ContentValues cv = new ContentValues(1);
		cv.put("key", key);
		cv.put("s", DatabaseHelper.DateFormat.format(value));
		dbHelper.getWritableDatabase().insertWithOnConflict("depot", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}
}
