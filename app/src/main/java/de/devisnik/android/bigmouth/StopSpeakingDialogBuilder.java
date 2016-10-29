package de.devisnik.android.bigmouth;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class StopSpeakingDialogBuilder extends Builder {

	public StopSpeakingDialogBuilder(final Bites bites) {
		super(bites);
		setCancelable(false);
		setTitle(R.string.stop_talking_question).setPositiveButton(R.string.stop_button,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						bites.stopSpeaking();
					}
				});
	}
}
