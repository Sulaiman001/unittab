package dh.sunicon.currency;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.LinkedList;

import android.app.Activity;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.UpdatingReport.MessageType;
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
		
		//always update USD_UNIT base to fill rate which has rates = 0 (eg: 1 VND = 0.00 EUR)
		
		Unit usdCurrency = Unit.findById(dbHelper_, Unit.USD_UNIT);
		UpdatingAgent yahooUsdAgent = new YahooUsdAgent(context_, usdCurrency, report);
		agents_.addLast(yahooUsdAgent);
	}

	public synchronized UpdatingReport importOnBackground(long currencyUnitId)
	{
		if (isDumped()) {
			throw new UnsupportedOperationException();
		}
		
		UpdatingReport report = new UpdatingReport();
		
		if (!isExpiry(currencyUnitId)) {
			report.add(report.new ReportEntry(MessageType.INFO, "Exchange rates data is still up to date (not yet expiried).")); //TODO multi-language 
			report.setCancel(true);
			return report;
		}
		
		Log.d("CURR", "importOnBackground BEGIN "+currencyUnitId);
		try
		{
			setupAgents(currencyUnitId, report);
			
			for (UpdatingAgent agent : agents_) {
				if (isDumped()) {
					return report;
				}
				agent.process();
			}
			
			if (report.successUpdateMostly()) {
				saveLastUpdate(currencyUnitId);
			}
			
			//TODO debug
//			for (int i=0; i<10; i++) {
//				if (isDumped())
//					break;
//				Thread.sleep(500);
//			}

		}
		catch (Exception ex)
		{
			report.add(report.new ReportEntry(MessageType.ERROR, "Update failed.", Log.getStackTraceString(ex)));
			Log.w(TAG, ex);
		}
		
		Log.d("CURR", "importOnBackground END "+currencyUnitId);
		return report;
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
		return context_.getPreferences(Activity.MODE_PRIVATE).getLong("CurrencyRateTimeToLive", 1000);
	}
	
	private boolean isExpiry(long currencyUnitId) {
		long now = getNow();
		return (now - getLastUpdate(dbHelper_, currencyUnitId)) > getTimeToLive();
	}
	
	public static long getNow() {
		return Calendar.getInstance().getTime().getTime();
	}
	
	public void dumpIt() {
		requestCancellation_ = true;
		
		for (UpdatingAgent agent : agents_) {
			try {
				agent.dumpIt();
			}
			catch (Exception ex) {
				Log.w(TAG, ex.getMessage());
			}
		}
	}

	public boolean isDumped()
	{
		return requestCancellation_;
	}
}
