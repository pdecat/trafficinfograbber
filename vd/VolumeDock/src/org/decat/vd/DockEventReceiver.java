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
	private static final int[] STREAMS = new int[] {
			AudioManager.STREAM_SYSTEM,
			AudioManager.STREAM_NOTIFICATION,
			AudioManager.STREAM_RING,
	};;

	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();

		if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_DESK_MODE.equals(intentAction)
				|| UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
			logMessage(context, "VolumeDock received a " + intentAction + " broadcast action.");

			// Get shared preferences
			SharedPreferences sharedPreferences = context.getSharedPreferences(DockEventReceiver.class.getSimpleName(), Context.MODE_PRIVATE);

			// Get the audio manager
			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

			if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction)) {
				// Get shared preferences editor
				SharedPreferences.Editor ed = sharedPreferences.edit();

				// Store current ringer mode
				int oldRingerMode = audioManager.getRingerMode();
				ed.putInt(PREVIOUS_RINGER_MODE, oldRingerMode);

				// Alter ringer mode
				int newRingerMode = UiModeManager.ACTION_ENTER_DESK_MODE.equals(intentAction) ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_NORMAL;
				audioManager.setRingerMode(newRingerMode);
				logMessage(context, "VolumeDock changed ringer mode from " + oldRingerMode + " to " + newRingerMode + ".");

				// Alter streams volume levels
				if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction)) {
					StringBuilder message = new StringBuilder();
					for (int i = 0; i < STREAMS.length; i++) {
						int stream = STREAMS[i];

						// Store current volume level for stream
						int oldStreamVolume = audioManager.getStreamVolume(stream);
						ed.putInt(PREVIOUS_VOLUME_LEVEL_SYSTEM + "_" + stream, oldStreamVolume);

						// Alter volume level for stream
						int newStreamVolume = audioManager.getStreamMaxVolume(stream);
						audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, newStreamVolume, 0);

						message.append("Stream " + stream + ": from " + oldStreamVolume + " to " + newStreamVolume + ".\n");
					}
					logMessage(context, "VolumeDock changed streams volume levels:\n" + message.toString());
				}

				// Commit preferences
				ed.commit();
			} else if (UiModeManager.ACTION_EXIT_DESK_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
				// Restore previous streams volume levels or max level if
				// unknown
				if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
					StringBuilder message = new StringBuilder();
					for (int i = 0; i < STREAMS.length; i++) {
						int stream = STREAMS[i];

						// Check current volume level for stream
						int oldStreamVolume = audioManager.getStreamVolume(stream);

						// Restore volume level for stream
						int newStreamVolume = sharedPreferences.getInt(PREVIOUS_VOLUME_LEVEL_SYSTEM + "_" + stream, audioManager.getStreamMaxVolume(stream));
						audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, newStreamVolume, 0);

						message.append("\tStream " + stream + ": from " + oldStreamVolume + " to " + newStreamVolume + ".\n");
					}
					logMessage(context, "VolumeDock restored streams volume levels:\n" + message.toString());
				}

				// Check current ringer mode
				int oldRingerMode = audioManager.getRingerMode();

				// Restore previous ringer mode or normal mode if unknown
				int newRingerMode = sharedPreferences.getInt(PREVIOUS_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
				audioManager.setRingerMode(newRingerMode);
				logMessage(context, "VolumeDock restored ringer mode from " + oldRingerMode + " to " + newRingerMode + ".");
			}
		}
	}

	private void logMessage(Context context, String message) {
		Log.i(VolumeDock.TAG, message);
		VolumeDock.showToast(context, message);
	}
}