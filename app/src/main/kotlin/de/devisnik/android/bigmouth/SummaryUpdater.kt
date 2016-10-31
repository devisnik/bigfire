/**

 */
package de.devisnik.android.bigmouth

import android.preference.ListPreference
import android.preference.Preference

internal class SummaryUpdater : Preference.OnPreferenceChangeListener {

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference) {
            is ListPreference -> updateListSummary(preference, newValue as String)
            else -> updateSummary(preference, newValue as String)
        }
        return true
    }

    fun updateListSummary(listPreference: ListPreference, newValue: String) {
        val entryIndex = listPreference.entryValues.indexOf(newValue)
        listPreference.summary = listPreference.entries[entryIndex]
    }

    fun updateSummary(listPreference: Preference, newValue: String) {
        listPreference.summary = newValue
    }

}
