package dh.sunicon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import dh.sunicon.currency.UpdatingReport;

public class ReportDialogFragment extends DialogFragment
{
	/**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    static ReportDialogFragment newInstance(UpdatingReport report) {
    	ReportDialogFragment dlg = new ReportDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putSerializable("report", report);
        dlg.setArguments(args);
        
        return dlg;
    }
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction

		final UpdatingReport report = (UpdatingReport) getArguments().getSerializable("report");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Exchange rates updating Report") // TODO multi-language
				.setMessage(report.getContentMessage());
		
		builder.setNegativeButton("Close", null);
		
		if (report.containsErrorOrWarning()) {
			builder.setNeutralButton("Report",
					new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							Intent send = new Intent(Intent.ACTION_SENDTO);
							String uriText;
					
							String subject = "Currency Exchange Rates updating Report";
							
							uriText = "mailto:dph.sunicon@gmail.com" 
							          +"?subject=" + Uri.encode(subject)  
							          +"&body="+Uri.decode(report.getDetailMessage());
							Uri uri = Uri.parse(uriText);
	
							send.setData(uri);
							startActivity(Intent.createChooser(send, "Send mail..."));
						}
					});
		}
		
		if (report.isCancel() || !report.successUpdateMostly()) {
			builder.setPositiveButton("Retry",
					new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							// TODO
						}
					});
		}
				
		return builder.create();
	}
	
//	@Override
//	public void onActivityCreated(Bundle savedInstanceState) {
//		super.onActivityCreated(savedInstanceState);
//		
//	}
//	
//	@Override
//	public void onStart()
//	{
//		super.onStart();
//		View v = getView();
//		if (v==null) {
//			return;
//		}
//		TextView msgTextView = (TextView) v.findViewById(android.R.id.message);
//		if (msgTextView !=null) {
//			msgTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
//		}
//	}
}