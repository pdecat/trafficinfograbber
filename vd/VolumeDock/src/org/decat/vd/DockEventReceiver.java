/*
 **
 **       Copyright (C) 2011 Patrick Decat
 ** 
 **       This file is part of VolumeDock.
 **
 **   VolumeDock is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   VolumeDock is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with VolumeDock.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

package org.decat.vd;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;
import android.widget.Toast;

public class DockEventReceiver extends BroadcastReceiver {
	public static final String TAG = "ODVD";
	public static final String PREVIOUS_RINGER_MODE = "PREVIOUS_RINGER_MODE";

	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();
		logAction(context, intentAction);

		if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_DESK_MODE.equals(intentAction)
				|| UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
			// logAction(context, intentAction);

			// Get shared preferences
			SharedPreferences sharedPreferences = context.getSharedPreferences(DockEventReceiver.class.getSimpleName(), Context.MODE_PRIVATE);

			// Get the audio manager
			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

			if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction)) {
				// Get shared preferences editor
				SharedPreferences.Editor ed = sharedPreferences.edit();

				// Store current ringer mode
				ed.putInt(PREVIOUS_RINGER_MODE, audioManager.getRingerMode());
				ed.commit();

				// Alter ringer mode
				audioManager.setRingerMode(UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_NORMAL);
			} else if (UiModeManager.ACTION_EXIT_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
				// Restore previous ringer mode
				int storedRingerMode = sharedPreferences.getInt(PREVIOUS_RINGER_MODE, 0);
				audioManager.setRingerMode(storedRingerMode);
			}
		}
	}

	private void logAction(Context context, String intentAction) {
		String message = "VolumeDock received a " + intentAction + " broadcast action.";
		Log.i(TAG, message);
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}