package dh.sunicon;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import dh.sunicon.currency.CurrencyUpdater;

public class SettingFragment extends Fragment
{	
	static final BiMap<Integer, Long> MAP_EXPIRY_TIME = ImmutableBiMap.of(
			0, 1000L, 
			1, 3600000L,
			2, CurrencyUpdater.DEFAULT_CURRENCY_EXPIRY_TIME, 
			3, 604800000L);
		
	private SharedPreferences preferences_;
	private Spinner currencyExpiryTime_;
	private Spinner currencyLiveUpdateOption_;
	private Spinner currencyUsdOnly_;
	
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
		
		preferences_ = this.getActivity().getPreferences(Activity.MODE_PRIVATE);
		
		initCurrencyLiveUpdateOption();
		initCurrencyExpiryTime();
		initCurrencyUsdOnly();
		
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
		
		int liveUpdateOption = currencyLiveUpdateOption_.getSelectedItemPosition();
		editor.putInt(CurrencyUpdater.OPTNAME_CURRENCY_LIVE_UPDATE, liveUpdateOption);
		Log.v("Setting", "Saved setting "+CurrencyUpdater.OPTNAME_CURRENCY_LIVE_UPDATE+" "+Integer.toString(liveUpdateOption));
		
		long expiryTime = MAP_EXPIRY_TIME.get(currencyExpiryTime_.getSelectedItemPosition());
		editor.putLong(CurrencyUpdater.OPTNAME_CURRENCY_EXPIRY_TIME, expiryTime);
		Log.v("Setting", "Saved setting "+CurrencyUpdater.OPTNAME_CURRENCY_EXPIRY_TIME+" "+Long.toString(expiryTime));
		
		boolean usdOnly = currencyUsdOnly_.getSelectedItemPosition() == 0 ? true : false;
		editor.putBoolean(CurrencyUpdater.OPTNAME_CURRENCY_USD_ONLY, usdOnly);
		Log.v("Setting", "Saved setting "+CurrencyUpdater.OPTNAME_CURRENCY_USD_ONLY+" "+Boolean.toString(usdOnly));
		
		editor.commit();
		
		Log.v("Setting", "Saved settings Done");
	}
	
	private void restoreFromPreferences() 
	{
		currencyLiveUpdateOption_.setSelection(preferences_.getInt(CurrencyUpdater.OPTNAME_CURRENCY_LIVE_UPDATE, 0));
		
		long expiryTime = preferences_.getLong(CurrencyUpdater.OPTNAME_CURRENCY_EXPIRY_TIME, CurrencyUpdater.DEFAULT_CURRENCY_EXPIRY_TIME);
		int expiryTimePos = MAP_EXPIRY_TIME.inverse().get(expiryTime);
		currencyExpiryTime_.setSelection(expiryTimePos);
		
		boolean usdOnly = preferences_.getBoolean(CurrencyUpdater.OPTNAME_CURRENCY_USD_ONLY, CurrencyUpdater.DEFAULT_CURRENCY_USD_ONLY);
		currencyUsdOnly_.setSelection(usdOnly ? 0 : 1);
		
		Log.v("Setting", "restoreFromPreferences");
	}
	
}