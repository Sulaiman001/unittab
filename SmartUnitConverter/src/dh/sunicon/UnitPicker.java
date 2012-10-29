package dh.sunicon;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import dh.sunicon.UnitsCursorAdapter.SuggestionData;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitPicker extends ListActivity {

	private EditText filterEdit_;
	private UnitsCursorAdapter unitCursorAdapter_;
	private DatabaseHelper dbHelper_;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unit_picker);
        dbHelper_ = new DatabaseHelper(this);
        
        initListView();
        initFilterEdit();
    }

	private void initListView() {
		final Cursor initialCursor = UnitsCursorAdapter.getInitialCursor(dbHelper_, true);
        unitCursorAdapter_ = new UnitsCursorAdapter(this, initialCursor, dbHelper_, false);
        getListView().setAdapter(unitCursorAdapter_);
        
        getListView().setOnItemClickListener(new OnItemClickListener() {
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

	private void initFilterEdit() {	
		filterEdit_ = (EditText) findViewById(R.id.filterEdit);
        filterEdit_.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				unitCursorAdapter_.getFilter().filter(s);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(filterEdit_, InputMethodManager.SHOW_IMPLICIT);
		
		filterEdit_.requestFocus();
	}
}

