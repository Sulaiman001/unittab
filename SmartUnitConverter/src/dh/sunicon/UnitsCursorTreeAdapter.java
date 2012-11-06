package dh.sunicon;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Filterable;
import android.widget.SimpleCursorTreeAdapter;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitsCursorTreeAdapter extends SimpleCursorTreeAdapter implements
		Filterable
{
	static final String TAG = UnitsCursorTreeAdapter.class.getName();

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
			" WHERE category.enabled=1 AND unit.enabled=1 AND category.id=?";
	static final String WHERE2_QUERY_PART = 
			" AND (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(category.name) LIKE ?)";
	static final String LIMIT_ORDER_QUERY_PART = " ORDER BY lower(unitName)";

	static final String CAT_SELECT_QUERY_PART = 
			"SELECT"
			+" category.id as _id"
			+", category.name as categoryName"
			+", COUNT(unit.id) as cnt" 
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id";
	static final String CAT_WHERE1_QUERY_PART = 
			" WHERE category.enabled=1 AND unit.enabled=1";
	static final String CAT_WHERE2_QUERY_PART = 
			" AND (lower(unit.name) LIKE ? OR  lower(unit.shortName) LIKE ? OR lower(category.name) LIKE ?)";
	static final String CAT_LIMIT_ORDER_QUERY_PART = " GROUP BY _id HAVING cnt > 0 ORDER BY lower(categoryName)";
	
	private final DatabaseHelper dbHelper_;
	private final UnitPicker3 owner_;
	private final ExecutorService categoryLoadingThread_ = Executors.newCachedThreadPool();
	private final SparseArray<Category> categoriesByPosition = new SparseArray<Category>(); 
	
	public UnitsCursorTreeAdapter(Context context,
			int groupLayout, String[] groupFrom, int[] groupTo,
			int childLayout, String[] childFrom, int[] childTo) {
		super(context, null, groupLayout, groupFrom, groupTo, childLayout, childFrom,
				childTo);
		owner_ = (UnitPicker3)context;
		dbHelper_ = owner_.getDatabaseHelper();
	}

	public UnitsCursorTreeAdapter(Context context,
			int collapsedGroupLayout, int expandedGroupLayout,
			String[] groupFrom, int[] groupTo, int childLayout,
			String[] childFrom, int[] childTo) {
		super(context, null, collapsedGroupLayout, expandedGroupLayout,
				groupFrom, groupTo, childLayout, childFrom, childTo);
		owner_ = (UnitPicker3)context;
		dbHelper_ = owner_.getDatabaseHelper();
	}

	public UnitsCursorTreeAdapter(Context context,
			int collapsedGroupLayout, int expandedGroupLayout,
			String[] groupFrom, int[] groupTo, int childLayout,
			int lastChildLayout, String[] childFrom, int[] childTo) {
		super(context, null, collapsedGroupLayout, expandedGroupLayout,
				groupFrom, groupTo, childLayout, lastChildLayout, childFrom,
				childTo);
		owner_ = (UnitPicker3)context;
		dbHelper_ = owner_.getDatabaseHelper();
	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		
		Log.v(TAG, "getChildrenCursor " + groupCursor.getPosition());
		class UnitLoader implements Runnable {
	        long categoryId_;
	        String constraint_;
	        final int categoryPosition_;
	        UnitLoader(long categoryId, String constraint, int categoryPostion) 
	        { 
	        	categoryId_ = categoryId;
	        	constraint_ = constraint;
	        	categoryPosition_ = categoryPostion;
	        }
	        public void run() {
	        	try
	        	{
		            final Cursor c = getUnitCursor(categoryId_, constraint_);
		            owner_.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							try
							{
								if (!owner_.isFinishing())
									setChildrenCursor(categoryPosition_, c);
								else
									c.close();
							}
							catch (Exception ex)
							{
								Log.w(TAG, "Exception on setChildrenCursor");
							}
						}
					});
	        	}
	        	catch (Exception ex)
	        	{
	        		Log.w(TAG, ex);
	        	}
	        }
	    }
		
		long categoryId = groupCursor.getLong(0);
		String categoryName = groupCursor.getString(1);
		Category cat = new Category(dbHelper_, categoryId, categoryName);
		categoriesByPosition.put(groupCursor.getPosition(), cat);
		
		categoryLoadingThread_.execute(new UnitLoader(categoryId, lastConstraint_, groupCursor.getPosition()));
		return null;
	}
	
	
	private final int DELAY_RUN_QUERY = 500;
	private Object lockLastConstraint_ = new Object();
	private String lastConstraint_;
	
	
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
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
				return dbHelper_.getReadableDatabase().rawQuery(CAT_SELECT_QUERY_PART+CAT_WHERE1_QUERY_PART+CAT_LIMIT_ORDER_QUERY_PART, null);
			}
			
			// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
				
			String wherePart = CAT_WHERE1_QUERY_PART;
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
				
				wherePart = wherePart.concat(CAT_WHERE2_QUERY_PART);
				selectionArgs.addLast('%'+word+'%');
				selectionArgs.addLast('%'+word+'%');
				selectionArgs.addLast('%'+word+'%');
			}
			
			String queryComplete = CAT_SELECT_QUERY_PART + wherePart + CAT_LIMIT_ORDER_QUERY_PART;
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
	
	public Cursor getUnitCursor(long categoryId, CharSequence constraint) {
		
		if (TextUtils.isEmpty(constraint))
		{
			return dbHelper_.getReadableDatabase().rawQuery(SELECT_QUERY_PART+WHERE1_QUERY_PART+LIMIT_ORDER_QUERY_PART, new String[]{Long.toString(categoryId)});
		}
		
		// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
			
		String wherePart = WHERE1_QUERY_PART;
		LinkedList<String> selectionArgs = null;
		
		String filterText = constraint.toString().trim().toLowerCase();
		selectionArgs = new LinkedList<String>();
		selectionArgs.add(Long.toString(categoryId));
		
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
	
// Always expand all
//	@Override
//	public View getGroupView(int groupPosition, boolean isExpanded,
//		View convertView, ViewGroup parent) {
//
//		View v= super.getGroupView(groupPosition, isExpanded, convertView, parent);
//		//expand the group then return
//		ExpandableListView l = (ExpandableListView) parent;
//		l.expandGroup(groupPosition);
//		
//		return v;
//	}
	
	
	@Override
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
		if (onChangeCursorListener_!=null)
		{
			onChangeCursorListener_.onChangeCursor(cursor);
		}
	}
	
	public Category getCategoryByPosition(int position) {
		return categoriesByPosition.get(position);
	}
	
	private OnChangeCursorListener onChangeCursorListener_;
	
	public void setOnChangeCursorListener(OnChangeCursorListener l) {
		onChangeCursorListener_ = l;
	}
	
	public interface OnChangeCursorListener {
		void onChangeCursor(Cursor c);
	}
}
