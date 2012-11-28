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
	private Spinner precisionIntSpinner_;
	private Spinner inputDelay_;
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
		
		currencyLiveUpdateOption_= initSpinnerSetting(R.id.currencyLiveUpdateOption, R.array.opt_currency_live_update);
		currencyExpiryTime_ = initSpinnerSetting(R.id.currencyExpiryTime, R.array.opt_currency_expiry_time);
		currencyUsdOnly_= initSpinnerSetting(R.id.currencyUsdOnly, R.array.opt_currency_usd_only);
		precisionSpinner_ = initSpinnerSetting(R.id.precision, R.array.opt_precision);
		precisionIntSpinner_ = initSpinnerSetting(R.id.precisionInt, R.array.opt_precision_int);
		inputDelay_ = initSpinnerSetting(R.id.inputDelay, R.array.opt_input_delay);
		
		initStrictMode();
		
		restoreFromPreferences();
	}
	
	private Spinner initSpinnerSetting(int spinnerResId, int stringsArrayResId)
	{
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
				stringsArrayResId, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) getView().findViewById(spinnerResId);
		spinner.setAdapter(adapter);
		return spinner;
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
			int precisionInt = Integer.parseInt((precisionIntSpinner_.getAdapter().getItem(precisionIntSpinner_.getSelectedItemPosition())).toString());
			editor.putInt(MainActivity.OPTNAME_PRECISION_INT, precisionInt);
		}
		{
			int inputDelay = Integer.parseInt((inputDelay_.getAdapter().getItem(inputDelay_.getSelectedItemPosition())).toString());
			editor.putInt(MainActivity.OPTNAME_INPUT_DELAY, inputDelay);
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
			restoreSpinner(precisionSpinner_,MainActivity.OPTNAME_PRECISION, MainActivity.DEFAULT_PRECISION, R.array.opt_precision);
		}
		{
			restoreSpinner(precisionIntSpinner_,MainActivity.OPTNAME_PRECISION_INT, MainActivity.DEFAULT_PRECISION_INT, R.array.opt_precision_int);
		}
		{
			restoreSpinner(inputDelay_,MainActivity.OPTNAME_INPUT_DELAY, MainActivity.DEFAULT_INPUT_DELAY, R.array.opt_input_delay);
		}
		{
			strictModeCheckBox_.setChecked(preferences_.getBoolean(MainActivity.OPTNAME_STRICTMODE, MainActivity.DEFAULT_STRICTMODE));
		}
		Log.v("Setting", "restoreFromPreferences");
	}
	
	//MainActivity.OPTNAME_PRECISION, MainActivity.DEFAULT_PRECISION, R.array.opt_precision, 
	private void restoreSpinner(Spinner spinner, String optName, int optDefaultValue, int stringsArrayResId) {
		int value = preferences_.getInt(optName, optDefaultValue);
		
		//find the equivalent position of value on the spinner 
		int pos = 0;
		String[] arr = getResources().getStringArray(stringsArrayResId);
		String precisionStr = Integer.toString(value);
		for (pos = 0; pos<arr.length; pos++) {
			if (precisionStr.equals(arr[pos])) {
				break;
			}
		}
		
		spinner.setSelection(pos);
	}
	
}