package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.runnable.ConversionsLoadingRunner;

/**
 * update 160+ currencies with base USD
 */
public class TmcRatesImporter extends RatesImporter
{	
	//private static final String TAG = YahooRatesImporter.class.getName();
	private static final String ns = null;

	public TmcRatesImporter(DatabaseHelper dbHelper, long baseCurrencyUnitId, ImportationReport report)
	{
		super(dbHelper, baseCurrencyUnitId, report);
	}
	
	public TmcRatesImporter(DatabaseHelper dbHelper, ImportationReport report)
	{
		super(dbHelper, ConversionsLoadingRunner.USD_UNIT, report);
	}
	
	protected void importFrom(InputStream inputStream) throws IOException, XmlPullParserException 
	{
		try
		{
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag(); //point to the the root tag <list>
            
            parser.nextTag(); //skip the START_TAG <list> and point to the START_TAG <meta>
            parser.require(XmlPullParser.START_TAG, ns, "meta"); //check meet the START_TAG <meta>
            skip(parser); //skip node <meta>..</meta>
            parser.nextTag();
            
            readResources(parser);
		}
		finally {
			inputStream.close();
		}
	}
	
	private boolean readResources(XmlPullParser parser) throws XmlPullParserException, IOException 
	{
	    parser.require(XmlPullParser.START_TAG, ns, "resources");
	    
	    while (parser.next() != XmlPullParser.END_TAG && !isDumped()) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the entry tag
	        if (name.equals("resource")) {
	            readRateEntry(parser);
	        } else {
	            skip(parser);
	        }
	    }  
	    return true;
	}
	
	private void readRateEntry(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "resource");
		
		String currencyCode = null;
	    String rateStr = null;
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        if (!parser.getName().equals("field")){
	        	continue;
	        }
	        
	        if (parser.getAttributeValue(ns, "name").equals("symbol")) {
	        	currencyCode = extractCurrencyCode(readField(parser));
	        }
	        else if (parser.getAttributeValue(ns, "name").equals("price")) {
	        	rateStr = readField(parser);
	        }
	        else {
	        	skip(parser);
	        }
	    }
	    
	    updateDatabase(currencyCode, rateStr);
	}

	private String readField(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "field");
	    String resu = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "field");
	    return resu;
	}
	
	/**
	 * Input KRW=X
	 * return KRW
	 */
	private String extractCurrencyCode(String rawCode) {
		return rawCode.substring(0,3);
	}

	@Override
	protected String getImporterName()
	{
		return "TMC";
	}
}
