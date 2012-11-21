package dh.sunicon;

import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.support.v4.content.AsyncTaskLoader;

import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import dh.sunicon.datamodel.DatabaseHelper;

public class UnitsCursorTreeAdapter extends BaseExpandableListAdapter implements
		Filterable, LoaderCallbacks<Cursor>
{
	static final String TAG = UnitsCursorTreeAdapter.class.getName();
	
	static final String SELECT_QUERY_PART = 
			"SELECT"
			+" unit.id as _id"
			+", unit.name as unitName" 
			+", unit.shortName as unitShortName" 
			+", category.Name as categoryName"
			+", category.Id as categoryId"
			+", unit.enabled as enabled"
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id";
	static final String WHERE1_QUERY_PART = 
			" WHERE category.id=?";
	static final String WHERE2_QUERY_PART = 
			" AND (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(category.name) LIKE ?)";
	static final String LIMIT_ORDER_QUERY_PART = " ORDER BY lower(unitName)";

	static final String CAT_SELECT_QUERY_PART = 
			"SELECT"
			+" category.id as _id"
			+", category.name as categoryName"
			+", category.enabled as enabled"
			+", COUNT(unit.id) as cnt" 
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id";
	static final String CAT_WHERE1_QUERY_PART = 
			" WHERE 1=1";
	static final String CAT_WHERE2_QUERY_PART = 
			" AND (lower(unit.name) LIKE ? OR  lower(unit.shortName) LIKE ? OR lower(category.name) LIKE ?)";
	static final String CAT_LIMIT_ORDER_QUERY_PART = " GROUP BY _id HAVING cnt > 0 ORDER BY lower(categoryName)";
	
	private final Fragment owner_;
	private final DatabaseHelper dbHelper_;
	
//	private final ExecutorService categoryLoadingThread_ = Executors.newCachedThreadPool();
//	private final SparseArray<Category> categoriesByPosition = new SparseArray<Category>(); 
	private final LayoutInflater inflater_;
	private final ExecutorService updateThread = Executors.newCachedThreadPool();
	private final Handler mainThread_ = new Handler();
	private boolean blockAllCheckChangedEvent_ = false;
	private Cursor categoryCursor_ = null;
	private SparseArray<Cursor> unitsOfCategory_ = new SparseArray<Cursor>();
	
	public UnitsCursorTreeAdapter(Fragment owner)
	{
		owner_ = owner;
		inflater_ = owner_.getActivity().getLayoutInflater();
		dbHelper_ = ((MainActivity)(owner_.getActivity())).getDatabaseHelper();
		owner_.getLoaderManager().initLoader(CATEGORY_LOADER_ID, null, this);
	}

	@Override
	public int getGroupCount() {
		if (categoryCursor_!=null) {
			return categoryCursor_.getCount();
		}
		return 0;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		Cursor c = getCursorOfGroup2(groupPosition);
		if (c!=null) {
			return c.getCount();
		}
		return 0;
	}

	@Override
	public Cursor getGroup(int groupPosition) {
		return categoryCursor_;
	}

	@Override
	public Cursor getChild(int groupPosition, int childPosition) {
		Cursor c = getCursorOfGroup2(groupPosition);
		if (c!=null) {
			c.moveToPosition(childPosition);
			return c;
		}
		return null;
	}

	@Override
	public long getGroupId(int groupPosition) {
		if (categoryCursor_!=null) {
			categoryCursor_.moveToPosition(groupPosition);
			return categoryCursor_.getInt(0);
		}
		return -1;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		Cursor c = getCursorOfGroup2(groupPosition);
		if (c!=null) {
			c.moveToPosition(childPosition);
			return c.getInt(0);
		}
		return -1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		
		View v;
		SubViewsHolder tag;
		if (convertView == null) {
			v=inflater_.inflate(R.layout.category_check_item, parent, false);
			
			tag = new SubViewsHolder();
			tag.label_ = (TextView) v.findViewById(R.id.label);
			tag.check_ = (CheckBox) v.findViewById(R.id.check);
			
			v.setTag(tag);
		}
		else {
			v = convertView;
			tag = (SubViewsHolder)convertView.getTag();
		}
		
		if (categoryCursor_!=null) {
			categoryCursor_.moveToPosition(groupPosition);
			
			tag.label_.setText(categoryCursor_.getString(categoryCursor_.getColumnIndex("categoryName")));
			int categoryId = categoryCursor_.getInt(0);
			
			if (tag.checkChangedEvent_ == null) {
				tag.check_.setChecked(categoryCursor_.getInt(categoryCursor_.getColumnIndex("enabled")) == 1);
				tag.checkChangedEvent_ = new CheckedChangeAdapter();
				tag.checkChangedEvent_.setId(categoryId, -1);
				tag.check_.setOnCheckedChangeListener(tag.checkChangedEvent_);
			}
			else {
				tag.checkChangedEvent_.setEnabled(false);
				try {
					tag.check_.setChecked(categoryCursor_.getInt(categoryCursor_.getColumnIndex("enabled")) == 1);
				}
				finally {
					tag.checkChangedEvent_.setEnabled(true);
				}
				tag.checkChangedEvent_.setId(categoryId, -1);
			}
			
			if (isExpanded) {
				checkInitLoaderOf(categoryId);
			}
		}
		
		return v;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		View v;
		SubViewsHolder tag;
		if (convertView == null) {
			v=inflater_.inflate(R.layout.unit_check_item, parent, false);
			
			tag = new SubViewsHolder();
			tag.label_ = (TextView) v.findViewById(R.id.label);
			tag.check_ = (CheckBox) v.findViewById(R.id.check);
			
			v.setTag(tag);
		}
		else {
			v = convertView;
			tag = (SubViewsHolder)convertView.getTag();
		}
		
		Object[] ret = getCursorOfGroup(groupPosition);
		
		if (ret!=null)
		{
			Cursor unitCursor = (Cursor)ret[0];
			int categoryId = (Integer)ret[1];
			
			if (unitCursor!=null) {
				unitCursor.moveToPosition(childPosition);
				
				tag.label_.setText(unitCursor.getString(unitCursor.getColumnIndex("unitName")));
				int unitId = unitCursor.getInt(0);
				
				if (tag.checkChangedEvent_ == null) {
					tag.check_.setChecked(unitCursor.getInt(unitCursor.getColumnIndex("enabled")) == 1);
					tag.checkChangedEvent_ = new CheckedChangeAdapter();
					tag.checkChangedEvent_.setId(categoryId, unitId);
					tag.check_.setOnCheckedChangeListener(tag.checkChangedEvent_);
				}
				else {
					tag.checkChangedEvent_.setEnabled(false);
					try {
						tag.check_.setChecked(unitCursor.getInt(unitCursor.getColumnIndex("enabled")) == 1);
					}
					finally {
						tag.checkChangedEvent_.setEnabled(true);
					}
					tag.checkChangedEvent_.setId(categoryId, unitId);
				}
			}
		}
		
		return v;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	@Override
	public Filter getFilter() {
		return filter_;
	}

	private static int CATEGORY_LOADER_ID = -1;  
	
	private CharSequence constraint_ = "";
	
	private Filter filter_ = new Filter() {

		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			return null;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results)
		{
			constraint_ = constraint;
			owner_.getLoaderManager().restartLoader(CATEGORY_LOADER_ID, null, UnitsCursorTreeAdapter.this);
		}
		
	};
	
	/**
	 * loaderId = -1 for CategoryLoader otherwise it is categoryId
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle paramBundle)
	{
		if (loaderId == CATEGORY_LOADER_ID)
		{
			if (TextUtils.isEmpty(constraint_))
			{
				return new SQLiteCursorLoader(
								owner_.getActivity(), 
					    		dbHelper_, 
					    		CAT_SELECT_QUERY_PART+CAT_WHERE1_QUERY_PART+CAT_LIMIT_ORDER_QUERY_PART,
					    		null);
			}
			else
			{
				// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
					
				String wherePart = CAT_WHERE1_QUERY_PART;
				LinkedList<String> selectionArgs = null;
				
				String filterText = constraint_.toString().trim().toLowerCase(Locale.US);
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
		
				
				return new SQLiteCursorLoader(
								owner_.getActivity(), 
					    		dbHelper_, 
					    		queryComplete, 
					    		argsArray);
			}
		}
		else 
		{
			/* loaderId is categoryId */
			
			if (TextUtils.isEmpty(constraint_))
			{
				return new SQLiteCursorLoader(
						owner_.getActivity(), 
			    		dbHelper_, 
			    		SELECT_QUERY_PART+WHERE1_QUERY_PART+LIMIT_ORDER_QUERY_PART,
			    		new String[]{Integer.toString(loaderId)}); //loaderId is categoryId
			}
			
			// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
				
			String wherePart = WHERE1_QUERY_PART;
			LinkedList<String> selectionArgs = null;
			
			String filterText = constraint_.toString().trim().toLowerCase(Locale.US);
			selectionArgs = new LinkedList<String>();
			selectionArgs.add(Integer.toString(loaderId));
			
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
	
			return new SQLiteCursorLoader(
					owner_.getActivity(), 
		    		dbHelper_, 
		    		queryComplete,
		    		argsArray);
		}
	}

	private void checkInitLoaderOf(int categoryId)
	{
		if (owner_.getLoaderManager().getLoader(categoryId) == null) {
			owner_.getLoaderManager().initLoader(categoryId, null, this);
		}
	}
	private Object[] getCursorOfGroup(int groupPosition) {
		if (categoryCursor_!=null) {
			categoryCursor_.moveToPosition(groupPosition);
			int categoryId = categoryCursor_.getInt(0);
			Cursor unitsCursor = unitsOfCategory_.get(categoryId, null);
			
			if (unitsCursor!=null) {
				return new Object[]{ unitsCursor, categoryId };
			}
			else {
				checkInitLoaderOf(categoryId);
			}
		}
		return null;
	}
	
	private Cursor getCursorOfGroup2(int groupPosition) {
		Object[] r = getCursorOfGroup(groupPosition);
		if (r==null || r[0]==null) {
			return null;
		}
		return (Cursor)r[0];
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		int loaderId = loader.getId();

		//swap the cursor
		if (loaderId == CATEGORY_LOADER_ID) {
			LoaderManager loaderManager = owner_.getLoaderManager();
			
        	categoryCursor_ = cursor;
        	while (categoryCursor_.moveToNext()) {
        		int categoryId = cursor.getInt(0);
        		if (loaderManager.getLoader(categoryId) !=null) {
        			loaderManager.restartLoader(categoryId, null, this);
        		}
        	}
        }
        else {
        	unitsOfCategory_.put(loaderId, cursor);
        }
		notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		// This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // inter using it.
		int loaderId = loader.getId();
		
        if (loaderId == CATEGORY_LOADER_ID) {
        	categoryCursor_ = null;
        }
        else {
        	unitsOfCategory_.put(loaderId, null);
        }
	}
	
	private void invokeSetEnableCateogryOrUnit(final int categoryId, final int unitId, final boolean enabled) {
		
		//getLoader of root (categoriesCursor) or of category which contains the unitId
		boolean forCategory = unitId<0;
		int loaderId = forCategory ? CATEGORY_LOADER_ID : categoryId;
		SQLiteCursorLoader loader = (SQLiteCursorLoader) ((AsyncTaskLoader<?>) owner_.getLoaderManager().getLoader(loaderId));
		
		//update and notify the loader
		ContentValues cvs = new ContentValues();
		cvs.put("enabled", enabled);
		loader.update(forCategory?"category":"unit", cvs, "id=?", new String[] {Integer.toString(forCategory ? categoryId : unitId)});
	}
	
	private class SubViewsHolder {
		TextView label_;
		CheckBox check_;
		CheckedChangeAdapter checkChangedEvent_;
	}
	
	private class CheckedChangeAdapter implements CompoundButton.OnCheckedChangeListener {
		private Runnable lastRun_ = null;
		private int unitId_;
		private int categoryId_;
		private boolean enabled_ = true;

		public CheckedChangeAdapter() {
			super();
		}
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
			try {
				if (!isEnabled() || blockAllCheckChangedEvent_) {
					return;
				}
				/* delayed event technique, prevent users crazy-continous-click on checkbox */
				
				if (lastRun_ != null) {
					mainThread_.removeCallbacks(lastRun_);
				}
				
				lastRun_ = new Runnable() {
						@Override
						public void run() {
							try {
								invokeSetEnableCateogryOrUnit(categoryId_, unitId_, isChecked);
							}
							catch (Exception ex) {
								Log.w(TAG, ex);
							}
						}
					};
				
				mainThread_.postDelayed(lastRun_, 500);
			}
			catch (Exception ex) {
				Log.w(TAG, ex);
			}
		}
		
		public boolean isEnabled() {
			return enabled_;
		}
		public void setEnabled(boolean enabled) {
			enabled_ = enabled;
		}
		public void setId(int categoryId, int id)
		{
			categoryId_ = categoryId;
			unitId_ = id;
		}
	}
}
