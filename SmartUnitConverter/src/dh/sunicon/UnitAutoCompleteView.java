package dh.sunicon;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class UnitAutoCompleteView extends AutoCompleteTextView
{
	public interface ReplaceTextListener
	{
		public void onReplaceText(UnitAutoCompleteView sender, String categoryName, String unitName, long categoryId, long unitId);
	}
	
	private ReplaceTextListener onReplaceTextListener;
	
	public UnitAutoCompleteView(Context context)
	{
		super(context);
		init(context);
	}

	public UnitAutoCompleteView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}

	public UnitAutoCompleteView(Context context, AttributeSet attrs,
			int defStyle)
	{
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context)
	{
		if (!isInEditMode())
		{
			setAdapter(((MainActivity)context).getUnitsCursorAdapter());
		}
		setThreshold(1);
	}
	
	@Override
	protected void replaceText(final CharSequence text)
	{
		//this.invalidate();
		
		//get the string returned by UnitsCursorAdapter.convertToString
		String[] resultfromCursorAdapter = text.toString().split("\n");

		//parse it
		String categoryName = resultfromCursorAdapter[0];
		String unitName = resultfromCursorAdapter[1];
		long categoryId = Long.parseLong(resultfromCursorAdapter[2]);
		long unitId = Long.parseLong(resultfromCursorAdapter[3]);
		
		//fill the autoCompleteTextBox with the unitName
		super.replaceText(unitName);
		
		//send other info to outside (MainActivity) so that it will set other label outside
		if (onReplaceTextListener!=null)
		{
			onReplaceTextListener.onReplaceText(this, categoryName, unitName, categoryId, unitId);
		}	
	}
	
	public ReplaceTextListener getOnReplaceTextListener()
	{
		return onReplaceTextListener;
	}

	public void setOnReplaceTextListener(ReplaceTextListener onReplaceTextListener)
	{
		this.onReplaceTextListener = onReplaceTextListener;
	}
}
