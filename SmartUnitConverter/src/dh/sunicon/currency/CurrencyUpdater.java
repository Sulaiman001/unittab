package dh.sunicon.currency;

import dh.sunicon.datamodel.Unit;
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
	public static final String OPTNAME_CURRENCY_LIVE_UPDATE = "CurrencyLiveUpdateOption";
	public static final String OPTNAME_CURRENCY_EXPIRY_TIME = "CurrencyLiveUpdateExpiryTime";
	public static final String OPTNAME_CURRENCY_USD_ONLY = "CurrencyLiveUpdateUSDOnly";
	public static final long DEFAULT_CURRENCY_EXPIRY_TIME = 3600000L;
	
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
		currencyUnitIdOnProcess_ = currencyUnitId;		
		cancel();
		currentRateUpdatingTask_ = new RateUpdatingTask();
		currentRateUpdatingTask_.execute(currencyUnitId);
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

	public interface OnUpdateFinishedListener {
		void onUpdateFinished(CurrencyUpdater sender, UpdatingReport result);
	}
	
	public interface BeforeUpdateStartedListener {
		void beforeUpdateStarted(long currencyUnitId);
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
				UpdatingAgentsManager agentsManager = new UpdatingAgentsManager(context_, this);
				agentsManager.setBeforeUpdateStarted(beforeUpdateStarted_);
				
				if (preferences_.getBoolean(OPTNAME_CURRENCY_USD_ONLY, true)) {
					return agentsManager.importOnBackground(Unit.USD_UNIT);
				}
				else {
					return agentsManager.importOnBackground(params[0]);
				} 
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
			currencyUnitIdOnProcess_ = -1;
			if (onUpdateFinished_!=null) {
				onUpdateFinished_.onUpdateFinished(CurrencyUpdater.this, null);
			}
		}
	}
}
