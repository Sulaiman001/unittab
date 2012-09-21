package dh.sunicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Future;

import android.text.TextUtils;
import android.util.Log;
import dh.sunicon.datamodel.Conversion;

/**
 * data of a Row in the result list, it is only used by ResultListAdapter
 * call setBaseValue() will invoke calculation on the pool thread of the owner resultListAdapter
 */
public final class RowData implements Runnable
{
	static final String TAG = RowData.class.getName();
	
	/**
	 * the owner
	 */
	private final ResultListAdapter resultListAdapter_;
	private final long baseUnitId_;
	private final long targetUnitId_;
	private final String targetUnitName_;
	private final String targetUnitShortName_;
	private final String keyword_;
	
	/* change on baseValue_ and value_ must be synchronized */
	private Double baseValue_ = Double.NaN;
	private double value_ = Double.NaN;
	private String valueHtmlized_ = "-";
	
	private Future<?> futureResult_; 
	
	private volatile boolean cancelCalculation_ = false;
	
	public RowData(ResultListAdapter resultListAdapter, long baseUnitId, long targetUnitId, String targetUnitName,
			String targetUnitShortName, double baseValue)
	{
		super();
		this.resultListAdapter_ = resultListAdapter;
		baseUnitId_ = baseUnitId;
		targetUnitId_ = targetUnitId;
		targetUnitName_ = targetUnitName;
		targetUnitShortName_ = targetUnitShortName;
		keyword_ = String.format("%s %s", targetUnitShortName_, targetUnitName_).toLowerCase();
		
		setBaseValue(baseValue);
	}
	public long getUnitId()
	{
		return targetUnitId_;
	}
	public String getUnitNameHtmlized()
	{
		if (TextUtils.isEmpty(targetUnitShortName_))
		{
			return targetUnitName_;
		}
		if (TextUtils.isEmpty(targetUnitName_))
		{
			return "<b>" + targetUnitShortName_ + "</b>";
		}
		if (targetUnitShortName_.equals(targetUnitName_))
		{
			return targetUnitShortName_;
		}
		return String.format("<b>%s</b> - %s", targetUnitShortName_,
				targetUnitName_);
	}
	public String getValueHtmlized()
	{
		if (TextUtils.isEmpty(valueHtmlized_))
		{
			return "-";
		}
		return valueHtmlized_;
	}

	public String getUnitName()
	{
		if (TextUtils.isEmpty(targetUnitShortName_))
		{
			return targetUnitName_;
		}
		if (TextUtils.isEmpty(targetUnitName_))
		{
			return targetUnitShortName_;
		}
		return String.format("%s - %s", targetUnitShortName_, targetUnitName_);
	}
	
	public double getValue()
	{
		return value_;
	}
	
	public long getTargetUnitId()
	{
		return targetUnitId_;
	}
	
	public String getKeyword()
	{
		return keyword_;
	}
	
	/**
	 * Compute the target value_ in the thread pool then refresh the UI
	 * this methode comes with a latch to observer when the calculation will
	 * be terminated (the latch will be count down).
	 * each call of this method should use a different latch
	 * @param baseValue
	 */
	void setBaseValue(double baseValue)
	{
		if (baseValue_.equals(baseValue) && !TextUtils.isEmpty(valueHtmlized_))
		{
			//no need to invoke calculation, the old value_ is just right 
			return;
		}
		
		synchronized (baseValue_)
		{
			baseValue_ = baseValue;
			value_ = Double.NaN; //reset the result before entering the calculation process
			valueHtmlized_ = null;
		}
		
		//synchronized (resultListAdapter_.calcFutureResult_)
		{
			if (futureResult_!=null)
			{
				Log.d(TAG, "Cancel old base value");
				//cancel old calcule
				futureResult_.cancel(true);
				resultListAdapter_.calcFutureResult_.remove(futureResult_);
			}
			
			futureResult_ = resultListAdapter_.getCalculationPoolThread().submit(this);
			resultListAdapter_.calcFutureResult_.offer(futureResult_);
		}
	}
	
	
	boolean clearTargetValue()
	{
		boolean isValueChanged = !Double.isNaN(baseValue_);
		value_ = Double.NaN;
		valueHtmlized_ = null;
		return isValueChanged;
	}
	
	/**
	 * Add more security layer to make sure that the calculation will stop, it shoud be called before we dump this RowData
	 * Once this methode is called, this object row data cannot be re-used to perform calculation
	 */
	void cancelCalculation()
	{
		cancelCalculation_ = true;
	}
	
