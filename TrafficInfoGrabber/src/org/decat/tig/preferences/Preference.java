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

import android.view.View;

class Preference {
	enum PreferenceType {
		TYPE_ACTIVITY,
		TYPE_ACTIVITY_VALUE,
		TYPE_BOOLEAN
	}

	enum PreferenceGroup {
		GROUP_ACTIVITIES,
		GROUP_ACTIVITIES_VALUES,
		GROUP_TOGGLES
	}

	protected String key;
	protected PreferenceGroup group;
	protected PreferenceType type;
	protected View view;

	public Preference(String key, PreferenceGroup group, PreferenceType type) {
		this.key = key;
		this.group = group;
		this.type = type;
		this.view = null;
	}
}