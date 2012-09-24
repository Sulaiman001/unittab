package dh.sunicon;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public class ValueSpinnerCursorAdapter extends CursorAdapter implements SpinnerAdapter
{

	public ValueSpinnerCursorAdapter(Context context, Cursor c,
			boolean autoRequery)
	{
		super(context, c, autoRequery);
		// TODO Auto-generated constructor stub
	}

	public ValueSpinnerCursorAdapter(Context context, Cursor c, int flags)
	{
		super(context, c, flags);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint)
	{
		// TODO Auto-generated method stub
		return super.runQueryOnBackgroundThread(constraint);
	}
}
