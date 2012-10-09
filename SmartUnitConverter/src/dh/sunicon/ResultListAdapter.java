package dh.sunicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import dh.sunicon.datamodel.Corresponding;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.EnumValue;
import dh.sunicon.runnable.ConversionsLoadingRunner;
import dh.sunicon.runnable.RowData;

public class ResultListAdapter extends BaseAdapter implements Filterable
{	
	static final String TAG = ResultListAdapter.class.getName();
	private final LayoutInflater inflater_;
	private final DatabaseHelper dbHelper_;
	
	/**
	 * Thread Pool to calculate the converted value
	 */
	private final ExecutorService calculationPoolThread_ = Executors.newCachedThreadPool();
	/**
	 * the future result of calculation is stock in here
	 */
	private volatile Queue<Future<?>> calculationWatingPool_ = new LinkedList<Future<?>>();
	
	private final ExecutorService awaitCalculationThread_ = Executors
			.newSingleThreadExecutor();
	/**
	 * Thread to read all Conversion from DB
	 */
	private final ExecutorService conversionsLoadingThread_ = Executors
			.newSingleThreadExecutor();
	private ConversionsLoadingRunner conversionsLoadingRunner_ = null;
	private ConverterFragment owner_;
	private long categoryId_;
	private long baseUnitId_;
	private double baseValue_ = Double.NaN;
	private long baseValueEnumId_ = -1;
	private TargetUnitFilter filter_;
	private FillDataTask fillDataTask_;
	

//	Timer notifyDataSetChangedTimer_ = null;
//	
//	@Override
//	public void notifyDataSetChanged()
//	{
//		((MainActivity)context_).setResultListVisible(false);
//		if (notifyDataSetChangedTimer_!=null) 
//		{
//			notifyDataSetChangedTimer_.cancel(); //cancel the old onTextChange event
//		}
//		notifyDataSetChangedTimer_ = new Timer();
//		notifyDataSetChangedTimer_.schedule(new TimerTask()
//		{
//			@Override
//			public void run()
//			{
//				((MainActivity)context_).runOnUiThread(new Runnable()
//				{
//					@Override
//					public void run()
//					{
//						ResultListAdapter.super.notifyDataSetChanged();
//						((MainActivity)context_).setResultListVisible(true);
//					}
//				});
//			}
//		}, 300);  
//	}
	
	/**
	 * write lock on data_. any write operation on data_ must be synch on this lock_ 
	 */
	private final Object lock_ = new Object();
	private ArrayList<RowData> data_;
	
