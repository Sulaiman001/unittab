package dh.sunicon;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class ResultListAdapter extends BaseAdapter implements Filterable
{	
	private final LayoutInflater inflater_;
	private final DatabaseHelper dbHelper_;
	
	private Context context_;
	private long categoryId_;
	private long baseUnitId_;
	private UnitFilter filter_;
	
	/**
	 * write lock on data_. any write operation on data_ must be synch on this lock_ 
	 */
	private final Object lock_ = new Object();
	private ArrayList<ConversionResult> data_;
	
	public ResultListAdapter(Context context)
	{
		context_ = context;
		dbHelper_ = ((MainActivity)context).getDatabaseHelper();
		inflater_ = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount()
	{
		if (data_!=null)
		{
			return data_.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position)
	{
		return data_.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		ConversionResult cr = data_.get(position);
		return cr.getUnitId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		/* create (or get) view */

		View v;
		TextView valueLabel;
		TextView unitLabel;

		if (convertView == null)
		{
			// create new view
			v = inflater_.inflate(R.layout.value_unit_item, parent, false);
			valueLabel = (TextView) v.findViewById(R.id.valueLabel);
			unitLabel = (TextView) v.findViewById(R.id.unitLabel);
			TextView[] viewsHolder = new TextView[] { valueLabel, unitLabel };
			v.setTag(viewsHolder);
		}
		else
		{
			// view already created, extract the children views
			v = convertView;
			TextView[] viewsHolder = (TextView[]) v.getTag();
			valueLabel = viewsHolder[0];
			unitLabel = viewsHolder[1];
		}

		/* bind value to view */

		ConversionResult cr = data_.get(position);
		valueLabel.setText(cr.getValue());
		unitLabel.setText(cr.getUnitName());

		return v;
	}
	
	/**
	 * Fill the list with all units in the category except the baseUnit
	 */
	public void populateData(long categoryId, long baseUnitId)
	{
		categoryId_ = categoryId;
		baseUnitId_ = baseUnitId;
		
		DataFillerAsyncTask ast = new DataFillerAsyncTask();
		ast.execute(categoryId_, baseUnitId_);
	}
	
	@Override
	public Filter getFilter()
	{
		if (filter_ == null)
		{
			filter_ = new UnitFilter();
		}
		return filter_;
	}
	
	private class DataFillerAsyncTask extends AsyncTask<Long, Void, ArrayList<ConversionResult>>
	{
		@Override
		protected ArrayList<ConversionResult> doInBackground(Long... params)
		{
			ArrayList<ConversionResult> resu = new ArrayList<ResultListAdapter.ConversionResult>();

			long categoryId = params[0];
			long baseUnitId = params[1];
			
			Cursor cur = dbHelper_.getReadableDatabase().
							query("unit", new String[]{"id", "name", "shortName"}, 
								"categoryId=? AND id<>?", 
								new String[] {Long.toString(categoryId), Long.toString(baseUnitId)}, 
								null, null, "name");
			
			int idColumnIndex = cur.getColumnIndex("id");
			int nameColumnIndex = cur.getColumnIndex("name");
			int shortNameColumnIndex = cur.getColumnIndex("shortName");
			
			while (cur.moveToNext() && !isCancelled()) 
			{
				ConversionResult co = new ConversionResult(
							cur.getLong(idColumnIndex),
							cur.getString(nameColumnIndex),
							cur.getString(shortNameColumnIndex)
						);
				resu.add(co);
			}

			cur.close();
			
			MainActivity.simulateLongOperation(1, 3);

			// TODO Auto-generated method stub
			return resu;
		}
		
		@Override
		protected void onPostExecute(ArrayList<ConversionResult> result)
		{
			synchronized (lock_)
			{
				data_ = result;
			}
			if (result == null || result.size() == 0)
			{
				notifyDataSetInvalidated();
			}
			else
			{
				notifyDataSetChanged();
			}
		}
	};
	
	public class ConversionResult
	{
		long unitId;
		String unitName;
		String unitShortName;
		Double value;
		
		public ConversionResult(long unitId, String unitName,
				String unitShortName)
		{
			super();
			this.unitId = unitId;
			this.unitName = unitName;
			this.unitShortName = unitShortName;
		}
		public long getUnitId()
		{
			return unitId;
		}
		public String getUnitName()
		{
			if (TextUtils.isEmpty(unitShortName))
			{
				return unitName;
			}
			return String.format("%1$s - %2$s", unitShortName, unitName);
		}
		public String getValue()
		{
			if (value == null)
			{
				return "computing..";
			}
			return value.toString();
		}
	}
	
	private class UnitFilter extends Filter 
	{

		/**
		 * copy of the original data_, then the data_ will contains only item matching the filter
		 */
		private ArrayList<ResultListAdapter.ConversionResult> fullData_;
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults resu = new FilterResults();
			
			if (fullData_ == null)
			{
				synchronized (lock_) 
				{
					fullData_ = new ArrayList<ResultListAdapter.ConversionResult>(data_);
				}
			}
			
			ArrayList<ConversionResult> l;
			if (TextUtils.isEmpty(constraint))
			{
				synchronized (lock_) 
				{
					l = new ArrayList<ResultListAdapter.ConversionResult>(fullData_);
				}
			}
			else
			{
				final String filterText = constraint.toString().toLowerCase();
				final int count = fullData_.size();
				l = new ArrayList<ResultListAdapter.ConversionResult>();
				for (int i = 0; i<count; i++)
				{
					final ResultListAdapter.ConversionResult cr = fullData_
							.get(i);
					final String valueText = cr.getUnitName().toLowerCase();
					if (valueText.contains(filterText))
					{
						l.add(cr);
					}
					else
					{
						final String[] words = filterText.split(" ");
						final int wordCount = words.length;
						boolean allWordMatched = true;
						for (int k = 0; k < wordCount; k++)
						{
							if (!valueText.contains(words[k]))
							{
								allWordMatched = false;
							}
						}
						
						if (allWordMatched)
						{
							l.add(cr);
						}
					}
				}
			}
			
			resu.values = l;
			resu.count = l.size();
			return resu;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results)
		{
			data_ = (ArrayList<ResultListAdapter.ConversionResult>) (results.values);
			if (results.count > 0)
			{
				notifyDataSetChanged();
			}
			else
			{
				notifyDataSetInvalidated();
			}
		}
		
	}
}
