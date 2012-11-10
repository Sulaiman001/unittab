package dh.sunicon.currency;

import android.app.Activity;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.ImportationReport.MessageType;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.Unit;
import dh.sunicon.runnable.ConversionsLoadingRunner;

/**
 * Implement the cancellable aspect of the updater
 */
public class RatesImportersManager
{
	private static final String TAG = RatesImportersManager.class.getName();
	private boolean requestCancellation_ = false;
	private Activity context_;
	private DatabaseHelper dbHelper_;
	private RatesImporter yahooImporter_;
	private RatesImporter tmcImporter_;

	public RatesImportersManager(Activity context)
	{
		context_ = context;
		dbHelper_ = ((MainActivity)context_).getDatabaseHelper();
	}

	public ImportationReport importOnBackground(long currencyUnitId)
	{
		if (isDumped()) {
			throw new UnsupportedOperationException();
		}
		
		Log.d("CURR", "importOnBackground BEGIN "+currencyUnitId);
		ImportationReport report = new ImportationReport();
		try
		{
			importFromYahoo(report);
			
			//TODO debug
//			for (int i=0; i<10; i++) {
//				if (isDumped())
//					break;
//				Thread.sleep(500);
//			}
//			
			//importFromTMC(currencyUnitId, report);
		}
		catch (Exception ex)
		{
			report.add(report.new ReportEntry(MessageType.ERROR, "Update failed.", Log.getStackTraceString(ex)));
			Log.w(TAG, ex);
		}
		
		Log.d("CURR", "importOnBackground END "+currencyUnitId);
		return report;
	}
	
	void importFromYahoo(ImportationReport report)
	{
		if (yahooImporter_!=null) {
			yahooImporter_.dumpIt();
		}
		Unit usdCurrency = Unit.findById(dbHelper_, Unit.USD_UNIT);
		yahooImporter_ = new YahooRatesImporter(dbHelper_, usdCurrency, report);
		yahooImporter_.importRates("http://finances.yahoo.com/webservice/v1/symbols/allcurrencies/quotes");
	}
	
	void importFromTMC(Unit baseCurrency, ImportationReport report)
	{
//		if (tmcImporter_!=null) {
//			tmcImporter_.dumpIt();
//		}
//		tmcImporter_ = new TmcRatesImporter(dbHelper_, currencyUnitId, report);
//	
		//TODO
	}

	public void dumpIt()
	{
		requestCancellation_ = true;
		if (yahooImporter_!=null)
		{
			try {
				yahooImporter_.dumpIt();
			}
			catch (Exception ex) {
				Log.w(TAG, ex.getMessage());
			}
		}
		if (tmcImporter_!=null) {
			try {
				tmcImporter_.dumpIt();
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
