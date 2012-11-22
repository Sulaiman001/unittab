package dh.sunicon;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;


public class SettingFragment extends Fragment
{	
	static final BiMap<Integer, Long> MAP_EXPIRY_TIME = ImmutableBiMap.of(
			0, 1000L, 
			1, 3600000L,
			2, MainActivity.DEFAULT_CURRENCY_EXPIRY_TIME, 
			3, 604800000L);
		
	private SharedPreferences preferences_;
	private Spinner currencyExpiryTime_;
	private Spinner currencyLiveUpdateOption_;
	private Spinner currencyUsdOnly_;
	private Spinner precisionSpinner_;
	private CheckBox strictModeCheckBox_;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
		{
			return null;
		}
		return inflater.inflate(R.layout.sunicon_setting, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//setRetainInstance(true);
		
		preferences_ = ((MainActivity)this.getActivity()).getPreferences();
		
		initCurrencyLiveUpdateOption();
		initCurrencyExpiryTime();
		initCurrencyUsdOnly();
		initPrecision();
		initStrictMode();
		
		restoreFromPreferences();
	}
	
	private void initCurrencyLiveUpdateOption()
	{
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
		        R.array.opt_currency_live_update, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		currencyLiveUpdateOption_ = (Spinner) getView().findViewById(R.id.currencyLiveUpdateOption);
		currencyLiveUpdateOption_.setAdapter(adapter);
	}
	private void initCurrencyExpiryTime()
	{
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
		        R.array.opt_currency_expiry_time, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		currencyExpiryTime_ = (Spinner) getView().findViewById(R.id.currencyExpiryTime);
		currencyExpiryTime_.setAdapter(adapter);
	}
	private void initCurrencyUsdOnly()
	{
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
		        R.array.opt_currency_usd_only, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		currencyUsdOnly_ = (Spinner) getView().findViewById(R.id.currencyUsdOnly);
		currencyUsdOnly_.setAdapter(adapter);
	}
	private void initPrecision()
	{
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
		        R.array.opt_precision, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		precisionSpinner_ = (Spinner) getView().findViewById(R.id.precision);
		precisionSpinner_.setAdapter(adapter);
	}
	
	private void initStrictMode()
	{
		strictModeCheckBox_ = (CheckBox) getView().findViewById(R.id.strictMode);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		savePrefs();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		savePrefs();
	}
	
	public void savePrefs()
	{
		SharedPreferences.Editor editor = preferences_.edit(); 
		{
			int liveUpdateOption = currencyLiveUpdateOption_.getSelectedItemPosition();
			editor.putInt(MainActivity.OPTNAME_CURRENCY_LIVE_UPDATE, liveUpdateOption);
			//Log.v("Setting", "Saved setting "+MainActivity.OPTNAME_CURRENCY_LIVE_UPDATE+" "+Integer.toString(liveUpdateOption));
		}
		{
			long expiryTime = MAP_EXPIRY_TIME.get(currencyExpiryTime_.getSelectedItemPosition());
			editor.putLong(MainActivity.OPTNAME_CURRENCY_EXPIRY_TIME, expiryTime);
			//Log.v("Setting", "Saved setting "+MainActivity.OPTNAME_CURRENCY_EXPIRY_TIME+" "+Long.toString(expiryTime));
		}
		{
			boolean usdOnly = currencyUsdOnly_.getSelectedItemPosition() == 0 ? true : false;
			editor.putBoolean(MainActivity.OPTNAME_CURRENCY_USD_ONLY, usdOnly);
			//Log.v("Setting", "Saved setting "+MainActivity.OPTNAME_CURRENCY_USD_ONLY+" "+Boolean.toString(usdOnly));
		}
		{
			int precision = Integer.parseInt((precisionSpinner_.getAdapter().getItem(precisionSpinner_.getSelectedItemPosition())).toString());
			editor.putInt(MainActivity.OPTNAME_PRECISION, precision);
		}
		{
			editor.putBoolean(MainActivity.OPTNAME_STRICTMODE, strictModeCheckBox_.isChecked());
		}
		editor.commit();
		
		Log.v("Setting", "Saved settings Done");
	}
	
	private void restoreFromPreferences() 
	{
		{
			currencyLiveUpdateOption_.setSelection(preferences_.getInt(MainActivity.OPTNAME_CURRENCY_LIVE_UPDATE, 0));
		}
		{
			long expiryTime = preferences_.getLong(MainActivity.OPTNAME_CURRENCY_EXPIRY_TIME, MainActivity.DEFAULT_CURRENCY_EXPIRY_TIME);
			int expiryTimePos = MAP_EXPIRY_TIME.inverse().get(expiryTime);
			currencyExpiryTime_.setSelection(expiryTimePos);
		}
		{
			boolean usdOnly = preferences_.getBoolean(MainActivity.OPTNAME_CURRENCY_USD_ONLY, MainActivity.DEFAULT_CURRENCY_USD_ONLY);
			currencyUsdOnly_.setSelection(usdOnly ? 0 : 1);
		}
		{
			int precision = preferences_.getInt(MainActivity.OPTNAME_PRECISION, MainActivity.DEFAULT_PRECISION);
			
			//find the equivalent position on the spinner 
			int precisionPos = 0;
			String[] arr = getResources().getStringArray(R.array.opt_precision);
			String precisionStr = Integer.toString(precision);
			for (precisionPos = 0; precisionPos<arr.length; precisionPos++) {
				if (precisionStr.equals(arr[precisionPos])) {
					break;
				}
			}
			
			precisionSpinner_.setSelection(precisionPos);
		}
		{
			strictModeCheckBox_.setChecked(preferences_.getBoolean(MainActivity.OPTNAME_STRICTMODE, MainActivity.DEFAULT_STRICTMODE));
		}
		Log.v("Setting", "restoreFromPreferences");
	}
	
}