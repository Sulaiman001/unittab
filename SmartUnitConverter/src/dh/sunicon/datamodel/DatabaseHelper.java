package dh.sunicon.datamodel;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class DatabaseHelper extends SQLiteAssetHelper
{
	public static final SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final int CURRENCY_COUNT = 165;
	
	private static final String DATABASE_NAME = "SmartUnitConverter.db";
	private static final int DATABASE_VERSION = 13;

	// private LruCache<Long, BaseEntity> entityCache;

	public DatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		setForcedUpgradeVersion(13);

		// Get memory class of this device, exceeding this amount will throw an
		// OutOfMemory exception.
		// final int memClass = ((ActivityManager) context
		// .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

		// Use 1/8th of the available memory for this memory cache.
		// final int cacheSize = 1024 * 1024 * memClass / 8;

		// this.entityCache = new LruCache<Long, BaseEntity>(cacheSize);
	}

	
	public static long getNow() {
		return Calendar.getInstance().getTime().getTime();
	}
	
	// public LruCache<Long, BaseEntity> getEntityCache()
	// {
	// return entityCache;
	// }
/*
	public double evalExpression(String expression, double x)
	{
		Calculable calc;
		try
		{
			calc = new ExpressionBuilder(expression).withVariableNames("x")
					.build();
		}
		catch (Exception e)
		{
			Log.e("DatabaseHelper", "Failed to evaluate expression", e);
			return Double.NaN;
		}
		calc.setVariable("x", x);
		return calc.calculate();
	}
*/
}
