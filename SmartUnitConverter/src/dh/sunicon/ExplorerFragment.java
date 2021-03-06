package dh.sunicon;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
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
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.workarounds.MyApplication;

public class ExplorerFragment extends Fragment
{
	protected static final String TAG = "ExplorerFragment";
	
	private ExpandableListView treeList_;
	private UnitsCursorTreeAdapter unitCursorTreeAdapter_;
	private EditText filterEdit_;
	private ImageButton menuButton_;
	private Handler mainThread_;
	private ExecutorService updateThread_ = Executors.newSingleThreadExecutor();
	private DatabaseHelper dbHelper_;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		return (LinearLayout)inflater.inflate(R.layout.sunicon_explorer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		dbHelper_ = ((MainActivity)getActivity()).getDatabaseHelper();
		//setRetainInstance(true);
		
		initFilterEdit();
		initTreeList();
		initMenuButton();
		initWarningBeta();
	}
	
	private void initMenuButton() {
		menuButton_ = (ImageButton)getView().findViewById(R.id.menu);
		menuButton_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					registerForContextMenu(v);
					try {
						getActivity().openContextMenu(v);
					}
					finally {
						unregisterForContextMenu(v);
					}
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
		});
	}

	private void initWarningBeta()
	{
		TextView warningMessage = (TextView) getView().findViewById(R.id.warningMessage);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle("Notice") //TODO multi-language
				.setMessage(getResources().getString(R.string.explorer_warning_full))
				.setPositiveButton("OK", null);
		final AlertDialog fullWarningDlg = builder.create();
		fullWarningDlg.setCanceledOnTouchOutside(true);
		warningMessage.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				fullWarningDlg.show();
			}
		});
	}
	
	private void initTreeList() {
		treeList_ = (ExpandableListView)(getView().findViewById(R.id.treeList));

		unitCursorTreeAdapter_ = new UnitsCursorTreeAdapter(this);
		treeList_.setAdapter(unitCursorTreeAdapter_);
		unitCursorTreeAdapter_.getFilter().filter("");
	}
	
	private void initFilterEdit() {
		filterEdit_ = (EditText) (getView().findViewById(R.id.filterEdit));
        filterEdit_.addTextChangedListener(new TextWatcher() {
			
        	private Runnable lastRunnable_ = null;
        	
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(final Editable s) {
				
				try
				{
					if (lastRunnable_ != null) {
						mainThread_.removeCallbacks(lastRunnable_);
					}
					
					lastRunnable_ = new Runnable() {
						@Override
						public void run() {
							try {
								if (unitCursorTreeAdapter_!=null) {
									unitCursorTreeAdapter_.getFilter().filter(s);
								}
							}
							catch (Exception ex)
							{
								Log.w(TAG, ex);
							}	
						}
					};
					
					mainThread_.postDelayed(lastRunnable_, 500);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
		
			}
		});
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		try
		{
			if (v == menuButton_)
			{
				MenuInflater inflater = this.getActivity().getMenuInflater();
	            inflater.inflate(R.menu.explorer_contextmenu, menu);
			}
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}	
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case R.id.selectAll:
        	invokeSelectAll();
        	return true;
        case R.id.selectCommuns:
        	invokeSelectCommuns(false);
        	return true;
        case R.id.selectCommunsOnly:
        	invokeSelectCommuns(true);
        	return true;
        default:
        	return super.onContextItemSelected(item);
		}
	}
	
	static final long[] commonsCategories = new long[] {
		1, 3, 4, 7, 10, Category.CURRENCY_CATEGORY, 13, 32, 45,
		47, 50, 70, 71, 80, 85, 89, 93, 96
	};
	static final long[] commonsCurrencies = new long[] {
		1249,1255,1256,1257,1259,1260,1261,1262,1263,1265,
		1267,1268,1269,1274,1276,1278,1280,1281,1286,1288,
		1291,1293,1294,1296,1298,1300,1303,1305,1307,1309,
		1310,1312,1313,1316,1317,1318,1319,1320,1322,1325,
		1326,1328,1329,1330,1333,1334,1336,1337,1338,1339,
		1344,1345,1347,1348,1350,1351,1353,1354,1355,1356,
		1357,1359,1361,1362,1364,1365,1366,1367,1368,1369,
		1371,1373,1375,1376,1384,1386,1389,1391,1393,1395,
		1396,1397,1399,1400,1403,1404,1406,1407,1409,1413
	};
//	static final long[] commonsCurrencies = new long[] {
//		1249,1255,1256,1268,1274,1276,
//		1280,1286,1288,1293,1296,1305,
//		1309,1312,1313,1319,1325,1336,
//		1347,1353,1355,1361,1364,1369,
//		1375,1376,1386,1391,1409,1413
//	};
	
	private void invokeSelectAll() {
		updateThread_.submit(new Runnable() {
			@Override
			public void run() {
				SQLiteDatabase db = dbHelper_.getWritableDatabase();
				db.beginTransaction();
				try {
					ContentValues cvsTrue = new ContentValues();
					cvsTrue.put("enabled", true);
					db.update("category", cvsTrue, null, null);
					db.update("unit", cvsTrue, null, null);
					
					db.setTransactionSuccessful();
					
					//reset filter
					mainThread_.post(new Runnable() {
						@Override
						public void run() {
							try {
								unitCursorTreeAdapter_.getFilter().filter("");
							}
							catch (Exception ex) {
								Log.w(TAG, ex);
							}
						}
					});
					
					Log.i(TAG, "Select All done.");
				}
				catch (Exception ex) {
					MyApplication.showErrorDialog(getFragmentManager(), "Failed select all units", ex);
				}
				finally {
					db.endTransaction();
				}
			}
		});
	}
	
	private void invokeSelectCommuns(final boolean disableOthers) {
		updateThread_.submit(new Runnable() {
			@Override
			public void run() {
				SQLiteDatabase db = dbHelper_.getWritableDatabase();
				db.beginTransaction();
				try {
					if (disableOthers) {
						//un select all
						ContentValues cvsFalse = new ContentValues();
						cvsFalse.put("enabled", false);
						db.update("category", cvsFalse, null, null);
						db.update("unit", cvsFalse, "categoryId="+Category.CURRENCY_CATEGORY, null);
					}
					
					//select  
					ContentValues cvsTrue = new ContentValues();
					cvsTrue.put("enabled", true);
					db.update("category", cvsTrue, "id in "+arrayToString(commonsCategories), null);
					db.update("unit", cvsTrue, "id in "+arrayToString(commonsCurrencies), null);
					
					db.setTransactionSuccessful();
					
					//reset filter
					mainThread_.post(new Runnable() {
						@Override
						public void run() {
							try {
								unitCursorTreeAdapter_.getFilter().filter("");
							}
							catch (Exception ex) {
								Log.w(TAG, ex);
							}
						}
					});
					
					Log.i(TAG, "Select Communs done");
				}
				catch (Exception ex) {
					MyApplication.showErrorDialog(getFragmentManager(), "Failed select communs units", ex);
				}
				finally {
					db.endTransaction();
				}
			}
		});
	}
	
	public static String arrayToString(long[] array) {
		if (array == null) {
			return "null";
		}
		if (array.length == 0) {
			return "()";
		}
		StringBuilder sb = new StringBuilder(array.length * 6);
		sb.append('(');
		sb.append(array[0]);
		for (int i = 1; i < array.length; i++) {
			sb.append(",");
			sb.append(array[i]);
		}
		sb.append(')');
		return sb.toString();
	}
}