package dh.sunicon.runnable;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.util.Log;
import dh.sunicon.currency.UpdatingAgentsManager;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.Conversion;
import dh.sunicon.datamodel.Corresponding;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.EnumValue;
import dh.sunicon.datamodel.Unit;

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
	
	private volatile boolean cancelled_ = false;
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
			
			if (categoryId_ == Category.CURRENCY_CATEGORY) 
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
		
		conversions_ = new ArrayList<Conversion>();
		try
		{
			readConversionFromCursor(cur);
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
	
	private void readCurrencyConversions()
	{
		if (cancelled_) return;
		
		//Log.d(TAG, "Begin loading currencies conversions of the currency unitId = "+baseUnitId_);

		Log.d("CURR", "readCurrencyConversions BEGIN baseUnitId = "+baseUnitId_);
		
		conversions_ = new ArrayList<Conversion>();
		
		//Load conversions to local variable (RAM)
		
		//Get the conversions from the optimizeBaseCurrencyId
		
		//Get the conversions from optimizeBaseCurrencyId
		long optimizeBaseCurrencyId = choseBetweenMostRecentCurrencyAnd(baseUnitId_);
		{
			Cursor cur1 = dbHelper_.getReadableDatabase().rawQuery("SELECT * FROM conversion WHERE base = ?", new String[]{Long.toString(optimizeBaseCurrencyId)});
			try
			{
				readConversionFromCursor(cur1);
			}
			finally
			{
				cur1.close();
			}
		}
		
		if (optimizeBaseCurrencyId!=Unit.USD_UNIT) //if not already load the USD conversion-set from above... 
		{  
			//Always use the conversions from USD to fill lacking rates (for eg, rate conversion from VND to EUR = 0.00)
			Cursor cur2 = dbHelper_.getReadableDatabase().rawQuery("SELECT * FROM conversion WHERE base = ?", new String[]{Long.toString(Unit.USD_UNIT)});
			try
			{
				readConversionFromCursor(cur2);
			}
			finally
			{
				cur2.close();
			}
		}
		
		if (conversions_.isEmpty())
		{
			// read all possible currency conversion! it can be really long!
			Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
					SELECT_CONVERSION_QUERY,
					new String[] { Long.toString(categoryId_) });
			try
			{
				readConversionFromCursor(cur);
			}
			finally
			{
				cur.close();
			}
		}
		
		if (conversions_.isEmpty()) {
			Log.w("CURR", "No conversion for Currency found!");
		}
		
		Log.d("CURR", String.format("readCurrencyConversions END baseUnitId = %d - found %d conversion", baseUnitId_, conversions_.size()));
	}
	
	private void readConversionFromCursor(Cursor cur)
	{
		final int idCi = cur.getColumnIndex("id");
		final int baseCi = cur.getColumnIndex("base");
		final int targetCi = cur.getColumnIndex("target");
		final int fxCi = cur.getColumnIndex("fx");
		final int formulaCi = cur.getColumnIndex("formula");
		final int reversedFormulaCi = cur.getColumnIndex("reversedFormula");
		
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
	
	private final static long MILIS_PER_DAY = 86400000L; 
	
	/**
	 * This method decide which conversions-set we will select to perform conversions (of currency exchange).
	 * 
	 * User has chose a base currency (preferedCurrency) for eg: JPY, we will normally load the conversions-set
	 * which take JPY as base.
	 * 
	 * Howerver, if these conversions are out-date (the currency rates of the JPY-base have not been updated for a long time).
	 * So it will better to get other conversion-set base on other currency which is the most up to date.
	 * And use this conversion-set to make a two-step conversion.
	 *   
	 * So this methode will find out mostRecentCurrency, and make choice:
	 * - if the mostRecentCurrency.updateTime is 7 days more recent than the preferedCurrency
	 * it will return mostRecentCurrency. 
	 * - otherwise, it will return the preferedCurrency
	 * 
	 * @param preferedCurrencyId
	 */
	private long choseBetweenMostRecentCurrencyAnd(long preferedCurrencyId)
	{
		//find the mostRecentCurrency 
		Cursor cur1 = dbHelper_.getReadableDatabase().rawQuery("SELECT * FROM depot WHERE key LIKE 'UA/%' ORDER BY i DESC LIMIT 1", null);
		try {
			if (cur1.moveToNext()) {
				String mostRecentCurrencyHistoryKey = cur1.getString(cur1.getColumnIndex("key"));
				long mostRecentCurrencyId = UpdatingAgentsManager.getCurrencyIdFromHistoryKey(mostRecentCurrencyHistoryKey);
				
				if (mostRecentCurrencyId != preferedCurrencyId) {
					long mostRecentCurrencyUpdateDate = cur1.getLong(cur1.getColumnIndex("i"));
					long preferedCurrencyUpdateDate = UpdatingAgentsManager.getLastUpdate(dbHelper_, preferedCurrencyId);
					
					if ((mostRecentCurrencyUpdateDate - preferedCurrencyUpdateDate) > 7 * MILIS_PER_DAY) {
						Log.w("CURR", "Use the Conversion-set of the most recent currency ("+Long.toString(mostRecentCurrencyId)+") with two-step conversion");
						return mostRecentCurrencyId;
					}
				}
			}
		}
		finally {
			cur1.close();
		}
		return preferedCurrencyId; 
	}
	
}