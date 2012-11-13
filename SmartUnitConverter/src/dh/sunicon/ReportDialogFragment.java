package dh.sunicon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ReportDialogFragment extends DialogFragment
{
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Importation Report") // TODO multi-language
				.setMessage(getTag())
				.setPositiveButton("Retry",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								// User cancelled the dialog
							}
						})
				.setNeutralButton("Report",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								// FIRE ZE MISSILES!
							}
						})
				
				.setNegativeButton("Close",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								// User cancelled the dialog
							}
						});
		
		// Create the AlertDialog object and return it
		return builder.create();
	}

}