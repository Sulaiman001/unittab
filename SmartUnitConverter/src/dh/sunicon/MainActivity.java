package dh.sunicon;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends FragmentActivity implements
		TabHost.OnTabChangeListener
{
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
}
