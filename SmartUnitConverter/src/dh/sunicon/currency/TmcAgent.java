package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.util.Log;
import android.util.Xml;
import dh.sunicon.datamodel.Unit;

/**
 * update 160+ currencies with base USD
 * this Importer has its own expiry time
 */
public class TmcAgent extends SimpleUpdatingAgent
{	
	//private static final String TAG = YahooRatesImporter.class.getName();
	private static final String ns = null;

	public TmcAgent(Activity context, Unit baseCurrency, UpdatingReport report) {
		super(context, baseCurrency, report);
	}
	public TmcAgent(Activity context, Unit baseCurrency, UpdatingReport report, boolean testMode) {
		super(context, baseCurrency, report, testMode);
	}
	
	protected boolean processUpdate(InputStream inputStream) throws IOException, XmlPullParserException 
	{
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(inputStream, null);
        parser.nextTag(); //point to the the root tag <rss>
        
        parser.nextTag(); //point to <channel>
        parser.nextTag(); //point to the START_TAG <title>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <link>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <description>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <lastBuildDate>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <language>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <copyright>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <docs>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <ttl>
        skip(parser);
        parser.nextTag(); //point to the START_TAG <image>
        skip(parser);
        
        boolean allSuccess = true; 
        while (parser.nextTag() != XmlPullParser.END_TAG && !isDumped()) {
        	if (!readItem(parser)) {
        		allSuccess = false;
        	}
        }
        return allSuccess;
	}
	
	private boolean readItem(XmlPullParser parser) throws XmlPullParserException, IOException 
	{
	    parser.require(XmlPullParser.START_TAG, ns, "item");
	    String code = null;
        String rate = null;
        
	    while (parser.next() != XmlPullParser.END_TAG && !isDumped()) {
	    	
	    	if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        if (parser.getName().equals("title")){
	        	code = extractCurrencyCode(UpdatingAgent.readContentTag(parser, ns, "title"));
	        }
	        else if (parser.getName().equals("description")){
	        	rate = extractCurrencyRate(UpdatingAgent.readContentTag(parser, ns, "description"));
	        }
	        else {
	        	skip(parser);
	        }
	    }
	    
	    if (isDumped()) {
	    	return false;
	    }
	    
	    return updateDatabase(code, rate);
	}
	
	/**
	 * Input KRW/USD
	 * return KRW
	 */
	private String extractCurrencyCode(String rawCode) {
		return rawCode.substring(0,3);
	}
	
	private static final Pattern PAT = Pattern.compile(".+= ([^ ]+) .+");
	
	/**
	 * Input: 1 Euro = 1,387.17400 South Korean Won
	 * return 1,387.17400
	 */
	private String extractCurrencyRate(String rawCode) {
		Matcher matcher = PAT.matcher(rawCode);
		if (matcher.find()) {
			return matcher.group(1);
		}
		else {
			throw new ParseException("Cannot extract currencyCode from '"+rawCode+"'");
		} 
	}

	@Override
	protected String getName() {
		return "Tmc";
	}

	@Override
	protected String getURL() {
		String resu = "http://themoneyconverter.com/rss-feed/"+baseCurrency_.getShortName()+"/rss.xml"; 
		if (testMode_) {
			Log.v("CURR", resu);
		}
		return resu;
	}
}
