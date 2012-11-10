package dh.sunicon.currency;

import android.app.Activity;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.CurrencyUpdater.UpdatingResult;
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

	public UpdatingResult importOnBackground(long currencyUnitId)
	{
		if (isDumped())
			throw new UnsupportedOperationException();
		try
		{
			Log.d("CURR", "importOnBackground BEGIN "+currencyUnitId);

			/* Get update from yahoo first */
			
			boolean successYahoo = importFromYahoo();
			
			/* Get update from themoneyconverter.com */
			
			boolean successTMC = importFromTMC(currencyUnitId);
			
			if (successYahoo || successTMC)
				return UpdatingResult.SUCCESS;
			else
				return UpdatingResult.FAILED;
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
			return UpdatingResult.FAILED;
		}
	}
	
	boolean importFromYahoo()
	{
		if (yahooImporter_!=null) {
			yahooImporter_.dumpIt();
		}
		yahooImporter_ = new YahooRatesImporter(dbHelper_);
		return yahooImporter_.importRates("http://finance.yahoo.com/webservice/v1/symbols/allcurrencies/quote");
	}
	
	boolean importFromTMC(long currencyUnitId)
	{
		if (tmcImporter_!=null) {
			tmcImporter_.dumpIt();
		}
		tmcImporter_ = new TmcRatesImporter(dbHelper_, currencyUnitId);
	
		//TODO
		
		return false;
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
