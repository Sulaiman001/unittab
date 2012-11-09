package dh.sunicon.currency;

import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
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
	private Object lockCurrencyImporter_ = new Object();
	private boolean enabled_ = true;
	
	public CurrencyUpdater(Activity context){
		context_ = context;
		preferences_ = context_.getPreferences(Activity.MODE_PRIVATE);
		timeToLive_ = preferences_.getLong("CurrencyRateExpiryDuration", 1000);
		updatingThread_ = Executors.newSingleThreadExecutor();
		mainThread_ = new Handler();
	}
	
	public long getLastUpdate(String currencyCode) {
		return preferences_.getLong("LastUpdate-"+currencyCode, 0);
	}
	
	public long getTimeToLive() {
		return timeToLive_;
	}
	
	public boolean isExpiry(String currencyCode) {
		long now = getNow();
		return (now - getLastUpdate(currencyCode)) > timeToLive_;
	}

	
	private boolean allowProcess(String currencyCode) {
		
		if (!enabled_) {
			return false;
		}
		
		if (isExpiry(currencyCode))
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
	
	public void process(final String currencyCode) {
		
		if (TextUtils.isEmpty(currencyCode))
			throw new InvalidParameterException("CurrencyCode must not NULL");
		
		if (!allowProcess(currencyCode))
			return;
		
		Log.d("CURR", "Process BEGIN "+currencyCode);
		
		mainThread_.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if (beforeUpdateStarted_!=null)
						beforeUpdateStarted_.beforeUpdateStarted(CurrencyUpdater.this);
				}
				catch (Exception ex){
					Log.wtf(TAG, ex);
				}
			}
		});
		
		class CallableWithParam implements Callable<UpdatingResult>
		{
			private final String currencyCode_;
			private final CurrencyImporter currencyImporter_;
			
			public CallableWithParam(String currencyCode, CurrencyImporter currencyImporter) {
				currencyCode_ = currencyCode;
				currencyImporter_ = currencyImporter;
			}
			
			@Override
			public UpdatingResult call() throws Exception
			{
				try
				{
					final UpdatingResult ret = currencyImporter_.importOnBackground(currencyCode_);
					
					if (ret != UpdatingResult.FAILED && !currencyImporter_.isDumped())
						saveLastTimeProcess(currencyCode_);
					
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
					
					return ret;
				}
				catch (Exception ex)
				{
					Log.wtf(TAG, ex);
					return UpdatingResult.FAILED;
				}
			}
		}
		
		/* 
		 * each call of this method will cancel old currencyImporter and create a new one
		 * to make sure that the previous process call will not invoke onUpdateFinished  .
		 * Need a lock here because this code can be call from more than one thread
		 */
		synchronized (lockCurrencyImporter_)
		{
			CurrencyImporter oldCurrencyImporter = currencyImporter_;
			currencyImporter_ = new CurrencyImporter(context_);
			lastProcessingResult_ = updatingThread_.submit(new CallableWithParam(currencyCode, currencyImporter_));
			
			if (oldCurrencyImporter != null) {
				try
				{
					oldCurrencyImporter.dumpIt();
				}
				catch (Exception ex) {
					Log.w(TAG, ex.getMessage());
				}
			}
		}
		
		Log.d("CURR", "Process END "+currencyCode);
	}
	
	public void cancel() {
		if (currencyImporter_ != null)
			currencyImporter_.dumpIt();
	}
	
	
	private void saveLastTimeProcess(String currencyCode) {
		try
		{
			long now = getNow();
			
			SharedPreferences.Editor editor = preferences_.edit();
			editor.putLong("LastUpdate-"+currencyCode, now);
			editor.commit();
			
			Log.d(TAG, "Save LastUpdate-"+currencyCode+" "+new Timestamp(now)); //TODO remove it
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
	
	public boolean isEnabled()
	{
		return enabled_;
	}

	public void setEnabled(boolean enabled)
	{
		enabled_ = enabled;
	}

	public enum UpdatingResult {FAILED, DATA_CHANGED, DATA_UNCHANGED}
	
	public interface OnUpdateFinishedListener {
		void onUpdateFinished(CurrencyUpdater sender, UpdatingResult result);
	}
	
	public interface BeforeUpdateStartedListener {
		void beforeUpdateStarted(CurrencyUpdater sender);
	}
}
