package dh.sunicon;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitPicker2 extends Activity {

	private static String TAG = UnitPicker2.class.toString();
	private DatabaseHelper dbHelper_;
	private EditText categoryFilterEdit_;
	private EditText unitFilterEdit_;
	private ListView unitList_;
	private ListView categoryList_;
	private SimpleCursorAdapter categoryAdapter_;
	private SimpleCursorAdapter unitAdapter_;
	
	private long categoryId_ = -1;
	private CharSequence categoryName_ = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unit_picker2);
        dbHelper_ = new DatabaseHelper(this);
        
        initCategoryAdapter();
        initCategoryEditor();
        initCategoryList();
        
        initUnitAdapter();
        initUnitEditor();
        initUnitList();
    }
    
    @Override
    protected void onStart()
    {
    	super.onStart();
    	Log.d(TAG, "init categoryId = "+categoryId_);
		categoryId_ = getIntent().getLongExtra("categoryId", -1);
		categoryName_ = getIntent().getCharSequenceExtra("categoryName");
		unitAdapter_.getFilter().filter(null);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	outState.putLong("categoryId", categoryId_);
    	outState.putCharSequence("categoryName", categoryName_);
    	outState.putCharSequence("categoryFilter", categoryFilterEdit_.getText());
    	outState.putCharSequence("unitFilter", unitFilterEdit_.getText());
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	categoryId_ = savedInstanceState.getLong("categoryId");
    	categoryName_ = savedInstanceState.getCharSequence("categoryName");
    	
    	Log.d(TAG, "restore categoryId = "+categoryId_);
    	
    	CharSequence categoryFilter = savedInstanceState.getCharSequence("categoryFilter");
    	CharSequence unitFilter = savedInstanceState.getCharSequence("unitFilter"); 
    	categoryFilterEdit_.setText(categoryFilter);
    	unitFilterEdit_.setText(unitFilter);
    	categoryAdapter_.getFilter().filter(categoryFilter);
    	unitAdapter_.getFilter().filter(unitFilter);
    }
    
    private void initCategoryEditor()
    {
    	categoryFilterEdit_ = (EditText)findViewById(R.id.filterCategoryEditor);
    	categoryFilterEdit_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
				categoryAdapter_.getFilter().filter(s);
			}
		});
    }
    
    private void initUnitEditor()
    {
    	unitFilterEdit_ = (EditText)findViewById(R.id.filterUnitEditor);
    	unitFilterEdit_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
				unitAdapter_.getFilter().filter(s);
			}
		});
    }
    
    private void initCategoryAdapter()
    {
//    	Cursor categoryCursor = dbHelper_.getReadableDatabase().query(
//    			"category", 
//    			new String[]{"id as _id, name"}, 
//    			null, null, null, null, 
//    			"lower(name)", null);
    	categoryAdapter_ = new SimpleCursorAdapter(this, R.layout.category_item, null, new String[] {"name"}, new int[]{R.id.label}, 0);
    	categoryAdapter_.setFilterQueryProvider(new FilterQueryProvider()
		{
			@Override
			public Cursor runQuery(CharSequence constraint)
			{
				if (TextUtils.isEmpty(constraint))
				{
					return dbHelper_.getReadableDatabase().query(
			    			"category", 
			    			new String[]{"id as _id, name"}, 
			    			"enabled=1", 
			    			null, null, null, 
			    			"lower(name)", null);
				}
				return dbHelper_.getReadableDatabase().query(
						"category", 
						new String[]{"id as _id, name"}, 
						"enabled=1 AND lower(name) LIKE ?", 
						new String[]{"%" + constraint + "%"}, 
						null, null, 
						"lower(name)", null);
			}
		}); 
    	categoryAdapter_.getFilter().filter("");
    }
    
    private void initUnitAdapter()
    {
    	unitAdapter_ = new SimpleCursorAdapter(this, R.layout.unit_item, null, new String[] {"name"}, new int[]{R.id.label}, 0);
    	unitAdapter_.setFilterQueryProvider(new FilterQueryProvider()
		{
			@Override
			public Cursor runQuery(CharSequence constraint)
			{
				if (TextUtils.isEmpty(constraint))
				{
					return dbHelper_.getReadableDatabase().query(
			    			"unit", 
			    			new String[]{"id as _id, name"}, 
			    			"enabled=1 AND categoryId=?", 
			    			new String[]{Long.toString(categoryId_)}, 
			    			null, null, 
			    			"lower(name)", null);
				}
				return dbHelper_.getReadableDatabase().query(
						"unit", 
						new String[]{"id as _id, name"}, 
						"enabled=1 AND categoryId=? AND lower(name) LIKE ?", 
						new String[]{Long.toString(categoryId_),"%" + constraint + "%"}, 
						null, null, 
						"lower(name)", null);
			}
		});    	
    }
    
    private void initCategoryList()
    {
    	categoryList_ = (ListView)findViewById(R.id.categoryList);
    	categoryList_.setAdapter(categoryAdapter_);
    	categoryList_.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				categoryId_ = id;
				//categoryName_ = ((TextView)view.findViewById(R.id.label)).getText().toString();
				categoryName_ = ((TextView)view).getText().toString();
				unitAdapter_.getFilter().filter(null);
			}
		});
    }

    private void initUnitList()
    {
    	unitList_ = (ListView)findViewById(R.id.unitList);
    	unitList_.setAdapter(unitAdapter_);
    	unitList_.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				Intent resultIntent = new Intent();
				//resultIntent.putExtra("unitName", ((TextView)(view.findViewById(R.id.label))).getText());
				resultIntent.putExtra("unitName", ((TextView)view).getText());
				resultIntent.putExtra("categoryName", categoryName_);
				resultIntent.putExtra("unitId", id);
				resultIntent.putExtra("categoryId", categoryId_);
				
				setResult(RESULT_OK, resultIntent);
				//dbHelper_.close();
				finish();
			}
		});
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

}
