package de.devisnik.android.bigmouth;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class BitePreferences extends PreferenceActivity {

	private SummaryUpdater itsSummaryUpdater = new SummaryUpdater();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		adjustListPreference(R.string.pref_language);
		adjustListPreference(R.string.pref_pitch);
		adjustListPreference(R.string.pref_volume);
		adjustListPreference(R.string.pref_speed);
	}

	private void adjustListPreference(final int key) {
		final ListPreference preference = (ListPreference) findPreference(getString(key));
		itsSummaryUpdater.onPreferenceChange(preference, preference.getValue());
		preference.setOnPreferenceChangeListener(itsSummaryUpdater);
	}

}
