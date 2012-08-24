package dh.sunicon;

import java.util.Random;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends ListActivity {

	private DatabaseHelper dbHelper_;
	private UnitsCursorAdapter unitsCursorAdapter_;
	private TextView categoryLabel_;
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
		
        baseUnitEditor_.setAdapter(unitsCursorAdapter_);
        baseUnitEditor_.setThreshold(1);
        
        setListAdapter(resultListAdapter_);
        
        clearBaseUnit(false);
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
	}

	public void onSelectBaseUnit(UnitAutoCompleteView sender, String categoryName,
			String unitName, long categoryId, long unitId)
	{
		categoryLabel_.setVisibility(View.VISIBLE);
		categoryLabel_.setText(categoryName);
		categoryId_ = categoryId;
		baseUnitId_ = unitId;
		targetUnitFilterEditor_.setEnabled(true);
		resultListAdapter_.populateData(categoryId_, baseUnitId_);
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
