package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
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
			UpdatingReport report) {
		super(context, baseCurrency, report);
	}
	public SimpleUpdatingAgent(Activity context, Unit baseCurrency,
			UpdatingReport report, boolean testMode) {
		super(context, baseCurrency, report, testMode);
	}
	
	
	@Override
	protected boolean processUpdate() {
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
			return processUpdate(inputStream);
		}
		catch (Exception ex)
		{
			report_.add(report_.new ReportEntry(MessageType.ERROR, 
					"Update failed.",  //TODO multi-language
					Log.getStackTraceString(ex)));
			return false;
		}
		finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				report_.add(report_.new ReportEntry(MessageType.WARNING, 
						"Somthing wrong: "+e.getMessage()  //TODO multi-language
						));
				return false;
			}
		}
	}
	
	/**
	 * return true only if all row is successfully updated 
	 */
	protected abstract boolean processUpdate(InputStream inputStream) throws IOException, XmlPullParserException;

	/**
	 * must return a unique string per agent. It can be the class name, but I might refactoring the class name!
	 */
	protected abstract String getName();
	protected abstract String getURL();
}
