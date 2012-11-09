package dh.sunicon.currency;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.CurrencyUpdater.UpdatingResult;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.Unit;

/**
 * Implement the cancellable aspect of the updater
 */
public class CurrencyImporter
{
	private static final String TAG = CurrencyImporter.class.getName();
	private boolean requestCancellation_ = false;
	private Activity context_;
	private DatabaseHelper dbHelper_;

	public CurrencyImporter(Activity context)
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
			
			InputStream streamYahoo = getStreamFromUrl("http://finance.yahoo.com/webservice/v1/symbols/allcurrencies/quote");
			boolean successYahoo = processStreamYahoo(streamYahoo);
			
			/* Get update from themoneyconverter.com */
			
			Unit baseCurrency = Unit.findById(dbHelper_, currencyUnitId);
			String currencyCode = baseCurrency.getShortName();
			InputStream streamTMC = getStreamFromUrl("http://themoneyconverter.com/rss-feed/"+currencyCode+"/rss.xml");
			boolean successTMC = processStreamTMC(streamTMC, currencyUnitId);
			
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

	private InputStream getStreamFromUrl(String address) {
		try
		{
			URL url = new URL(address);
			HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK)
				return httpConnection.getInputStream();
			else
				Log.e(TAG, "Failed to connect to "+address+": "+ httpConnection.getResponseMessage()+" ("+ responseCode+")");
		}
		catch (Exception ex)
		{
			Log.e(TAG, "Failed to connect to "+address);
		}
		return null;
	}

	
	/**
	 * update 160+ currencies with base USD
	 */
	private boolean processStreamYahoo(InputStream inputStream)
	{
		if (inputStream == null) return false;
		
		//TODO
		
		
		
		return false;
	}
	
	/**
	 * update 90 currencies with base currencyUnitId
	 */
	private boolean processStreamTMC(InputStream inputStream, long currencyUnitId)
	{
		if (inputStream == null) return false;
		
		// TODO
		
		return false;
	}
	
	
	public void dumpIt()
	{
		requestCancellation_ = true;
	}

	public boolean isDumped()
	{
		return requestCancellation_;
	}
}
