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
	static final String SELECT_QUERY_PART = 
			"SELECT"
			+" unit.id as _id"
			+", unit.name as unitName" 
			+", unit.shortName as unitShortName" 
			+", category.Name as categoryName"
			+", category.Id as categoryId"
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id ";
	
	static final String WHERE1_QUERY_PART = 
			"WHERE (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) ";
	
	static final String WHERE2_QUERY_PART = 
			"WHERE (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) "
			+"AND (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) ";
	
	/**
	 * Cursor contains 60 rows max 
	 */
	static final String LIMIT_ORDER_QUERY_PART = "ORDER BY unitName LIMIT 60";
	
	/*
	static final long EventsAbsorberLatency = 1000; //milisecond
	static long lastInvokeTime;
	*/
	
	private final LayoutInflater inflater;
	private final DatabaseHelper dbHelper;
	
	public UnitsCursorAdapter(Context context, Cursor c,
			boolean autoRequery)
	{
		super(context, c, autoRequery);
		dbHelper = ((MainActivity)context).getDatabaseHelper();
		inflater = LayoutInflater.from(context);
	}

	public UnitsCursorAdapter(Context context, Cursor c,
			int flags)
	{
		super(context, c, flags);
		dbHelper = ((MainActivity)context).getDatabaseHelper();
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
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
		/*
		//Event absorber technique
		
		long currentInvokeTime = System.nanoTime();
		lastInvokeTime = currentInvokeTime;
		try
		{
			//wait 300ms, to be sure that there are not other query (user didn't type other letter)
			Thread.sleep(EventsAbsorberLatency);
		}
		catch (InterruptedException e)
		{
			Log.wtf(this.toString(), e);
		}
		
		//in case user type other letter, lastInvokeTime will be changed
		if (lastInvokeTime > currentInvokeTime)
		{
			Log.d("EventAbsorber", "Ignore query");
			//other query has already been called, so, no need for this query
			return null;
		}
		
		*/
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
				wherePart = WHERE1_QUERY_PART;
			}
			else
			{
				//split filterText in 2 parts use the first space as separator 
				String filterTextPart1 = "%"+filterText.substring(0, firstSpacePos)+"%";
				String filterTextPart2 = "%"+filterText.substring(firstSpacePos, filterText.length()).trim()+"%";
				
				wherePart = WHERE2_QUERY_PART;
				selectionArgs = new String[]{filterTextPart1, filterTextPart1, filterTextPart1, filterTextPart2, filterTextPart2, filterTextPart2};
			}
		}
		
		MainActivity.simulateLongOperation(1, 3);
		
		final String queryComplete = SELECT_QUERY_PART + wherePart + LIMIT_ORDER_QUERY_PART;
		//Log.i("UnitsCursorAdapter", queryComplete);

		return dbHelper.getReadableDatabase().rawQuery(queryComplete, selectionArgs); 
		
	}

}
