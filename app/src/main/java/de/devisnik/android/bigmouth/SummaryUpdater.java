/**
 * 
 */
package de.devisnik.android.bigmouth;

import java.util.Arrays;

import android.preference.ListPreference;
import android.preference.Preference;

class SummaryUpdater implements Preference.OnPreferenceChangeListener {

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference instanceof ListPreference) {
			updateListSummary((ListPreference)preference, (String) newValue);
		}
		else 
			updateSummary(preference, (String) newValue);
		return true;
	}
	
	void updateListSummary(ListPreference listPreference, String newValue) {
		int entryIndex = Arrays.asList(listPreference.getEntryValues()).indexOf(newValue);
		listPreference.setSummary(listPreference.getEntries()[entryIndex]);	
	}
	
	void updateSummary(Preference listPreference, String newValue) {
		listPreference.setSummary(newValue);	
	}

}