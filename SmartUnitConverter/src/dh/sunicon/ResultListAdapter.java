package dh.sunicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import dh.sunicon.datamodel.Conversion;
import dh.sunicon.datamodel.DatabaseHelper;

public class ResultListAdapter extends BaseAdapter implements Filterable
{	
	static final String TAG = ResultListAdapter.class.getName();
	private final LayoutInflater inflater_;
	private final DatabaseHelper dbHelper_;
	/**
	 * Thread Pool to calculate the converted value
	 */
	private final ExecutorService computeExecutor_ = Executors.newFixedThreadPool(5);
	/**
	 * Thread to read all Conversion from DB
	 */
	private final ExecutorService readConversionExecutor_ = Executors.newSingleThreadExecutor();
	private ReadConversionsTask readConversionsTask_ = null; 
	private Context context_;
	private long categoryId_;
	private long baseUnitId_;
	private double baseValue_ = Double.NaN;
	private TargetUnitFilter filter_;
	private FillDataTask fillDataTask_;
	
	/**
	 * write lock on data_. any write operation on data_ must be synch on this lock_ 
	 */
	private final Object lock_ = new Object();
	private ArrayList<RowData> data_;
	
	public ResultListAdapter(Context context)
	{
		context_ = context;
		dbHelper_ = ((MainActivity)context_).getDatabaseHelper();
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
		RowData cr = data_.get(position);
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

		RowData cr = data_.get(position);
		valueLabel.setText(cr.getValue());
		unitLabel.setText(cr.getUnitName());

		return v;
	}
	
	/**
	 * Fill the list with all units in the category except the baseUnit
	 */
	public void populateData(long categoryId, long baseUnitId) throws IllegalAccessException
	{
		if (onGuiThread())
		{
			categoryId_ = categoryId;
			baseUnitId_ = baseUnitId;
			
			//read all conversion of the category
			if (readConversionsTask_ != null)
			{
				readConversionsTask_.cancel();
			}
			readConversionsTask_ = new ReadConversionsTask();
			readConversionExecutor_.execute(readConversionsTask_);
			
			//fill the list with related target unit (of the same category)
			if (fillDataTask_ != null)
			{
				fillDataTask_.cancel(false);
			}
			fillDataTask_ = new FillDataTask();
			fillDataTask_.execute(categoryId_, baseUnitId_);
		}
		else
		{
			throw new IllegalAccessException("populateData() must be called from UI Thread.");
		}
	}
	
	/**
	 * set base value and compute conversion values
	 * @throws Exception 
	 */
	public void setBaseValue(double baseValue) throws IllegalAccessException
	{
		if (onGuiThread())
		{
			baseValue_ = baseValue;
			
			/* set all the value to "-" */
			if (filter_!=null)
			{
				filter_.clearAllTargetValues();
			}
			
			if (data_ == null)
			{
				return;
			}
			
			int count = data_.size();
			for (int i = 0; i<count; i++)
			{
				data_.get(i).clearTargetValue();
			}
			notifyDataSetChanged();
			
			/* calculate all value */
			
			for (int i = 0; i<count; i++)
			{
				data_.get(i).setBaseValue(baseValue_);
			}
		}
		else
		{
			throw new IllegalAccessException("setBaseValue() must be called from UI Thread. To be sure that data_ will not be changed during the computing");
		}
	}

	@Override
	public Filter getFilter()
	{
		if (filter_ == null)
		{
			filter_ = new TargetUnitFilter();
		}
		return filter_;
	}
	
