package dh.sunicon;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import dh.sunicon.currency.CurrencyUpdater;
import dh.sunicon.currency.CurrencyUpdater.BeforeUpdateStartedListener;
import dh.sunicon.currency.CurrencyUpdater.OnUpdateFinishedListener;
import dh.sunicon.currency.UpdatingReport;
import dh.sunicon.currency.UpdatingReport.MessageType;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.runnable.RowData;
import dh.sunicon.workarounds.MyApplication;

public class ConverterFragment extends ListFragment implements LoaderCallbacks<Cursor>
{
	static final String TAG = ConverterFragment.class.getName();
	static final int VALUE_SPINNER_LOADER = 0;
	public static final long EVENTS_DELAY = 500L;
	private DatabaseHelper dbHelper_;
	private TextView categoryLabel_;
	private ViewSwitcher baseValueSwitcher_;
	private EditText baseValueEditor_;
	private Spinner baseValueSpinner_;
	private Button baseUnitPickerButton_;
	private Button baseUnitPicker2Button_;
	private EditText targetUnitFilterEditor_;
	private Button clearTargetUnitFilterButton_;
	private ViewSwitcher resultListSwitcher_;

	private ResultListAdapter resultListAdapter_;
	private SimpleCursorAdapter baseValueSpinnerAdapter_;
	private CurrencyUpdater currencyUpdater_;
	
	private View updateInProgressPanel_;
	private TextView currencyLoadingLabel_;
	private ProgressBar currencyLoadingProgressBar_;
	
	private int spinnerPositionToRestore_ = -1;
	private long baseUnitId_ = -1;
	private long categoryId_ = -1;
	
	private UnitHistoryManager unitHistory_;
	private Handler mainThread_;
	private boolean strictMode_ = false;
	private SharedPreferences preferences_;
	private int precision_;
	
	//private CountDownLatch spinnerLoadingLatch_;
	
	private boolean isActivityRunning_ = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		//setRetainInstance(true);
		mainThread_ = new Handler();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
		{
			return null;
		}
		return inflater.inflate(R.layout.sunicon_converter, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		try
		{
			super.onActivityCreated(savedInstanceState);

			dbHelper_ = ((MainActivity)getActivity()).getDatabaseHelper();
			preferences_ =  ((MainActivity)getActivity()).getPreferences();
			unitHistory_ = new UnitHistoryManager(dbHelper_);
			
			categoryLabel_ = (TextView)this.getView().findViewById(R.id.categoryLabel);
			baseValueSwitcher_ = (ViewSwitcher)this.getView().findViewById(R.id.baseValueSwitcher);
			resultListSwitcher_ = (ViewSwitcher)this.getView().findViewById(R.id.resultListSwitcher);
			
			if (dbHelper_ == null)
			{
				throw new IllegalStateException("dbHelper is null");
			}
			
			initBaseUnitPickerButton();
			initBaseUnitPicker2Button();
			initBaseValueEditor();
			initBaseValueSpinner();
			initTargetUnitFilterEditor();
			initResultList();
			initResultListShowHideAnimation();
			initCurrencyUpdater();
			clearBaseUnit(false);
			
			if (savedInstanceState != null)
			{
				restoreFromBundles(savedInstanceState);
			}
			else
			{
				restoreFromPreferences();
			}
		}
		catch (Exception ex)
		{
			MyApplication.showErrorDialog(getFragmentManager(), null, ex);
		}
	}

//	@Override
//	public void onResume()
//	{
//		super.onResume();
//	}

