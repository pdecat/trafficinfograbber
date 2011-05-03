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

package org.decat.tig.receivers;

import org.decat.tig.TIG;
import org.decat.tig.preferences.PreferencesHelper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootCompletedReceiver extends android.content.BroadcastReceiver {
	private boolean notificationShortcutOnBoot = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Log.i(TIG.TAG, "Boot completed intent received.");

			// Get shared preferences
			SharedPreferences sharedPreferences = context.getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);

			// Get current value
			boolean value = sharedPreferences.getBoolean(PreferencesHelper.NOTIFICATION_SHORTCUT_ON_BOOT, true);

			if (value != notificationShortcutOnBoot) {
				// Update notification shortcut state
				TIG.updateNotificationShortcutVisibility(context);
			}
		}
	}
}