/*
 **
 **       Copyright (C) 2010 Patrick Decat
 ** 
 **       This file is part of dear2dear.
 **
 **   dear2dear is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   dear2dear is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with dear2dear.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

package org.decat.d2d;

import java.util.ArrayList;
import java.util.List;

import org.decat.d2d.Preference.PreferenceGroup;
import org.decat.d2d.Preference.PreferenceType;

import android.content.SharedPreferences;

public class PreferencesHelper {
	public static final String VALUE_SUFFIX = "_VALUE";

	public final Preference[] preferences = {
			new Preference("MESSAGE_1", PreferenceGroup.GROUP_MESSAGES, PreferenceType.TYPE_STRING, "Message 1"),
			new Preference("MESSAGE_2", PreferenceGroup.GROUP_MESSAGES, PreferenceType.TYPE_STRING, "Message 2"),
			new Preference("MESSAGE_3", PreferenceGroup.GROUP_MESSAGES, PreferenceType.TYPE_STRING, "Message 3"),
			// Values must be before keys so they are loaded when the view is
			// created
			new Preference("CONTACT_1" + VALUE_SUFFIX, PreferenceGroup.GROUP_CONTACTS_VALUES, PreferenceType.TYPE_CONTACT_VALUE, "Contact value 1"),
			new Preference("CONTACT_2" + VALUE_SUFFIX, PreferenceGroup.GROUP_CONTACTS_VALUES, PreferenceType.TYPE_CONTACT_VALUE, "Contact value 2"),
			new Preference("CONTACT_3" + VALUE_SUFFIX, PreferenceGroup.GROUP_CONTACTS_VALUES, PreferenceType.TYPE_CONTACT_VALUE, "Contact value 3"),
			new Preference("CONTACT_1", PreferenceGroup.GROUP_CONTACTS, PreferenceType.TYPE_CONTACT, "Contact 1"),
			new Preference("CONTACT_2", PreferenceGroup.GROUP_CONTACTS, PreferenceType.TYPE_CONTACT, "Contact 2"),
			new Preference("CONTACT_3", PreferenceGroup.GROUP_CONTACTS, PreferenceType.TYPE_CONTACT, "Contact 3"),
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