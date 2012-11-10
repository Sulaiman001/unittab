package dh.sunicon.currency;

import android.app.Activity;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.ImportationReport.MessageType;
import dh.sunicon.datamodel.DatabaseHelper;

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
		
		ImportationReport report = new ImportationReport();
		try
		{
			Log.d("CURR", "importOnBackground BEGIN "+currencyUnitId);
			
			Thread.sleep(5000);
			
			importFromYahoo(report);
			
			//importFromTMC(currencyUnitId, report);
		}
		catch (Exception ex)
		{
			report.add(report.new ReportEntry(MessageType.ERROR, "Update failed.", Log.getStackTraceString(ex)));
			Log.w(TAG, ex);
		}
		
		return report;
	}
	
	void importFromYahoo(ImportationReport report)
	{
		if (yahooImporter_!=null) {
			yahooImporter_.dumpIt();
		}
		yahooImporter_ = new YahooRatesImporter(dbHelper_, report);
		yahooImporter_.importRates("http://finance.yahoo.com/webservice/v1/symbols/allcurrencies/quote");
	}
	
	void importFromTMC(long currencyUnitId, ImportationReport report)
	{
		if (tmcImporter_!=null) {
			tmcImporter_.dumpIt();
		}
		tmcImporter_ = new TmcRatesImporter(dbHelper_, currencyUnitId, report);
	
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
