package dh.sunicon.currency;

import java.security.InvalidParameterException;
import java.util.LinkedList;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.CurrencyUpdater.BeforeUpdateStartedListener;
import dh.sunicon.currency.UpdatingReport.MessageType;
import dh.sunicon.currency.UpdatingReport.OperationType;
import dh.sunicon.currency.UpdatingReport.UpdateItem;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.Depot;
import dh.sunicon.datamodel.Unit;

/**
 * Implement the cancellable aspect of the updater
 * An UpdatingAgentsManager will be create each time CurrencyUpdater process an update
 * and it will be destroy (dump) if CurrencyUpdater.cancel() is called
 */
public class UpdatingAgentsManager
{
	private static final String TAG = UpdatingAgentsManager.class.getName();
	private Activity context_;
	private DatabaseHelper dbHelper_;
	private LinkedList<UpdatingAgent> agents_;
	private AsyncTask asyncTask_;
	private BeforeUpdateStartedListener beforeUpdateStarted_;

	public UpdatingAgentsManager(Activity context, AsyncTask asyncTask)
	{
		context_ = context;
		asyncTask_ = asyncTask;
		dbHelper_ = ((MainActivity)context_).getDatabaseHelper();
	}
	
	private void setupAgents(long currencyId, UpdatingReport report) {
		agents_ = new LinkedList<UpdatingAgent>();
		
		Unit baseCurrency = Unit.findById(dbHelper_, currencyId);
		
		UpdatingAgent tmcAgent = new TmcAgent(context_, baseCurrency, report, asyncTask_);
		agents_.addLast(tmcAgent);
		
		UpdatingAgent yahooCsvAgent = new YahooCsvAgent(context_, baseCurrency, report, asyncTask_);
		agents_.addLast(yahooCsvAgent);
		
		//if (!MyApplication.DEBUG_MODE) { //if not in debug mode i'm going easy to minimize error
			
			//Go easy: always update USD_UNIT base to fill rate which has rates = 0 (eg: 1 VND = 0.00 EUR)
			Unit usdCurrency = Unit.USD_UNIT == currencyId ? baseCurrency : Unit.findById(dbHelper_, Unit.USD_UNIT);
			UpdatingAgent yahooUsdAgent = new YahooUsdAgent(context_, usdCurrency, report, asyncTask_);
			agents_.addLast(yahooUsdAgent);
		//}
	}

	public UpdatingReport importOnBackground(final long currencyUnitId)
	{
		if (isDumped()) {
			return null;
		}
		
		int currencyUpdaterOption = getCurrencyUpdaterOption();
		
		UpdatingReport report = new UpdatingReport();
		
		if (currencyUpdaterOption == CurrencyUpdater.OPT_NEVER) {
			report.add(report.new ReportEntry(MessageType.INFO, "Exchange rates live update is disabled.")); //TODO multi-language 
			report.forceSuccessAll();
			return report;
		}
		
		if (!isExpiry(currencyUnitId)) {
			report.add(report.new ReportEntry(MessageType.INFO, "Exchange rates data is still up to date (not yet expiried).")); //TODO multi-language 
			report.forceSuccessAll();
			return report;
		}
		
		NetworkInfo networkInfo = ((MainActivity)context_).getNetworkInfo();
		
		if (networkInfo!=null && currencyUpdaterOption == CurrencyUpdater.OPT_WIFI_ONLY) {
			if (networkInfo.getType()!=ConnectivityManager.TYPE_WIFI) {
				//only update on wifi
				report.forceSuccessAll();
				report.add(report.new ReportEntry(MessageType.INFO, "Allow update on WIFI only.")); //TODO mutli-language
				return report;
			}
		}
		
		if (beforeUpdateStarted_ != null) {
			context_.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (beforeUpdateStarted_ != null) {
						beforeUpdateStarted_.beforeUpdateStarted(currencyUnitId);
					}
				}
			});
		}
		
		if (networkInfo==null || !networkInfo.isConnected()) { 
			//no network connection
			report.add(report.new ReportEntry(MessageType.ERROR, "Network not avaiable.")); //TODO mutli-language
			return report;
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
				if (isDumped()) {
					break;
				}
				if (i.getOperation() == OperationType.UPDATE) {
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
		Depot.putLong(dbHelper_, getHistoryKey(currencyUnitId), DatabaseHelper.getNow());
	}
	
	private long getTimeToLive() {
		long ttl = context_.getPreferences(Activity.MODE_PRIVATE).getLong(CurrencyUpdater.OPTNAME_CURRENCY_EXPIRY_TIME, 10*1000);
		Log.v("CURR", "Get TTL = "+ttl);
		return ttl;
	}
	
	private boolean isExpiry(long currencyUnitId) {
		long now = DatabaseHelper.getNow();
		return (now - getLastUpdate(dbHelper_, currencyUnitId)) > getTimeToLive();
	}
	
	private int getCurrencyUpdaterOption() {
		SharedPreferences preferences_ = context_.getPreferences(Activity.MODE_PRIVATE);
		int o = preferences_.getInt(CurrencyUpdater.OPTNAME_CURRENCY_LIVE_UPDATE, CurrencyUpdater.OPT_ALL_NETWORK);
		Log.v("CURR", "Get Currency Updater Option = "+o);
		return o;
	}

	public boolean isDumped()
	{
		return asyncTask_ == null || asyncTask_.isCancelled();
	}

	void setBeforeUpdateStarted(BeforeUpdateStartedListener beforeUpdateStarted)
	{
		beforeUpdateStarted_ = beforeUpdateStarted;
	}
}
