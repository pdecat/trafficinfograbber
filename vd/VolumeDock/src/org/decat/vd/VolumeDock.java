package org.decat.vd;

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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class VolumeDock extends Activity {
	public static final String TAG = "ODVD";

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(VolumeDock.TAG, "onCreate called");
		// Show about dialog
		showAbout();
	}

	@Override
	public void onStart() {
		super.onStart();

		Log.d(VolumeDock.TAG, "onStart called");
	}

	@Override
	public void onRestart() {
		super.onRestart();

		Log.d(VolumeDock.TAG, "onRestart called");
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.d(VolumeDock.TAG, "onResume called");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(VolumeDock.TAG, "onActivityResult called (requestCode=" + requestCode + ", resultCode" + resultCode + ")");

		switch (requestCode) {
		case ACTIVITY_REQUEST_OI_ABOUT_LAUNCH:
			if (resultCode == RESULT_OK) {
				Log.d(VolumeDock.TAG, "Back from OI About");
				finish();
			}
			break;
		case ACTIVITY_REQUEST_OI_ABOUT_INSTALL:
			if (resultCode == RESULT_CANCELED) {
				Log.d(VolumeDock.TAG, "Back from Android Market");
				showAbout();
			}
			break;
		default:
			Log.w(VolumeDock.TAG, "Unknown activity request code " + requestCode);
		}
	}

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	private void showToast(String message) {
		showToast(this, message);
	}

	private void showAbout() {
		Intent intent = new Intent(ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG);
		int activityRequest = ACTIVITY_REQUEST_OI_ABOUT_LAUNCH;

		try {
			PackageManager pm = getPackageManager();
			if (pm.queryIntentActivities(intent, 0).size() == 0) {
				String message = "Requires 'OI About' to show about dialog. Searching Android Market for it...";
				Log.i(VolumeDock.TAG, message);
				showToast(message);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:org.openintents.about"));
				activityRequest = ACTIVITY_REQUEST_OI_ABOUT_INSTALL;
			}

			startActivityForResult(intent, activityRequest);
		} catch (Exception e) {
			String message = "Failed to start activity for intent " + intent.toString();
			Log.e(VolumeDock.TAG, message, e);
			showToast(message);
		}
	}

}
