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

public class MainActivity extends FragmentActivity implements
		TabHost.OnTabChangeListener
{
	private static final String TAG = MainActivity.class.getName();
	private TabHost tabHost_;
	private DatabaseHelper dbHelper_;
	private int currentTabTag_;
	private SettingFragment settingFragment_;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		dbHelper_ = new DatabaseHelper(this);
		setContentView(R.layout.sunicon_main_tabhosts);
		
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
		if (settingFragment_!=null && currentTabTag_ == R.id.settingTab) {
			settingFragment_.savePrefs();
		}
		
		//Toast.makeText(this, "onTabChanged: "+tag, Toast.LENGTH_SHORT).show();
		int placeHolder = Integer.parseInt(tag);
		
		FragmentManager fm = getSupportFragmentManager();
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
        			settingFragment_ = new SettingFragment();
        			fg = settingFragment_;
        			break;
        		default: throw new UnsupportedOperationException();
        	}
        	        	
            fm.beginTransaction()
                    .replace(placeHolder, fg, tag)
                    .commit();
        }
        
        currentTabTag_ = placeHolder;
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
			
			SharedPreferences preferences = this.getPreferences(Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit(); 
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
