package dh.sunicon;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;
import dh.sunicon.UnitsCursorAdapter.SuggestionData;
import dh.sunicon.UnitsCursorSimpleTreeAdapter.OnChangeCursorListener;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitPicker3 extends FragmentActivity {
	private static final String TAG = UnitPicker3.class.toString();
	private static final int EVENT_DELAY = 200;
	
	private EditText filterEdit_;
	private ViewSwitcher listSwitcher_;
	private ListView flatList_;
	private ExpandableListView treeList_;
	private UnitsCursorAdapter unitCursorAdapter_;
	private UnitsCursorSimpleTreeAdapter unitCursorTreeAdapter_;
	private Button backButton_;
	private ToggleButton listToggle_;
	private Handler mainThread_;
	private DatabaseHelper dbHelper_;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate UnitPicker3");
        setContentView(R.layout.unit_picker3);
        dbHelper_ = new DatabaseHelper(this);
        listSwitcher_ = (ViewSwitcher) findViewById(R.id.listSwitcher);
        mainThread_ = new Handler();
        
        initFilterEdit();
        initBackButton();
        initListToggle();
        initTreeList();
        initFlatList();
        
        switchList(true, null);
    }

    private void initBackButton() {
    	backButton_ = (Button)findViewById(R.id.backButton);
    	backButton_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try
				{
					setResult(RESULT_CANCELED);
					finish();
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		});
    }
    
    private void initListToggle() {
    	listToggle_ = (ToggleButton)findViewById(R.id.listToggle);
    	listToggle_.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				//save preferences
				try
				{
					Log.d(TAG, "Save ListToggle to preferences");
					SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
					SharedPreferences.Editor editor = preferences.edit(); 
					editor.putBoolean("ListToggle", listToggle_.isChecked());
					editor.commit();
					
					switchList(isChecked, filterEdit_.getText());
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		});
    	
    	
    	SharedPreferences preferences = this.getPreferences(Activity.MODE_PRIVATE);
		if (preferences == null)
		{
			return;
		}
    	listToggle_.setChecked(preferences.getBoolean("ListToggle", true));
    }
    
	private void initFlatList() {
		flatList_ = (ListView)findViewById(R.id.flatList);
        unitCursorAdapter_ = new UnitsCursorAdapter(this, null, dbHelper_, false);
        flatList_.setAdapter(unitCursorAdapter_);
        
        flatList_.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				if (view!=null)
				{
					SuggestionData data = (SuggestionData)view.getTag();
					Intent resultIntent = new Intent();
					resultIntent.putExtra("unitName", data.getUnitName());
					resultIntent.putExtra("categoryName", data.getCategoryName());
					resultIntent.putExtra("unitId", data.getUnitId());
					resultIntent.putExtra("categoryId", data.getCategoryId());
					
					setResult(RESULT_OK, resultIntent);
					finish();
				}
			}
		});
	}

	private void initTreeList() {
		treeList_ = (ExpandableListView)findViewById(R.id.treeList);
//		unitCursorTreeAdapter_ = new UnitsCursorTreeAdapter(this, 
//				android.R.layout.simple_expandable_list_item_1,
//				new String[] { "categoryName" },
//	            new int[] { android.R.id.text1 },
//				android.R.layout.simple_expandable_list_item_1,  
//				new String[] { "unitName" },
//                new int[] { android.R.id.text1 });
		unitCursorTreeAdapter_ = new UnitsCursorSimpleTreeAdapter(this, 
				R.layout.category_item2,
				new String[] { "categoryName" },
	            new int[] { R.id.label },
				R.layout.unit_item2,  
				new String[] { "unitName" },
                new int[] { R.id.label });
		treeList_.setAdapter(unitCursorTreeAdapter_);
		
		treeList_.setOnChildClickListener(new OnChildClickListener() 
		{
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				try
				{
					Category cat = unitCursorTreeAdapter_.getCategoryByPosition(groupPosition);
					
					if (cat == null)
					{
						Log.w(TAG, "Unexpected situation: category not found");
						return false;
					}
					
					Intent resultIntent = new Intent();
					resultIntent.putExtra("unitName", ((TextView)v).getText());
					resultIntent.putExtra("categoryName", (CharSequence)(cat.getName()));
					resultIntent.putExtra("unitId", id);
					resultIntent.putExtra("categoryId", cat.getId());
					
					setResult(RESULT_OK, resultIntent);
					finish();
					return true;
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
				return false;
			}
		});
		
		unitCursorTreeAdapter_.setOnChangeCursorListener(new OnChangeCursorListener() {
			
			@Override
			public void onChangeCursor(Cursor c) {
				try
				{
					//expand all list if there are not many
					int count = c.getCount();
					if (count<70) {
						for (int i = 0; i < count; i++) {
//							Category cat = unitCursorTreeAdapter_.getCategoryByPosition(i);
//							if (cat!=null && cat.getId() == Category.CURRENCY_CATEGORY) {
//								continue; //Never expand the currency category
//							}
						    treeList_.expandGroup(i);
						}
					}
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		});
	}
	
	private void initFilterEdit() {	
		filterEdit_ = (EditText) findViewById(R.id.filterEdit);
        filterEdit_.addTextChangedListener(new TextWatcher() {
			
        	private Runnable lastRunnable_ = null;
        	
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(final Editable s) {
				
				try
				{
					//listSwitcher_.setVisibility(View.GONE);
					
					if (lastRunnable_ != null) {
						mainThread_.removeCallbacks(lastRunnable_);
					}
					
					lastRunnable_ = new Runnable() {
						@Override
						public void run() {
							try
							{
								switchList(listToggle_.isChecked(), s);
								//listSwitcher_.setVisibility(View.VISIBLE);
							}
							catch (Exception ex)
							{
								Log.w(TAG, ex);
							}
						}
					};
					
					mainThread_.postDelayed(lastRunnable_, EVENT_DELAY);
					
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		});
	}
	
	private void switchList(boolean toHistory, CharSequence constraint)
	{
		if (TextUtils.isEmpty(constraint) || toHistory)
		{
			if (unitCursorAdapter_!=null)
				unitCursorAdapter_.getFilter().filter(constraint);
			
			//show hsitoryList
			if (listSwitcher_.getNextView() == flatList_)
			{
				listSwitcher_.showNext();
			}
		}
		else
		{
			if (unitCursorTreeAdapter_!=null)
				unitCursorTreeAdapter_.getFilter().filter(constraint);
			
			//show expandableList
			if (listSwitcher_.getNextView() == treeList_)
			{
				listSwitcher_.showNext();
			}
		}
	}
//	
//	@Override
//	protected void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//		//outState.putBoolean("ListToggle", listToggle_.isChecked());
//		
//	}
	
//	@Override
//	protected void onRestoreInstanceState(Bundle savedInstanceState) {
//		super.onRestoreInstanceState(savedInstanceState);
//		//listToggle_.setChecked(savedInstanceState.getBoolean("ListToggle"));
//		SharedPreferences preferences = this.getPreferences(Activity.MODE_PRIVATE);
//		if (preferences == null)
//		{
//			return;
//		}
//		try
//		{
//			Log.d(TAG, "Restore ListToggle from Preferences");
//			listToggle_.setChecked(preferences.getBoolean("ListToggle", true));
//		}
//		catch (Exception ex)
//		{
//			Log.w(TAG, ex);
//		}
//	}
	
//	@Override
//	protected void onResume() {
//		super.onResume();
////		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
////		imm.showSoftInput(filterEdit_, InputMethodManager.SHOW_IMPLICIT);
//		
//		filterEdit_.requestFocus();
//	}

//	@Override
//	public void finish()
//	{
//		try
//    	{
//    		Log.d(TAG, "Close database of UnitPicker3");
//    		dbHelper_.close();
//    	}
//    	catch (Exception ex)
//    	{
//    		Log.w(TAG, ex);
//    	}
//		super.finish();
//	}
	
	@Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	try
    	{
    		Log.d(TAG, "Close database of UnitPicker3");
    		dbHelper_.close();
    	}
    	catch (Exception ex)
    	{
    		Log.w(TAG, ex);
    	}
    }

	public DatabaseHelper getDatabaseHelper()
	{
		return dbHelper_;
	}
}

