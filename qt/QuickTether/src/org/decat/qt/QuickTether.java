package org.decat.qt;

/*
 **
 **       Copyright (C) 2010-2011 Patrick Decat
 ** 
 **       This file is part of QuickTether.
 **
 **   QuickTether is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   QuickTether is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with QuickTether.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class QuickTether extends Activity {
	private static final int REQUEST_TOGGLE_TETHERING = 1;
	private static final String TAG = "QT";

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");

		startActivityForResult(intent, REQUEST_TOGGLE_TETHERING);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String message;
		switch (requestCode) {
		case REQUEST_TOGGLE_TETHERING:
			Log.d(TAG, "Back from toggling tethering with resultCode=" + resultCode);
			if (resultCode == RESULT_OK) {
				message = "Tethering toggling done.";
			} else {
				message = "Failed to toggle tethering.";
			}
			break;
		default:
			message = "Unknown activity request code " + requestCode;
		}
		Log.i(TAG, message);

		finish();
	}
}
