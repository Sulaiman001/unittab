package dh.sunicon.currency;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Activity;
import android.content.SharedPreferences;
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
	
	private final Activity context_;
	private final SharedPreferences preferences_;
	private long timeToLive_;
	private final Handler mainThread_;
	private final ExecutorService updatingThread_;
	private Future<UpdatingResult> lastProcessingResult_;
	private CurrencyImporter currencyImporter_;
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
			private final long currencyUnitId_;
			private final CurrencyImporter currencyImporter_;
			
			public CallableWithParam(long currencyUnitId, CurrencyImporter currencyImporter) {
				currencyUnitId_ = currencyUnitId;
				currencyImporter_ = currencyImporter;
			}
			
			@Override
			public UpdatingResult call() throws Exception
			{
				try
				{
					currencyUnitIdOnProcess_ = currencyUnitId_;
					
					final UpdatingResult ret = currencyImporter_.importOnBackground(currencyUnitId_);
					
					if (ret != UpdatingResult.FAILED && !currencyImporter_.isDumped())
						saveLastTimeProcess(currencyUnitId_);
					
					if (currencyImporter_ == CurrencyUpdater.this.currencyImporter_) //if the currencyImporter has not been changed evens it is dumped
					{
						mainThread_.post(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									if (onUpdateFinished_!=null)
										onUpdateFinished_.onUpdateFinished(CurrencyUpdater.this, ret);
								}
								catch (Exception ex){
									Log.wtf(TAG, ex);
								}
							}
						});
					}
					
					currencyUnitIdOnProcess_ = -1;
					
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
		currencyImporter_ = new CurrencyImporter(context_);
		lastProcessingResult_ = updatingThread_.submit(new CallableWithParam(currencyUnitId, currencyImporter_));
				
		Log.d("CURR", "Process END "+currencyUnitId);
	}
	
	public void cancel() {
		if (currencyImporter_ != null)
			currencyImporter_.dumpIt();
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

	public enum UpdatingResult {FAILED, DATA_CHANGED, DATA_UNCHANGED}
	
	public interface OnUpdateFinishedListener {
		void onUpdateFinished(CurrencyUpdater sender, UpdatingResult result);
	}
	
	public interface BeforeUpdateStartedListener {
		void beforeUpdateStarted(CurrencyUpdater sender, long currencyUnitId);
	}
}
