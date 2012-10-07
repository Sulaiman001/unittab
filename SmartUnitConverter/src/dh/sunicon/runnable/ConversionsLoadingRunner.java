package dh.sunicon.runnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.database.Cursor;
import android.util.Log;
import dh.sunicon.datamodel.Conversion;
import dh.sunicon.datamodel.Corresponding;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.EnumValue;

/**
 * Load the Conversions or Correspondings + EnumValues of a category 
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
	
	private boolean cancelled_;
	private boolean finished_ = false;
	private CountDownLatch countDownLatch_ = new CountDownLatch(1);
	
	private ArrayList<Conversion> conversions_;
	private ArrayList<Corresponding> correspondings_;
	private HashMap<Long, EnumValue> enumValues_;
	
	public ConversionsLoadingRunner(DatabaseHelper dbHelper, long categoryId)
	{
		dbHelper_ = dbHelper;
		categoryId_ = categoryId;
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

	public void cancel()
	{
		cancelled_ = true;
	}
	
	public boolean isFinished()
	{
		return finished_;
	}
	
	public void waitToFinish(long timeout, TimeUnit timeUnit) throws InterruptedException
	{
		countDownLatch_.await(timeout, timeUnit);
	}

	@Override
	public void run()
	{
		try
		{	
			Log.d(TAG, "Begin loading conversions / correspondings for Category "+categoryId_);
			
			if (!cancelled_)
			{
				readConversions();
			}
			
			if (!cancelled_)
			{
				readCorrespondings();
			}
			
			if (!cancelled_)
			{
				readEnumValues();
			}
			
			if (cancelled_)
			{
				conversions_ = null;
				Log.d(TAG, "Canceled loading conversions / correspondings for Category "+categoryId_);
			}
			else
			{
				Log.d(TAG, "Finish loading conversions / correspondings for Category "+categoryId_);
			}
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
		finally
		{
			finished_ = true;
			//notify other thread that this ones is finished
			countDownLatch_.countDown();
		}
	}
	
	private void readConversions()
	{
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
						SELECT_CONVERSION_QUERY,
						new String[] { Long.toString(categoryId_) });

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
		
		cur.close();
	}
	
	private void readCorrespondings()
	{
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
						SELECT_CORRESPONDING_QUERY,
						new String[] { Long.toString(categoryId_) });

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
		
		cur.close();
	}
	
	private void readEnumValues()
	{
		Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
				SELECT_ENUMVALUE_QUERY,
				new String[] { Long.toString(categoryId_) });

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
		
		cur.close();
	}
	
}