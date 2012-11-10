package dh.sunicon.currency;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dh.sunicon.MainActivity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * CurrencyUdpdater has a TimeToLive = 1 hour for example, that means after 1 hours from the LastUpdate
 * the updater will be expiry.
 * 
 * process() will check isExpiry()
 * - if isExpiry() return false (the CurrencyUpdater is sill fresh), it will immediately return false (means nothing has been updated in the database)
 * - if the CurrencyUpdater is expiried, it will download the data from internet and update the local database from the downloaded data in a background thread
 * 
 * This base class manages the updatingThread and execute the events (BeforeStart + UpdateFinished) on the main thread
 * 
 * @author hiep
 *
 */
public class CurrencyUpdater
{
	private static final String TAG = CurrencyUpdater.class.getName();
	
	private static int OPT_ALL_NETWORK = 0;
	private static int OPT_WIFI_ONLY = 1;
	private static int OPT_NEVER = 2;
	
	private final Activity context_;
	private final SharedPreferences preferences_;
	private long timeToLive_;
	private final Handler mainThread_;
	private final ExecutorService updatingThread_;
	private Future<UpdatingResult> lastProcessingResult_;
	private RatesImportersManager currencyImporter_;
	private long currencyUnitIdOnProcess_ = -1;
	
	public CurrencyUpdater(Activity context){
		context_ = context;
		preferences_ = context_.getPreferences(Activity.MODE_PRIVATE);
		timeToLive_ = preferences_.getLong("CurrencyRateExpiryDuration", 500);
		updatingThread_ = Executors.newSingleThreadExecutor();
		mainThread_ = new Handler();
	}
	
	public long getLastUpdate(long currencyUnitId) {
		return preferences_.getLong("LastUpdateCurrency-"+currencyUnitId, 0);
	}
	
	public long getTimeToLive() {
		return timeToLive_;
	}
	
	public boolean isExpiry(long currencyUnitId) {
		long now = getNow();
		return (now - getLastUpdate(currencyUnitId)) > timeToLive_;
	}
	
	private boolean allowProcess(long currencyUnitId) {
	
		if (getCurrencyUpdaterOption() == OPT_NEVER)
			return false;
		
		if (isExpiry(currencyUnitId))
			return true;		
		
		//not expiry

		if (lastProcessingResult_ == null)
			return true;

		//lastProcessingResult returned something
		
		if (lastProcessingResult_.isCancelled())
			return true;
		
		if (lastProcessingResult_.isDone()) { //lastProcessingResult_ is terminated normally 
			try
			{
				UpdatingResult lastUpdatingResult = lastProcessingResult_.get();
				if (lastUpdatingResult == UpdatingResult.FAILED) //but it is a FAILED
					return true;
			}
			catch (Exception ex)
			{
				Log.w(TAG, ex.getMessage());
				return false;
			}
		}
		
		//if the updater is still processing or it is not expiry  
		
		return false;
	}
	
	/**
	 * Must be call on the Main Thread
	 * @param currencyUnitId
	 * @throws IllegalAccessException 
	 */
	public void process(final long currencyUnitId) throws IllegalAccessException {
		
		if (Looper.getMainLooper().getThread() != Thread.currentThread())
		{
			throw new IllegalAccessException("This methode must be called from UI Thread.");
		}

		if (currencyUnitId<0)
			return;
		
		if (!allowProcess(currencyUnitId))
			return;
		
		Log.d("CURR", "Process BEGIN "+currencyUnitId);
		
		if (beforeUpdateStarted_!=null)
			beforeUpdateStarted_.beforeUpdateStarted(CurrencyUpdater.this, currencyUnitId);
		
		class CallableWithParam implements Callable<UpdatingResult>
		{
			private final long currencyUnitId__;
			private final RatesImportersManager currencyImporter__;
			
			public CallableWithParam(long currencyUnitId, RatesImportersManager currencyImporter) {
				currencyUnitId__ = currencyUnitId;
				currencyImporter__ = currencyImporter;
			}
			
			/**
			 * Check NetWork connectivity which must be appropriated with Preferences option 
			 * before start importing
			 */
			private UpdatingResult checkOptionAndImport()
			{
				UpdatingResult ret = UpdatingResult.FAILED;
				NetworkInfo networkInfo = ((MainActivity)context_).getNetworkInfo();
				if (networkInfo!=null && networkInfo.isConnected()) //if we have network connected
				{
					if (getCurrencyUpdaterOption() != OPT_WIFI_ONLY || 
							networkInfo.getType()==ConnectivityManager.TYPE_WIFI) //if the preferences is WIFI_ONLY and current network is really wifi
					{
						ret = currencyImporter__.importOnBackground(currencyUnitId__);
					}
				}
				return ret;
			}
			
			@Override
			public UpdatingResult call() throws Exception
			{
				try
				{
					currencyUnitIdOnProcess_ = currencyUnitId__;
					
					final UpdatingResult ret = checkOptionAndImport();
					
					if (ret == UpdatingResult.SUCCESS && !currencyImporter__.isDumped())
						saveLastTimeProcess(currencyUnitId__);
					
					//if this is the last called proccess (the last currencyImporter)
					//it might not neccessary because the currencyUpdater processed on single thread (updatingThread_)
					if (currencyImporter__ == CurrencyUpdater.this.currencyImporter_)   
					{
						currencyUnitIdOnProcess_ = -1;
						mainThread_.post(new Runnable()
						{
							@Override
							public void run()
							{
								if (onUpdateFinished_!=null)
									onUpdateFinished_.onUpdateFinished(CurrencyUpdater.this, ret);
							}
						});
					}
					
					return ret;
				}
				catch (Exception ex)
				{
					Log.wtf(TAG, ex);
					return UpdatingResult.FAILED;
				}
			}
		}
		
		//dump old currencyImporter to cancel old update which is currently running on the updatingThread (if there is one)
		if (currencyImporter_ != null) {
			currencyImporter_.dumpIt();
		}
		currencyImporter_ = new RatesImportersManager(context_);
		lastProcessingResult_ = updatingThread_.submit(new CallableWithParam(currencyUnitId, currencyImporter_));
				
		Log.d("CURR", "Process END "+currencyUnitId);
	}
	
	public void cancel() {
		if (currencyImporter_ != null)
		{
			currencyImporter_.dumpIt();
			currencyImporter_ = null;
		}
	}
	
	private void saveLastTimeProcess(long currencyUnitId) {
		try
		{
			long now = getNow();
			
			SharedPreferences.Editor editor = preferences_.edit();
			editor.putLong("LastUpdateCurrency-"+currencyUnitId, now);
			editor.commit();
			
			Log.d(TAG, "Save LastUpdate-"+currencyUnitId+" "+new Timestamp(now)); //TODO remove it
		}
		catch (Exception ex)
		{
			Log.wtf(TAG, ex);
		}
	}
	
	public static long getNow()
	{
		return Calendar.getInstance().getTime().getTime();
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
		return preferences_.getInt("CurrencyUpdaterOption", OPT_ALL_NETWORK);
	}

	public enum UpdatingResult {FAILED, SUCCESS}
	
	public interface OnUpdateFinishedListener {
		void onUpdateFinished(CurrencyUpdater sender, UpdatingResult result);
	}
	
	public interface BeforeUpdateStartedListener {
		void beforeUpdateStarted(CurrencyUpdater sender, long currencyUnitId);
	}
}
