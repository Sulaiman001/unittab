package dh.sunicon.workarounds;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import dh.sunicon.ErrorDialogFragment;
import dh.sunicon.datamodel.DatabaseHelper;

public class MyApplication extends Application {

	public static final boolean DEBUG_MODE = true;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		if (DEBUG_MODE) {
			//StrictMode.enableDefaults();
		}
	}
	
	public static void showErrorDialog(final FragmentManager fm, final String message, final Throwable ex) {
		try {
			Log.w("sunicon.err", ex);
			if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
				ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(message, ex);
				dialog.show(fm, "ReportErrorDialog "+DatabaseHelper.getNow());
			}
			else {
				Handler mainThread = new Handler(Looper.getMainLooper());
				mainThread.post(new Runnable() {
					@Override
					public void run() {
						try {
							ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(message, ex);
							dialog.show(fm, "ReportErrorDialog "+DatabaseHelper.getNow());
						}
						catch (Exception ex1) {
							Log.wtf("MyApp", ex1);
						}
					}
				});
			}
		}
		catch (Exception ex2) {
			Log.wtf("MyApp", ex2);
		}
	} 
}