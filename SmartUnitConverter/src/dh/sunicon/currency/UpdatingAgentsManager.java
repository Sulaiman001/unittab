package dh.sunicon.currency;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.LinkedList;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.UpdatingReport.MessageType;
import dh.sunicon.currency.UpdatingReport.OperationType;
import dh.sunicon.currency.UpdatingReport.ReportEntry;
import dh.sunicon.currency.UpdatingReport.UpdateItem;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.Depot;
import dh.sunicon.datamodel.Unit;
import dh.sunicon.workarounds.MyApplication;

/**
 * Implement the cancellable aspect of the updater
 * An UpdatingAgentsManager will be create each time CurrencyUpdater process an update
 * and it will be destroy (dump) if CurrencyUpdater.cancel() is called
 */
public class UpdatingAgentsManager
{
	private static final String TAG = UpdatingAgentsManager.class.getName();
	private boolean requestCancellation_ = false;
	private Activity context_;
	private DatabaseHelper dbHelper_;
	private LinkedList<UpdatingAgent> agents_;

	public UpdatingAgentsManager(Activity context)
	{
		context_ = context;
		dbHelper_ = ((MainActivity)context_).getDatabaseHelper();
	}
	
	private void setupAgents(long currencyId, UpdatingReport report) {
		agents_ = new LinkedList<UpdatingAgent>();
		
		Unit baseCurrency = Unit.findById(dbHelper_, currencyId);
		
		UpdatingAgent yahooCsvAgent = new YahooCsvAgent(context_, baseCurrency, report);
		agents_.addLast(yahooCsvAgent);
		
		UpdatingAgent tmcAgent = new TmcAgent(context_, baseCurrency, report);
		agents_.addLast(tmcAgent);
		
		if (!MyApplication.DEBUG_MODE) { //if not in debug mode i'm going easy to minimize error
			
			//Go easy: always update USD_UNIT base to fill rate which has rates = 0 (eg: 1 VND = 0.00 EUR)
			Unit usdCurrency = Unit.findById(dbHelper_, Unit.USD_UNIT);
			UpdatingAgent yahooUsdAgent = new YahooUsdAgent(context_, usdCurrency, report);
			agents_.addLast(yahooUsdAgent);
		}
	}

	public synchronized UpdatingReport importOnBackground(long currencyUnitId)
	{
		if (isDumped()) {
			return null;
		}
		
		UpdatingReport report = new UpdatingReport();
		
		if (!isExpiry(currencyUnitId)) {
			report.add(report.new ReportEntry(MessageType.INFO, "Exchange rates data is still up to date (not yet expiried).")); //TODO multi-language 
			report.forceSuccessAll();
			return report;
		}
		
		NetworkInfo networkInfo = ((MainActivity)context_).getNetworkInfo();
		if (networkInfo==null || !networkInfo.isConnected()) { 
			//no network connection
			report.add(report.new ReportEntry(MessageType.ERROR, "Network not avaiable.")); //TODO mutli-language
			return report;
		} 
		
		if (getCurrencyUpdaterOption() == CurrencyUpdater.OPT_WIFI_ONLY) {
			if (networkInfo.getType()!=ConnectivityManager.TYPE_WIFI) {
				//only update on wifi
				report.add(report.new ReportEntry(MessageType.INFO, "Allow update on WIFI only.")); //TODO mutli-language
				return report;
			}
		}
		
		Log.d("CURR", "importOnBackground BEGIN "+currencyUnitId);
		try
		{
			setupAgents(currencyUnitId, report);
			report.setInProgress(true);

			buildUpdateCache(report);
			processUpdateCache(report);
			
			if (isDumped()) {
				report.add(report.new ReportEntry(MessageType.WARNING, "Update cancelled by User.")); //TODO multi-language
			}
			else if (report.isSuccessAll()) {
				saveLastUpdate(currencyUnitId);
			}
		}
		catch (Exception ex)
		{
			report.add(report.new ReportEntry(MessageType.ERROR, "Update failed: "+ex.getMessage(), Log.getStackTraceString(ex)));
			Log.w(TAG, ex);
		}
		finally {
			report.setInProgress(false);
		}
		
		Log.d("CURR", "importOnBackground END "+currencyUnitId);
		return report;
	}
	
