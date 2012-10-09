package dh.sunicon;

import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends FragmentActivity implements
		TabHost.OnTabChangeListener
{
	private TabHost tabHost_;
	private HashMap<String, TabInfo> tabsInfo_ = new HashMap<String, MainActivity.TabInfo>();
	private TabInfo lastTab_ = null;
	private DatabaseHelper dbHelper_;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		dbHelper_ = new DatabaseHelper(this);
		setContentView(R.layout.sunicon_main_tabhosts);
		initTabHost(savedInstanceState);
		if (savedInstanceState != null)
		{
			tabHost_.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putString("tab", tabHost_.getCurrentTabTag()); // save the tab selected
		super.onSaveInstanceState(outState);
	}

	/**
	 * Initialise the Tab Host
	 */
	private void initTabHost(Bundle args)
	{
		tabHost_ = (TabHost) findViewById(android.R.id.tabhost);
		tabHost_.setup();
		
		final String conversionTag = "Converter";
		final String conversionIndicator = "Converter"; //TODO: multi language
        {
	        TabInfo tabInfo = new TabInfo(conversionTag, ConverterFragment.class, args);
	        MainActivity.addTab(
	        		this, 
	        		tabHost_, 
	        		tabHost_.newTabSpec(conversionTag).setIndicator(conversionIndicator), 
	        		tabInfo 
	        		);
	        tabsInfo_.put(tabInfo.tag_, tabInfo);
        }
        
        final String explorerTag = "Explorer";
        final String explorerIndicator = "Explorer"; //TODO: multi language
        {
	        TabInfo tabInfo = new TabInfo(explorerTag, ExplorerFragment.class, args);
	        MainActivity.addTab(
	        		this, 
	        		tabHost_, 
	        		tabHost_.newTabSpec(explorerTag).setIndicator(explorerIndicator), 
	        		tabInfo 
	        		);
	        tabsInfo_.put(tabInfo.tag_, tabInfo);
        }
        
        // Default to first tab
        onTabChanged(conversionTag);

        tabHost_.setOnTabChangedListener(this);
		
//		TabInfo tabInfo = null;
//		TabsFragmentActivity.addTab(this, this.tabHost_, this.tabHost_
//				.newTabSpec("Tab1").setIndicator("Tab 1"),
//				(tabInfo = new TabInfo("Tab1", Tab1Fragment.class, args)));
//		this.mapTabInfo_.put(tabInfo.tag_, tabInfo);
//		TabsFragmentActivity.addTab(this, this.tabHost_, this.tabHost_
//				.newTabSpec("Tab2").setIndicator("Tab 2"),
//				(tabInfo = new TabInfo("Tab2", Tab2Fragment.class, args)));
//		this.mapTabInfo_.put(tabInfo.tag_, tabInfo);
//		TabsFragmentActivity.addTab(this, this.tabHost_, this.tabHost_
//				.newTabSpec("Tab3").setIndicator("Tab 3"),
//				(tabInfo = new TabInfo("Tab3", Tab3Fragment.class, args)));
//		this.mapTabInfo_.put(tabInfo.tag_, tabInfo);
//		// Default to first tab
//		this.onTabChanged("Tab1");
//		//
//		tabHost_.setOnTabChangedListener(this);
	}

	/**
	 * @param activity
	 * @param tabHost
	 * @param tabSpec
	 * @param clss_
	 * @param args_
	 */
	private static void addTab(MainActivity activity, TabHost tabHost,
			TabHost.TabSpec tabSpec, TabInfo tabInfo)
	{
		// Attach a Tab view factory to the spec
		tabSpec.setContent(activity.new TabFactory(activity));
		String tag = tabSpec.getTag();

		// Check to see if we already have a fragment for this tab, probably
		// from a previously saved state. If so, deactivate it, because our
		// initial state is that a tab isn't shown.
		tabInfo.fragment_ = activity.getSupportFragmentManager()
				.findFragmentByTag(tag);
		
		if (tabInfo.fragment_ != null && !tabInfo.fragment_.isDetached())
		{
			FragmentTransaction ft = activity.getSupportFragmentManager()
					.beginTransaction();
			ft.detach(tabInfo.fragment_);
			ft.commit();
			activity.getSupportFragmentManager().executePendingTransactions();
		}

		tabHost.addTab(tabSpec);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
	 */
	public void onTabChanged(String tag)
	{
		TabInfo newTab = this.tabsInfo_.get(tag);
		if (lastTab_ != newTab)
		{
			FragmentTransaction ft = this.getSupportFragmentManager()
					.beginTransaction();
			if (lastTab_ != null)
			{
				if (lastTab_.fragment_ != null)
				{
					ft.detach(lastTab_.fragment_);
				}
			}
			if (newTab != null)
			{
				if (newTab.fragment_ == null)
				{
					newTab.fragment_ = Fragment.instantiate(this,
							newTab.clss_.getName(), newTab.args_);
					ft.add(R.id.realtabcontent, newTab.fragment_, newTab.tag_);
				}
				else
				{
					ft.attach(newTab.fragment_);
				}
			}

			lastTab_ = newTab;
			ft.commit();
			this.getSupportFragmentManager().executePendingTransactions();
		}
	}

	public DatabaseHelper getDatabaseHelper()
	{
		return dbHelper_;
	}
	
	private class TabInfo
	{
		private String tag_;
		private Class<?> clss_;
		private Bundle args_;
		private Fragment fragment_;
	
		TabInfo(String tag, Class<?> clazz, Bundle args)
		{
			this.tag_ = tag;
			this.clss_ = clazz;
			this.args_ = args;
		}
	
	}

	class TabFactory implements TabContentFactory
	{
		private final Context context_;
	
		public TabFactory(Context context)
		{
			context_ = context;
		}
	
		@Override
		public View createTabContent(String tag)
		{
			View v = new View(context_);
			v.setMinimumWidth(0);
			v.setMinimumHeight(0);
			return v;
		}
	
	}
}
