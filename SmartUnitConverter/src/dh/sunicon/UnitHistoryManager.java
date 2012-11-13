package dh.sunicon;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import dh.sunicon.datamodel.DatabaseHelper;

/**
 * All operation related to history. Example: save a selected unit into history... 
 */
public class UnitHistoryManager
{
	static final String TAG = UnitHistoryManager.class.getName();
	private final int MAX_ENTRIES = 10;
	
	private final DatabaseHelper dbHelper_;
	
	private final ExecutorService saveHistoryThread_ = Executors
			.newSingleThreadExecutor();
	
	//private static final String INSERT_HISTORY = "INSERT OR REPLACE INTO unitHistory VALUES (datetime(), ?)";
	
	public UnitHistoryManager(DatabaseHelper dbHelper)
	{
		dbHelper_ = dbHelper;
	}
	
	public void invokeSaveToHistory(final long unitId)
	{
		saveHistoryThread_.execute(new Runnable()
		{
			@Override
			public void run()
			{
				saveToHistory(unitId);
			}
		});
	}
	
	private void saveToHistory(final long unitId)
	{
		//dbHelper_.getWritableDatabase().execSQL(INSERT_HISTORY, new String[] {Long.toString(unitId)});
		//Log.v(TAG, "Begin save history");
		dbHelper_.getWritableDatabase().beginTransaction();
		try
		{
			/* insert entry to the history table */
			
			ContentValues cv = new ContentValues();
			cv.put("lastUsed", DatabaseHelper.DateFormat.format(new Date()));
			cv.put("unitId", unitId);
			dbHelper_.getWritableDatabase().insertWithOnConflict("unitHistory", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
			
//			int countEntries = dbHelper_.getReadableDatabase().rawQuery("select * from unitHistory", null).getCount();
//			Log.d(TAG, "History entries count = "+countEntries);
			
			/* clean old entries */
			
			dbHelper_.getWritableDatabase().delete("unitHistory", 
					"lastUsed NOT IN (SELECT lastUsed FROM unitHistory ORDER BY lastUsed DESC LIMIT ?)",
					new String[]{Integer.toString(MAX_ENTRIES)});
			
			/* commit */
			
			dbHelper_.getWritableDatabase().setTransactionSuccessful(); 
			
			
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
		finally
		{
			dbHelper_.getWritableDatabase().endTransaction(); //rollback if not marked as successfull
			//Log.v(TAG, "End save history");
		}
	}
}
