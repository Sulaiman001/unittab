package dh.sunicon.currency;

import android.app.Activity;
import android.util.Log;
import dh.sunicon.currency.CurrencyUpdater.UpdatingResult;

/**
 * Implement the cancellable aspect of the updater
 */
public class CurrencyImporter
{
	private static final String TAG = CurrencyImporter.class.getName();
	private boolean requestCancellation_ = false;
	private Activity context_;

	public CurrencyImporter(Activity context)
	{
		context_ = context;
	}

	public UpdatingResult importOnBackground(long currencyUnitId)
	{
		if (isDumped())
			throw new UnsupportedOperationException();

		Log.d("CURR", "importOnBackground BEGIN "+currencyUnitId);
		
		// TODO

		for (int i = 0; i <= 20; i++)
		{
			if (requestCancellation_)
			{
				Log.d("CURR", "importOnBackground CANCEL "+currencyUnitId);
				return UpdatingResult.FAILED;
			}
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException ex)
			{
				Log.w(TAG, ex);
			}
		}

		Log.d("CURR", "importOnBackground END "+currencyUnitId);
		
		return UpdatingResult.DATA_CHANGED;
	}

	public void dumpIt()
	{
		requestCancellation_ = true;
	}

	public boolean isDumped()
	{
		return requestCancellation_;
	}
}
