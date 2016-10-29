package de.devisnik.android.bigmouth

import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity

class BitePreferences : PreferenceActivity() {

    private val itsSummaryUpdater = SummaryUpdater()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        adjustListPreference(R.string.pref_language)
        adjustListPreference(R.string.pref_pitch)
        adjustListPreference(R.string.pref_volume)
        adjustListPreference(R.string.pref_speed)
    }

    private fun adjustListPreference(key: Int) {
        val preference = findPreference(getString(key)) as ListPreference
        itsSummaryUpdater.onPreferenceChange(preference, preference.value)
        preference.onPreferenceChangeListener = itsSummaryUpdater
    }

}
