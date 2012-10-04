package dh.sunicon;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class UnitAutoCompleteView extends AutoCompleteTextView
{

	public UnitAutoCompleteView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public UnitAutoCompleteView(Context context, AttributeSet attrs,
			int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean enoughToFilter()
	{
		return true;
	}

//	@Override
//	protected void replaceText(final CharSequence text)
//	{
//		//get the string returned by UnitsCursorAdapter.convertToString
//		String[] resultfromCursorAdapter = text.toString().split("\n");
//
//		//parse it
//		String categoryName = resultfromCursorAdapter[0];
//		String unitName = resultfromCursorAdapter[1];
//		long categoryId = Long.parseLong(resultfromCursorAdapter[2]);
//		long unitId = Long.parseLong(resultfromCursorAdapter[3]);
//		
//		//fill the autoCompleteTextBox with the unitName
//		super.replaceText(unitName);
//		
//		//send other info to outside (MainActivity) so that it will set other label outside
//		if (onReplaceTextListener!=null)
//		{
//			onReplaceTextListener.onReplaceText(this, categoryName, unitName, categoryId, unitId);
//		}	
//	}
}
