/*
 **
 **       Copyright (C) 2010-2011 Patrick Decat
 ** 
 **       This file is part of TIG.
 **
 **   TIG is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   TIG is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with TIG.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

package org.decat.tig.preferences;

import java.util.ArrayList;
import java.util.List;

import org.decat.tig.preferences.Preference.PreferenceGroup;
import org.decat.tig.preferences.Preference.PreferenceType;

import android.content.SharedPreferences;

public class PreferencesHelper {
	public static final String VALUE_SUFFIX = "_VALUE";

	public static final String INSTALLED_VERSION = "INSTALLED_VERSION";

	public static final String OTHER_ACTIVITY = "OTHER_ACTIVITY";

	public static final String NOTIFICATION_SHORTCUT = "NOTIFICATION_SHORTCUT";

	public static final String NOTIFICATION_SHORTCUT_ON_BOOT = "NOTIFICATION_SHORTCUT_ON_BOOT";

	public static final String SHOW_REFRESH_BUTTON = "SHOW_REFRESH_BUTTON";

	public static final String SHOW_DAY_NIGHT_SWITCH_BUTTON = "SHOW_DAY_NIGHT_SWITCH_BUTTON";

	public static final String SHOW_QUIT_BUTTON = "SHOW_QUIT_BUTTON";

	public static final String FORCE_PORTRAIT_ORIENTATION = "FORCE_PORTRAIT_ORIENTATION";

	public static final String SHOW_ADS = "SHOW_ADS";

	public final Preference[] preferences = {
			// Values must be before keys so they are loaded when the view is
			// created
			new Preference(OTHER_ACTIVITY + VALUE_SUFFIX, PreferenceGroup.GROUP_ACTIVITIES_VALUES, PreferenceType.TYPE_ACTIVITY_VALUE),
			new Preference(OTHER_ACTIVITY, PreferenceGroup.GROUP_ACTIVITIES, PreferenceType.TYPE_ACTIVITY),
			new Preference(NOTIFICATION_SHORTCUT, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
			new Preference(NOTIFICATION_SHORTCUT_ON_BOOT, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
			new Preference(SHOW_REFRESH_BUTTON, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
			new Preference(SHOW_DAY_NIGHT_SWITCH_BUTTON, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
			new Preference(SHOW_QUIT_BUTTON, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
			new Preference(FORCE_PORTRAIT_ORIENTATION, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
			new Preference(SHOW_ADS, PreferenceGroup.GROUP_TOGGLES, PreferenceType.TYPE_BOOLEAN),
	};

	SharedPreferences sharedPreferences;

	public PreferencesHelper(SharedPreferences sharedPreferences) {
		this.sharedPreferences = sharedPreferences;
	}

	public List<Preference> getPreferencesByGroup(PreferenceGroup group) {
		List<Preference> result = new ArrayList<Preference>();
		for (Preference preference : preferences) {
			if (preference.group.equals(group)) {
				result.add(preference);
			}
		}
		return result;
	}
}