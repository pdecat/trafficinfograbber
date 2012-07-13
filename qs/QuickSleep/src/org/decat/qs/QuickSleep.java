/**
 * Copyright (C) 2011-2012 Patrick Decat
 *
 * QuickSleep is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickSleep is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QuickSleep.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.qs;

/*
 **
 **       Copyright (C) 2010-2011 Patrick Decat
 ** 
 **       This file is part of QuickSleep.
 **
 **   QuickSleep is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   QuickSleep is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with QuickSleep.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class QuickSleep extends Activity {
	// Interaction with the DevicePolicyManager
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdminSample;

	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
	private static final String TAG = "QS";

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Prepare to work with the DPM
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdminSample = new ComponentName(this, QuickSleepDeviceAdminReceiver.class);

		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminSample);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.add_admin_extra_app_text));
		startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String message;
		switch (requestCode) {
		case REQUEST_CODE_ENABLE_ADMIN:
			Log.d(TAG, "Back from picking requesting admin priviledges with resultCode=" + resultCode);
			if (resultCode == RESULT_OK) {
				message = "Admin priviledges granted.";

				Timer t = new Timer();

				TimerTask lockTask = new TimerTask() {
					public void run() {
						mDPM.lockNow();
						finish();
					}
				};

				t.schedule(lockTask, 500);

			} else {
				message = "Failed to request admin priviledges.";
			}
			break;
		default:
			message = "Unknown activity request code " + requestCode;
		}
		Log.i(TAG, message);
	}
}
