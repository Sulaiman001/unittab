package dh.sunicon;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.runnable.RowData;

public class ConverterFragment extends ListFragment 
{
	static final String TAG = ConverterFragment.class.getName();
	private DatabaseHelper dbHelper_;
	private TextView categoryLabel_;
	private ViewSwitcher baseValueSwitcher_;
	private EditText baseValueEditor_;
	private Spinner baseValueSpinner_;
	private AutoCompleteTextView baseUnitEditor_;
	private EditText targetUnitFilterEditor_;
	private Button clearTargetUnitFilterButton_;
	private Timer baseValueEditorTimer_;

	private ResultListAdapter resultListAdapter_;
	private SimpleCursorAdapter baseValueSpinnerAdapter_;
	
	private long baseUnitId_ = -1;
	private long categoryId_ = -1;
	
	private UnitHistoryManager unitHistory_;
	private CountDownLatch spinnerLoadingLatch_;
	
	private boolean isActivityRunning_ = false;

	
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
	public void onActivityCreated(Bundle savedInstanceState)
	{
		try
		{
			super.onActivityCreated(savedInstanceState);
			setRetainInstance(true);
			
			dbHelper_ = ((MainActivity)getActivity()).getDatabaseHelper();
			unitHistory_ = new UnitHistoryManager(dbHelper_);
			
			categoryLabel_ = (TextView)this.getView().findViewById(R.id.categoryLabel);
			baseValueSwitcher_ = (ViewSwitcher)this.getView().findViewById(R.id.baseValueSwitcher);
			
			isActivityRunning_ = true;
			
			if (dbHelper_ == null)
			{
				Log.wtf(TAG, "dbHelper is null");
				return;
			}
			
			initBaseUnitAutoCompleteEditor();
			initBaseValueEditor();
			initBaseValueSpinner();
			initTargetUnitFilterEditor();
			initResultList();
			initResultListShowHideAnimation();
			clearBaseUnit(false);
		}
		catch (Exception ex)
		{
			Log.wtf(TAG, ex);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		SharedPreferences preferences = this.getActivity().getPreferences(Activity.MODE_PRIVATE);
		
		try
		{
			categoryId_ = preferences.getLong("categoryId", -1);
			baseUnitId_ = preferences.getLong("baseUnitId", -1);
			
			setBaseUnit(
					preferences.getString("categoryName", null), 
					preferences.getString("baseUnitName", null),
					categoryId_,
					baseUnitId_);
		
			baseValueEditor_.setText(preferences.getString("baseValue", null));
			setBaseValueSpinnerSelection(preferences.getInt("spinnerPos", -1));
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		SharedPreferences preferences = this.getActivity().getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit(); 

		editor.putLong("categoryId", categoryId_); 
		editor.putLong("baseUnitId", baseUnitId_);
		editor.putInt("spinnerPos", baseValueSpinner_.getSelectedItemPosition());
		editor.putString("categoryName", categoryLabel_.getText().toString());
		editor.putString("baseUnitName", baseUnitEditor_.getText().toString());
		editor.putString("baseValue", baseValueEditor_.getText().toString());
		
		editor.commit();
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		isActivityRunning_ = false;
	}
	
	private void initBaseValueEditor()
	{
		baseValueEditor_= (EditText)this.getView().findViewById(R.id.valueEditor);
		baseValueEditor_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(final CharSequence s, int start, int before, int count)
			{
				try
				{
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
					
					setResultListVisible(false); //hide the list while waiting and perform calculation
					
					/* events absorber technique */
					
					if (baseValueEditorTimer_!=null) 
					{
						baseValueEditorTimer_.cancel(); //cancel the old onTextChange event
					}
	
					//the timer is dumped, we must to create a new one
					baseValueEditorTimer_ = new Timer();
					
					//schedule a task which will be execute in 500ms if the timer won't canceled due 
					//to other (possible future) onTextChanged event
					baseValueEditorTimer_.schedule(new TimerTask()  
					{
						@Override
						public void run()
						{
							ConverterFragment.this.getActivity().runOnUiThread(new Runnable()
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
										
										if (TextUtils.isEmpty(s))
										{
											getResultListAdapter().setBaseValue(Double.NaN, -1);
										}
										else
										{
											double baseValue = Double.parseDouble(s.toString());
											getResultListAdapter().setBaseValue(baseValue, -1);
										}
									}
									catch (Exception ex)
									{
										Log.w(TAG, ex);
									}
								}
							});
						}
					}, 500);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
	}	
	
	private void initBaseValueSpinner()
	{
	//		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
	//        R.array.planets_array, android.R.layout.simple_spinner_item);
	//
	//// Specify the layout to use when the list of choices appears
	//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	
		baseValueSpinner_ = (Spinner)this.getView().findViewById(R.id.valueSpinner);
		baseValueSpinnerAdapter_ = new SimpleCursorAdapter(this.getActivity(), 
				R.layout.spinner_item, 
				null, 
				new String[]{"v"}, ///the query of enumValues must return a column named "v" 
				new int[]{R.id.spinnerLabelItem}, 
				0);
		baseValueSpinner_.setAdapter(baseValueSpinnerAdapter_);
		baseValueSpinnerAdapter_.setFilterQueryProvider(new FilterQueryProvider()
		{
			/**
			 * constraint must be the unitId, this function will return the cursor of the enumValues of a unit 
			 */
			@Override
			public Cursor runQuery(CharSequence unitIdStr)
			{
				spinnerLoadingLatch_ = new CountDownLatch(1);
				try
				{
					if (TextUtils.isEmpty(unitIdStr))
					{
						return null;
					}
					
					Cursor cur = dbHelper_.getReadableDatabase().
							query("enumvalue", new String[]{"id as _id", "value as v"}, 
								"unitid=?", 
								new String[] {unitIdStr.toString()}, 
								null, null, "value");

					//simulateLongOperation(3, 5);
					
					/* switch the baseValueEditor to spinner or normal editor*/
					if (cur.getCount()>0)
					{
						//switch to the spinner
						getActivity().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									if (baseValueSwitcher_.getNextView() == baseValueSpinner_)
									{
										baseValueSwitcher_.showNext();
									}
								}
								catch (Exception ex)
								{
									Log.w(TAG, ex);
								}
							}
						});
					}
					else
					{
						//switch to the editor
						getActivity().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									if (baseValueSwitcher_.getNextView() == baseValueEditor_)
									{
										baseValueSwitcher_.showNext();
									}
								}
								catch (Exception ex)
								{
									Log.w(TAG, ex);
								}
							}
						});
					}
					
					return cur;
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
					return null;
				}
				finally
				{
					spinnerLoadingLatch_.countDown();
				}
			}
		});
		
		
		baseValueSpinner_.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id)
			{
				try
				{
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
					
					//Log.i(TAG, "convert unit with enumId = "+id);
					getResultListAdapter().setBaseValue(Double.NaN, id);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				try
				{
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
					
					getResultListAdapter().setBaseValue(Double.NaN, (long)-1);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
        	
		});
	}
	
	private void initBaseUnitAutoCompleteEditor()
	{
		baseUnitEditor_ = (AutoCompleteTextView)this.getView().findViewById(R.id.baseUnitEditor);
		
		final Cursor initialCursor = UnitsCursorAdapter.getInitialCursor(dbHelper_, true);
		UnitsCursorAdapter adapter = new UnitsCursorAdapter(this.getActivity(), initialCursor, false);
		
		baseUnitEditor_.setAdapter(adapter);
	    baseUnitEditor_.setThreshold(0);
	    
	    baseUnitEditor_.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				try
				{
					if (view == null)
					{
						return;
					}
					UnitsCursorAdapter.SuggestionData baseUnitData = (UnitsCursorAdapter.SuggestionData)view.getTag();
					setBaseUnit(baseUnitData.getCategoryName(), baseUnitData.getUnitName(), baseUnitData.getCategoryId(), baseUnitData.getUnitId());
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}				
			}
		});
	    
	    baseUnitEditor_.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				try
				{	
					if (keyMakeTextChanged(keyCode))
					{
						clearBaseUnit(true);
					}
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
				return false;
			}
		});
	    
	    baseUnitEditor_.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				try
				{	
					if (isActivityRunning_ && hasFocus)
					{
						baseUnitEditor_.showDropDown();
					}
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		});
	    
	    ((Button)(this.getView().findViewById(R.id.clearBaseUnitButton))).setOnClickListener(new android.view.View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				clearBaseUnit(false);
			}
		});
	}

	private void initTargetUnitFilterEditor()
	{
		targetUnitFilterEditor_ = (EditText)this.getView().findViewById(R.id.targetUnitFilterEditor);
		targetUnitFilterEditor_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(final CharSequence s, int start, int before, int count)
			{
				try
				{
					if (!isActivityRunning_ || getResultListAdapter() == null)
					{
						return;
					}
					
					getResultListAdapter().getFilter().filter(s);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
		
		targetUnitFilterEditor_.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				try
				{	
					if (isActivityRunning_ && hasFocus)
					{
						getResultListAdapter().getFilter().filter(targetUnitFilterEditor_.getText());
					}
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		});
		
		clearTargetUnitFilterButton_ = (Button)(this.getView().findViewById(R.id.clearTargetUnitFilterButton)); 
		clearTargetUnitFilterButton_.setOnClickListener(new android.view.View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				targetUnitFilterEditor_.setText(null);
			}
		});
	}

	private void initResultList()
	{
		resultListAdapter_ = new ResultListAdapter(this);
		setListAdapter(resultListAdapter_);
		registerForContextMenu(getListView());
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
			Log.w(TAG, ex);
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
			Log.w(TAG, ex);
		}
		return super.onContextItemSelected(item);
	}
	
	public void setBaseUnit(CharSequence categoryName, CharSequence unitName, long categoryId, long unitId) throws IllegalAccessException
	{
		if (categoryId == -1 || unitId == -1)
		{
			clearBaseUnit(false);
			return;
		}
		
		Log.d(TAG, "setBaseUnit("+categoryName+","+unitName+")");
		
		categoryLabel_.setVisibility(View.VISIBLE);
		if (categoryName != null)
		{
			categoryLabel_.setText(categoryName);
		}
		categoryId_ = categoryId;
		baseUnitId_ = unitId;
		targetUnitFilterEditor_.setEnabled(true);
		clearTargetUnitFilterButton_.setEnabled(true);
		
		Log.d(TAG, "setBaseUnit.invokeSaveToHistory");
		unitHistory_.invokeSaveToHistory(unitId);
		
		Log.d(TAG, "setBaseUnit.filterSpinner");
		baseValueSpinnerAdapter_.getFilter().filter(Long.toString(unitId));
		if (unitName != null)
		{
			baseUnitEditor_.setText(unitName);
		}	
		
		Log.d(TAG, "setBaseUnit.adapterSetBaseUnitId");
		getResultListAdapter().setBaseUnitId(categoryId_, baseUnitId_);
	}
	
	private void clearBaseUnit(boolean keepTextOnBaseUnitEditor)
	{
		categoryLabel_.setVisibility(View.GONE);
		categoryId_ = -1;
		baseUnitId_ = -1;
		targetUnitFilterEditor_.setEnabled(false);
		clearTargetUnitFilterButton_.setEnabled(false);
		if (!keepTextOnBaseUnitEditor)
		{
			baseUnitEditor_.setText(null);
			
			//switch to the editor
			if (baseValueSwitcher_.getNextView() == baseValueEditor_)
			{
				baseValueSwitcher_.showNext();
			}
		}
	}
	
	/**
	 * wait the spinner finish loading
	 * @param position
	 * @throws InterruptedException 
	 */
	private void setBaseValueSpinnerSelection(final int position) throws InterruptedException
	{
		Thread.sleep(500); //make sure that the spinner is populating
		
		if (spinnerLoadingLatch_!= null) 
		{
			//the spinner is not finish populating
			
			ExecutorService waitingThread = Executors.newSingleThreadExecutor();
			waitingThread.execute(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						spinnerLoadingLatch_.await(5, TimeUnit.SECONDS);
					}
					catch (InterruptedException e)
					{
						Log.w(TAG, e);
					}
					ConverterFragment.this.getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							baseValueSpinner_.setSelection(position);
						}
					});
				}
			});
		}
		else
		{
			baseValueSpinner_.setSelection(position);
		}
	}
	
	public DatabaseHelper getDatabaseHelper(){
		return dbHelper_;
	}
	
	/**
	 * Simulate a thread of long operation
	 * @param minSecond
	 * @param maxSecond
	 */
	public static void simulateLongOperation(int minSecond, int maxSecond)
	{
		Random rand = new Random(System.currentTimeMillis());
		long timeToSleep = (rand.nextInt(maxSecond-minSecond)+minSecond)*1000;
		
		try
		{
			Thread.sleep(timeToSleep);
		}
		catch (InterruptedException e)
		{
			Log.w("SimulationQuery", e);
		}
	}

//	private void restoreSpinnerSelection()
//	{
//		if (baseValueSpinner_.getTag()!=null)
//		{
//			baseValueSpinner_.setSelection((Integer)(baseValueSpinner_.getTag()));
//			baseValueSpinner_.setTag(null);
//		}
//	}

	private boolean keyMakeTextChanged(int keyCode)
	{
		return (7<=keyCode && keyCode<=17) || (29<=keyCode && keyCode<=56)
				|| (67<=keyCode && keyCode<=77) || (144<=keyCode && keyCode<=163)
				|| keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL;
	}
	
	public void setResultListVisible(boolean visible)
	{
		if (isActivityRunning_ && isVisible())
		{
			getListView().setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}
	
	public ResultListAdapter getResultListAdapter()
	{
		return resultListAdapter_;
	}
	
}
