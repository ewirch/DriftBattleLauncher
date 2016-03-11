package edu.wirch.driftbattlelauncher;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {
	public static final String KEY_SWITCH_DURATION = "switchDuration";
	public static final String KEY_USE_START_TONE = "useStartTone";
	public static final String KEY_START_TONE_DURATION = "startToneDuration";
	public static final String KEY_AFTER_START = "afterStart";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}

	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(edu.wirch.driftbattlelauncher.R.xml.preferences);
		}
	}

}
