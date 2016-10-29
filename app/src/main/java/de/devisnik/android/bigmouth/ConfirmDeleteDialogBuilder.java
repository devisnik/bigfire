package de.devisnik.android.bigmouth;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class ConfirmDeleteDialogBuilder extends Builder {

	public ConfirmDeleteDialogBuilder(final Bites bites) {
		super(bites);
		setTitle(R.string.confirm_delete_title)
			.setMessage("")
			.setNegativeButton(R.string.cancel_button, null)
			.setPositiveButton(R.string.ok_button, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						bites.onDeleteConfirmed();
					}
				});
	}

}
