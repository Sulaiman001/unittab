package dh.sunicon;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitsCursorAdapter extends CursorAdapter implements
		Filterable
{
	static final String queryPartSelect = 
			"SELECT"
			+" unit.id as _id"
			+", unit.name as unitName" 
			+", unit.shortName as unitShortName" 
			+", category.Name as categoryName"
			+", category.Id as categoryId"
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id ";
	
	static final String queryPartWhere1 = 
			"WHERE (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) ";
	
	static final String queryPartWhere2 = 
			"WHERE (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) "
			+"AND (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) ";
	
	/**
	 * Cursor contains 60 rows max 
	 */
	static final String queryPartLimit = "LIMIT 60"; 
	private final DatabaseHelper dbHelper;
	
	public UnitsCursorAdapter(Context context, Cursor c,
			boolean autoRequery)
	{
		super(context, c, autoRequery);
		dbHelper = ((MainActivity)context).getDatabaseHelper();
	}

	public UnitsCursorAdapter(Context context, Cursor c,
			int flags)
	{
		super(context, c, flags);
		dbHelper = ((MainActivity)context).getDatabaseHelper();
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		final LayoutInflater inflater = LayoutInflater.from(context);

		//get the LinearLayout from the unit_dropdown_item
		View unitDropDownItemView = inflater.inflate(R.layout.unit_dropdown_item, parent, false);
		
		//save children views in the tag to avoid call findViewById
		TextView categoryLabel = (TextView) unitDropDownItemView.findViewById(R.id.categoryLabel);
		TextView unitLabel = (TextView) unitDropDownItemView.findViewById(R.id.unitLabel);
		unitDropDownItemView.setTag(new TextView[] {categoryLabel, unitLabel});
		
		return unitDropDownItemView;
	}

	@Override
    public void bindView(View view, Context context, Cursor cursor) 
	{
//		if (cursor!=mCursor)
//		{
//			Log.i("CursorAdapterBindView", "skip bind view");
//		}
		
		//get children views from tag objects
		TextView[] childrenViews = (TextView[])view.getTag();
		TextView categoryLabel = childrenViews[0];
		TextView unitLabel = childrenViews[1];
		
		///bind data to the dropdown item view
		categoryLabel.setText(cursor.getString(cursor.getColumnIndex("categoryName")));
		unitLabel.setText(cursor.getString(cursor.getColumnIndex("unitName")));
    }

	@Override
	public String convertToString(Cursor cursor)
	{
		// this method dictates what is shown when the user clicks each entry in
		// your autocomplete list
		
		String unitName = cursor.getString(cursor.getColumnIndex("unitName"));
		String categoryName = cursor.getString(cursor.getColumnIndex("categoryName"));
		long unitId = cursor.getLong(cursor.getColumnIndex("_id"));
		long categoryId = cursor.getLong(cursor.getColumnIndex("categoryId"));
		
		return categoryName+'\n'+unitName+'\n'
				+categoryId+'\n'+unitId;
	}
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint)
	{
		// this is how you query for suggestions 
		
		if (getFilterQueryProvider() != null)
		{
			return getFilterQueryProvider().runQuery(constraint);
		}
		
		// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
		
		String wherePart = "";
		String[] selectionArgs = null;
		
		if (!TextUtils.isEmpty(constraint))
		{
			String filterText = constraint.toString().trim().toLowerCase();
			
			int firstSpacePos = filterText.indexOf(' ', 0); 
			if (firstSpacePos < 0) //no space has been found
			{
				filterText = "%"+filterText+"%";
				selectionArgs = new String[]{filterText, filterText, filterText};
				wherePart = queryPartWhere1;
			}
			else
			{
				//split filterText in 2 parts use the first space as separator 
				String filterTextPart1 = "%"+filterText.substring(0, firstSpacePos)+"%";
				String filterTextPart2 = "%"+filterText.substring(firstSpacePos, filterText.length()).trim()+"%";
				
				wherePart = queryPartWhere2;
				selectionArgs = new String[]{filterTextPart1, filterTextPart1, filterTextPart1, filterTextPart2, filterTextPart2, filterTextPart2};
			}
		}
		
		final String queryComplete = queryPartSelect + wherePart + queryPartLimit;
		
		//Log.i("UnitsCursorAdapter", queryComplete);

		return dbHelper.getReadableDatabase().rawQuery(queryComplete, selectionArgs); 
		
	}

}
