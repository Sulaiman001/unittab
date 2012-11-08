package dh.sunicon.workarounds;

import android.app.Application;
import android.os.StrictMode;

public class MyApplication extends Application {

	public static final boolean DEBUG_MODE = false;

	@Override
	public void onCreate()
	{
		super.onCreate();
		if (DEBUG_MODE) {
			StrictMode.enableDefaults();
		}
	}
}