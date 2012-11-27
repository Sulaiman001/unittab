package dh.sunicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import dh.sunicon.workarounds.DoubleFormatter;

public class ResultListAdapter extends BaseAdapter implements Filterable
{	
	static final String TAG = ResultListAdapter.class.getName();
	private final LayoutInflater inflater_;
	private final DatabaseHelper dbHelper_;
	
	/**
	 * Thread Pool to calculate the converted value
	 */
	//private final ExecutorService calculationPoolThread_ = Executors.newCachedThreadPool();
	private final ExecutorService calculationPoolThread_ = Executors.newFixedThreadPool(MAX_THREADS_CALCULATION);
	
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
	private Future<?> conversionsLoadingFuture_;
	private ConversionsLoadingRunner conversionsLoadingRunner_ = null;
	private ConverterFragment owner_;
	private long categoryId_;
	private long baseUnitId_;
	private double baseValue_ = Double.NaN;
	private long baseValueEnumId_ = -1;
	private TargetUnitFilter filter_;
	private FillDataTask fillDataTask_;
	
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
	
	public ResultListAdapter(ConverterFragment owner, JSONObject jsonSavedState) throws JSONException {
		this(owner);
		categoryId_ = jsonSavedState.getLong("categoryId");
		baseUnitId_ = jsonSavedState.getLong("baseUnitId");
		if (jsonSavedState.has("baseValue")) {
			baseValue_ = jsonSavedState.getDouble("baseValue");
		}
		else {
			baseValue_ = Double.NaN;
		}
		baseValueEnumId_ = jsonSavedState.getLong("baseValueEnumId");
	
		if (jsonSavedState.has("filter"))
			filter_ = new TargetUnitFilter(this, jsonSavedState.getJSONArray("filter"));
		
		if (jsonSavedState.has("conversionsLoadingRunner"))
		{
			conversionsLoadingRunner_ = new ConversionsLoadingRunner(dbHelper_, jsonSavedState.getJSONObject("conversionsLoadingRunner"));
			conversionsLoadingFuture_ = conversionsLoadingThread_.submit(conversionsLoadingRunner_);
		}
		
		if (jsonSavedState.has("data"))
		{
			JSONArray jsonRowData = jsonSavedState.getJSONArray("data");
			if (jsonRowData!=null)
			{
				int n = jsonRowData.length();
				if (n<=0){ 
					return;
				}
				data_ = new ArrayList<RowData>();
				for (int i = 0; i<n; i++) {
					data_.add(
						new RowData(
								this,
								jsonRowData.getJSONObject(i))
					);
				}
			}
		}
		else {
			invokeFillData();
		}
	}
	
	public JSONObject serialize() throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("categoryId", categoryId_);
		json.put("baseUnitId", baseUnitId_);
		if (!Double.isNaN(baseValue_)) {
			json.put("baseValue", baseValue_);
		}
		json.put("baseValueEnumId", baseValueEnumId_);
		
		if (conversionsLoadingRunner_!=null)
			json.put("conversionsLoadingRunner", conversionsLoadingRunner_.serialize());
	
		if (filter_ != null)
			json.put("filter", filter_.serialize());
		
		if (data_!=null) {
			JSONArray jsonRowData = new JSONArray();
			for (RowData r : data_){
				jsonRowData.put(r.serialize());
			}
			json.put("data", jsonRowData);
		}
		
		return json;
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

		RowData cr = null;
		if (data_ != null && position < data_.size()) {
			cr = data_.get(position);
		}
		if (cr != null) {
			valueLabel.setText(Html.fromHtml(cr.getValueHtmlized()));
			unitLabel.setText(Html.fromHtml(cr.getUnitNameHtmlized()));
		}
		else {
			valueLabel.setText("-");
			unitLabel.setText("-");
		}

