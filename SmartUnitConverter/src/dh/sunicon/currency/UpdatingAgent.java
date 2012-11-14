package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import dh.sunicon.MainActivity;
import dh.sunicon.currency.UpdatingReport.MessageType;
import dh.sunicon.currency.UpdatingReport.OperationType;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.MathEval;
import dh.sunicon.datamodel.Unit;

/**
 * Implement common aspect of an updating agent (UA):
 * An UA parse data from the internet from address getURL(), and must update the local database
 * - An UA must store, the last time the update occurs in the database depot:
 *  + the key is the UA's name + base currency code the value is last time updating
 *  eg ("Yahoo USD" => "15/12/2012 16:35:14")
 *  
 */
public abstract class UpdatingAgent
{	
	private static final String TAG = UpdatingAgent.class.getName();
	private boolean requestCancellation_ = false;
	private HttpGet httpGet_;
	private HttpURLConnection httpConnection_;
	protected DatabaseHelper dbHelper_;
	protected Unit baseCurrency_;
	protected UpdatingReport report_;
	private Activity context_;
	protected boolean testMode_ = false;
	protected AsyncTask asyncTask_;
	
	public UpdatingAgent(Activity context, Unit baseCurrency, UpdatingReport report, AsyncTask asyncTask)
	{
		this(context, baseCurrency, report, asyncTask, false);
	}
	
	UpdatingAgent(Activity context, Unit baseCurrency, UpdatingReport report, AsyncTask asyncTask, boolean testMode) {
		context_ = context;
		dbHelper_ = ((MainActivity)context).getDatabaseHelper();
		baseCurrency_ = baseCurrency;
		report_ = report;
		asyncTask_ = asyncTask;
		testMode_ = testMode;
	}
	
	/**
	 * process only if last update is expiry. save the lastTime updating if processUpdate successfully returned.
	 * this is the entry point of the updating agent.
	 */
	public void buildCache() {
		if (isDumped()) {
			return;
		}
		if (report_.isCacheFull()) { //all the rates has been cached by other agents
			return;
		}
		try
		{
			importToCache();
		}
		catch (Exception ex)
		{
			String msg = String.format("Oops! error from %s agent: %s", getName(), ex.getMessage()); //TODO multi-language
			report_.add(report_.new ReportEntry(MessageType.ERROR, 
					msg,  
					Log.getStackTraceString(ex)));
			
			Log.d("CURR", msg + Log.getStackTraceString(ex)); //TODO remove it
		}
	}

	/**
	 * return true if update success. you must download data, parse it and call updateDatabse here.
	 * if one of the row is not imported, it must return false
	 */
	protected abstract boolean importToCache();

	/**
	 * must return a unique string per agent. It can be the class name, but I might refactoring the class name!
	 */
	protected abstract String getName();
	
	/**
	 * update the rate of a currency into local database and report the update result
	 */
	protected boolean importToCache(String currencyCode, String rate) {
		
		try
		{
			return putToCache(currencyCode, MathEval.parseDouble(rate));
		}
		catch (Exception ex)
		{
			String msg = String.format("[%s %s] Failed to set '%s' rate to '%s': %s", //TODO multi-language
						getName(), baseCurrency_.getShortName(), currencyCode, rate, ex.getMessage()
					);
			report_.add(report_.new ReportEntry(MessageType.WARNING, msg));
			Log.d("CURR", msg);
			return false;
		}
	}
	
	/**
	 * update the rate of a currency into local database and report the update result
	 */
	protected boolean putToCache(String currencyCode, double rate) {
		
		try
		{
			if (rate<=0) {
				throw new InvalidParameterException("Invalid rate");
			}
			if (TextUtils.isEmpty(currencyCode)) {
				throw new InvalidParameterException("invalide currency code");
			}
			if (report_.isCached(currencyCode)) { //already reported by other agent(s)
				return true;
			}
			
			if (testMode_) {
				if (TextUtils.isEmpty(currencyCode) || rate<=0) {
					throw new InvalidParameterException("CurrencyCode = null or rate <= 0");
				}
				Log.v("CURR", getName()+" "+currencyCode + " / "+rate);
			}
			else {
				checkAndPutToCache(currencyCode, rate);
			}
			
			return true;
		}
		catch (Exception ex)
		{
			String msg = String.format("[%s %s] Failed to set '%s' rate to '%s': %s", //TODO multi-language
						getName(), baseCurrency_.getShortName(), currencyCode, rate, ex.getMessage()
					);
			report_.add(report_.new ReportEntry(MessageType.WARNING, msg));
			Log.d("CURR", msg);
			return false;
		}
	}
	
	private boolean checkAndPutToCache(String targetCurrencyCode, double rate)
	{
		//get targetCurrency
		Unit targetCurrency = null;
		{
			Cursor cur = dbHelper_.getReadableDatabase().query("unit", null,
					"categoryId=11 AND shortName=?", new String[] {
						targetCurrencyCode },
					null, null, null, null);
			try
			{
				if (cur.moveToNext())
				{
					targetCurrency = Unit.parseCursor(dbHelper_, cur);
				}
			}
			finally
			{
				cur.close();
			}
		}
		
		if (targetCurrency == null)
			return false;
		
		//check if the conversion exits
		boolean conversionExist = false;
		{
			Cursor cur = dbHelper_.getReadableDatabase().query("conversion", null,
					"base=? AND target=?", 
					new String[] {Long.toString(baseCurrency_.getId()), Long.toString(targetCurrency.getId()) },
					null, null, null, null);
			try {
				conversionExist = cur.getCount()>0;
			}
			finally
			{
				cur.close();
			}
		}
		
		report_.cacheUpdate(targetCurrencyCode, 
				report_.new UpdateItem(
						conversionExist ? OperationType.UPDATE : OperationType.INSERT,
						baseCurrency_.getId(),
						targetCurrency.getId(),
						rate
						));
		
		return true;
	}

	public boolean isDumped()
	{
		return asyncTask_.isCancelled();
	}
	
	/* Toolset */
	
	protected InputStream getStreamFromUrl2(String address) throws IOException {
		URL url = new URL(address);
		httpConnection_ = (HttpURLConnection)url.openConnection();
		int responseCode = httpConnection_.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK)
			return httpConnection_.getInputStream();
		else
			throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
	}

	protected InputStream getStreamFromUrl(String address) throws IllegalStateException, IOException
	{
		httpGet_ = new HttpGet(address);
		HttpClient httpclient = new DefaultHttpClient();
		
		// Execute HTTP Get Request
		HttpResponse response = httpclient.execute(httpGet_);
		return response.getEntity().getContent();
	}
	
	static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	static void skip(XmlPullParser parser)
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
	
	/**
	 * return value from 
	 * "<field>value</field>"
	 */
	static String readContentTag(XmlPullParser parser, String ns, String tagName) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, tagName);
	    String resu = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, tagName);
	    return resu;
	}
	
	static void assertEqual(int x, int expectedX) {
		if (x != expectedX)
			throw new ParseException("Got "+x+" but expected "+expectedX);
	}
	static void assertEqual(String x, String expectedX) {
		if (x == expectedX)
			throw new ParseException("Got "+x+" but expected "+expectedX);
	}
}
