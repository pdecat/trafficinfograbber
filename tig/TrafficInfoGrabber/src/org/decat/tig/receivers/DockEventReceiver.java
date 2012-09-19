package org.decat.tig.receivers;

/*
 * #%L
 * TrafficInfoGrabber
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

import org.decat.tig.TIG;
import org.decat.tig.preferences.PreferencesHelper;

import android.annotation.TargetApi;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/* 
 * This receiver uses Android SDK level 8 APIs but will never get reached on older platforms. 
 * Indeed, android.app.action.ENTER_CAR_MODE and android.app.action.EXIT_CAR_MODE are also new intents since level 8.
 */
@TargetApi(8)
public class DockEventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();
		Log.d(TIG.TAG, "DockEventReceiver.onReceive: intentAction=" + intentAction);

		if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction) || UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction)) {
			// Get shared preferences
			SharedPreferences sharedPreferences = context.getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);

			if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intentAction) && sharedPreferences.getBoolean(PreferencesHelper.LAUNCH_TIG_ON_ENTER_CAR_DOCK, false)) {
				Log.i(TIG.TAG, "DockEventReceiver.onReceive: launching TIG...");

				Intent startTigIntent = new Intent(context, TIG.class);
				startTigIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(startTigIntent);
			} else if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(intentAction) && sharedPreferences.getBoolean(PreferencesHelper.QUIT_TIG_ON_EXIT_CAR_DOCK, false)) {
				Log.i(TIG.TAG, "DockEventReceiver.onReceive: quitting TIG...");
				Intent finishTigIntent = new Intent(context, TIG.class);
				finishTigIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				finishTigIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				finishTigIntent.putExtra(TIG.QUIT, true);
				context.startActivity(finishTigIntent);
			}
		}
	}
}