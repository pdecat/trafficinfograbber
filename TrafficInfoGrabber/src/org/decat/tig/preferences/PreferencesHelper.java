/*
 **
 **       Copyright (C) 2010 Patrick Decat
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

	public final Preference[] preferences = {
			// Values must be before keys so they are loaded when the view is
			// created
			new Preference("OTHER_ACTIVITY" + VALUE_SUFFIX, PreferenceGroup.GROUP_ACTIVITIES_VALUES, PreferenceType.TYPE_ACTIVITY_VALUE, "Activity value"),
			new Preference("OTHER_ACTIVITY", PreferenceGroup.GROUP_ACTIVITIES, PreferenceType.TYPE_ACTIVITY, "Activity to launch on Search"),
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