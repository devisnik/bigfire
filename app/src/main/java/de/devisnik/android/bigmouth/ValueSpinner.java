package de.devisnik.android.bigmouth;

import java.util.Arrays;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Spinner;

public class ValueSpinner extends Spinner {

	private CharSequence[] itsValues;
	private String itsInitialValue;

	public ValueSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.ValueSpinner, 0, 0);
		itsValues = a.getTextArray(R.styleable.ValueSpinner_entry_values);
		a.recycle();
	}

	public CharSequence getSelectedValue() {
		int selectionIndex = getSelectedItemPosition();
		if (selectionIndex == INVALID_POSITION)
			return null;
		return itsValues[selectionIndex];
	}
	
	public void setInitialSelectionFromValue(String value) {		
		itsInitialValue = value;
		setSelection(Arrays.asList(itsValues).indexOf(value));
	}
	
	public boolean hasChanged() {
		return !itsInitialValue.equals(getSelectedValue());
	}
}
