package dh.sunicon;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitsCursorAdapter extends CursorAdapter implements
		Filterable
{
	static final String TAG = UnitsCursorAdapter.class.getName();

	static final String SELECT_HISTORY = 
			"SELECT"
			+" unit.id as _id"
			+", unit.name as unitName" 
			+", unit.shortName as unitShortName" 
			+", category.Name as categoryName"
			+", category.Id as categoryId"
			+" FROM unit "
			+" INNER JOIN unitHistory ON _id = unitHistory.unitId" 
			+" INNER JOIN category ON unit.categoryId = category.id"
			+" WHERE category.enabled=1 AND unit.enabled=1"
			+" ORDER BY unitHistory.lastUsed DESC";
	
	static final String SELECT_QUERY_PART = 
			"SELECT"
			+" unit.id as _id"
			+", unit.name as unitName" 
			+", unit.shortName as unitShortName" 
			+", category.Name as categoryName"
			+", category.Id as categoryId"
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id";
	
	static final String WHERE1_QUERY_PART = 
			" WHERE category.enabled=1 AND unit.enabled=1";
	
	static final String WHERE2_QUERY_PART = 
			" AND (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?)";
	
	/**
	 * Cursor contains 200 rows max 
	 */
	static final String LIMIT_ORDER_QUERY_PART = " ORDER BY lower(unitName) ASC";
	
	private final LayoutInflater inflater_;
	private final DatabaseHelper dbHelper_;
	
	public UnitsCursorAdapter(Context context, Cursor c, DatabaseHelper dbHelper,
			boolean autoRequery)
	{
		super(context, c, autoRequery);
		dbHelper_ = dbHelper;
		inflater_ = LayoutInflater.from(context);
	}

	public UnitsCursorAdapter(Context context, Cursor c, DatabaseHelper dbHelper,
			int flags)
	{
		super(context, c, flags);
		dbHelper_ = dbHelper;
		inflater_ = LayoutInflater.from(context);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		//get the LinearLayout from the unit_dropdown_item
		View v = inflater_.inflate(R.layout.unit_category_item, parent, false);
		
		//save children views in the tag to avoid call findViewById
		TextView categoryLabel = (TextView) v.findViewById(R.id.categoryLabel);
		TextView unitLabel = (TextView) v.findViewById(R.id.unitLabel);
		v.setTag(new SuggestionData(categoryLabel, unitLabel));
		
		return v;
	}

	@Override
    public void bindView(View view, Context context, Cursor cursor) 
	{
		//get children views from tag objects
		SuggestionData suggestionData = (SuggestionData)view.getTag();
		TextView categoryLabel = suggestionData.getCategoryLabel();
		TextView unitLabel = suggestionData.getUnitLabel();
		
		///bind data to the dropdown item view
		categoryLabel.setText(cursor.getString(cursor.getColumnIndex("categoryName")));
		unitLabel.setText(cursor.getString(cursor.getColumnIndex("unitName")));
		suggestionData.setUnitId(cursor.getLong(cursor.getColumnIndex("_id")));
		suggestionData.setCategoryId(cursor.getLong(cursor.getColumnIndex("categoryId")));
    }

	@Override
	public String convertToString(Cursor cursor)
	{
		// this method dictates what is shown when the user clicks each entry in
		// your autocomplete list
		
		String unitName = cursor.getString(cursor.getColumnIndex("unitName"));
		return unitName;
	}
	
	private final int DELAY_RUN_QUERY = 500;
	private Object lockLastConstraint_ = new Object();
	private String lastConstraint_;
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint)
	{
		try
		{
			if (!TextUtils.isEmpty(constraint))
			{
				/* delayer events technique */
				
				if (constraint!=null)
				{
					synchronized (lockLastConstraint_)
					{
						lastConstraint_ = new String(constraint.toString());
					}
				}
				
				Thread.sleep(DELAY_RUN_QUERY);
			
				if (lastConstraint_!=null)
				{
					if (!lastConstraint_.equals(constraint))
					{
						/*
						 * lastConstraint_ has been changed after 500ms 
						 * => other runQueryOnBackgroundThread has been called
						 * => no need to execute this one
						 */ 
						return null; 
					}
				}
			}
			
			/* this is how you query for suggestions */
			
			if (getFilterQueryProvider() != null)
			{
				return getFilterQueryProvider().runQuery(constraint);
			}
			
			if (TextUtils.isEmpty(constraint))
			{
				Cursor historyCursor = dbHelper_.getReadableDatabase().rawQuery(SELECT_HISTORY,  null);
				int historyCursorSize = historyCursor.getCount();
				if (historyCursorSize > 0) 
				{
					Log.v(TAG, "Return history");
					return historyCursor;
				}
				else
				{
					//history is empty
					historyCursor.close();
					
					Log.v(TAG, "Return top 60 unit");
					String topUnitQuery = SELECT_QUERY_PART + WHERE1_QUERY_PART + LIMIT_ORDER_QUERY_PART;
					return dbHelper_.getReadableDatabase().rawQuery(topUnitQuery,  null);
				}
			}
			
			// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
				
			String wherePart = WHERE1_QUERY_PART;
			LinkedList<String> selectionArgs = null;
			
			String filterText = constraint.toString().trim().toLowerCase();
			selectionArgs = new LinkedList<String>();
			
			final String[] words = filterText.split(" ");
			
			final int wordCount = words.length;
			for (int k = 0; k < wordCount; k++)
			{
				String word = words[k];
				if (TextUtils.isEmpty(word))
				{
					continue;
				}
				
				wherePart = wherePart.concat(WHERE2_QUERY_PART);
				selectionArgs.addLast('%'+word+'%');
				selectionArgs.addLast('%'+word+'%');
				selectionArgs.addLast('%'+word+'%');
			}
			
			String queryComplete = SELECT_QUERY_PART + wherePart + LIMIT_ORDER_QUERY_PART;
			String[] argsArray = selectionArgs.toArray(new String[selectionArgs.size()]);

			Cursor c = dbHelper_.getReadableDatabase().rawQuery(queryComplete,  argsArray);
			return c;
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
			return null;
		}
	}
	

	/**
	 * The children view place holder and data of a suggestion row
	 */
	public class SuggestionData
	{
		private TextView categoryLabel_;
		private TextView unitLabel_; 
		private long unitId_;
		private long categoryId_;
		
		public SuggestionData(TextView categoryLabel, TextView unitLabel)
		{
			super();
			categoryLabel_ = categoryLabel;
			unitLabel_ = unitLabel;
		}

		public long getUnitId()
		{
			return unitId_;
		}

		public void setUnitId(long unitId)
		{
			unitId_ = unitId;
		}

		public long getCategoryId()
		{
			return categoryId_;
		}

		public void setCategoryId(long categoryId)
		{
			categoryId_ = categoryId;
		}

		public CharSequence getCategoryName()
		{
			return categoryLabel_.getText();
		}

		public CharSequence getUnitName()
		{
			return unitLabel_.getText();
		}

		public TextView getCategoryLabel()
		{
			return categoryLabel_;
		}

		public TextView getUnitLabel()
		{
			return unitLabel_;
		}
	}
}
