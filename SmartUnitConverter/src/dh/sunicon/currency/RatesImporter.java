package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownServiceException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import dh.sunicon.datamodel.DatabaseHelper;

import android.util.Log;

/**
 * update 160+ currencies with base USD
 */
public abstract class RatesImporter
{	
	private static final String TAG = RatesImporter.class.getName();
	private boolean requestCancellation_ = false;
	private HttpGet httpGet_;
	private HttpURLConnection httpConnection_;
	private DatabaseHelper dbHelper_;
	private long baseCurrencyUnitId_;
	
	public RatesImporter(DatabaseHelper dbHelper, long baseCurrencyUnitId)
	{
		dbHelper_ = dbHelper;
		baseCurrencyUnitId_ = baseCurrencyUnitId;
	}
	
	public boolean importRates(String url) {
		if (isDumped()) {
			throw new UnsupportedOperationException();
		}
		try
		{
			InputStream inputStream = getStreamFromUrl(url);
			return importFrom(inputStream);
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
		return false;
	}
	
	protected abstract boolean importFrom(InputStream inputStream) throws IOException, XmlPullParserException;
	 
	
	protected boolean updateDatabase(String currencyCode, String rate) {
		
		try
		{
			//TODO
			Log.i(TAG, currencyCode + " "+rate);
			return true;
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
		return false;
	}
	
	public void dumpIt()
	{
		requestCancellation_ = true;
		if (httpGet_!=null) {
			try {
				httpGet_.abort();
			}
			catch (Exception ex) {
				Log.w(TAG, ex.getMessage());
			}
		}
		if (httpConnection_!=null) {
			try {
				httpConnection_.disconnect();
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
	
	
	/* Toolset */
	
	private InputStream getStreamFromUrl(String address) throws IOException {
			URL url = new URL(address);
			httpConnection_ = (HttpURLConnection)url.openConnection();
			int responseCode = httpConnection_.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK)
				return httpConnection_.getInputStream();
			else
				throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
	}

	private InputStream getInputStreamFromUrl2(String address) throws IllegalStateException, IOException
	{
		httpGet_ = new HttpGet(address);
		HttpClient httpclient = new DefaultHttpClient();
		// Execute HTTP Get Request
		HttpResponse response = httpclient.execute(httpGet_);
		return response.getEntity().getContent();
	}
	
	public static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	public static void skip(XmlPullParser parser)
			throws XmlPullParserException, IOException
	{
		if (parser.getEventType() != XmlPullParser.START_TAG)
		{
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0)
		{
			switch (parser.next())
			{
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
			}
		}
	}
	public static void assertEqual(int x, int expectedX) {
		if (x != expectedX)
			throw new ParseException("Got "+x+" but expected "+expectedX);
	}
	public static void assertEqual(String x, String expectedX) {
		if (x == expectedX)
			throw new ParseException("Got "+x+" but expected "+expectedX);
	}
}