	/* this methode init the loader so it should be called in onActivityCreated */
	private void restoreFromPreferences() {
		//Log.i(TAG + "-SR", "Restore State from Preference");
		if (preferences_ == null)
		{
			return;
		}
		try
		{
			//precision_ = preferences_.getLong("precision", 12);dd
			categoryId_ = preferences_.getLong("categoryId", -1);
			baseUnitId_ = preferences_.getLong("baseUnitId", -1);
			
			String baseValue = preferences_.getString("baseValue", null);
			
			setBaseUnit(
					preferences_.getString("categoryName", null), 
					preferences_.getString("baseUnitName", null),
					categoryId_,
					baseUnitId_);
			
			baseValueEditor_.setText(baseValue);
			setBaseValue(baseValue);
			
			//restore baseValueSpinnerItemPosition
			spinnerPositionToRestore_ = preferences_.getInt("spinnerPos", -1);
			getLoaderManager().initLoader(VALUE_SPINNER_LOADER, null, this); //should be called in onActivityCreated
			//setBaseValueSpinnerSelection(preferences.getInt("spinnerPos", -1));
			
			onPreferencesChanged();
		}
		catch (Exception ex)
		{
			showError(ex);
		}
	}
	
	/* this methode init the loader so it should be called in onActivityCreated */
	private void restoreFromBundles(Bundle savedState) {
		suspendEvents();
		try
		{
			categoryLabel_.setVisibility(savedState.getInt("categoryLabelVisibility"));
			categoryLabel_.setText(savedState.getCharSequence("categoryLabelText"));
			categoryId_ = savedState.getLong("categoryId");
			baseUnitId_ = savedState.getLong("baseUnitId");
			baseValueEditor_.setText(savedState.getCharSequence("baseValueEditorText"));
			baseUnitPickerButton_.setText(savedState.getCharSequence("baseUnitPickerButtonText"));
			targetUnitFilterEditor_.setText(savedState.getCharSequence("targetUnitFilterEditorText"));
			targetUnitFilterEditor_.setEnabled(savedState.getBoolean("targetUnitFilterEditorEnable"));
			clearTargetUnitFilterButton_.setEnabled(savedState.getBoolean("targetUnitFilterEditorEnable"));
			
			//restore baseValueSpinnerItemPosition
			spinnerPositionToRestore_ = savedState.getInt("baseValueSpinnerItemPosition");
			getLoaderManager().initLoader(VALUE_SPINNER_LOADER, null, this); //should be called in onActivityCreated
			if (savedState.getBoolean("spinnerVisible") && baseValueSwitcher_.getNextView() == baseValueSpinner_)
			{
				baseValueSwitcher_.showNext();
			}
			
			//restore resultListAdapter_
			JSONObject jsonResultListAdapter = new JSONObject(savedState.getString("resultListAdapter"));
			resultListAdapter_ = new ResultListAdapter(this, jsonResultListAdapter);
			setListAdapter(resultListAdapter_);

			//restore CurrencyUpdater
			long currencyUnitIdOnLoading = savedState.getLong("currencyUnitIdOnLoading"); 
			
			if (currencyUnitIdOnLoading>0) { //update in process
				currencyUpdater_.process(currencyUnitIdOnLoading);
			}
			else {
				UpdatingReport report = (UpdatingReport) savedState.getSerializable("importationReport");
				updateInProgressPanel_.setTag(report);
				updateCurrencyNotificationBar();
			}
			
			resultListAdapter_.invokeCalculation(false);
			
			//Log.i(TAG + "-SR", "Restore Spinner selection "+savedState.getInt("baseValueSpinnerItemPosition"));
		}
		catch (Exception ex)
		{
			showError(ex);
		}
		finally {
			resumeEvents();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i(TAG + "-SR", "Save State"); 
		try
		{
			outState.putCharSequence("categoryLabelText", categoryLabel_.getText());
			outState.putInt("categoryLabelVisibility", categoryLabel_.getVisibility());
			outState.putLong("categoryId", categoryId_);
			outState.putLong("baseUnitId", baseUnitId_);
			outState.putCharSequence("baseValueEditorText", baseValueEditor_.getText());
			outState.putInt("baseValueSpinnerItemPosition", baseValueSpinner_.getSelectedItemPosition());
			outState.putCharSequence("baseUnitPickerButtonText", baseUnitPickerButton_.getText());
			outState.putCharSequence("targetUnitFilterEditorText", targetUnitFilterEditor_.getText());
			outState.putBoolean("targetUnitFilterEditorEnable", targetUnitFilterEditor_.isEnabled());
			outState.putString("resultListAdapter", resultListAdapter_.serialize().toString());
			outState.putBoolean("spinnerVisible", baseValueSwitcher_.getNextView() == baseValueEditor_);
			outState.putLong("currencyUnitIdOnLoading", currencyUpdater_.getCurrencyUnitIdOnLoading());
			
			if (updateInProgressPanel_.getTag()!=null) {
				outState.putSerializable("importationReport", (UpdatingReport) updateInProgressPanel_.getTag());
			}
		}
		catch (Exception ex)
		{
			showError(ex);
		}

	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		SharedPreferences.Editor editor = preferences_.edit(); 

		editor.putLong("categoryId", categoryId_); 
		editor.putLong("baseUnitId", baseUnitId_);
		editor.putString("categoryName", categoryLabel_.getText().toString());
		editor.putString("baseUnitName", baseUnitPickerButton_.getText().toString());
		
		editor.putInt("spinnerPos", baseValueSpinner_.getSelectedItemPosition());
		editor.putString("baseValue", baseValueEditor_.getText().toString());
		
		editor.commit();
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		currencyUpdater_.cancel();
		isActivityRunning_ = false;
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		isActivityRunning_ = true;
	}
	
	private void initBaseValueEditor()
	{
		baseValueEditor_= (EditText)this.getView().findViewById(R.id.valueEditor);
		baseValueEditor_.addTextChangedListener(new TextWatcher()
		{
			private Runnable lastRunnable_;
			
			@Override
			public void onTextChanged(final CharSequence s, int start, int before, int count)
			{				
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(final Editable s)
			{
				try
				{
					if (isEventsSuspending())
					{
						return;
					}
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
										
					setComputationStateFinished(false); //hide the list while waiting and perform calculation
					
					/* events absorber technique */
					
					if (lastRunnable_!=null) {
						mainThread_.removeCallbacks(lastRunnable_);
					}
					
					lastRunnable_ = new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								/*
								 * do whatever onTextChanged event have to do. But it should be quick 
								 * heavy process must be executed on other thread  
								 */
								setBaseValue(s);
							}
							catch (Exception ex)
							{
								showError(ex);
							}
						}
					};
					
					mainThread_.postDelayed(lastRunnable_, EVENTS_DELAY);
				}
				catch (Exception ex)
				{
					showError(ex);
				}
			}
		});
	}	
	
	private void initBaseValueSpinner()
	{
		baseValueSpinner_ = (Spinner)this.getView().findViewById(R.id.valueSpinner);
		baseValueSpinnerAdapter_ = new SimpleCursorAdapter(this.getActivity(), 
				R.layout.spinner_item, 
				null, 
				new String[]{"v"}, ///the query of enumValues must return a column named "v" 
				new int[]{R.id.spinnerLabelItem}, 
				0);
		baseValueSpinner_.setAdapter(baseValueSpinnerAdapter_);
		baseValueSpinner_.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id)
			{
				try
				{
					if (isEventsSuspending())
					{
						return;
					}
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
					
					//Log.d(TAG, "spinner changes position = "+position);
					if (id>0) {
						getResultListAdapter().setBaseValue(null, id);
					}
				}
				catch (Exception ex)
				{
					showError(ex);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
			}
        	
		});
	}
	
	public final static int UNIT_PICKER_REQUEST = 1000;
	
	private void initBaseUnitPickerButton()
	{
		baseUnitPickerButton_ = (Button)this.getView().findViewById(R.id.baseUnitPickerButton);
		baseUnitPickerButton_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//ConverterFragment.this.getActivity().startActivityForResult(new Intent(ConverterFragment.this.getActivity(), UnitPicker.class), UNIT_PICKER_REQUEST);
				ConverterFragment.this.getActivity().startActivityForResult(new Intent(ConverterFragment.this.getActivity(), UnitPicker3.class), UNIT_PICKER_REQUEST);
			}
		});
	}

	private void initBaseUnitPicker2Button()
	{
		baseUnitPicker2Button_ = (Button)this.getView().findViewById(R.id.baseUnitPicker2Button);
		baseUnitPicker2Button_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ConverterFragment.this.getActivity(), UnitPicker2.class);
				i.putExtra("categoryId", categoryId_);
				i.putExtra("categoryName", categoryLabel_.getText());
				ConverterFragment.this.getActivity().startActivityForResult(i, UNIT_PICKER_REQUEST);
			}
		});
	}
	
	
	private void initTargetUnitFilterEditor()
	{
		targetUnitFilterEditor_ = (EditText)this.getView().findViewById(R.id.targetUnitFilterEditor);
		targetUnitFilterEditor_.addTextChangedListener(new TextWatcher()
		{
			private Runnable lastRunnable_;
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(final Editable s)
			{
				try
				{
					if (isEventsSuspending())
					{
						return;
					}
					
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
					
					/* events absorber technique */
					
					if (lastRunnable_!=null) {
						mainThread_.removeCallbacks(lastRunnable_);
					}
					
					lastRunnable_ = new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								/*
								 * do whatever onTextChanged event have to do. But it should be quick 
								 * heavy process must be executed on other thread  
								 */
								getResultListAdapter().getFilter().filter(s);
							}
							catch (Exception ex)
							{
								showError(ex);
							}
						}
					};
					
					mainThread_.postDelayed(lastRunnable_, EVENTS_DELAY);
				}
				catch (Exception ex)
				{
					showError(ex);
				}
			}
		});
		
		targetUnitFilterEditor_.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				try
				{	
					if (isEventsSuspending())
					{
						return;
					}
					if (isActivityRunning_ && hasFocus)
					{
						getResultListAdapter().getFilter().filter(targetUnitFilterEditor_.getText());
					}
				}
				catch (Exception ex)
				{
					showError(ex);
				}
			}
		});
		
		clearTargetUnitFilterButton_ = (Button)(this.getView().findViewById(R.id.clearTargetUnitFilterButton)); 
		clearTargetUnitFilterButton_.setOnClickListener(new android.view.View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try {
					targetUnitFilterEditor_.setText(null);
				}
				catch (Exception ex) {
					showError(ex);
				}
			}
		});
	}

	private void initResultList()
	{
		resultListAdapter_ = new ResultListAdapter(this);
		setListAdapter(resultListAdapter_);
		registerForContextMenu(getListView());
	}

	private void initCurrencyUpdater() 
	{
		updateInProgressPanel_ = getView().findViewById(R.id.updateInProgressPanel);
		currencyLoadingLabel_ = (TextView) getView().findViewById(R.id.currencyLoadingLabel); //TODO only for debug => remove it
		currencyLoadingProgressBar_ = (ProgressBar) getView().findViewById(R.id.currencyLoadingProgressBar); 
		
		currencyUpdater_ = new CurrencyUpdater(this.getActivity());
		currencyUpdater_.setBeforeUpdateStarted_(new BeforeUpdateStartedListener()
		{
			@Override
			public void beforeUpdateStarted(long currencyUnitId)
			{
				try
				{
					updateInProgressPanel_.setTag(null);
					currencyLoadingProgressBar_.setVisibility(View.VISIBLE);
					
					setCurrencyNotification(View.VISIBLE, 
							Color.WHITE,
							"Updating currency rate. Touch to skip..."); //TODO Multi-language
				}
				catch (Exception ex)
				{
					showError(ex);
				}	
			}
		});
		currencyUpdater_.setOnUpdateFinished(new OnUpdateFinishedListener()
		{
			@Override
			public void onUpdateFinished(CurrencyUpdater sender, UpdatingReport report)
			{
				try
				{
					updateInProgressPanel_.setTag(report);
					updateCurrencyNotificationBar();
					
					if (report!=null) {
						//Log.v("CURR", report.getContentMessage());
						
						//re-calculate resultList after updating currency rate. Warning, donnot process update again
						if (report.isDatabaseChanged())
							resultListAdapter_.reComputeAll();
					}
				}
				catch (Exception ex)
				{
					showError(ex);
				}	
			}
		});
		updateInProgressPanel_.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					if (!isActivityRunning_) return;
					
					UpdatingReport report = (UpdatingReport) updateInProgressPanel_.getTag();
					
					if (report == null || report.isInProgress()) {
						currencyUpdater_.cancel(); //skip
					}
					else {
						showUpdatingReport(report);
					}
				}
				catch (Exception ex)
				{
					showError(ex);
				}	
			}
		});
	}
	
	private void showUpdatingReport(UpdatingReport report) {
		ReportDialogFragment dialog = ReportDialogFragment.newInstance(report);
		dialog.setClickRetryListener(new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				try
				{
					if (categoryId_ == Category.CURRENCY_CATEGORY) { //prudent
						currencyUpdater_.cancel();
						currencyUpdater_.process(baseUnitId_);
					}
				}
				catch (Exception ex)
				{
					showError(ex);
				}
			}
		});
		
		dialog.show(getActivity().getSupportFragmentManager(), "ReportDialog");
	} 
	
	/**
	 * update base on updateInProgressPanel_.getTag
	 */
	private void updateCurrencyNotificationBar()
	{
		UpdatingReport report = (UpdatingReport) updateInProgressPanel_.getTag();
		
		if (report == null) {
			updateInProgressPanel_.setVisibility(View.GONE);
			return;
		}
	
		Log.v("CURR", "updateCurrencyNotificationBar");
		
		MessageType type = report.getType();
		
		int bgr = Color.WHITE;
		String label = report.getDisplayMessage();
		switch (type) {
			case ERROR: 
				bgr = getResources().getColor(R.color.light_red);
				break;
			case WARNING: 
				bgr = getResources().getColor(R.color.light_yellow);
				break;
			case INFO: 
				bgr = getResources().getColor(R.color.light_green);
			default: 
		}
		
		int visibility = View.VISIBLE;
		
		if (report.isSuccessAll()) {
			visibility = View.GONE;
		}
		
		setCurrencyNotification(visibility, bgr, label);
		
		currencyLoadingProgressBar_.setVisibility(report.isInProgress() ? View.VISIBLE : View.GONE);
	}
	private void setCurrencyNotification(int visibility, int bgr, String label)
	{
		updateInProgressPanel_.setVisibility(visibility);
		updateInProgressPanel_.setBackgroundColor(bgr);
		currencyLoadingLabel_.setText(label);
	}

	private void initResultListShowHideAnimation()
	{
//		ObjectAnimator fadeinAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fadein);
//		ObjectAnimator fadeoutAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fadeout);
//		LayoutTransition transitioner = new LayoutTransition();
//		transitioner.setAnimator(LayoutTransition.APPEARING, fadeinAnim);
//		transitioner.setAnimator(LayoutTransition.DISAPPEARING, fadeoutAnim);
//		
//		ViewGroup container = (ViewGroup)this.getWindow().getDecorView().findViewById(R.id.mainView);
//		container.setLayoutTransition(transitioner);		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		try
		{
			if (v == getListView())
			{
				MenuInflater inflater = this.getActivity().getMenuInflater();
	            inflater.inflate(R.menu.result_list_contextmenu, menu);
	            
	            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	            RowData rowData = (RowData)getListAdapter().getItem(info.position);
	            menu.setHeaderTitle(rowData.getFullUnitName());
			}
		}
		catch (Exception ex)
		{
			showError(ex);
		}	
	}
	
	@SuppressLint("NewApi")
	private void copyToClipboard(String text)
	{
		int sdk = android.os.Build.VERSION.SDK_INT;
		if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
		{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager)(getActivity().getSystemService(Activity.CLIPBOARD_SERVICE)); 
		    clipboard.setText(text);
		} 
		else 
		{
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)(getActivity().getSystemService(Activity.CLIPBOARD_SERVICE)); 
			android.content.ClipData clip = android.content.ClipData.newPlainText(text, text);
	        clipboard.setPrimaryClip(clip); 
		}
		Toast.makeText(this.getActivity(), "copied '"+text+"'", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		try
		{
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			if (info == null) {
				return false;
			}
			RowData rowData = (RowData)getListAdapter().getItem(info.position);
		    switch (item.getItemId()) 
		    {
		        case R.id.copyValueItem:
		        	copyToClipboard(rowData.getValueToCopy());
		            return true;
		        case R.id.copyUnitNameItem:
		        	copyToClipboard(rowData.getFullUnitName());
		            return true;
		        case R.id.copyValueAndUnitItem:
		        	copyToClipboard(rowData.getValueToCopy()+ " " + rowData.getFullUnitName());
		            return true;
		        case R.id.setUnitAsBaseItem:
		        	setBaseUnit(null, rowData.getUnitName(), rowData.getCategoryId(), rowData.getUnitId());
		            return true;
		    }
		}
		catch (Exception ex)
		{
			showError(ex);
		}
		return super.onContextItemSelected(item);
	}
	
	public void setBaseUnit(CharSequence categoryName, CharSequence unitName, long categoryId, long unitId) throws IllegalAccessException
	{
		if (categoryId == -1 || unitId == -1)
		{
			clearBaseUnit(true);
			return;
		}
		
		Log.d(TAG, "setBaseUnit("+categoryName+","+unitName+")");
		
		setComputationStateFinished(false);
		
		categoryLabel_.setVisibility(View.VISIBLE);
		if (categoryName != null)
		{
			categoryLabel_.setText(categoryName);
		}
		categoryId_ = categoryId;
		baseUnitId_ = unitId;
		targetUnitFilterEditor_.setEnabled(true);
		clearTargetUnitFilterButton_.setEnabled(true);
		baseUnitPickerButton_.setText(unitName);
		
		//Log.v(TAG, "setBaseUnit.invokeSaveToHistory");
		unitHistory_.invokeSaveToHistory(unitId);
		
		//Log.d(TAG, "setBaseUnit.filterSpinner");
		//baseValueSpinnerAdapter_.getFilter().filter(Long.toString(unitId));
		getLoaderManager().restartLoader(VALUE_SPINNER_LOADER, null, this);
		if (unitName != null)
		{
			baseUnitPickerButton_.setText(unitName);
		}	
		
		getResultListAdapter().setBaseUnitId(categoryId_, baseUnitId_);
		
		currencyUpdater_.cancel(); //cancel previous
		if (categoryId_ == Category.CURRENCY_CATEGORY) {
			//currencyUpdaterProcessDelayed();
			currencyUpdater_.process(baseUnitId_);
		}
		else {
			updateInProgressPanel_.setTag(null);
			updateInProgressPanel_.setVisibility(View.GONE);
		}
	}

