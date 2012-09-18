package org.decat.tig.preferences;

/*
 * #%L
 * TrafficInfoGrabber
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2012 Patrick Decat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.decat.tig.R;
import org.decat.tig.TIG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class PreferencesEditor extends PreferenceActivity {
	public static final String EXTRA_ID = "id";
	public static final String EXTRA_VALUE = "value";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize preference manager and activity
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(TIG.class.getSimpleName());

		addPreferencesFromResource(R.xml.preferences);

		// Update select third party activity button
		SharedPreferences sharedPreferences = getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);
		String thirdPartyActivity = sharedPreferences.getString(PreferencesHelper.OTHER_ACTIVITY, "Not set");
		final Preference otherActivityPref = findPreference(PreferencesHelper.OTHER_ACTIVITY);
		otherActivityPref.setSummary(thirdPartyActivity);
		otherActivityPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				// Useless check?
				if (otherActivityPref.equals(preference)) {
					Intent intent = new Intent(PreferencesEditor.this, ActivitySelector.class);
					otherActivityPref.setIntent(intent);
					startActivityForResult(intent, 0);
				}
				return true;
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		StringBuilder sb = new StringBuilder();

		sb.append("Back from picking activity with resultCode=");
		sb.append(resultCode);
		if (resultCode == RESULT_OK) {
			String dataString = data.getDataString();
			String value = data.getStringExtra(EXTRA_VALUE);

			sb.append(", dataString=");
			sb.append(dataString);
			sb.append(", id=");
			sb.append(data.getLongExtra(EXTRA_ID, -1));
			sb.append(", value=");
			sb.append(value);

			// Store new activity
			updateStringPreference(PreferencesHelper.OTHER_ACTIVITY, value);
			updateStringPreference(PreferencesHelper.OTHER_ACTIVITY + PreferencesHelper.VALUE_SUFFIX, value);

			// Update select third party activity button
			findPreference(PreferencesHelper.OTHER_ACTIVITY).setSummary(value);
		}
		Log.d(TIG.TAG, sb.toString());
	}

	private void updateStringPreference(String preference, String value) {
		SharedPreferences sharedPreferences = getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = sharedPreferences.edit();
		ed.putString(preference, value);
		ed.commit();
	}
}