	public boolean onGuiThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}
	
	/*
	 * **** Inner classes ****
	 */
	
	private final class ReadConversionsTask implements Runnable
	{
		private boolean cancelled_;
		private boolean finished_ = false;
		private CountDownLatch countDownLatch_ = new CountDownLatch(1);
		
		private HashMap<Long, Conversion> conversions_ = new HashMap<Long, Conversion>();
		
		public void cancel()
		{
			cancelled_ = true;
		}
		
		public boolean isFinished()
		{
			return finished_;
		}
		
		public void waitToFinish(long timeout, TimeUnit timeUnit) throws InterruptedException
		{
			countDownLatch_.await(timeout, timeUnit);
		}

		@Override
		public void run()
		{
			try
			{	
				Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
								"SELECT conversion.* FROM conversion JOIN unit ON conversion.base = unit.id WHERE unit.categoryId = ?",
								new String[] { Long.toString(categoryId_) });
			
				final int idCi = cur.getColumnIndex("id");
				final int baseCi = cur.getColumnIndex("base");
				final int targetCi = cur.getColumnIndex("target");
				final int fxCi = cur.getColumnIndex("fx");
				final int formulaCi = cur.getColumnIndex("formula");
				final int reversedFormulaCi = cur.getColumnIndex("reversedFormula");
				
				while (cur.moveToNext() && !cancelled_)
				{
					Conversion c = new Conversion(dbHelper_, 
							cur.getLong(idCi),
							cur.getLong(baseCi), 
							cur.getLong(targetCi), 
							cur.getDouble(fxCi), 
							cur.getString(formulaCi), 
							cur.getString(reversedFormulaCi));
					conversions_.put(c.getId(), c);
				}
				
				cur.close();
				
				if (cancelled_)
				{
					conversions_ = null;
				}
			}
			catch (Exception ex)
			{
				Log.w(TAG, ex);
			}
			finally
			{
				//notify other thread that this ones is finished
				countDownLatch_.countDown();
				finished_ = true;
			}
		}
	}

	private final class FillDataTask extends AsyncTask<Long, Void, ArrayList<RowData>>
	{
		@Override
		protected ArrayList<RowData> doInBackground(Long... params)
		{
			ArrayList<RowData> resu = new ArrayList<ResultListAdapter.RowData>();

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
				RowData co = new RowData(
							baseUnitId,
							cur.getLong(idColumnIndex),
							cur.getString(nameColumnIndex),
							cur.getString(shortNameColumnIndex)
						);
				resu.add(co);
			}

			cur.close();
			
			MainActivity.simulateLongOperation(1, 3);

			return resu;
		}
		
		@Override
		protected void onPostExecute(ArrayList<RowData> result)
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
	
	/**
	 * data of a Row
	 */
	private final class RowData implements Runnable
	{
		/*change of baseValue_ and value_ must be synchronized*/
		private Double baseValue_ = Double.NaN;
		private long baseUnitId_;
		private long targetUnitId_;
		private String targetUnitName_;
		private String targetUnitShortName_;
		private double value_ = Double.NaN;
		
		public RowData(long baseUnitId, long targetUnitId, String targetUnitName,
				String targetUnitShortName)
		{
			super();
			baseUnitId_ = baseUnitId;
			targetUnitId_ = targetUnitId;
			targetUnitName_ = targetUnitName;
			targetUnitShortName_ = targetUnitShortName;
			this.setBaseValue(ResultListAdapter.this.baseValue_);
		}
		public long getUnitId()
		{
			return targetUnitId_;
		}
		public String getUnitName()
		{
			if (TextUtils.isEmpty(targetUnitShortName_))
			{
				return targetUnitName_;
			}
			return String.format("%1$s - %2$s", targetUnitShortName_, targetUnitName_);
		}
		public String getValue()
		{
			if (Double.isNaN(value_))
			{
				return "-";
			}
			return Double.toString(value_);
		}

		/**
		 * Compute the target value in the thread pool (computeExecutor) then refresh the UI
		 * @param baseValue
		 */
		public void setBaseValue(double baseValue)
		{
			if (baseValue_.equals(baseValue) && !Double.isNaN(value_))
			{
				//no need to calculate, the old value_ is just right 
				return;
			}
			
			//change the baseValue and convert it
			synchronized (baseValue_)
			{
				baseValue_ = baseValue;
			}
			
			computeExecutor_.execute(this);
		}
		
		public boolean clearTargetValue()
		{
			boolean isValueChanged = !Double.isNaN(baseValue_);
			value_ = Double.NaN;
			return isValueChanged;
		}
		
		/**
		 * Compute the value
		 */
		@Override
		public void run()
		{
			try
			{
				if (!readConversionsTask_.isFinished())
				{
					readConversionsTask_.waitToFinish(10, TimeUnit.SECONDS);
				}
				
				/* copy the current baseValue_ to original Value */
				double originalValue;
				synchronized (baseValue_)
				{
					originalValue = baseValue_;
				}
				
				double resu;
				
				/* compute */
				
				if (baseUnitId_ == targetUnitId_)
				{
					resu = originalValue;
				}
				else if (Double.isNaN(originalValue))
				{
					resu = Double.NaN;
				}
				else
				{
					// TODO: convert the original value
					
					resu = originalValue + 1;
					MainActivity.simulateLongOperation(1, 4);
				}
				
				synchronized (baseValue_)
				{
					if (baseValue_.equals(originalValue))
					{
						value_ = resu;
						invokeRefreshGui();
					}
					//else, a newer setBaseValue() was called, we must ignore the resu 
				}
			}
			catch (Exception ex)
			{
				Log.w(TAG, ex);
			}
		}
		
		private void invokeRefreshGui()
		{
			((MainActivity)context_).runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						notifyDataSetChanged();
					}
					catch (Exception ex)
					{
						Log.w(TAG, ex);
					}
				}
			}); 
		}
	}
	
	private class TargetUnitFilter extends Filter 
	{
		/**
		 * copy of the original data_, then the data_ will contains only item matching the filter
		 */
		private ArrayList<ResultListAdapter.RowData> fullData_;
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults resu = new FilterResults();
			
			if (fullData_ == null)
			{
				synchronized (lock_) 
				{
					fullData_ = new ArrayList<ResultListAdapter.RowData>(data_);
				}
			}
			
			ArrayList<RowData> l;
			if (TextUtils.isEmpty(constraint))
			{
				synchronized (lock_) 
				{
					l = new ArrayList<ResultListAdapter.RowData>(fullData_);
					final int count = l.size();
					for (int i = 0; i<count; i++)
					{
						l.get(i).setBaseValue(baseValue_);
					}
				}
			}
			else
			{
				final String filterText = constraint.toString().toLowerCase();
				final int count = fullData_.size();
				l = new ArrayList<ResultListAdapter.RowData>();
				for (int i = 0; i<count; i++)
				{
					final ResultListAdapter.RowData row = fullData_.get(i);
					
					boolean matched = false;
					final String valueText = row.getUnitName().toLowerCase();
					if (valueText.contains(filterText))
					{
						matched = true;
					}
					else
					{
						final String[] words = filterText.split(" ");
						final int wordCount = words.length;
						/* check if every word matches */
						matched = true;
						for (int k = 0; k < wordCount; k++)
						{
							if (!valueText.contains(words[k]))
							{
								matched = false;
								break;
							}
						}
					}
					
					if (matched)
					{
						row.setBaseValue(baseValue_);
						l.add(row);
					}
				}
			}
			
			resu.values = l;
			resu.count = l.size();
			return resu;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results)
		{
			data_ = (ArrayList<ResultListAdapter.RowData>) (results.values);
			if (results.count > 0)
			{
				notifyDataSetChanged();
			}
			else
			{
				notifyDataSetInvalidated();
			}
		}
		
		public void clearAllTargetValues()
		{
			if (fullData_ == null)
			{
				return;
			}
			int count = fullData_.size();
			for (int i = 0; i<count; i++)
			{
				fullData_.get(i).clearTargetValue();
			}
		}
		
	}
}
