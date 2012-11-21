package dh.sunicon.runnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import dh.sunicon.ResultListAdapter;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.Conversion;
import dh.sunicon.datamodel.Corresponding;
import dh.sunicon.datamodel.EnumValue;
import dh.sunicon.datamodel.Unit;

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
	
	private final long targetUnitId_;
	private final String targetUnitName_;
	private final String targetUnitShortName_;
	private final String keyword_;
	
	/* change on baseValue_ and targetValue_ must be synchronized */
	private volatile long baseUnitId_;
	private volatile double baseValue_ = Double.NaN;
	private volatile long baseValueEnumId_ = -1L;
	
	private Long lastBaseUnitId_ = null;
	private Double lastBaseValue_ = null;
	private Long lastBaseValueEnumId_ = null;

	private double targetValue_ = Double.NaN;
	private String targetValueHtml_ = "-";
	private EnumValue targetEnumValue_ = null; 
	
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
		keyword_ = String.format("%s %s", targetUnitShortName_, targetUnitName_).toLowerCase(Locale.US);
	}
	
	public RowData(ResultListAdapter resultListAdapter, 
			long categoryId, 
			long baseUnitId, 
			long targetUnitId, 
			String targetUnitName,
			String targetUnitShortName, 
			double baseValue, 
			long baseValueEnumId)
	{
		this(resultListAdapter, categoryId, baseUnitId, targetUnitId, targetUnitName, targetUnitShortName);
		baseUnitId_ = baseUnitId;
		baseValue_ = baseValue;
		targetValue_ = Double.NaN;
		targetValueHtml_ = "-";
		baseValueEnumId_ = baseValueEnumId;
		targetEnumValue_ = null;
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
			targetValueHtml_ = json.getString("targetValueHtml");
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
		
		if (!Double.isNaN(baseValue_)) {	
			json.put("baseValue", baseValue_); //can be null
		}
		if (!Double.isNaN(targetValue_)) {
			json.put("targetValue", targetValue_);
			json.put("targetValueHtml", targetValueHtml_);
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
		
		return targetValueHtml_;
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
	 * 
	 * must call from UI thread!
	 * @param baseValue
	 * @throws IllegalAccessException 
	 */
	public void setBase(double baseValue, long enumId, long baseUnitId, boolean forceClearTarget) throws IllegalAccessException
	{
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			throw new IllegalAccessException("Must be call on GUI thread");
		}

		if (forceClearTarget) {
			baseUnitId_ = baseUnitId;
			baseValue_ = baseValue;
			baseValueEnumId_ = enumId;
		
			targetValue_ = Double.NaN;
			targetValueHtml_ = "-";
			targetEnumValue_ = null;
		}
		else {
			if (baseUnitId_!=baseUnitId) {
				baseUnitId_ = baseUnitId;
				
				//clean all target
				targetValue_ = Double.NaN;
				targetValueHtml_ = "-";
				targetEnumValue_ = null;
			}
			
			if (baseValue_ != baseValue) {
				baseValue_ = baseValue;
				targetValue_ = Double.NaN;
				targetValueHtml_ = "-";
			}
			
			if (baseValueEnumId_ != enumId) {
				baseValueEnumId_ = enumId;
				targetEnumValue_ = null;
			}
		}
	}

	/**
	 * Add more security layer to make sure that the calculation will stop, it shoud be called before we dump this RowData
	 * Warning: Once this methode is called, this object row data cannot be re-used to perform calculation
	 */
	public void kill()
	{
		cancelCalculation_ = true;
	}
	
	public boolean isKilled() {
		return cancelCalculation_ || Thread.currentThread().isInterrupted();
	}
	
	/**
	 * Compute the value
	 */
	@Override
	public synchronized void run()
	{
		try
		{
			if (isKilled()) {
				return;
			}
			
			if (Double.isNaN(targetValue_) 
					|| (lastBaseValue_!=null && !lastBaseValue_.equals(baseValue_)) 
					|| (lastBaseUnitId_!=null && !lastBaseUnitId_.equals(baseUnitId_)))
			{
				targetValue_ = computeTargetValue(baseValue_);				
				targetValueHtml_ = formatDouble(targetValue_);
				lastBaseValue_ = baseValue_;
			}
			
			if (targetEnumValue_ == null 
					|| (lastBaseValueEnumId_!=null && !lastBaseValueEnumId_.equals(baseValueEnumId_)) 
					|| (lastBaseUnitId_!=null && !lastBaseUnitId_.equals(baseUnitId_)))
			{
				targetEnumValue_ = computeTargetValue(baseValueEnumId_);
				lastBaseValueEnumId_ = baseValueEnumId_;
			}
			
			lastBaseUnitId_ = baseUnitId_;
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}
	
	/**
	 * Convert the "36" of "clothing size woman france" to "XX-Small" of "clothing size woman US"; 
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 */
	EnumValue computeTargetValue(long baseValueEnumId) throws IllegalAccessException, InterruptedException, ExecutionException, TimeoutException
	{
		if (isKilled() || baseValueEnumId == -1)
		{
			return null;
		}
		
		HashMap<Long, EnumValue> enumValues = resultListAdapter_.getEnumValues();
		
		if (enumValues == null || enumValues.isEmpty()) {
			return null;
		}
		
		if (baseUnitId_ == targetUnitId_) {
			return enumValues.get(baseValueEnumId);
		}
		
		//Log.v(TAG, String.format("convert %d to %s", baseValueEnumId, targetUnitName_));
		
		ArrayList<Corresponding> correspondings = resultListAdapter_.getCorrespondings();
		
		if (correspondings == null || correspondings.isEmpty())
		{
			//Log.v(TAG, String.format("Cancel convert baseValueEnumId=%d from baseUnitId=%d to targetUnitId=%d. Because correspondings or enumValues tables is null", baseValueEnumId, baseUnitId, targetUnitId));
			return null;
		}
		
		int correspondingsCount = correspondings.size();
		
		/* use BFS to find path from baseValueEnumId to one of enumValue of the targetUnitId on the correspondings graph */
		
		ArrayDeque<Long> visitedEnumValueQueue = new ArrayDeque<Long>(correspondingsCount);
		ArrayList<Long> visitedEnumValue = new ArrayList<Long>();
		visitedEnumValueQueue.offer(baseValueEnumId);
		
		while (!visitedEnumValueQueue.isEmpty() && !isKilled())
		{
			long visitingEnumValue = visitedEnumValueQueue.poll();
			visitedEnumValue.add(visitingEnumValue);
			
			// check if the target unit is reached
			
			EnumValue visitingEnumValueObj = enumValues.get(visitingEnumValue);
			if (visitingEnumValueObj != null && visitingEnumValueObj.getUnitId() == targetUnitId_)
			{
				//Log.v(TAG, String.format("Convert baseValueEnumId=%d from baseUnitId=%d to targetUnitId=%d, found enumId = %d", baseValueEnumId, baseUnitId, targetUnitId, visitingEnumValueObj.getId()));
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
			
		//Log.v(TAG, String.format("Convert baseValueEnumId=%d from baseUnitId=%d to targetUnitId=%d, NO RESU", baseValueEnumId, baseUnitId, targetUnitId));
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
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 */
	double computeTargetValue(double baseValue) throws InterruptedException, ExecutionException, TimeoutException, IllegalAccessException
	{
		if (isKilled() || Double.isNaN(baseValue))
		{
			return Double.NaN;
		}		
		
		if (baseUnitId_ == targetUnitId_) {
			return baseValue;
		}
		
		//Log.v(TAG, String.format("convert %f to %s", baseValue, targetUnitName_));
		
		ArrayList<Conversion> conversions = this.resultListAdapter_.getConversions();
		
		if (conversions == null)
		{
			//Log.v(TAG, String.format("Cancel convert %f from baseUnitId=%d to targetUnitId=%d. Because conversions table is Null", baseValue, baseUnitId, targetUnitId));
			return Double.NaN;
		}
		
		//Log.v(TAG, String.format("Start convert %f from baseUnitId=%d to targetUnitId=%d", baseValue, baseUnitId, targetUnitId));
		
		//boost-up the calculation speed in case of currency 
		if (categoryId_ == Category.CURRENCY_CATEGORY)
		{
			long optimizeCurrencyId = this.resultListAdapter_.getOptimizeCurrencyId();
			
			Conversion convBaseOptimize = null;
			Conversion convOptimizeTarget = null;
			Conversion convBaseUsd = null;
			Conversion convUsdTarget = null;
			Conversion convBaseTarget = null;
			
			for (Conversion conv : conversions)
			{
				if (isKilled()) {
					return Double.NaN;
				} 
				
				if (conv.getBaseUnitId() == baseUnitId_ && conv.getTargetUnitId() == targetUnitId_)
				{
					//The trivial case: if there is a direct conversion between baseUnitId and targetUnitId 
					//it usually happen when optimizeCurrencyId == baseUnitId_
					return conv.convert(baseValue, baseUnitId_);
				}
				
				if (conv.isIncidentEdgeOf(baseUnitId_))
				{
					if (conv.isIncidentEdgeOf(targetUnitId_)) {
						convBaseTarget = conv;
					}
					if (conv.isIncidentEdgeOf(optimizeCurrencyId)) {
						convBaseOptimize = conv;
					}
					if (conv.isIncidentEdgeOf(Unit.USD_UNIT)) {
						convBaseUsd = conv;
					}
				}
				if (conv.isIncidentEdgeOf(targetUnitId_)) 
				{
					if (conv.isIncidentEdgeOf(optimizeCurrencyId)) {
						convOptimizeTarget = conv;
					}
					if (conv.isIncidentEdgeOf(Unit.USD_UNIT)) {
						convUsdTarget = conv;
					}
				}
			}
			
			//use inverse-rate conversions
			
			if (convBaseTarget!=null) {
				Log.v("CURR", "compute: use reverse conversion");
				return convBaseTarget.convert(baseValue, baseUnitId_);
			}
			
			//use two-steps conversion
			
			if (convBaseOptimize != null && convOptimizeTarget != null)
			{
				Log.v("CURR", "compute: two-steps conversions cross currencyId = "+optimizeCurrencyId);
				return convOptimizeTarget.convert(convBaseOptimize.convert(baseValue, baseUnitId_), optimizeCurrencyId);
			}
			
			//last chance with USD
			
			if (optimizeCurrencyId != Unit.USD_UNIT) {
				Log.v("CURR", "compute: two-steps conversions cross USD");
				if (convBaseUsd!=null && convUsdTarget!=null) {
					return convUsdTarget.convert(convBaseUsd.convert(baseValue, baseUnitId_), Unit.USD_UNIT);
				}
			}
		}
		
		//long s0 = DatabaseHelper.getNow();
		
		/* use BFS to find the shortest path from the baseUnitId to the targetUnitId on the Converions graph */
		
		ArrayDeque<Long> visitedUnitQueue = new ArrayDeque<Long>();
		HashMap<Long, Conversion> previous = new HashMap<Long, Conversion>();
		
		ArrayList<Long> visitedUnit = new ArrayList<Long>();  
		boolean pathFound = false; //turn to true if we can build a path from baseUnitId to targetUnitId
		
		//add source to baseUnitId
		visitedUnitQueue.offer(baseUnitId_);
		previous.put(baseUnitId_, null);
		
//		long maxOp = 0;
		
		while (!visitedUnitQueue.isEmpty() && !isKilled())
		{
			//long s4 = DatabaseHelper.getNow();
			
			long visitingUnit = visitedUnitQueue.poll();
			visitedUnit.add(visitingUnit);
			
//			long elapsed1 = DatabaseHelper.getNow() - s4;
//			if (maxOp<elapsed1) maxOp = elapsed1;
			
			if (visitingUnit == targetUnitId_)
			{
				pathFound = true;
				break; //gotcha!
			}
			
			//find all neighbors which are not visited
			
			for (Conversion conv : conversions)
			{
				if (conv.isIncidentEdgeOf(visitingUnit))
				{
					long neighborUnit = conv.getOtherUnitId(visitingUnit);
					//check if it is not visited yet
					if (!visitedUnitQueue.contains(neighborUnit) && !visitedUnit.contains(neighborUnit))
					{
						//long s5 = DatabaseHelper.getNow();
						
						//neighborUnit is not visited => add it to the queue
						visitedUnitQueue.offer(neighborUnit);
						previous.put(neighborUnit, conv);
						
//						long elapsed2 = DatabaseHelper.getNow() - s5;
//						if (maxOp<elapsed2) maxOp = elapsed2;
					}
				}
			}
		}
		
		//long s1 = DatabaseHelper.getNow();
		
		/* play back (using previous list) to build the path and compute the value */
		
		if (!pathFound)
		{
			return Double.NaN;
		}
		
		ArrayDeque<Conversion> path = new ArrayDeque<Conversion>();
		long uid = targetUnitId_;
		Conversion conv;
		
		do {
			conv = previous.get(uid); 
			path.addFirst(conv);
			uid = conv.getOtherUnitId(uid); 
		}
		while (uid != baseUnitId_ && !isKilled());
		
		/* use the path to convert the value */
		
		double returned = baseValue;
		uid = baseUnitId_;
		while (!path.isEmpty() && !isKilled())
		{
			conv = path.poll();
			returned = conv.convert(returned, uid);
			uid = conv.getOtherUnitId(uid);
		}
		
		if (isKilled())
		{
			return Double.NaN;
		}
		
//		long s2 = DatabaseHelper.getNow();
		
//		long elapsed = s2-s0;
//		if (elapsed > 100) {
//			Log.i(TAG, String.format("Finish convert to targetUnitId=%d (%d + %d = %d ms) maxOp=%d",  targetUnitId_, s1-s0, s2-s1, elapsed, maxOp));
//		}
		
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
		String s = String.format(Locale.US, "%.12g", d);
		StringBuilder resu = new StringBuilder("<b>");
		
		int firstPoint = s.indexOf('.');
		int firstComma = s.indexOf(',');
		int p1 = Math.max(firstPoint, firstComma);
		
		if (p1>0) {
			resu.append(s.substring(0, p1));
			resu.append("</b>");
		}
		else {
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

	public void clearTargetValue() {
		targetValue_ = Double.NaN;
		targetValueHtml_ = "-"; 
		targetEnumValue_ = null; 
	}
}