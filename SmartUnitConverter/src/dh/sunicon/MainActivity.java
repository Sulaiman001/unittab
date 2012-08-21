package dh.sunicon;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends Activity {

	private DatabaseHelper dbHelper;
	private UnitsCursorAdapter unitsCursorAdapter;
	private TextView categoryLabel;
	private UnitAutoCompleteView baseUnitEditor;
	private EditText targetUnitFilterEditor;
	private long baseUnitId = -1;
	private long categoryId = -1;
	//dh.sunicon.UnitAutoCompleteView:onReplaceTextListener = "base_"
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.dbHelper = new DatabaseHelper(this);
		final String initialQuery = UnitsCursorAdapter.queryPartSelect + UnitsCursorAdapter.queryPartLimit;
		final Cursor initialCursor = dbHelper.getReadableDatabase().rawQuery(initialQuery, null);
		this.unitsCursorAdapter = new UnitsCursorAdapter(this, initialCursor, true);
		
		setContentView(R.layout.sunicon_main);
		
		this.categoryLabel = (TextView)findViewById(R.id.categoryLabel);
		this.baseUnitEditor = (UnitAutoCompleteView)findViewById(R.id.baseUnitEditor);
		this.targetUnitFilterEditor = (EditText)findViewById(R.id.targetUnitFilterEditor);
        
        this.baseUnitEditor.setAdapter(unitsCursorAdapter);
        this.baseUnitEditor.setThreshold(1);
        
        clearBaseUnit(false);
        this.baseUnitEditor.setOnReplaceTextListener(new UnitAutoCompleteView.ReplaceTextListener()
		{
			@Override
			public void onReplaceText(UnitAutoCompleteView sender, String categoryName,
					String unitName, long categoryId, long unitId)
			{
				onSelectBaseUnit(sender, categoryName, unitName, categoryId, unitId);
			}
		});
        
        this.baseUnitEditor.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode == KeyEvent.KEYCODE_DPAD_UP && 
						keyCode == KeyEvent.KEYCODE_DPAD_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT && 
						keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
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
		this.categoryLabel.setVisibility(View.VISIBLE);
		this.categoryLabel.setText(categoryName);
		this.categoryId = categoryId;
		this.baseUnitId = unitId;
		this.targetUnitFilterEditor.setEnabled(true);
	}
	
	private void clearBaseUnit(boolean keepTextOnBaseUnitEditor)
	{
		this.categoryLabel.setVisibility(View.GONE);
		this.categoryId = -1;
		this.baseUnitId = -1;
		this.targetUnitFilterEditor.setEnabled(false);
		if (!keepTextOnBaseUnitEditor)
		{
			this.baseUnitEditor.setText(null);
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
		return this.dbHelper;
	}
	
	public UnitsCursorAdapter getUnitsCursorAdapter()
	{
		return this.unitsCursorAdapter;
	}
	
}
