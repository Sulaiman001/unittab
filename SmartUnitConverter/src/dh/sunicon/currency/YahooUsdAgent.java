package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.util.Xml;
import dh.sunicon.datamodel.Unit;

/**
 * update 160+ currencies with base USD
 * this Importer has its own expiry time
 */
public class YahooUsdAgent extends SimpleUpdatingAgent
{	
	//private static final String TAG = YahooRatesImporter.class.getName();
	private static final String ns = null;

	public YahooUsdAgent(Activity context, Unit baseCurrency, UpdatingReport report)
	{
		this(context, baseCurrency, report, false);
	}
	public YahooUsdAgent(Activity context, Unit baseCurrency, UpdatingReport report, boolean testMode)
	{
		super(context, baseCurrency, report, testMode);
		if (baseCurrency.getId()!=Unit.USD_UNIT) {
			throw new InvalidParameterException();
		}
	}
	
	protected boolean processUpdate(InputStream inputStream) throws IOException, XmlPullParserException 
	{
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(inputStream, null);
        parser.nextTag(); //point to the the root tag <list>
        
        parser.nextTag(); //skip the START_TAG <list> and point to the START_TAG <meta>
        parser.require(XmlPullParser.START_TAG, ns, "meta"); //check meet the START_TAG <meta>
        skip(parser); //skip node <meta>..</meta>
        parser.nextTag();
        return readResources(parser);
	}
	
	private boolean readResources(XmlPullParser parser) throws XmlPullParserException, IOException 
	{
	    parser.require(XmlPullParser.START_TAG, ns, "resources");
	    boolean allSuccess = true;
	    
	    while (parser.next() != XmlPullParser.END_TAG && !isDumped()) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the entry tag
	        if (name.equals("resource")) {
	        	if (!readRateEntry(parser)) {
	        		allSuccess = false;
	        	}
	        } else {
	            skip(parser);
	        }
	    }  
	    
	    return allSuccess;
	}
	
	private boolean readRateEntry(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "resource");
		
		String currencyCode = null;
	    String rateStr = null;
	    while (parser.next() != XmlPullParser.END_TAG && !isDumped()) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        if (!parser.getName().equals("field")){
	        	continue;
	        }
	        
	        if (parser.getAttributeValue(ns, "name").equals("symbol")) {
	        	currencyCode = extractCurrencyCode(UpdatingAgent.readContentTag(parser, ns, "field"));
	        }
	        else if (parser.getAttributeValue(ns, "name").equals("price")) {
	        	rateStr = UpdatingAgent.readContentTag(parser, ns, "field");
	        }
	        else {
	        	skip(parser);
	        }
	    }
	    
	    if (isDumped()) {
	    	return false;
	    }
	    
	    return updateDatabase(currencyCode, rateStr);
	}
	
	/**
	 * Input KRW=X
	 * return KRW
	 */
	private String extractCurrencyCode(String rawCode) {
		return rawCode.substring(0,3);
	}

	@Override
	protected String getName() {
		return "YahooUSD";
	}

	@Override
	protected String getURL() {
		return "http://finances.yahoo.com/webservice/v1/symbols/allcurrencies/quotes";
	}
}
