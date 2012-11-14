package dh.sunicon.currency;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

/**
 * CurrencyUdpdater is the entry point to call update currencies exchange rates.
 * to start the update from the main thread, call process(baseCurrency) 

 * Capture the BeforeUpdateStarted and OnUpdateFinished on the main thread to update the UI
 * call cancel() to stop the process at anytime. OnUpdateFinish will be callback.
 * 
 * @author hiep
 */
public class CurrencyUpdater
{
	private static final String TAG = CurrencyUpdater.class.getName();
	
	public static int OPT_ALL_NETWORK = 0;
	public static int OPT_WIFI_ONLY = 1;
	public static int OPT_NEVER = 2;
	public static final String CurrencyUpdaterOptionName = "CurrencyUpdaterOption";
	
	private final Activity context_;
	private final SharedPreferences preferences_;
	private long currencyUnitIdOnProcess_ = -1;
	private RateUpdatingTask currentRateUpdatingTask_;
	
	public CurrencyUpdater(Activity context){
		context_ = context;
		preferences_ = context_.getPreferences(Activity.MODE_PRIVATE);
	}
	
	
	/**
	 * Must be call on the Main Thread
	 * @param currencyUnitId - base Currency, the update will be perform on this base
	 * @throws IllegalAccessException 
	 */
	public void process(final long currencyUnitId) throws IllegalAccessException {
		
		if (Looper.getMainLooper().getThread() != Thread.currentThread())
		{
			throw new IllegalAccessException("This methode must be called from UI Thread.");
		}

		if (currencyUnitId<0) {
			return;
		}
		
		if (currencyUnitId == getCurrencyUnitIdOnLoading()) { //the currencyUnitId is already in processing
			Log.d("CURR", "process of "+currencyUnitId+" is happen");
			return;
		}
		
		final int currencyUpdaterOption = getCurrencyUpdaterOption();
		
		if (currencyUpdaterOption == OPT_NEVER) {
			return;
		}
		
		Log.v("CURR", "Process BEGIN "+currencyUnitId);
		
		
		if (beforeUpdateStarted_!=null)
			beforeUpdateStarted_.beforeUpdateStarted(CurrencyUpdater.this, currencyUnitId);
		
		currencyUnitIdOnProcess_ = currencyUnitId;
		
		cancel();
		currentRateUpdatingTask_ = new RateUpdatingTask();
		currentRateUpdatingTask_.execute(currencyUnitId);
	
		Log.v("CURR", "Process END "+currencyUnitId);
	}
	
	public void cancel() {
		if (currentRateUpdatingTask_!=null) {
			currentRateUpdatingTask_.cancel(false);
		}
	}
	
	/** events handling **/
	
	private OnUpdateFinishedListener onUpdateFinished_;
	private BeforeUpdateStartedListener beforeUpdateStarted_;
	
	public void setOnUpdateFinished(OnUpdateFinishedListener listener) {
		onUpdateFinished_ = listener;
	}
	
	public void setBeforeUpdateStarted_(BeforeUpdateStartedListener listener) {
		beforeUpdateStarted_ = listener;
	}

	/**
	 * return -1 if no currency is loading
	 */
	public long getCurrencyUnitIdOnLoading()
	{
		return currencyUnitIdOnProcess_;
	}
	
	private int getCurrencyUpdaterOption() {
		return preferences_.getInt(CurrencyUpdaterOptionName, OPT_ALL_NETWORK);
	}

	public interface OnUpdateFinishedListener {
		void onUpdateFinished(CurrencyUpdater sender, UpdatingReport result);
	}
	
	public interface BeforeUpdateStartedListener {
		void beforeUpdateStarted(CurrencyUpdater sender, long currencyUnitId);
	}
	
	private final class RateUpdatingTask extends AsyncTask<Long, Void, UpdatingReport>
	{
		@Override
		protected UpdatingReport doInBackground(Long... params)
		{
			try
			{
				if (isCancelled()) {
					return null;
				}
				UpdatingAgentsManager agentsManager_ = new UpdatingAgentsManager(context_, this);
				return agentsManager_.importOnBackground(params[0]);
			}
			catch (Exception ex)
			{
				Log.wtf(TAG, ex);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(UpdatingReport report)
		{
			try {
				currencyUnitIdOnProcess_ = -1;
				if (onUpdateFinished_!=null) {
					onUpdateFinished_.onUpdateFinished(CurrencyUpdater.this, report);
				}
			}
			catch (Exception ex)
			{
				Log.wtf(TAG, ex);
			}
		}
		
		@Override
		protected void onCancelled()
		{
			if (onUpdateFinished_!=null) {
				onUpdateFinished_.onUpdateFinished(CurrencyUpdater.this, null);
			}
		}
	}
}
