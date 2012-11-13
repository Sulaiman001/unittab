package dh.sunicon.datamodel;

import java.security.InvalidParameterException;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class Unit extends BaseEntity
{
	public static final String TAG = Unit.class.getName();
	public final static long USD_UNIT = 1413;
	
	
	private long categoryId;
	private String name;
	private String shortName; // optional
	private HashMap<Long, Conversion> conversions;
	
	public Unit(DatabaseHelper dbHelper, long categoryId, long id, String name,
			String shortName)
	{
		super(dbHelper, id);
		this.categoryId = categoryId;
		this.name = name;
		this.shortName = shortName;
	}

	public String getShortName()
	{
		return shortName;
	}

	public long getCategoryId()
	{
		return categoryId;
	}

	public String getName()
	{
		return name;
	}

	public static Unit findById(DatabaseHelper dbHelper, long id)
	{
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cur = db.query("unit", null, "id=?",
				new String[] { Long.toString(id) }, null, null, null, null);
		Unit resu = null;
		try
		{
			if (cur.moveToNext())
				resu = parseCursor(dbHelper, cur);
		}
		catch (Exception ex) {
			Log.wtf(TAG, ex);
			return null;
		}
		finally {
			cur.close();
		}
		return resu;
	}

	public static Unit parseCursor(DatabaseHelper dbHelper, Cursor cur)
	{
		return new Unit(dbHelper,
				cur.getLong(cur.getColumnIndex("categoryId")), cur.getLong(cur
						.getColumnIndex("id")), cur.getString(cur
						.getColumnIndex("name")), cur.getString(cur
						.getColumnIndex("shortName")));
	}

	public Category getCategory()
	{
		return Category.findById(this.getDbHelper(), this.getCategoryId());
	}

	/**
	 * Get conversions relate to this unit (regardless it is base or target in
	 * the conversion)
	 */
	public HashMap<Long, Conversion> getConversions()
	{
		if (this.conversions != null)
		{
			return this.conversions;
		}
		
		this.conversions = new HashMap<Long, Conversion>();

		SQLiteDatabase db = this.getDbHelper().getReadableDatabase();
		String unitIdStr = Long.toString(this.getId());
		Cursor cur = db.query("conversion", null, "base=? OR target=?",
				new String[] { unitIdStr, unitIdStr }, null, null, null, null);
		try
		{
			while (cur.moveToNext())
			{
				Conversion u = Conversion.parseCursor(this.getDbHelper(), cur);
				this.conversions.put(u.getId(), u);
			}
		}
		finally {
			cur.close();
		}

		return this.conversions;
	}

	public Conversion getConversion(long otherUnitId)
	{
		Conversion resu = null;

		SQLiteDatabase db = this.getDbHelper().getReadableDatabase();
		
		String unitIdStr = Long.toString(this.getId());
		String otherUnitIdStr = Long.toString(otherUnitId);

		Cursor cur = db.query("conversion", null,
				"(base=? AND target=?) OR (base=? AND target=?)", new String[] {
						unitIdStr, otherUnitIdStr, otherUnitIdStr, unitIdStr },
				null, null, null, null);
		try
		{
			if (cur.moveToNext())
			{
				resu = Conversion.parseCursor(this.getDbHelper(), cur);
			}
		}
		finally
		{
			cur.close();
		}
		return resu;
	}
	
	public boolean insertOrUpdateCurrency(String targetCurrencyShortName, double rate)
	{
		if (categoryId!=Category.CURRENCY_CATEGORY) {
			throw new UnsupportedOperationException();
		}
		
		if (rate<=0) {
			throw new InvalidParameterException("negative or 0 rate");
		}
		
		if (TextUtils.isEmpty(targetCurrencyShortName)) {
			throw new InvalidParameterException("invalide currency code null");
		}
		
		//get targetCurrency
		Unit targetCurrency = null;
		{
			Cursor cur = getDbHelper().getReadableDatabase().query("unit", null,
					"categoryId=11 AND shortName=?", new String[] {
						targetCurrencyShortName },
					null, null, null, null);
			try
			{
				if (cur.moveToNext())
				{
					targetCurrency = Unit.parseCursor(getDbHelper(), cur);
				}
			}
			finally
			{
				cur.close();
			}
		}
		
		if (targetCurrency == null)
			return false;
		
		//check if the conversion exits
		boolean conversionExist = false;
		{
			Cursor cur = getDbHelper().getReadableDatabase().query("conversion", null,
					"base=? AND target=?", 
					new String[] {Long.toString(getId()), Long.toString(targetCurrency.getId()) },
					null, null, null, null);
			try {
				conversionExist = cur.getCount()>0;
			}
			finally
			{
				cur.close();
			}
		}
		
		//make insert or update
		if (conversionExist) { //make update
			ContentValues contentValues = new ContentValues();
			contentValues.put("fx", rate);
			getDbHelper().getWritableDatabase().update("conversion", contentValues, "base=? and target=?", 
					new String[] {Long.toString(getId()), Long.toString(targetCurrency.getId()) });
		}
		else { //make insert
			ContentValues contentValues = new ContentValues();
			contentValues.put("fx", rate);
			contentValues.put("base", getId());
			contentValues.put("target", targetCurrency.getId());
			getDbHelper().getWritableDatabase().insert("conversion", null, contentValues);
		}
		
		return true;
	}
}
