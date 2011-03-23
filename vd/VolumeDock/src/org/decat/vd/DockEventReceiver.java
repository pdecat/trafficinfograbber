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

public class DockEventReceiver extends BroadcastReceiver {
	public static final String PREVIOUS_RINGER_MODE = "PREVIOUS_RINGER_MODE";
	public static final String PREVIOUS_VOLUME_LEVEL_SYSTEM = "PREVIOUS_VOLUME_LEVEL_SYSTEM";

	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();

		if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_DESK_MODE.equals(intentAction)
				|| UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
			logAction(context, intentAction);

			// Get shared preferences
			SharedPreferences sharedPreferences = context.getSharedPreferences(DockEventReceiver.class.getSimpleName(), Context.MODE_PRIVATE);

			// Get the audio manager
			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

			if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction)) {
				// Get shared preferences editor
				SharedPreferences.Editor ed = sharedPreferences.edit();

				// Store current ringer mode
				ed.putInt(PREVIOUS_RINGER_MODE, audioManager.getRingerMode());

				// Store current volume level for system stream
				ed.putInt(PREVIOUS_VOLUME_LEVEL_SYSTEM, audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));

				ed.commit();

				// Alter volume level for system stream
				if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction)) {
					audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM), 0);
				}

				// Alter ringer mode
				audioManager.setRingerMode(UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_NORMAL);
			} else if (UiModeManager.ACTION_EXIT_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
				// Restore previous ringer mode or normal mode if unknown
				audioManager.setRingerMode(sharedPreferences.getInt(PREVIOUS_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL));

				// Restore previous volume level for system stream or max level
				// if unknown
				audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, sharedPreferences.getInt(PREVIOUS_VOLUME_LEVEL_SYSTEM, audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)), 0);
			}
		}
	}

	private void logAction(Context context, String intentAction) {
		String message = "VolumeDock received a " + intentAction + " broadcast action.";
		Log.i(VolumeDock.TAG, message);
		VolumeDock.showToast(context, message);
	}
}