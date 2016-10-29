package de.devisnik.android.bigmouth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import de.devisnik.android.bigmouth.data.SoundBite;

public class BiteEditor extends Activity {

	public static final String BITE_DATA = "bite";
	public static final int BITE_SAVED = 999;
	public static final int BITE_ABORTED = 888;
	public static final int BITE_DELETED = 777;
	private EditText itsTitle;
	private EditText itsMessage;
	private SoundBite itsBite;
	private ValueSpinner itsLocaleSpinner;
	private ValueSpinner itsPitchSpinner;
	private ValueSpinner itsVolumeSpinner;
	private ValueSpinner itsSpeedSpinner;
	private int itsMarkerCount = 0;
	
	private void markAsUnchanged(TextView view) {
		CharSequence oldText = view.getText();
		if (oldText.charAt(oldText.length() - 1) == '*') {
			view.setText(oldText.subSequence(0, oldText.length() - 1));
			itsMarkerCount--;
		}
	}

	private void markAsChanged(TextView view) {
		CharSequence oldText = view.getText();
		if (!(oldText.charAt(oldText.length() - 1) == '*')) {
			view.setText(oldText + "*");
			itsMarkerCount++;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editor);
		itsBite = (SoundBite) getIntent().getSerializableExtra(BITE_DATA);
		initUi();
	}

	private void initUi() {
		itsTitle = initLabelledInput(R.id.titleLabel, R.id.title, itsBite.title);
		itsMessage = initLabelledInput(R.id.messageLabel, R.id.message, itsBite.message);
		itsLocaleSpinner = initSpinner(R.id.LocaleLabel, R.id.LocaleSpinner, itsBite.language);
		itsPitchSpinner = initSpinner(R.id.PitchLabel, R.id.PitchSpinner, itsBite.pitch);
		itsVolumeSpinner = initSpinner(R.id.VolumeLabel, R.id.VolumeSpinner, itsBite.volume);
		itsSpeedSpinner = initSpinner(R.id.SpeedLabel, R.id.SpeedSpinner, itsBite.speed);
	}

	private EditText initLabelledInput(int labelId, int editId, final String initialValue) {
		final TextView label = (TextView) findViewById(labelId);
		EditText edit = (EditText) findViewById(editId);
		edit.setText(initialValue);
		edit.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if (!s.toString().equals(initialValue)) {
					markAsChanged(label);
				} else {
					markAsUnchanged(label);
				}
			}
		});
		return edit;
	}

	private ValueSpinner initSpinner(int labelId, int spinnerId, String value) {
		final ValueSpinner spinner = (ValueSpinner) findViewById(spinnerId);
		final TextView label = (TextView) findViewById(labelId);
		spinner.setInitialSelectionFromValue(value);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (spinner.hasChanged())
					markAsChanged(label);
				else 
					markAsUnchanged(label);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		return spinner;
	}

	private void handleCancel() {
		setResult(BITE_ABORTED);
	}

	private void handleDelete() {
		Intent intent = new Intent(getIntent());
		intent.putExtra(BITE_DATA, itsBite);
		setResult(BITE_DELETED, intent);
	}

	private void handleSave() {
		if (itsTitle.getText().length() == 0 && itsMessage.getText().length() == 0) {
			// no input, nothing to save
			handleCancel();
			return;
		}
		if (itsMarkerCount == 0) { // no changes, nothing to save			
			handleCancel();
			return;
		}
		SoundBite bite = new SoundBite();
		bite.id = itsBite.id;
		bite.title = itsTitle.getText().toString();
		bite.message = itsMessage.getText().toString();
		bite.language = itsLocaleSpinner.getSelectedValue().toString();
		bite.pitch = itsPitchSpinner.getSelectedValue().toString();
		bite.volume = itsVolumeSpinner.getSelectedValue().toString();
		bite.speed = itsSpeedSpinner.getSelectedValue().toString();
		Intent intent = new Intent(getIntent());
		intent.putExtra(BITE_DATA, bite);
		setResult(BITE_SAVED, intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.editor_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save:
			handleSave();
			break;
		case R.id.delete:
			handleDelete();
			break;
		case R.id.cancel:
			handleCancel();
			break;
		}
		finish();
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
			handleSave();
		return super.onKeyDown(keyCode, event);
	}

}