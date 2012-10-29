package dh.sunicon;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends FragmentActivity implements
		TabHost.OnTabChangeListener
{
	private static final String TAG = MainActivity.class.getName();
	private TabHost tabHost_;
//	private HashMap<String, TabInfo> tabsInfo_ = new HashMap<String, MainActivity.TabInfo>();
//	private TabInfo lastTab_ = null;
	private DatabaseHelper dbHelper_;
	private String currentTabTag_;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		dbHelper_ = new DatabaseHelper(this);
		setContentView(R.layout.sunicon_main_tabhosts);
		
		/* init tabHost */
		
		tabHost_ = (TabHost) findViewById(android.R.id.tabhost);
		tabHost_.setup();
		
		final String conversionTag = Integer.toString(R.id.converterTab);
		final String conversionIndicator = "Converter"; // TODO: multi language
		final TabSpec converterTabSpec = tabHost_.newTabSpec(conversionTag).setIndicator(conversionIndicator).setContent(R.id.converterTab);
		
		final String explorerTag = Integer.toString(R.id.explorerTab);
		final String explorerIndicator = "Explorer"; // TODO: multi language
		final TabSpec explorerTabSpec = tabHost_.newTabSpec(explorerTag).setIndicator(explorerIndicator).setContent(R.id.explorerTab);

		tabHost_.addTab(converterTabSpec);
		tabHost_.addTab(explorerTabSpec);
		
		onTabChanged(conversionTag);
		
		tabHost_.setOnTabChangedListener(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString("currentTabTag", currentTabTag_);
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
		//Toast.makeText(this, "onTabChanged: "+tag, Toast.LENGTH_SHORT).show();
		int placeHolder = Integer.parseInt(tag);
		
		FragmentManager fm = getSupportFragmentManager();
		Fragment fg = fm.findFragmentByTag(tag);
		
        if (fg == null) 
        {
        	if (placeHolder == R.id.converterTab)
        	{
        		fg = new ConverterFragment();
        	}
        	else
        	{
        		fg = new ExplorerFragment();
        	}
        	
            fm.beginTransaction()
                    .replace(placeHolder, fg, tag)
                    .commit();
        }
        
        currentTabTag_ = tag;
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
}