	public ResultListAdapter(ConverterFragment owner)
	{
		owner_ = owner;
		dbHelper_ = owner_.getDatabaseHelper();
		inflater_ = LayoutInflater.from(owner.getActivity());
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
		valueLabel.setText(Html.fromHtml(cr.getValueHtmlized()));
		unitLabel.setText(Html.fromHtml(cr.getUnitNameHtmlized()));

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
		
			((ConverterFragment)owner_).setResultListVisible(false);
			
			categoryId_ = categoryId;
			baseUnitId_ = baseUnitId;
			
			//read all conversion of the category
			if (conversionsLoadingRunner_ != null)
			{
				conversionsLoadingRunner_.cancel();
			}
			conversionsLoadingRunner_ = new ConversionsLoadingRunner(dbHelper_, categoryId_);
			conversionsLoadingThread_.execute(conversionsLoadingRunner_);
			
			data_ = null;
			
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
	 * @throws IllegalAccessException 
	 */
	public void setBaseValue(double baseValue, long baseValueEnumId) throws IllegalAccessException
	{
		if (onGuiThread())
		{
			Log.i(TAG, String.format("setBaseValue = %f, %d", baseValue, baseValueEnumId));
			
			baseValue_ = baseValue;
			baseValueEnumId_ = baseValueEnumId;
			
			/* set all the value to "-" */
			if (filter_!=null)
			{
				filter_.clearAllTargetValues();
			}
			
			if (data_ == null || data_.size() == 0)
			{
				Log.d(TAG, "RowData list is empty");
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
				if (baseValueEnumId>0 && Double.isNaN(baseValue))
				{
					data_.get(i).setBaseValueEnum(baseValueEnumId);
				}
				else
				{
					data_.get(i).setBaseValue(baseValue_);
				}
			}
			
			/* wait the calculations finished then update the list View */
			awaitCalculationThread_.execute(new Runnable()
			{
				@Override
				public void run()
				{
					awaitCalculation();
					owner_.getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								notifyDataSetChanged();
								((ConverterFragment)owner_).setResultListVisible(true);
							}
							catch (Exception ex)
							{
								Log.w(TAG, ex);
							}
						}
					});
				}
			});
		}
		else
		{
			throw new IllegalAccessException("setBaseValue() must be called from UI Thread. To be sure that data_ will not be changed during the computing");
		}
	}
	
	/**
	 * unregister a calculation so the methode awaitCalculation() will NOT wait for it to finish 
	 */
	public void unregisterCalculationFromWatingPool(Future<?> f)
	{
		calculationWatingPool_.remove(f);
	}
	
	/**
	 * register a calculation so the methode awaitCalculation() will wait for it to finish 
	 */
	public void registerCalculationToWatingPool(Future<?> f)
	{
		calculationWatingPool_.offer(f);
	}
	
	/**
	 * waiting for calculationPoolThread_ to finish all the calculation
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void awaitCalculation()
	{
		try
		{
			long startTime = System.currentTimeMillis();
			
			while (!calculationWatingPool_.isEmpty())
			{
				Future<?> futRe = calculationWatingPool_.poll();
				try
				{
					futRe.get();
				}
				catch (InterruptedException e)
				{
					Log.w(TAG, e);
				}
				catch (ExecutionException e)
				{
					Log.w(TAG, e);
				}
			}
			
			long endTime = System.currentTimeMillis();
			Log.i(TAG, "await calculation "+(endTime - startTime)+"ms");
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}

	public ExecutorService getCalculationPoolThread()
	{
		return calculationPoolThread_;
	}
	
	public ArrayList<Conversion> getConversions() throws IllegalAccessException, InterruptedException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getConversions();
	}

	public ArrayList<Corresponding> getCorrespondings() throws IllegalAccessException, InterruptedException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getCorrespondings();
	}
	
	public HashMap<Long, EnumValue> getEnumValues() throws IllegalAccessException, InterruptedException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getEnumValues();
	}
	
	private void waitConversionLoadingRunner() throws IllegalAccessException, InterruptedException
	{
		if (conversionsLoadingRunner_==null)
		{
			throw new IllegalAccessException("The conversion loading has not been started. Base Unit was not set");
		}
		if (!conversionsLoadingRunner_.isFinished())
		{
			conversionsLoadingRunner_.waitToFinish(10, TimeUnit.SECONDS);
		}
	}

	public boolean onGuiThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}
	
	
	/*
	 * **** Inner classes ****
	 */
	
	/**
	 * this AsynTask populates the DataRow list of the owner
	 */
	private final class FillDataTask extends AsyncTask<Long, Void, ArrayList<RowData>>
	{
		@Override
		protected ArrayList<RowData> doInBackground(Long... params)
		{
			try
			{
				ArrayList<RowData> resu = new ArrayList<RowData>();
	
				long categoryId = params[0];
				long baseUnitId = params[1];
				
				Cursor cur = dbHelper_.getReadableDatabase().
								query("unit", new String[]{"id", "name", "shortName"}, 
									"enabled=1 AND categoryId=? AND id<>?", 
									new String[] {Long.toString(categoryId), Long.toString(baseUnitId)}, 
									null, null, "name");
				
				int idColumnIndex = cur.getColumnIndex("id");
				int nameColumnIndex = cur.getColumnIndex("name");
				int shortNameColumnIndex = cur.getColumnIndex("shortName");
				double baseValue = baseValue_;
				long baseValueEnumId = baseValueEnumId_;
				
				
				while (cur.moveToNext() && !isCancelled()) 
				{
					RowData co;
					if (baseValueEnumId < 0)
					{
						co = new RowData(
									ResultListAdapter.this, categoryId, 
									baseUnitId,
									cur.getLong(idColumnIndex),
									cur.getString(nameColumnIndex),
									cur.getString(shortNameColumnIndex),
									baseValue
								);
					}
					else
					{
						co = new RowData(
								ResultListAdapter.this, categoryId, 
								baseUnitId,
								cur.getLong(idColumnIndex),
								cur.getString(nameColumnIndex),
								cur.getString(shortNameColumnIndex),
								baseValueEnumId
							);
					}
					resu.add(co);
				}
	
				cur.close();
				
				//MainActivity.simulateLongOperation(1, 3);
	
				awaitCalculation();
				
				return resu;
			}
			catch (Exception ex)
			{
				Log.w(TAG, ex);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(ArrayList<RowData> result)
		{
			try
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
					ResultListAdapter.this.setBaseValue(baseValue_, baseValueEnumId_); //redo the calculation
				}
				((ConverterFragment)owner_).setResultListVisible(true);
			}
			catch (Exception e)
			{
				Log.w(TAG, e);
			}
		}
	};
	
	private class TargetUnitFilter extends Filter 
	{
		/**
		 * copy of the original data_, then the data_ will contains only item matching the filter
		 */
		private ArrayList<RowData> fullData_;
		
		private final int DELAY_PERFORM_FILTERING = 500;
		private Object lockLastConstraint_ = new Object();
		private String lastConstraint_;
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			try
			{
				if (data_ == null && fullData_ == null) // data_ has not been populated (or the population is not finished yet) 
				{
					return null;
				}
			
				/* delayer events technique */
				
				if (constraint!=null)
				{
					synchronized (lockLastConstraint_)
					{
						lastConstraint_ = new String(constraint.toString());
					}
				}
				
				Thread.sleep(DELAY_PERFORM_FILTERING);
			
				if (lastConstraint_!=null)
				{
					if (!lastConstraint_.equals(constraint))
					{
						/*
						 * lastConstraint_ has been changed after 500ms 
						 * => other performFiltering has been called
						 * => no need to execute this one
						 */ 
						return null; 
					}
				}
				
				/* main */
				
				Log.d(TAG, "Perform filtering: "+constraint);
				
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
							if (baseValueEnumId_<0)
							{
								l.get(i).setBaseValue(baseValue_);
							}
							else
							{
								l.get(i).setBaseValueEnum(baseValueEnumId_);
							}
						}
					}
					awaitCalculation();
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
			catch (Exception ex)
			{
				Log.w(TAG, ex);
				return null;
			}
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
