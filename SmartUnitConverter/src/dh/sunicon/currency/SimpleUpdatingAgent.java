package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import dh.sunicon.currency.UpdatingReport.MessageType;
import dh.sunicon.datamodel.Unit;

/**
 * Download data (often XML) from an URL, parse and import it to the database
 */
public abstract class SimpleUpdatingAgent extends UpdatingAgent
{	
	//private static final String TAG = SimpleUpdatingAgent.class.getName();
	
	public SimpleUpdatingAgent(Activity context, Unit baseCurrency,
			UpdatingReport report, AsyncTask asyncTask) {
		super(context, baseCurrency, report, asyncTask);
	}
	public SimpleUpdatingAgent(Activity context, Unit baseCurrency,
			UpdatingReport report, AsyncTask asyncTask, boolean testMode) {
		super(context, baseCurrency, report, asyncTask, testMode);
	}
	
	
	@Override
	protected boolean importToCache() {
		InputStream inputStream = null;
		
		try
		{
			inputStream = getStreamFromUrl(getURL());
		}
		catch (Exception ex)
		{
			report_.add(report_.new ReportEntry(MessageType.ERROR, 
					"Network problem.",  //TODO multi-language
					Log.getStackTraceString(ex)));
			return false;
		}
		
		try
		{
			return parseAndImportToCache(inputStream);
		}
		catch (Exception ex)
		{
			report_.add(report_.new ReportEntry(MessageType.ERROR, 
					"Update failed: "+ex.getMessage(),  //TODO multi-language
					Log.getStackTraceString(ex)));
			return false;
		}
		finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				report_.add(report_.new ReportEntry(MessageType.WARNING, 
						"Oops: "+e.getMessage()  //TODO multi-language
						));
				return false;
			}
		}
	}
	
	/**
	 * return true only if all row is successfully updated 
	 */
	protected abstract boolean parseAndImportToCache(InputStream inputStream) throws IOException, XmlPullParserException;

	/**
	 * must return a unique string per agent. It can be the class name, but I might refactoring the class name!
	 */
	protected abstract String getName();
	protected abstract String getURL();
}
