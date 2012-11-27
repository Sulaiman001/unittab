package dh.sunicon;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.workarounds.MyApplication;

public class MainActivity extends FragmentActivity implements
		TabHost.OnTabChangeListener
{
	public static final int DEFAULT_PRECISION = 6;
	public static final int DEFAULT_PRECISION_INT = 6;
	public static final boolean DEFAULT_CURRENCY_USD_ONLY = false;
	public static final boolean DEFAULT_STRICTMODE = false;
	public static final long DEFAULT_CURRENCY_EXPIRY_TIME = 86400000L;
	public static final String OPTNAME_CURRENCY_USD_ONLY = "CurrencyLiveUpdateUSDOnly";
	public static final String OPTNAME_CURRENCY_EXPIRY_TIME = "CurrencyLiveUpdateExpiryTime";
	public static final String OPTNAME_CURRENCY_LIVE_UPDATE = "CurrencyLiveUpdateOption";
	public static final String OPTNAME_PRECISION = "Precision";
	public static final String OPTNAME_PRECISION_INT = "PrecisionInt";
	public static final String OPTNAME_STRICTMODE = "StrictMode";
	public static int OPT_NEVER = 2;
	public static int OPT_WIFI_ONLY = 1;
	public static int OPT_ALL_NETWORK = 0;
	
	private static final String TAG = MainActivity.class.getName();
	private TabHost tabHost_;
	private DatabaseHelper dbHelper_;
	private int currentTabTag_;
	private SharedPreferences preferences_;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		dbHelper_ = new DatabaseHelper(this);
		setContentView(R.layout.sunicon_main_tabhosts);
		
		preferences_ = getPreferences(Activity.MODE_PRIVATE);
		
		/* init tabHost */
		
		tabHost_ = (TabHost) findViewById(android.R.id.tabhost);
		//tabHost_.getTabWidget().setDividerDrawable(R.drawable.tab_divider);
		tabHost_.setup();

		final String conversionTag = Integer.toString(R.id.converterTab);
		final String conversionIndicator = "Converter"; // TODO: multi language
		final TabSpec converterTabSpec = tabHost_.newTabSpec(conversionTag).
				setIndicator(createTabLabelView(conversionIndicator)).setContent(R.id.converterTab);
		
		final String explorerTag = Integer.toString(R.id.explorerTab);
		final String explorerIndicator = "Explorer"; // TODO: multi language
		final TabSpec explorerTabSpec = tabHost_.newTabSpec(explorerTag).
				setIndicator(createTabLabelView(explorerIndicator)).setContent(R.id.explorerTab);

		final String settingTag = Integer.toString(R.id.settingTab);
		final TabSpec settingTabSpec = tabHost_.newTabSpec(settingTag).
				setIndicator(createTabImageView()).setContent(R.id.settingTab);
		
		tabHost_.addTab(converterTabSpec);
		tabHost_.addTab(explorerTabSpec);
		tabHost_.addTab(settingTabSpec);
		
		onTabChanged(conversionTag);
		
		tabHost_.setOnTabChangedListener(this);
	}
	
	private View createTabLabelView(String textIndicator)
	{
		View indicatorView = getLayoutInflater().inflate(R.layout.tab_label_indicator, null);
		TextView label = (TextView)indicatorView.findViewById(R.id.label);
		label.setText(textIndicator);
//		indicatorView.setText(textIndicator);
		return indicatorView;
	}
	private View createTabImageView()
	{
		View indicatorView = getLayoutInflater().inflate(R.layout.tab_image_indicator, null);
		return indicatorView;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString("currentTabTag", Integer.toString(currentTabTag_));
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		String currentTabTag = savedInstanceState.getString("currentTabTag");
		onTabChanged(currentTabTag);
		tabHost_.setCurrentTabByTag(currentTabTag);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
	 */
	public void onTabChanged(String tag)
	{
		try 
		{
			int placeHolder = Integer.parseInt(tag);
			FragmentManager fm = getSupportFragmentManager();
			
			/* save setting */
			
			SettingFragment settingFragment = (SettingFragment) fm.findFragmentById(R.id.settingTab);
			ConverterFragment converterFragment = (ConverterFragment) fm.findFragmentById(R.id.converterTab);;
			if (settingFragment!=null && currentTabTag_ == R.id.settingTab) {
				settingFragment.savePrefs();
				if (converterFragment!=null) {
					converterFragment.onPreferencesChanged();
				}
			}
			
			/*switch to tab*/
			
			Fragment fg = fm.findFragmentByTag(tag);
			
	        if (fg == null) 
	        {
	        	switch (placeHolder) {
	        		case R.id.converterTab:
	        			fg = new ConverterFragment();
	        			break;
	        		case R.id.explorerTab:
	        			fg = new ExplorerFragment();
	        			break;
	        		case R.id.settingTab:
	        			fg = new SettingFragment();
	        			break;
	        		default: throw new UnsupportedOperationException();
	        	}
	        	        	
	            fm.beginTransaction()
	                    .replace(placeHolder, fg, tag)
	                    .commit();
	        }
	        
	        currentTabTag_ = placeHolder;
		}
		catch (Exception ex) {
			showError(ex);
		}
	}

	public void showError(Exception ex)
	{
		if (preferences_.getBoolean(OPTNAME_STRICTMODE, DEFAULT_STRICTMODE)) {
			MyApplication.showErrorDialog(getSupportFragmentManager(), null, ex);
		}
		else {
			Log.w(TAG, ex);
		}
	}
	
	public DatabaseHelper getDatabaseHelper()
	{
		return dbHelper_;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ConverterFragment.UNIT_PICKER_REQUEST && resultCode == RESULT_OK) {

			long categoryId = data.getLongExtra("categoryId", -1);
			long unitId = data.getLongExtra("unitId", -1);
			String categoryName = data.getStringExtra("categoryName");
			String unitName = data.getStringExtra("unitName");
			
			/* save to preferences */
			
			SharedPreferences.Editor editor = preferences_.edit(); 
			editor.putLong("categoryId", categoryId); 
			editor.putLong("baseUnitId", unitId);
			editor.putString("categoryName", categoryName);
			editor.putString("baseUnitName", unitName);
			editor.commit();
			
			/* set on interfaces */
			
			FragmentManager fm = getSupportFragmentManager();
			ConverterFragment convfg = (ConverterFragment)(fm.findFragmentByTag(Integer.toString(R.id.converterTab)));
			try {
				convfg.setBaseUnit(categoryName, unitName, 
						categoryId, unitId);
			} catch (IllegalAccessException e) {
				Log.w(TAG, e);
			}
			
		}
	}
	
	@Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	try
    	{
    		dbHelper_.close();
    	}
    	catch (Exception ex)
    	{
    		Log.w(TAG, ex);
    	}
    }
	
	public NetworkInfo getNetworkInfo() {
	    ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
	    return cm.getActiveNetworkInfo();
	}
	
	
	public SharedPreferences getPreferences()
	{
		return preferences_;
	}
	
	/**
	 * Simulate a thread of long operation
	 * @param minSecond
	 * @param maxSecond
	 */
	public static void simulateLongOperation(int minSecond, int maxSecond)
	{
		Random rand = new Random(System.currentTimeMillis());
		long timeToSleep = (rand.nextInt(maxSecond-minSecond)+minSecond)*1000;
		
		try
		{
			Thread.sleep(timeToSleep);
		}
		catch (InterruptedException e)
		{
			Log.w("SimulationQuery", e);
		}
	}
}
