package dh.sunicon.datamodel;

import java.util.HashMap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Category extends BaseEntity {

	private String name;

	public Category(DatabaseHelper dbHelper, long id, String name) {
		super(dbHelper, id);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static Category findById(DatabaseHelper dbHelper, long id) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cur = db.query("category", null, "id=?",
				new String[] { Long.toString(id) }, null, null, null, null);

		Category resu = null;

		if (cur.moveToNext()) {
			resu = new Category(dbHelper,
						cur.getLong(cur.getColumnIndex("id")),
						cur.getString(cur.getColumnIndex("name"))
					);
		}

		cur.close();
		return resu;
	}

	public HashMap<Long, Unit> getUnits() {
		HashMap<Long, Unit> resu = new HashMap<Long, Unit>();

		SQLiteDatabase db = this.getDbHelper().getReadableDatabase();
		Cursor cur = db.query("unit", null, "categoryId=?",
						new String[] { Long.toString(this.getId()) }, null, null,
						null, null);
		
		while (cur.moveToNext()) {
			Unit u = Unit.parseCursor(this.getDbHelper(), cur);
			resu.put(u.getId(), u);
		}

		cur.close();
		
		return resu;
	}
}
