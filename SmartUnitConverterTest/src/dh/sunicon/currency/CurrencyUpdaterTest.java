package dh.sunicon.currency;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import dh.sunicon.ConverterFragment;
import dh.sunicon.MainActivity;
import dh.sunicon.R;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.Unit;

public class CurrencyUpdaterTest extends ActivityInstrumentationTestCase2<MainActivity> {
	static final String TAG = CurrencyUpdaterTest.class.getName();
	
	ConverterFragment converterFragment_;

	public CurrencyUpdaterTest() {
		super(MainActivity.class);
	}

	public void setUp() {
		FragmentManager fm = this.getActivity().getSupportFragmentManager();
		converterFragment_ = (ConverterFragment)fm.findFragmentByTag(Integer.toString(R.id.converterTab));
	}

//	public void testImportFromYahooUsd() {
//		Unit usdCurrency = Unit.findById(converterFragment_.getDatabaseHelper(), Unit.USD_UNIT);
//		UpdatingReport report = new UpdatingReport();
//		UpdatingAgent yahooUsdAgent = new YahooUsdAgent(this.getActivity(), usdCurrency, report, true);
//		yahooUsdAgent.processUpdate();
//		
//		Log.i("CURR", String.format("Updated %d / %d currencies", report.getUpdatedCurrencies().size(), DatabaseHelper.CURRENCY_COUNT));
//		Log.i("CURR", report.getContentMessage());
//		
//		//ConversionsLoadingRunner.USD_UNIT
//	}
	
//	public void testImportFromYahooCsv() {
//		long baseCurrencyId = Unit.USD_UNIT;
//		//long baseCurrencyId = 1400;
//		Unit baseCurrency = Unit.findById(converterFragment_.getDatabaseHelper(), baseCurrencyId);
//		UpdatingReport report = new UpdatingReport();
//		UpdatingAgent yahooCsvAgent = new YahooCsvAgent(this.getActivity(), baseCurrency, report, true);
//		yahooCsvAgent.processUpdate();
//		
//		Log.i("CURR", String.format("Updated %d / %d currencies", report.getUpdatedCurrencies().size(), DatabaseHelper.CURRENCY_COUNT));
//		Log.i("CURR", report.getContentMessage());
//		
//		assertEquals(true, report.successUpdateMostly());
//		//ConversionsLoadingRunner.USD_UNIT
//	}
	
//	public void testImportFromTmc() {
//		//long baseCurrencyId = Unit.USD_UNIT;
//		long baseCurrencyId = 1400;
//		Unit baseCurrency = Unit.findById(converterFragment_.getDatabaseHelper(), baseCurrencyId);
//		UpdatingReport report = new UpdatingReport();
//		UpdatingAgent tmcAgent = new TmcAgent(this.getActivity(), baseCurrency, report, true);
//		tmcAgent.processUpdate();
//		
//		Log.i("CURR", String.format("Updated %d / %d currencies", report.getUpdatedCurrencies().size(), DatabaseHelper.CURRENCY_COUNT));
//		Log.i("CURR", report.getContentMessage());
//		
//		assertEquals(true, report.getUpdatedCurrencies().size()>80);
//		//ConversionsLoadingRunner.USD_UNIT
//	}
	
	
	public void testImportRates() {
		
		Log.i("CURR", "Start test import rates");
		
		//long baseCurrencyId = Unit.USD_UNIT;
		long baseCurrencyId = 1400;
		Unit baseCurrency = Unit.findById(converterFragment_.getDatabaseHelper(), baseCurrencyId);
		
		UpdatingReport report = new UpdatingReport();
		
		
		UpdatingAgent yahooCsvAgent = new YahooCsvAgent(this.getActivity(), baseCurrency, report, null, true);
		yahooCsvAgent.importToCache();

		UpdatingAgent tmcAgent = new TmcAgent(this.getActivity(), baseCurrency, report, null, true);
		tmcAgent.importToCache();
		
		Unit usdCurrency = Unit.findById(converterFragment_.getDatabaseHelper(), Unit.USD_UNIT);
		UpdatingAgent yahooUsdAgent = new YahooUsdAgent(this.getActivity(), usdCurrency, report, null, true);
		yahooUsdAgent.importToCache();
		
		Log.i("CURR", report.getContentMessage());
		
		//assertEquals(true, report.isSuccessAll());
		//ConversionsLoadingRunner.USD_UNIT
	}

	public void testCreateURI() {
		Uri u = Uri.fromParts("mailto", "dph.sunicon@gmail.com", "abcdef ghi jk");
		Log.i("TEST", u.toString());
		
	}
	
//	@UiThreadTest
//	public void testCalculationComplex()
//	{
//		
//	}
}