	/**
	 * Compute the value
	 */
	@Override
	public void run()
	{
		try
		{
			/* copy the current baseValue_ to original Value */
			double originalValue;
			synchronized (baseValue_)
			{
				originalValue = baseValue_;
			}
			
			double resu;
			
			/* start the calculation process */
			
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
				resu = computeTargetValue(originalValue, baseUnitId_, targetUnitId_);
				//MainActivity.simulateLongOperation(1, 4);
			}
			
			String resuStr = formatDouble(resu);
			
			synchronized (baseValue_)
			{
				if (baseValue_.equals(originalValue)) //baseValue_ has not been changed during the calculation process
				{
					value_ = resu;
					valueHtmlized_ = resuStr;
					//invokeRefreshGui();
				}
				//else, a newer setBaseValue() was called, we must ignore the resu 
			}
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}
	
//	private void invokeRefreshGui()
//	{
//		((MainActivity)this.resultListAdapter_.getContext()).runOnUiThread(new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				try
//				{
//					RowData.this.resultListAdapter_.notifyDataSetChanged();
//				}
//				catch (Exception ex)
//				{
//					Log.w(TAG, ex);
//				}
//			}
//		}); 
//	}
	
	/**
	 * Convert the base value using the conversion table.
	 * Algo breadth-first search (BFS) find the shortest path from baseUnitId to targetUnitId 
	 * on the 'conversions' graph
	 * Input data
	 * - the baseUnitId,
	 * - targetUnitId
	 * - conversions is the conversion graph (link between unit_unit) 
	 * - ResultListAdapter.data_ contains all unitId of the category except the baseUnitId
	 * 
	 * @return the converted value
	 * 
	 * @throws InterruptedException 
	 * @throws IllegalAccessException 
	 */
	double computeTargetValue(double baseValue, long baseUnitId, long targetUnitId) throws IllegalAccessException, InterruptedException
	{
		if (cancelCalculation_)
		{
			return Double.NaN;
		}		
		
		/* use BFS to find the shortest path from the baseUnitId to the targetUnitId on the Converions graph */
		
		ArrayList<Conversion> conversions = this.resultListAdapter_.getConversions();
		
		Log.d(ResultListAdapter.TAG, String.format("Start convert %f from baseUnitId=%d to targetUnitId=%d", baseValue, baseUnitId, targetUnitId));
		
		LinkedList<Long> visitedUnitQueue = new LinkedList<Long>();
		HashMap<Long, Conversion> previous = new HashMap<Long, Conversion>();
		ArrayList<Long> visitedUnit = new ArrayList<Long>();  
		boolean pathFound = false; //turn to true if we can build a path from baseUnitId to targetUnitId
		
		//add source to baseUnitId
		visitedUnitQueue.offer(baseUnitId);
		previous.put(baseUnitId, null);
		
		while (!visitedUnitQueue.isEmpty() && !cancelCalculation_)
		{
			long visitingUnit = visitedUnitQueue.poll();
			visitedUnit.add(visitingUnit);
			
			if (visitingUnit == targetUnitId)
			{
				pathFound = true;
				break; //gotcha!
			}
			
			//find all neighbors which are not visited
			int n = conversions.size();
			for (int i=0; i<n; i++)
			{
				Conversion conv = conversions.get(i);
				
				if (conv.isIncidentEdgeOf(visitingUnit))
				{
					long neighborUnit = conv.getOtherUnitId(visitingUnit);
					//check if it is not visited yet
					if (!visitedUnitQueue.contains(neighborUnit) && !visitedUnit.contains(neighborUnit))
					{
						//neighborUnit is not visited => add it to the queue
						visitedUnitQueue.offer(neighborUnit);
						previous.put(neighborUnit, conv);
					}
				}
			}
		}
		
		/* play back (using previous list) to build the path and compute the value */
		
		if (!pathFound)
		{
			return Double.NaN;
		}
		
		LinkedList<Conversion> path = new LinkedList<Conversion>();
		long uid = targetUnitId;
		Conversion conv;
		
		do {
			conv = previous.get(uid); 
			path.addFirst(conv);
			uid = conv.getOtherUnitId(uid); 
		}
		while (uid != baseUnitId && !cancelCalculation_);
		
		/* use the path to convert the value */
		
		double returned = baseValue;
		uid = baseUnitId;
		while (!path.isEmpty() && !cancelCalculation_)
		{
			conv = path.poll();
			returned = conv.convert(returned, uid);
			uid = conv.getOtherUnitId(uid);
		}
		
		if (cancelCalculation_)
		{
			return Double.NaN;
		}
		
		return returned;
	}
	
	/**
	 * Format 3.1416e+5 to "<b>3</b>.1416<b>e+5</b>" 
	 * @param d
	 * @return
	 */
	public static String formatDouble(double d)
	{
		if (Double.isNaN(d))
		{
			return "-";
		}
		String s = String.format("%.12g", d);
		StringBuilder resu = new StringBuilder("<b>");
		
		int firstPoint = s.indexOf('.');
		int firstComma = s.indexOf(',');
		int p1 = Math.max(firstPoint, firstComma);
		
		if (p1>0)
		{
			resu.append(s.substring(0, p1));
			resu.append("</b>");
		}
		else
		{
			p1 = 0;
		}
		
		int p2 = s.lastIndexOf('e');
		if (p2>0)
		{
			resu.append(s.substring(p1, p2));
			resu.append("<b>");
			resu.append(s.substring(p2, s.length()));
			resu.append("</b>");
		}
		else
		{
			resu.append(s.substring(p1, s.length()));
			if (p1==0)
			{
				resu.append("</b>");
			}
		}
		
		return resu.toString();
	}
	
}