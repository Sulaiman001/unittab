package dh.sunicon.runnable;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.util.Log;
import dh.sunicon.ConverterFragment;
import dh.sunicon.datamodel.Conversion;
import dh.sunicon.datamodel.Corresponding;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.EnumValue;

/**
 * Load the Conversions or Correspondings + EnumValues of a category, Once an instance is dump, it cannot be used again 
 */
public final class ConversionsLoadingRunner implements Runnable
{
	static final String TAG = ConversionsLoadingRunner.class.getName();
	
	static final String SELECT_CONVERSION_QUERY = "SELECT conversion.* FROM conversion JOIN unit ON conversion.base = unit.id WHERE unit.categoryId = ?";
	
	static final String SELECT_CORRESPONDING_QUERY = "SELECT corresponding.* FROM corresponding"
			+ " JOIN enumvalue ON corresponding.enumId1 = enumValue.id OR corresponding.enumId2 = enumValue.id"
			+ " JOIN unit ON enumValue.unitId = unit.id"
			+ " WHERE unit.categoryId = ?"
			+ " GROUP BY corresponding.id"; 
	
	static final String SELECT_ENUMVALUE_QUERY = "SELECT enumValue.* FROM enumValue JOIN unit ON enumValue.unitId = unit.id WHERE unit.categoryId=?";

	private final DatabaseHelper dbHelper_;
	private final long categoryId_;
	private final long baseUnitId_;
	
	private boolean cancelled_ = false;
	//private CountDownLatch countDownLatch_ = new CountDownLatch(1);
	
	private ArrayList<Conversion> conversions_;
	private ArrayList<Corresponding> correspondings_;
	private HashMap<Long, EnumValue> enumValues_;
	
	public ConversionsLoadingRunner(DatabaseHelper dbHelper, long categoryId, long baseUnitId)
	{
		dbHelper_ = dbHelper;
		categoryId_ = categoryId;
		baseUnitId_ = baseUnitId;
		//countDownLatch_ = new CountDownLatch(1);
	}
	
	public ConversionsLoadingRunner(DatabaseHelper dbHelper, JSONObject savedState) throws JSONException
	{
		dbHelper_ = dbHelper;
		categoryId_ = savedState.getLong("categoryId");
		baseUnitId_ = savedState.getLong("baseUnitId");
		//countDownLatch_ = new CountDownLatch(1);
//		if (!finished_)
//		{
//			countDownLatch_ = new CountDownLatch(1);
//		}
//		else
//		{
//			countDownLatch_ = new CountDownLatch(0);
//			conversions_ = new ArrayList<Conversion>();
//			JSONArray jsonConversions = 
//		}
	}
	
	public JSONObject serialize() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("categoryId", categoryId_);
		json.put("baseUnitId", baseUnitId_);
//		json.put("finished", finished_);
//
//		if (!finished_) {
//			return json;
//		}
//			
//		JSONArray jsonConversions = new JSONArray();
//		for (Conversion c : conversions_) {
//			jsonConversions.put(c.serialize());
//		}
//		json.put("conversions", jsonConversions);
//		
//		JSONArray jsonCorrespondings = new JSONArray();
//		for (Corresponding c : correspondings_) {
//			jsonCorrespondings.put(c.serialize());
//		}
//		json.put("correspondings", jsonCorrespondings);
//		
//		JSONArray jsonEnumValues_ = new JSONArray();
//		for (EnumValue e : enumValues_.values()) {
//			jsonEnumValues_.put(e.serialize());
//		}
//		json.put("enumValues", jsonEnumValues_);
		
		return json;
	}
	
	
	public ArrayList<Conversion> getConversions()
	{
		return conversions_;
	}
	public ArrayList<Corresponding> getCorrespondings()
	{
		return correspondings_;
	}
	public HashMap<Long, EnumValue> getEnumValues()
	{
		return enumValues_;
	}

	/**
	 * Soft-destroy this object, never use it again after calling this methode.
	 */
	public void dumpIt()
	{
		cancelled_ = true;
	}
	
	public boolean isDumped() {
		return cancelled_;
	}
	