	private void buildUpdateCache(UpdatingReport report) {
		for (UpdatingAgent agent : agents_) {
			if (isDumped()) {
				return;
			}
			agent.buildCache();
		}
	}
	
	private void processUpdateCache(UpdatingReport report) {
		
		SQLiteDatabase db = dbHelper_.getWritableDatabase(); 
		db.beginTransaction();
		try
		{
			for (UpdateItem i : report.getCacheUpdates().values()) {
				if (!isDumped())
				{
					if (i.getOperation() == OperationType.INSERT) {
						ContentValues contentValues = new ContentValues();
						contentValues.put("fx", i.getRate());
						db.update("conversion", contentValues, "base=? and target=?", 
								new String[] {Long.toString(i.getBase()), Long.toString(i.getTarget()) });
					}
					else {
						ContentValues contentValues = new ContentValues();
						contentValues.put("fx", i.getRate());
						contentValues.put("base", i.getBase());
						contentValues.put("target", i.getTarget());
						db.insert("conversion", null, contentValues);
					}
					report.incrementUpdatedCount();
				}
			}
	
			db.setTransactionSuccessful(); 
		}
		catch (Exception ex)
		{
			report.add(report.new ReportEntry(MessageType.ERROR, "Update failed: "+ex.getMessage(), Log.getStackTraceString(ex)));
			report.resetUpdatedCount();
			Log.w(TAG, ex);
		}
		finally
		{
			db.endTransaction(); //rollback if not marked as successfull
		}
	}
	
	/**
	 * return "UA/"
	 */
	public static String getHistoryKey(long currencyUnitId) {
		return "UA/"+Long.toString(currencyUnitId);
	}
	
	public static long getCurrencyIdFromHistoryKey(String historyKey) {
		if (!historyKey.startsWith("UA/")) {
			throw new InvalidParameterException("Not a history key");
		}
		return Long.parseLong(historyKey.substring(3));
	}
	
	/**
	 * read time of last update of this currencyUnitId
	 */
	public static long getLastUpdate(DatabaseHelper dbHelper, long currencyUnitId) {
		Long l = Depot.getLong(dbHelper, getHistoryKey(currencyUnitId));
		if (l==null) return 0;
		return l.longValue();
	}
	
	/**
	 * save time of last update of this currencyUnitId
	 */
	private void saveLastUpdate(long currencyUnitId) {
		Depot.putLong(dbHelper_, getHistoryKey(currencyUnitId), getNow());
	}
	
	private long getTimeToLive() {
		return context_.getPreferences(Activity.MODE_PRIVATE).getLong("CurrencyRateTimeToLive", 3600*1000);
	}
	
	private boolean isExpiry(long currencyUnitId) {
		long now = getNow();
		return (now - getLastUpdate(dbHelper_, currencyUnitId)) > getTimeToLive();
	}
	
	private int getCurrencyUpdaterOption() {
		SharedPreferences preferences_ = context_.getPreferences(Activity.MODE_PRIVATE);
		return preferences_.getInt(CurrencyUpdater.CurrencyUpdaterOptionName, CurrencyUpdater.OPT_ALL_NETWORK);
	}
	
	public static long getNow() {
		return Calendar.getInstance().getTime().getTime();
	}
	
	public void dumpIt() {
		requestCancellation_ = true;
		if (agents_!=null) 
		{
			for (UpdatingAgent agent : agents_) {
				try {
					agent.dumpIt();
				}
				catch (Exception ex) {
					Log.w(TAG, ex.getMessage());
				}
			}
		}
	}

	public boolean isDumped()
	{
		return requestCancellation_;
	}
}
