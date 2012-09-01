package dh.sunicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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
	/* change of baseValue_ and value_ must be synchronized */
	private Double baseValue_ = Double.NaN;
	private long baseUnitId_;
	private long targetUnitId_;
	private String targetUnitName_;
	private String targetUnitShortName_;
	private double value_ = Double.NaN;
	
	public RowData(ResultListAdapter resultListAdapter, long baseUnitId, long targetUnitId, String targetUnitName,
			String targetUnitShortName)
	{
		super();
		this.resultListAdapter_ = resultListAdapter;
		baseUnitId_ = baseUnitId;
		targetUnitId_ = targetUnitId;
		targetUnitName_ = targetUnitName;
		targetUnitShortName_ = targetUnitShortName;
		this.setBaseValue(this.resultListAdapter_.getBaseValue());
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
		if (TextUtils.isEmpty(targetUnitName_))
		{
			return targetUnitShortName_;
		}
		if (targetUnitShortName_.equals(targetUnitName_))
		{
			return targetUnitShortName_;
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
	 * Compute the target value_ in the thread pool then refresh the UI
	 * @param baseValue
	 */
	void setBaseValue(double baseValue)
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
		
		this.resultListAdapter_.getCalculationPoolThread().execute(this);
	}
	
	boolean clearTargetValue()
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
			/* copy the current baseValue_ to original Value */
			double originalValue;
			synchronized (baseValue_)
			{
				originalValue = baseValue_;
			}
			
			double resu;
			
			/* sstart the calculation process */
			
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
			
			synchronized (baseValue_)
			{
				if (baseValue_.equals(originalValue)) //baseValue_ has not been changed during the calculation process
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
		((MainActivity)this.resultListAdapter_.getContext()).runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					RowData.this.resultListAdapter_.notifyDataSetChanged();
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		}); 
	}
	
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
		Log.d(ResultListAdapter.TAG, String.format("Start convert %f from baseUnitId=%d to targetUnitId=%d", baseValue, baseUnitId, targetUnitId));
		
		/* use BFS to find the shortest path from the baseUnitId to the targetUnitId on the Converions graph */
		
		ArrayList<Conversion> conversions = this.resultListAdapter_.getConversions();
		LinkedList<Long> visitedUnitQueue = new LinkedList<Long>();
		HashMap<Long, Conversion> previous = new HashMap<Long, Conversion>();
		ArrayList<Long> visitedUnit = new ArrayList<Long>();  
		boolean pathFound = false; //turn to true if we can build a path from baseUnitId to targetUnitId
		
		//add source to baseUnitId
		visitedUnitQueue.offer(baseUnitId);
		previous.put(baseUnitId, null);
		
		while (!visitedUnitQueue.isEmpty())
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
		while (uid != baseUnitId);
		
		/* use the path to convert the value */
		
		double returned = baseValue;
		uid = baseUnitId;
		while (!path.isEmpty())
		{
			conv = path.poll();
			returned = conv.convert(returned, uid);
			uid = conv.getOtherUnitId(uid);
		}
			
		return returned;
	}
	
}