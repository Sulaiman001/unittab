package dh.sunicon;

import java.util.Random;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends ListActivity 
{
	static final String TAG = MainActivity.class.getName();
	private DatabaseHelper dbHelper_;
	private UnitsCursorAdapter unitsCursorAdapter_;
	private TextView categoryLabel_;
	private EditText baseValueEditor_;
	private UnitAutoCompleteView baseUnitEditor_;
	private EditText targetUnitFilterEditor_;
	private ResultListAdapter resultListAdapter_;
	
	private long baseUnitId_ = -1;
	private long categoryId_ = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dbHelper_ = new DatabaseHelper(this);
		final String initialQuery = UnitsCursorAdapter.SELECT_QUERY_PART + UnitsCursorAdapter.LIMIT_ORDER_QUERY_PART;
		final Cursor initialCursor = dbHelper_.getReadableDatabase().rawQuery(initialQuery, null);
		unitsCursorAdapter_ = new UnitsCursorAdapter(this, initialCursor, true);
		resultListAdapter_ = new ResultListAdapter(this);
		
		setContentView(R.layout.sunicon_main);
		
		categoryLabel_ = (TextView)findViewById(R.id.categoryLabel);
		baseUnitEditor_ = (UnitAutoCompleteView)findViewById(R.id.baseUnitEditor);
		targetUnitFilterEditor_ = (EditText)findViewById(R.id.targetUnitFilterEditor);
		baseValueEditor_= (EditText)findViewById(R.id.valueEditor);
		
        baseUnitEditor_.setAdapter(unitsCursorAdapter_);
        baseUnitEditor_.setThreshold(1);
        
        setListAdapter(resultListAdapter_);
        
        clearBaseUnit(false);
        initEvents();
	}

	private void initEvents()
	{
		baseUnitEditor_.setOnReplaceTextListener(new UnitAutoCompleteView.ReplaceTextListener()
		{
			@Override
			public void onReplaceText(UnitAutoCompleteView sender, String categoryName,
					String unitName, long categoryId, long unitId)
			{
				onSelectBaseUnit(sender, categoryName, unitName, categoryId, unitId);
			}
		});
        baseUnitEditor_.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_DPAD_UP && 
						keyCode != KeyEvent.KEYCODE_DPAD_DOWN && keyCode != KeyEvent.KEYCODE_DPAD_LEFT && 
						keyCode != KeyEvent.KEYCODE_DPAD_RIGHT && keyCode != KeyEvent.KEYCODE_ENTER &&
						keyCode != KeyEvent.KEYCODE_TAB)
				{
					clearBaseUnit(true);
				}
				return false;
			}
		});
        targetUnitFilterEditor_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				resultListAdapter_.getFilter().filter(s);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
        baseValueEditor_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				try
				{
					if (TextUtils.isEmpty(s))
					{
						resultListAdapter_.setBaseValue(Double.NaN);
					}
					else
					{
						double baseValue = Double.parseDouble(s.toString());
						resultListAdapter_.setBaseValue(baseValue);
					}
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex.toString());
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
	}

	public void onSelectBaseUnit(UnitAutoCompleteView sender, String categoryName,
			String unitName, long categoryId, long unitId)
	{
		categoryLabel_.setVisibility(View.VISIBLE);
		categoryLabel_.setText(categoryName);
		categoryId_ = categoryId;
		baseUnitId_ = unitId;
		targetUnitFilterEditor_.setEnabled(true);
		try
		{
			resultListAdapter_.populateData(categoryId_, baseUnitId_);
		}
		catch (IllegalAccessException e)
		{
			Log.w(TAG, e);
		}
	}
	
	private void clearBaseUnit(boolean keepTextOnBaseUnitEditor)
	{
		categoryLabel_.setVisibility(View.GONE);
		categoryId_ = -1;
		baseUnitId_ = -1;
		targetUnitFilterEditor_.setEnabled(false);
		if (!keepTextOnBaseUnitEditor)
		{
			baseUnitEditor_.setText(null);
		}
	}
	
	public void clearBaseUnitButton_Click(View v)
	{
		clearBaseUnit(false);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_lesson_one, menu);
		return true;
	}
	
	public DatabaseHelper getDatabaseHelper(){
		return dbHelper_;
	}
	
	public UnitsCursorAdapter getUnitsCursorAdapter()
	{
		return unitsCursorAdapter_;
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
			Log.wtf("SimulationQuery", e);
		}
	}
	
}
