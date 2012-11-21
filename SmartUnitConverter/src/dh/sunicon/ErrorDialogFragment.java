package dh.sunicon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;
import dh.sunicon.currency.UpdatingReport;

public class ErrorDialogFragment extends DialogFragment
{
	/**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    static public ErrorDialogFragment newInstance(String message, Throwable ex) {
    	ErrorDialogFragment dlg = new ErrorDialogFragment();
    	
        Bundle args = new Bundle();
        args.putString("message", TextUtils.isEmpty(message) ? ex.getMessage() : message);
        args.putString("stack", Log.getStackTraceString(ex));
        dlg.setArguments(args);
        
        return dlg;
    }
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction

		final String message = getArguments().getString("message");
		final String stackTrace = getArguments().getString("stack");
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		TextView contentView = (TextView)inflater.inflate(R.layout.report_dialog, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Error") // TODO multi-language
			.setView(contentView);
		builder.setNegativeButton("Close", null);
		
		builder.setNeutralButton("Report",
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						Intent send = new Intent(Intent.ACTION_SENDTO);
						String uriText;
				
						String subject = "[error] "+message;
						
						uriText = "mailto:dph.sunicon@gmail.com" 
						          +"?subject=" + Uri.encode(subject)  
						          +"&body="+Uri.decode(stackTrace);
						Uri uri = Uri.parse(uriText);

						send.setData(uri);
						startActivity(Intent.createChooser(send, "Send mail..."));
					}
				});
				
		contentView.setText(stackTrace);
		contentView.setMovementMethod(new ScrollingMovementMethod()); //make it scrollable
		return builder.create();
	}
}