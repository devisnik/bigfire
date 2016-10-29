package de.devisnik.android.bigmouth

import android.content.Context
import android.util.AttributeSet
import android.widget.AdapterView
import android.widget.Spinner
import java.util.*

class ValueSpinner(context: Context, attrs: AttributeSet) : Spinner(context, attrs) {

    private val itsValues: Array<CharSequence>
    private var itsInitialValue: String? = null

    init {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.ValueSpinner, 0, 0)
        itsValues = a.getTextArray(R.styleable.ValueSpinner_entry_values)
        a.recycle()
    }

    val selectedValue: CharSequence?
        get() {
            val selectionIndex = selectedItemPosition
            if (selectionIndex == AdapterView.INVALID_POSITION) return null
            return itsValues[selectionIndex]
        }

    fun setInitialSelectionFromValue(value: String) {
        itsInitialValue = value
        setSelection(Arrays.asList(*itsValues).indexOf(value))
    }

    fun hasChanged(): Boolean {
        return itsInitialValue != selectedValue
    }
}
