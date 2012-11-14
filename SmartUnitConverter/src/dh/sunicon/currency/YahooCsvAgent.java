package dh.sunicon.currency;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.Unit;

/**
 * update 160+ currencies with base USD
 * this Importer has its own expiry time
 */
public class YahooCsvAgent extends SimpleUpdatingAgent
{	
	//private static final String TAG = YahooRatesImporter.class.getName();

	public YahooCsvAgent(Activity context, Unit baseCurrency, UpdatingReport report, AsyncTask asyncTask) {
		super(context, baseCurrency, report, asyncTask);
	}
	public YahooCsvAgent(Activity context, Unit baseCurrency, UpdatingReport report, AsyncTask asyncTask, boolean testMode) {
		super(context, baseCurrency, report, asyncTask, testMode);
	}
	
	protected boolean parseAndImportToCache(InputStream inputStream) throws IOException, XmlPullParserException 
	{
		Scanner sc = new Scanner(inputStream).useDelimiter("\n");
		boolean allSuccess = false;
		try {
			while (sc.hasNext() && !isDumped()) {
				if (!importRow(sc.next())) {
					allSuccess = false;
				}
			}
		}
		finally {
			sc.close();
		}
		return allSuccess;
	}
	
	private boolean importRow(String row) {
		Scanner sc = new Scanner(row).useDelimiter(",").useLocale(Locale.US);
		try {
			String code = extractCurrencyCode(sc.next());
			double rate = sc.nextDouble();
			putToCache(code, rate);
		}
		finally {
			sc.close();
		}
		return false;
	}

	/**
	 * Input "EURKRW=X" (incluse the double-quotes)
	 * return KRW
	 */
	private String extractCurrencyCode(String rawCode) {
		return rawCode.substring(4,7);
	}

	@Override
	protected String getName() {
		return "YahooCSV";
	}

	@Override
	protected String getURL() {
		String params = "";
		Cursor cur = dbHelper_.getReadableDatabase().query("unit",
				new String[]{"shortName"}, 
				"categoryId=? AND id<>?", 
				new String[] {Long.toString(Category.CURRENCY_CATEGORY), Long.toString(baseCurrency_.getId())}, 
				null, null, null);
		try {
			while (cur.moveToNext())
			{
				//append +EURKRW=X
				
				if (!params.isEmpty()) {
					params = params + "+";
				}
				String code = cur.getString(0);
				params = params + baseCurrency_.getShortName()+code+"=X";
			}
		}
		finally {
			cur.close();
		}
		String resu = "http://finance.yahoo.com/d/quotes.csv?f=sl1d1t1&s="+params;
		if (testMode_) {
			Log.v("CURR", resu);
		}
		return resu;
	}
}