//	public void waitToFinish(long timeout, TimeUnit timeUnit) throws InterruptedException
//	{
//		countDownLatch_.await(timeout, timeUnit);
//	}

	@Override
	public void run()
	{
		try
		{
			if (isDumped()) {
				throw new UnsupportedOperationException();
			}
			
			Log.v(TAG, "Start loading conversions / correspondings for Category "+categoryId_);
			
			if (categoryId_ == ConverterFragment.CURRENCY_CATEGORY) 
			{
				readCurrencyConversions();
				return;
			}
			
			readConversions();
			readCorrespondings();
			readEnumValues();
			
			if (cancelled_)
			{
				conversions_ = null;
				Log.v(TAG, "Canceled loading conversions / correspondings for Category "+categoryId_);
			}
			else
			{
				Log.v(TAG, "Finish loading conversions / correspondings for Category "+categoryId_);
			}
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}
	
	private void readConversions()
	{
		if (cancelled_) return;
		
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
						SELECT_CONVERSION_QUERY,
						new String[] { Long.toString(categoryId_) });
		try
		{
			final int idCi = cur.getColumnIndex("id");
			final int baseCi = cur.getColumnIndex("base");
			final int targetCi = cur.getColumnIndex("target");
			final int fxCi = cur.getColumnIndex("fx");
			final int formulaCi = cur.getColumnIndex("formula");
			final int reversedFormulaCi = cur.getColumnIndex("reversedFormula");
			
			conversions_ = new ArrayList<Conversion>();
			
			while (cur.moveToNext() && !cancelled_)
			{
				Conversion c = new Conversion(dbHelper_, 
						cur.getLong(idCi),
						cur.getLong(baseCi), 
						cur.getLong(targetCi), 
						cur.getDouble(fxCi), 
						cur.getString(formulaCi), 
						cur.getString(reversedFormulaCi));
				conversions_.add(c);
			}
		}
		finally
		{
			cur.close();
		}
	}
	
	private void readCorrespondings()
	{
		if (cancelled_) return;
		
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
						SELECT_CORRESPONDING_QUERY,
						new String[] { Long.toString(categoryId_) });
		try
		{
			final int idCi = cur.getColumnIndex("id");
			final int enumId1Ci = cur.getColumnIndex("enumId1");
			final int enumId2Ci = cur.getColumnIndex("enumId2");
			
			correspondings_ = new ArrayList<Corresponding>();
			
			while (cur.moveToNext() && !cancelled_)
			{
				Corresponding c = new Corresponding(dbHelper_, 
						cur.getLong(idCi),
						cur.getLong(enumId1Ci), 
						cur.getLong(enumId2Ci));
				correspondings_.add(c);
			}
		}
		finally {
			cur.close();
		}
	}
	
	private void readEnumValues()
	{
		if (cancelled_) return;
		
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
				SELECT_ENUMVALUE_QUERY,
				new String[] { Long.toString(categoryId_) });
		try
		{
			final int idCi = cur.getColumnIndex("id");
			final int unitIdCi = cur.getColumnIndex("unitId");
			final int valueCi = cur.getColumnIndex("value");
			
			enumValues_ = new HashMap<Long, EnumValue>();
			
			while (cur.moveToNext() && !cancelled_)
			{
				long enumId = cur.getLong(idCi); 
				EnumValue ev = new EnumValue(dbHelper_, 
						cur.getLong(unitIdCi),
						enumId, 
						cur.getString(valueCi));
				enumValues_.put(enumId, ev);
			}
		}
		finally
		{
			cur.close();
		}
	}
	
	private final static long USD_UNIT = 1413;
	
	private void readCurrencyConversions()
	{
		if (cancelled_) return;
		
		//Log.d(TAG, "Begin loading currencies conversions of the currency unitId = "+baseUnitId_);

		Log.d("CURR", "readCurrencyConversions BEGIN baseUnitId = "+baseUnitId_);
		
		//Get the conversions from baseUnitId_ or from USD
		
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery("SELECT * FROM conversion WHERE base = ?", new String[]{Long.toString(baseUnitId_)});
		if (cur.getCount()==0 && baseUnitId_ != USD_UNIT)
		{
			cur.close();
			cur = dbHelper_.getReadableDatabase().rawQuery("SELECT * FROM conversion WHERE base = ?", new String[]{Long.toString(USD_UNIT)});
		}
		
		//Load conversions to local variable (RAM)
		
		try
		{
			final int idCi = cur.getColumnIndex("id");
			final int baseCi = cur.getColumnIndex("base");
			final int targetCi = cur.getColumnIndex("target");
			final int fxCi = cur.getColumnIndex("fx");
			final int formulaCi = cur.getColumnIndex("formula");
			final int reversedFormulaCi = cur.getColumnIndex("reversedFormula");
			
			conversions_ = new ArrayList<Conversion>();
			
			while (cur.moveToNext() && !cancelled_)
			{
				Conversion c = new Conversion(dbHelper_, 
						cur.getLong(idCi),
						cur.getLong(baseCi), 
						cur.getLong(targetCi), 
						cur.getDouble(fxCi), 
						cur.getString(formulaCi), 
						cur.getString(reversedFormulaCi));
				conversions_.add(c);
			}
		}
		finally
		{
			cur.close();
		}
		
		Log.d("CURR", "readCurrencyConversions END baseUnitId = "+baseUnitId_);
	}
	
}