package dh.sunicon;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Looper;
import android.text.Html;
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
	private final ExecutorService calculationPoolThread_ = Executors.newFixedThreadPool(5);

	/**
	 * Thread to read all Conversion from DB
	 */
	private final ExecutorService conversionsLoadingThread_ = Executors
			.newSingleThreadExecutor();
	private ConversionsLoadingRunner conversionsLoadingRunner_ = null;
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
		valueLabel.setText(Html.fromHtml(cr.getValue()));
		unitLabel.setText(Html.fromHtml(cr.getUnitName()));

		return v;
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
	
	/**
	 * Fill data_ list with all units in the category except the baseUnit + read the Conversion graph 
	 */
	public void setBaseUnitId(long categoryId, long baseUnitId) throws IllegalAccessException
	{
		if (onGuiThread())
		{
			Log.i(TAG, String.format("Populate category = %d baseUnit = %d", categoryId, baseUnitId));
			
			categoryId_ = categoryId;
			baseUnitId_ = baseUnitId;
			
			//read all conversion of the category
			if (conversionsLoadingRunner_ != null)
			{
				conversionsLoadingRunner_.cancel();
			}
			conversionsLoadingRunner_ = new ConversionsLoadingRunner();
			conversionsLoadingThread_.execute(conversionsLoadingRunner_);
			
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
			throw new IllegalAccessException("this methode must be called from UI Thread.");
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
			Log.i(TAG, String.format("setBaseValue = %f", baseValue));
			
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

	public Context getContext()
	{
		return context_;
	}

	public double getBaseValue()
	{
		return baseValue_;
	}
	
	public ExecutorService getCalculationPoolThread()
	{
		return calculationPoolThread_;
	}
	
	public ArrayList<Conversion> getConversions() throws IllegalAccessException, InterruptedException
	{
		if (conversionsLoadingRunner_==null)
		{
			throw new IllegalAccessException("The conversion loading has not been started. Base Unit was not set");
		}
		if (!conversionsLoadingRunner_.isFinished())
		{
			conversionsLoadingRunner_.waitToFinish(5, TimeUnit.SECONDS);
		}
		return conversionsLoadingRunner_.getConversions();
	}
	
	public boolean onGuiThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}
	
	
	/*
	 * **** Inner classes ****
	 */
	
	final class ConversionsLoadingRunner implements Runnable
	{
		private boolean cancelled_;
		private boolean finished_ = false;
		private CountDownLatch countDownLatch_ = new CountDownLatch(1);
		
		private ArrayList<Conversion> conversions_;
		
		public ArrayList<Conversion> getConversions()
		{
			return conversions_;
		}

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
				/* read all */
				
				Cursor cur = dbHelper_.getReadableDatabase().rawQuery(
								"SELECT conversion.* FROM conversion JOIN unit ON conversion.base = unit.id WHERE unit.categoryId = ?",
								new String[] { Long.toString(categoryId_) });
			
				final int idCi = cur.getColumnIndex("id");
				final int baseCi = cur.getColumnIndex("base");
				final int targetCi = cur.getColumnIndex("target");
				final int fxCi = cur.getColumnIndex("fx");
				final int formulaCi = cur.getColumnIndex("formula");
				final int reversedFormulaCi = cur.getColumnIndex("reversedFormula");
				
				conversions_ = new ArrayList<Conversion>();
				
				while (cur.moveToNext() && !cancelled_)
				{
					Conversion c = new Conversion(dbHelper_, 
							cur.getLong(idCi),
							cur.getLong(baseCi), 
							cur.getLong(targetCi), 
							cur.getDouble(fxCi), 
							cur.getString(formulaCi), 
							cur.getString(reversedFormulaCi));
					conversions_.add(c);
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
				finished_ = true;
				//notify other thread that this ones is finished
				countDownLatch_.countDown();
			}
		}
	}

	/**
	 * this AsynTask populates the DataRow list of the owner
	 */
	private final class FillDataTask extends AsyncTask<Long, Void, ArrayList<RowData>>
	{
		@Override
		protected ArrayList<RowData> doInBackground(Long... params)
		{
			ArrayList<RowData> resu = new ArrayList<RowData>();

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
							ResultListAdapter.this, baseUnitId,
							cur.getLong(idColumnIndex),
							cur.getString(nameColumnIndex),
							cur.getString(shortNameColumnIndex)
						);
				resu.add(co);
			}

			cur.close();
			
			//MainActivity.simulateLongOperation(1, 3);

			return resu;
		}
		
		@Override
		protected void onPostExecute(ArrayList<RowData> result)
		{
			synchronized (lock_)
			{
				data_ = result;
			}
			
			//reset filter
			if (filter_ != null)
			{
				filter_.resetFilterData();
			}
			
			if (result == null || result.size() == 0)
			{
				notifyDataSetInvalidated();
			}
			else
			{
				Log.i(TAG, String.format("Finished fill data for category %d, found %d units", categoryId_, result.size()));
				notifyDataSetChanged();
			}
		}
	};
	
	private class TargetUnitFilter extends Filter 
	{
		/**
		 * copy of the original data_, then the data_ will contains only item matching the filter
		 */
		private ArrayList<RowData> fullData_;
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			if (data_ == null && fullData_ == null) // data_ has not been populated (or the population is not finished yet) 
			{
				return null;
			}
			
			FilterResults resu = new FilterResults();
			
			if (fullData_ == null)
			{
				synchronized (lock_) 
				{
					fullData_ = new ArrayList<RowData>(data_);
				}
			}
			
			ArrayList<RowData> l;
			if (TextUtils.isEmpty(constraint))
			{
				synchronized (lock_) 
				{
					l = new ArrayList<RowData>(fullData_);
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
				l = new ArrayList<RowData>();
				for (int i = 0; i<count; i++)
				{
					final RowData row = fullData_.get(i);
					
					boolean matched = false;
					final String valueText = row.getKeyword();
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
			if (results == null)
			{
				return;
			}
			
			data_ = (ArrayList<RowData>) (results.values);
			
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
		
		/**
		 * Must be called each time the result list (data_) is re-populate
		 */
		public void resetFilterData()
		{
			if (fullData_ == null)
			{
				return;
			}
			// dump the data row in order to replace
			int n = fullData_.size();
			for (int i = 0; i < n; i++)
			{
				fullData_.get(i).cancelCalculation();
			}
			fullData_ = null;
		}
		
	}
}