//	private void currencyUpdaterProcessDelayed()
//	{
//		mainThread_.postDelayed(new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				try
//				{
//					currencyUpdater_.process(baseUnitId_);
//				}
//				catch (IllegalAccessException e)
//				{
//					e.printStackTrace();
//				}
//			}
//		}, 2000);
//	}
	
	private void clearBaseUnit(boolean keepTextOnBaseUnitEditor)
	{
		categoryLabel_.setVisibility(View.GONE);
		categoryId_ = -1;
		baseUnitId_ = -1;
		targetUnitFilterEditor_.setEnabled(false);
		clearTargetUnitFilterButton_.setEnabled(false);
		if (!keepTextOnBaseUnitEditor)
		{
			baseUnitPickerButton_.setText(null);
			
			//switch to the editor
			if (baseValueSwitcher_.getNextView() == baseValueEditor_)
			{
				baseValueSwitcher_.showNext();
			}
		}
	}
	
	private void setBaseValue(CharSequence s) throws IllegalAccessException
	{
		if (TextUtils.isEmpty(s))
		{
			getResultListAdapter().setBaseValue(Double.NaN, null);
		}
		else
		{
			double baseValue = Double.parseDouble(s.toString()); //use default culture local to parse
			getResultListAdapter().setBaseValue(baseValue, null);
			
			if (currencyUpdater_!=null && categoryId_ == Category.CURRENCY_CATEGORY) { //prudent) {
				//currencyUpdaterProcessDelayed();
				currencyUpdater_.process(baseUnitId_);
			}
		}
	}
	
	public DatabaseHelper getDatabaseHelper(){
		return dbHelper_;
	}
	
	private int eventsSuspendingLevel_ = 0;
	private boolean isEventsSuspending()
	{
		return eventsSuspendingLevel_ > 0;
	}	
	private void suspendEvents()
	{
		if (eventsSuspendingLevel_ < 0)
		{
			eventsSuspendingLevel_ = 0;
		}
		eventsSuspendingLevel_++;
	}
	private void resumeEvents()
	{
		if (eventsSuspendingLevel_<=0)
		{
			return;
		}
		eventsSuspendingLevel_--;
	}
	
	private Runnable lastSetComputationStateRunnable_ = null;
	
	public void setComputationStateFinished(final boolean finished) {
		
		if (lastSetComputationStateRunnable_ != null) {
			mainThread_.removeCallbacks(lastSetComputationStateRunnable_);
		}
		
		lastSetComputationStateRunnable_ = new Runnable()
		{
			@Override
			public void run()
			{
				try {
					if (!isActivityRunning_) {
						return;
					}
					if (resultListSwitcher_ != null) {
						if (finished) {
							// Log.v(TAG, "switch to result list");
							if (resultListSwitcher_.getNextView() == getListView()) {
								resultListSwitcher_.showNext();
							}
						} else {
							// Log.v(TAG, "switch to progress bar");
							if (resultListSwitcher_.getNextView() != getListView()) {
								resultListSwitcher_.showNext();
							}
						}
						// getListView().setVisibility(visible ? View.VISIBLE : View.GONE);
					}
				} catch (Exception ex) {
					showError(ex);
				}
			}
		};
		
		mainThread_.postDelayed(lastSetComputationStateRunnable_, EVENTS_DELAY);
	}

	public ResultListAdapter getResultListAdapter()
	{
		return resultListAdapter_;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args)
	{
		if (loaderId != VALUE_SPINNER_LOADER)
		{
			return null;
		}
		
		//Log.v(TAG + "-Loader", "onCreateLoader baseUnitId="+baseUnitId_);
		
		Loader<Cursor> loader=
		    new SQLiteCursorLoader(this.getActivity(), dbHelper_, 
		    		"SELECT id as _id, value as v "
		        + "FROM enumvalue WHERE unitid=?", new String[] {Long.toString(baseUnitId_)});
		
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		baseValueSpinnerAdapter_.changeCursor(cursor);
		if (cursor.getCount()>0)
	    {
			Log.v(TAG + "-Loader", "onLoadFinished set switch to spinner & restore position="+spinnerPositionToRestore_);

			//switch to the spinner
			if (baseValueSwitcher_.getNextView() == baseValueSpinner_)
			{
				baseValueSwitcher_.showNext();
			}
			
			//restore position if needed
			if (spinnerPositionToRestore_>=0) {
		    	baseValueSpinner_.setSelection(spinnerPositionToRestore_);
		    	spinnerPositionToRestore_ = -1;
		    }
	    }
		else
		{
			Log.v(TAG + "-Loader", "onLoadFinished set switch to editor");
			
			//switch to the editor
			if (baseValueSwitcher_.getNextView() == baseValueEditor_)
			{
				baseValueSwitcher_.showNext();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		baseValueSpinnerAdapter_.changeCursor(null);
	}

	public CurrencyUpdater getCurrencyUpdater()
	{
		return currencyUpdater_;
	}
	
	public void showError(Exception ex)
	{
		if (strictMode_) {
			MyApplication.showErrorDialog(getFragmentManager(), null, ex);
		}
		else {
			Log.w(TAG, ex);
		}
	}
	public void onPreferencesChanged() {
		if (preferences_!=null)
		{
			strictMode_ = preferences_.getBoolean(MainActivity.OPTNAME_STRICTMODE, MainActivity.DEFAULT_STRICTMODE);
			precision_ = preferences_.getInt(MainActivity.OPTNAME_PRECISION, MainActivity.DEFAULT_PRECISION);
		}
	}

	public int getPrecision()
	{
		return precision_;
	}
}