		return v;
	}
	
	@Override
	public Filter getFilter()
	{
		if (filter_ == null)
			filter_ = new TargetUnitFilter();
		return filter_;
	}
	
	/**
	 * Fill data_ list with all units in the category except the baseUnit + read the Conversion graph 
	 */
	public void setBaseUnitId(long categoryId, long baseUnitId) throws IllegalAccessException
	{
		if (!onGuiThread())
			throw new IllegalAccessException("this methode must be called from UI Thread.");
		
		boolean sameCategory = (categoryId == categoryId_);
		
		if (sameCategory && baseUnitId == baseUnitId_)
		{
			((ConverterFragment)owner_).setComputationStateFinished(true);
			return; //nothing changed
		}
		
		Log.d(TAG, String.format("setBaseUnit category = %d baseUnit = %d", categoryId, baseUnitId));
	
		((ConverterFragment)owner_).setComputationStateFinished(false);
		
		categoryId_ = categoryId;
		baseUnitId_ = baseUnitId;
		
		/*read all conversion of the category*/
		
		/* replace old conversionsLoadingRunner_ by a new one. No need to lock conversionsLoadingRunner_ because this code runs on main thread */
		if (conversionsLoadingRunner_ != null)
			conversionsLoadingRunner_.dumpIt();
		conversionsLoadingRunner_ = new ConversionsLoadingRunner(dbHelper_, categoryId_, baseUnitId_);
		conversionsLoadingFuture_ = conversionsLoadingThread_.submit(conversionsLoadingRunner_);
		
		if (sameCategory ) {
			invokeCalculation(false);
		}
		else {
			data_ = null;
			
			/*fill the list with related target unit (of the same category)*/
			invokeFillData();
		}
		
	}

	/**
	 * set base value and compute conversion values
	 * @throws IllegalAccessException 
	 */
	public void setBaseValue(Double baseValue, Long baseValueEnumId) throws IllegalAccessException
	{
		if (!onGuiThread())
			throw new IllegalAccessException("setBaseValue() must be called from UI Thread. To be sure that data_ will not be changed during the computing");
		
		if ((baseValue!=null && !baseValue.equals(baseValue_)) || (baseValueEnumId!=null && !baseValueEnumId.equals(baseValueEnumId_)))
		{
			Log.v(TAG, String.format("setBaseValue = %f, %d", baseValue, baseValueEnumId));
			
			if (baseValue!=null)
				baseValue_ = baseValue;
			
			if (baseValueEnumId!=null)
				baseValueEnumId_ = baseValueEnumId;

			/* calculate all value */
			
			invokeCalculation(false);
		}
		else
		{
			Log.w(TAG, "Ignore calculation because the baseValue has not been changed");
			((ConverterFragment)owner_).setComputationStateFinished(true);
		}
	}

	public void reComputeAll() throws IllegalAccessException {

		if (!onGuiThread())
			throw new IllegalAccessException("reComputeAll() must be called from UI Thread");
		
		Log.v(TAG, "reComputeAll");
		
		((ConverterFragment)owner_).setComputationStateFinished(false);
		
		/* refresh Conversions. No need to lock conversionsLoadingRunner_ because this code runs on main thread */
		
		if (conversionsLoadingRunner_ != null)
			conversionsLoadingRunner_.dumpIt();
		conversionsLoadingRunner_ = new ConversionsLoadingRunner(dbHelper_, categoryId_, baseUnitId_);
		conversionsLoadingFuture_ = conversionsLoadingThread_.submit(conversionsLoadingRunner_);
		
//		/* re-calculate target value */
//
//		if (data_!= null)
//		{
//			for (RowData rowdata : data_)
//			{
//				rowdata.clearTargetValue(); //reset target value
//				rowdata.setBase(baseValue_, baseValueEnumId_, baseUnitId_); //recalcule target value
//			}
//		}
		
		//invalid all targetValue
		if (filter_!=null) {
			filter_.clearAllTargetValues();
		}
		
		invokeCalculation(true);
	}
	
	/* wait the calculations finished then update the list View */
	private void invokeGuiUpdateAfterCalculation()
	{
		awaitCalculationThread_.execute(new Runnable()
		{
			@Override
			public void run()
			{
				awaitCalculation();
				if (owner_.getActivity() == null)
				{
					return;
				}
				owner_.getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							if (data_!=null && data_.size() > 0)
							{
								notifyDataSetChanged();
							}
							else
							{
								notifyDataSetInvalidated();
							}
							((ConverterFragment)owner_).setComputationStateFinished(true);
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
	
//	/**
//	 * unregister a calculation so the methode awaitCalculation() will NOT wait for it to finish 
//	 */
//	public void unregisterCalculationFromWatingPool(Future<?> f)
//	{
//		try {
//			calculationWatingPool_.remove(f);
//		}
//		catch (Exception ex) {
//			Log.w(TAG, ex);
//		}
//	}
	
	/**
	 * register a calculation so the methode awaitCalculation() will wait for it to finish 
	 */
	public void registerCalculationToWatingPool(Future<?> f)
	{
		try {
			calculationWatingPool_.offer(f);
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
		}
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
			long startTime = DatabaseHelper.getNow();
			
			while (!calculationWatingPool_.isEmpty())
			{
				Future<?> futRe = calculationWatingPool_.poll();
				try
				{
					futRe.get();
				}
				catch (Exception e)
				{
					Log.w(TAG, e);
				}
			}
			
			Log.i(TAG, "await calculation "+(DatabaseHelper.getNow() - startTime)+"ms");
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}
	/**
	 * waiting for calculationPoolThread_ to finish all the calculation
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void cancelCalculation()
	{
		try
		{
			if (!calculationWatingPool_.isEmpty())
				Log.i(TAG, "cancel "+calculationWatingPool_.size()+ " batch calculation");
			
			//long startTime = DatabaseHelper.getNow();
			
			while (!calculationWatingPool_.isEmpty())
			{
				Future<?> futRe = calculationWatingPool_.poll();
				futRe.cancel(true);
			}
			
			//Log.d(TAG, "cancel calculation "+(DatabaseHelper.getNow() - startTime)+"ms");
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}

	public ArrayList<Conversion> getConversions() throws InterruptedException, ExecutionException, TimeoutException, IllegalAccessException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getConversions();
	}

	public ArrayList<Corresponding> getCorrespondings() throws InterruptedException, ExecutionException, TimeoutException, IllegalAccessException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getCorrespondings();
	}
	
	public HashMap<Long, EnumValue> getEnumValues() throws InterruptedException, ExecutionException, TimeoutException, IllegalAccessException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getEnumValues();
	}
	
	public long getOptimizeCurrencyId() throws InterruptedException, ExecutionException, TimeoutException, IllegalAccessException
	{
		waitConversionLoadingRunner();
		return conversionsLoadingRunner_.getOptimizeCurrencyId();
	}
	
	private void waitConversionLoadingRunner() throws InterruptedException, ExecutionException, TimeoutException, IllegalAccessException
	{
		if (conversionsLoadingFuture_==null)
		{
			throw new UnsupportedOperationException("The conversion loading has not been started. Base Unit was not set");
		}
	
		long startTime = DatabaseHelper.getNow();
		
		conversionsLoadingFuture_.get(10, TimeUnit.SECONDS);
		
		long elapsed = DatabaseHelper.getNow()-startTime;
		if (elapsed >= 1000) {
			Log.i(TAG, String.format("waitConversionLoadingRunner %d ms",elapsed));
		}
	}

	public boolean onGuiThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}
	
	public DatabaseHelper getDbHelper()
	{
		return dbHelper_;
	}
	
	private void invokeFillData()
	{
		Log.d(TAG, "InvokeFillData");
		cancelCalculation();
		data_ = null;
		if (fillDataTask_ != null) {
			fillDataTask_.cancel(true);
		}
		fillDataTask_ = new FillDataTask();
		fillDataTask_.execute(categoryId_, baseUnitId_);
	}
	
	private final static int MAX_THREADS_CALCULATION = 1;
	
	public void invokeCalculation(boolean forceCalculation) throws IllegalAccessException {
		if (!onGuiThread()) {
			throw new IllegalAccessException();
		}
		
		if (data_ == null || data_.isEmpty())
		{
			Log.w(TAG, "RowData list is empty");
			notifyDataSetInvalidated();
			((ConverterFragment)owner_).setComputationStateFinished(true);
			return;
		}
		
		((ConverterFragment)owner_).setComputationStateFinished(false);

		cancelCalculation();
		
		for (RowData r : data_) {
			r.setBase(baseValue_, baseValueEnumId_, baseUnitId_, forceCalculation);
		}
		
		int count = data_.size();
		int rowDataPerBatch = (count / MAX_THREADS_CALCULATION)+1;
		
		Log.i(TAG,(forceCalculation? "Force " : "" )+"invokeCalculation rowDataCount="+count);
		
		int s = 0;
		int e = rowDataPerBatch;
		while (s<count) {
			if (e>count) {
				e = count;
			}
			BatchCalculation bc = new BatchCalculation(data_.subList(s, e));
			registerCalculationToWatingPool(calculationPoolThread_.submit(bc));
			s = s + rowDataPerBatch;
			e = e + rowDataPerBatch;
		}
		invokeGuiUpdateAfterCalculation();
	}
	
	public DoubleFormatter getDoubleFormatter()
	{
		return owner_.getDoubleFormatter();
	}
	/*
	 * **** Inner classes ****
	 */
	private final class BatchCalculation implements Callable<Integer> {

		private List<RowData> data_;
		
		public BatchCalculation(List<RowData> data)
		{
			super();
			data_ = data;
		}

		@Override
		public Integer call() throws Exception
		{
			int c = 0;
			for (RowData r : data_) {
				if (Thread.currentThread().isInterrupted()) {
					return c;
				}
				r.run();
				c++;
			}
			return c;
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
			try
			{
				ArrayList<RowData> resu = new ArrayList<RowData>();
	
				long categoryId = params[0];
				long baseUnitId = params[1];
				
//				Cursor cur = dbHelper_.getReadableDatabase().
//								query("unit", new String[]{"id", "name", "shortName"}, 
//									"enabled=1 AND categoryId=? AND id<>?", 
//									new String[] {Long.toString(categoryId), Long.toString(baseUnitId)}, 
//									null, null, "lower(name)");
				
				Cursor cur = dbHelper_.getReadableDatabase().
						query("unit", new String[]{"id", "name", "shortName"}, 
							"enabled=1 AND categoryId=?", 
							new String[] {Long.toString(categoryId)}, 
							null, null, "lower(name)");
				try
				{
				
					int idColumnIndex = cur.getColumnIndex("id");
					int nameColumnIndex = cur.getColumnIndex("name");
					int shortNameColumnIndex = cur.getColumnIndex("shortName");
					
					while (cur.moveToNext() && !isCancelled()) 
					{
						RowData co = new RowData(
								ResultListAdapter.this, categoryId, 
								baseUnitId,
								cur.getLong(idColumnIndex),
								cur.getString(nameColumnIndex),
								cur.getString(shortNameColumnIndex),
								baseValue_, baseValueEnumId_
							);
						resu.add(co);
					}
				}
				finally {
					cur.close();
				}
				
				//MainActivity.simulateLongOperation(1, 3);
	
				//awaitCalculation();
				
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
				Log.i(TAG, "FillData finish catId="+categoryId_);
				data_ = result;
				if (filter_!=null) {
					filter_.resetFilter();
				}
				invokeCalculation(false);
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
		
		public TargetUnitFilter() {
			super();
		}
		
		public TargetUnitFilter(ResultListAdapter owner, JSONArray jsonData) throws JSONException{
			super();
			if (jsonData == null) {
				return;
			}
			int n = jsonData.length();
			if (n<=0){ 
				return;
			}
			fullData_ = new ArrayList<RowData>();
			for (int i = 0; i<n; i++) {
				fullData_.add(
					new RowData(
							owner,
							jsonData.getJSONObject(i))
				);
			}
		}
		
		public JSONArray serialize() throws JSONException {
			if (fullData_ == null) {
				return null;
			}
			JSONArray json = new JSONArray();
			for (RowData r : fullData_){
				json.put(r.serialize());
			}
			return json;
		}
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			try
			{
				if (data_ == null && fullData_ == null) // data_ has not been populated (or the population is not finished yet) 
				{
					return null;
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
					l = new ArrayList<RowData>(fullData_);
				}
				else
				{
					final String filterText = constraint.toString().toLowerCase(Locale.US);
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
			try
			{
				invokeCalculation(false);
			}
			catch (IllegalAccessException e)
			{
				Log.w(TAG, e);
			}
		}
		
		public void resetFilter() {
			fullData_ = null;
		}
		
		public void clearAllTargetValues()
		{
			for (RowData row : fullData_)
			{
				row.clearTargetValue();
			}
		}
	}
}
