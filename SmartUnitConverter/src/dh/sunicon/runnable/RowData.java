package dh.sunicon.runnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Future;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import dh.sunicon.ResultListAdapter;
import dh.sunicon.datamodel.Conversion;
import dh.sunicon.datamodel.Corresponding;
import dh.sunicon.datamodel.EnumValue;

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
	private final long categoryId_;
	private final long baseUnitId_;
	private final long targetUnitId_;
	private final String targetUnitName_;
	private final String targetUnitShortName_;
	private final String keyword_;
	
	/* change on baseValue_ and value_ must be synchronized */
	private Double baseValue_ = Double.NaN;
	private double targetValue_ = Double.NaN;
	private EnumValue targetEnumValue_ = null; 
	private Long baseValueEnumId_ = (long)-1;
	
	private Future<?> futureResult_; 
	
	private volatile boolean cancelCalculation_ = false;
	
	private RowData(ResultListAdapter resultListAdapter, long categoryId, long baseUnitId, long targetUnitId, String targetUnitName,
			String targetUnitShortName)
	{
		super();
		this.resultListAdapter_ = resultListAdapter;
		categoryId_ = categoryId;
		baseUnitId_ = baseUnitId;
		targetUnitId_ = targetUnitId;
		targetUnitName_ = targetUnitName;
		targetUnitShortName_ = targetUnitShortName;
		keyword_ = String.format("%s %s", targetUnitShortName_, targetUnitName_).toLowerCase();
	}
	
	public RowData(ResultListAdapter resultListAdapter, long categoryId, long baseUnitId, long targetUnitId, String targetUnitName,
			String targetUnitShortName, double baseValue)
	{
		this(resultListAdapter, categoryId, baseUnitId, targetUnitId, targetUnitName, targetUnitShortName);
		setBaseValue(baseValue);
	}
	
	public RowData(ResultListAdapter resultListAdapter, long categoryId, long baseUnitId, long targetUnitId, String targetUnitName,
			String targetUnitShortName, long baseValueEnumId)
	{
		this(resultListAdapter, categoryId, baseUnitId, targetUnitId, targetUnitName, targetUnitShortName);
		setBaseValueEnum(baseValueEnumId);
	}
	
	public RowData(ResultListAdapter resultListAdapter, JSONObject json) throws JSONException
	{
		this(resultListAdapter, 
				json.getLong("categoryId"), 
				json.getLong("baseUnitId"), 
				json.getLong("targetUnitId"), 
				json.getString("targetUnitName"), 
				json.optString("targetUnitShortName")
				);
		if (json.has("baseValue")) {			
			baseValue_ = json.getDouble("baseValue");
		}
		if (json.has("targetValue")) {
			targetValue_ = json.getDouble("targetValue");
		}
		else {
			targetValue_ = Double.NaN;
		}
		if (json.has("targetEnumValue")) {			
			targetEnumValue_ = new EnumValue(resultListAdapter.getDbHelper(), json.getJSONObject("targetEnumValue"));
		}
		if (json.has("baseValueEnumId")) {
			baseValueEnumId_ = json.getLong("baseValueEnumId");
		}
	}
	
	public JSONObject serialize() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("categoryId", categoryId_);
		json.put("baseUnitId", baseUnitId_);
		json.put("targetUnitId", targetUnitId_);
		json.put("targetUnitName", targetUnitName_);
		json.put("targetUnitShortName", targetUnitShortName_);
		
		if (baseValue_!=null && !baseValue_.isNaN()) {	
			json.put("baseValue", baseValue_); //can be null
		}
		if (!Double.isNaN(targetValue_)) {
			json.put("targetValue", targetValue_);
		}
		if (targetEnumValue_!=null) {
			json.put("targetEnumValue", targetEnumValue_.serialize());
		}
		json.put("baseValueEnumId", baseValueEnumId_);
		
		return json;
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
		if (targetEnumValue_ != null)
		{
			return targetEnumValue_.toString();
		}
		
		return formatDouble(targetValue_);
	}
	
	public String getValueToCopy()
	{
		if (targetEnumValue_ != null)
		{
			return targetEnumValue_.getValue();
		}
		else
		{
			return Double.toString(targetValue_);
		}
	}
	
	/**
	 * return "km/h - Kilometer per hour"
	 */
	public String getFullUnitName()
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
	
	/**
	 * return the long name of the unit "Kilometer per hour"
	 */
	public String getUnitName()
	{
		return targetUnitName_;
	}
	
	public double getValue()
	{
		return targetValue_;
	}
	
	public long getCategoryId()
	{
		return categoryId_;
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
	public void setBaseValue(double baseValue)
	{
		if (baseValue_.equals(baseValue) && !Double.isNaN(targetValue_))
		{
			//no need to invoke calculation, the current targetValue_ is just right 
			return;
		}
		
		synchronized (baseValue_)
		{
			baseValue_ = baseValue;
			baseValueEnumId_ = (long) -1;
			clearTargetValue();
		}
		
		invokeCalculation();
	}	
	
	public void setBaseValueEnum(long enumId)
	{
		if (baseValueEnumId_ == enumId && targetEnumValue_ != null)
		{
			//no need to invoke calculation, the old value_ is just right 
			return;
		}
		
		synchronized (baseValueEnumId_)
		{
			baseValue_ = Double.NaN;
			baseValueEnumId_ = enumId;
			clearTargetValue();
		}
		
		invokeCalculation();
	}
	
	/**
	 * Cancel old calculation, start a new one
	 */
	private void invokeCalculation()
	{
		//synchronized (resultListAdapter_.calcFutureResult_)
		
		if (futureResult_!=null)
		{
			Log.v(TAG, "Cancel old base value");
			//cancel old calculation
			futureResult_.cancel(true);
			resultListAdapter_.unregisterCalculationFromWatingPool(futureResult_);
		}
		
		//start a new calculation
		futureResult_ = resultListAdapter_.getCalculationPoolThread().submit(this);
		resultListAdapter_.registerCalculationToWatingPool(futureResult_);
	}
	
	public void clearTargetValue()
	{
		targetValue_ = Double.NaN;
		targetEnumValue_ = null;
	}
	
	/**
	 * Add more security layer to make sure that the calculation will stop, it shoud be called before we dump this RowData
	 * Once this methode is called, this object row data cannot be re-used to perform calculation
	 */
	public void cancelCalculation()
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
			//if (baseValueEnumId_ < 0) //normal case: km/h..
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
				
				synchronized (baseValue_)
				{
					if (baseValue_.equals(originalValue)) //baseValue_ has not been changed during the calculation process
					{
						targetValue_ = resu;
						//invokeRefreshGui();
					}
					//else, a newer setBaseValue() was called, we must ignore the resu 
				}
			}
			//else //enum unit: clothing size..
			if (baseValueEnumId_ >= 0)
			{
				long originalValueEnumId;
				
				synchronized (baseValueEnumId_)
				{
					originalValueEnumId = baseValueEnumId_;
				}

				EnumValue resu = computeTargetValue(originalValueEnumId, baseUnitId_, targetUnitId_);
				
				synchronized (baseValueEnumId_)
				{
					if (baseValueEnumId_.equals(originalValueEnumId)) //baseValue_ has not been changed during the calculation process
					{
						targetEnumValue_ = resu;
					}
					//else, a newer setBaseValueEnum() was called, we must ignore the resu 
				}
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
	 * Convert the "36" of "clothing size woman france" to "XX-Small" of "clothing size woman US"; 
	 */
	EnumValue computeTargetValue(long baseValueEnumId, long baseUnitId, long targetUnitId) throws IllegalAccessException, InterruptedException
	{
		if (cancelCalculation_ || baseValueEnumId == -1)
		{
			return null;
		}
		
		ArrayList<Corresponding> correspondings = resultListAdapter_.getCorrespondings();
		HashMap<Long, EnumValue> enumValues = resultListAdapter_.getEnumValues();

		if (correspondings == null || enumValues == null)
		{
			Log.w(TAG, String.format("Cancel convert baseValueEnumId=%d from baseUnitId=%d to targetUnitId=%d. Because correspondings or enumValues tables is null", baseValueEnumId, baseUnitId, targetUnitId));
		}
		
		int correspondingsCount = correspondings.size();
		
		/* use BFS to find path from baseValueEnumId to one of enumValue of the targetUnitId on the correspondings graph */
		
		LinkedList<Long> visitedEnumValueQueue = new LinkedList<Long>();
		ArrayList<Long> visitedEnumValue = new ArrayList<Long>();
		visitedEnumValueQueue.offer(baseValueEnumId);
		
		while (!visitedEnumValueQueue.isEmpty() && !cancelCalculation_)
		{
			long visitingEnumValue = visitedEnumValueQueue.poll();
			visitedEnumValue.add(visitingEnumValue);
			
			// check if the target unit is reached
			
			EnumValue visitingEnumValueObj = enumValues.get(visitingEnumValue);
			if (visitingEnumValueObj != null && visitingEnumValueObj.getUnitId() == targetUnitId)
			{
				Log.v(TAG, String.format("Convert baseValueEnumId=%d from baseUnitId=%d to targetUnitId=%d, found enumId = %d", baseValueEnumId, baseUnitId, targetUnitId, visitingEnumValueObj.getId()));
				return visitingEnumValueObj;
			}
			
			// find all neighbors which are not visited
			
			for (int i = 0; i<correspondingsCount; i++)
			{
				Corresponding crp = correspondings.get(i);
				if (crp.isIncidentEdgeOf(visitingEnumValue))
				{
					long neigborEnumValue = crp.getOtherEnumId(visitingEnumValue);
					if (!visitedEnumValue.contains(neigborEnumValue) && !visitedEnumValueQueue.contains(neigborEnumValue))
					{
						visitedEnumValueQueue.offer(neigborEnumValue);
					}
				}
			}
		}
			
		Log.v(TAG, String.format("Convert baseValueEnumId=%d from baseUnitId=%d to targetUnitId=%d, NO RESU", baseValueEnumId, baseUnitId, targetUnitId));
		return null;
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
		if (cancelCalculation_ || Double.isNaN(baseValue))
		{
			return Double.NaN;
		}		
		
		ArrayList<Conversion> conversions = this.resultListAdapter_.getConversions();
		
		if (conversions == null)
		{
			Log.w(TAG, String.format("Cancel convert %f from baseUnitId=%d to targetUnitId=%d. Because conversions table is Null", baseValue, baseUnitId, targetUnitId));
			return Double.NaN;
		}
		
		Log.v(TAG, String.format("Start convert %f from baseUnitId=%d to targetUnitId=%d", baseValue, baseUnitId, targetUnitId));
		
		int conversionsCount = conversions.size();
		
		/* use BFS to find the shortest path from the baseUnitId to the targetUnitId on the Converions graph */
		
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
			
			for (int i=0; i<conversionsCount; i++)
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