package dh.sunicon.runnable;

import java.util.HashMap;

import android.support.v4.app.FragmentManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.Log;
import dh.sunicon.ConverterFragment;
import dh.sunicon.MainActivity;
import dh.sunicon.R;
import dh.sunicon.ResultListAdapter;
import dh.sunicon.datamodel.Category;
import dh.sunicon.datamodel.DatabaseHelper;
import dh.sunicon.datamodel.EnumValue;
import dh.sunicon.datamodel.Unit;

public class DataModelTest extends ActivityInstrumentationTestCase2<MainActivity> {
	static final String TAG = DataModelTest.class.getName();
	
	ConverterFragment converterFragment_;

	public DataModelTest() {
		super(MainActivity.class);
	}

	public void setUp() {
		FragmentManager fm = this.getActivity().getSupportFragmentManager();
		converterFragment_ = (ConverterFragment)fm.findFragmentByTag(Integer.toString(R.id.converterTab));
	}

	public void testGetCategory() {
		DatabaseHelper dbHelper = converterFragment_.getDatabaseHelper();
		Category cat47 = Category.findById(dbHelper, 47);
		assertEquals("Length / Distance", cat47.getName());
		
		HashMap<Long, Unit> unitsCat47 = cat47.getUnits();
		assertEquals(45, unitsCat47.size());
		
		Log.i("DataModelTest", "Test DB OK");
		
		//assertEquals(7.0, dbHelper.evalExpression("(4*x+9)^(1/2)", 10));
	}
	
	@UiThreadTest
	public void testCalculationEnum()
	{
		ResultListAdapter resultListAdapter = converterFragment_.getResultListAdapter();
		
		final long categoryId = 7; //Clothing size woman
		final long baseUnitId = 1211; //japan
		final long targetUnitId = 1213; //us
		final long baseEnumValueId = 51; //size 7 japan 
		
		try
		{
			resultListAdapter.setBaseUnitId(categoryId, baseUnitId); //47=length 650=milimeter
			RowData rowData = new RowData(resultListAdapter, categoryId, baseUnitId, targetUnitId, "japan", "", Double.NaN, baseEnumValueId);
			EnumValue convertedResu = rowData.computeTargetValue(baseEnumValueId);
			assertEquals("4", convertedResu.getValue()); //size 7 japan = size 4 us
		}
		catch (Exception e)
		{
			Log.w(TAG, e);
			e.printStackTrace();
		}
	}
	
	
	@UiThreadTest
	public void testCalculationSimple()
	{
		ResultListAdapter resultListAdapter = converterFragment_.getResultListAdapter();
		
		final long categoryId = 47; //length - distance
		final long baseUnitId = 650; //milimeter
		final long targetUnitId = 626; //centimeter
		
		try
		{
			resultListAdapter.setBaseUnitId(categoryId, baseUnitId); //47=length 650=milimeter
			RowData rowData = new RowData(resultListAdapter, categoryId, baseUnitId, targetUnitId, "centimeter", "milimeter", (double)25, -1);
			double convertedResu = rowData.computeTargetValue(25f);
			assertEquals(2.5, convertedResu);
		}
		catch (Exception e)
		{
			Log.w(TAG, e);
			e.printStackTrace();
		}
	}
	@UiThreadTest
	public void testCalculationComplex()
	{
		ResultListAdapter resultListAdapter = converterFragment_.getResultListAdapter();
		
		try
		{
			long categoryId = 80; //Temperature
			long baseUnitId = 964; //deg. Reaumur
			long targetUnitId = 965; //degree Celsius
			//bridgeId = 963 //Kelvin
			
			resultListAdapter.setBaseUnitId(categoryId, baseUnitId);
			RowData rowData = new RowData(resultListAdapter, categoryId, baseUnitId, targetUnitId, "deg. Reaumur", "degree Celsius", (double)500, -1);
			double convertedResu = rowData.computeTargetValue(500f);
			//bridgeValue = 898.15
			assertEquals(625.0, convertedResu);
			
			Thread.sleep(2000);
			
			categoryId = 32; //Fuel Consumption
			baseUnitId = 468; //centimeter/liter
			targetUnitId = 473; //kilometer/liter
			//bridgeId = 467 //meter/liter
			
			resultListAdapter.setBaseUnitId(categoryId, baseUnitId);
			rowData = new RowData(resultListAdapter, categoryId, baseUnitId, targetUnitId, "centimeter/liter", "kilometer/liter", (double)500, -1);
			convertedResu = rowData.computeTargetValue(500f);
			//bridgeValue = 5
			assertEquals(0.005, convertedResu);
		}
		catch (Exception e)
		{
			Log.w(TAG, e);
			e.printStackTrace();
		}
	}
}
