package org.decat.sandbox;

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

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.Toast;

public class Sandbox extends Activity {
	public static final String TAG = "Sandbox";

	class MyGestureDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 60;
		private static final int SWIPE_THRESHOLD_VELOCITY = 100;
		private Toast toast;
		private Context context;

		public MyGestureDetector() {
			this.context = Sandbox.this;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY || Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
					float deltaX = e1.getX() - e2.getX();
					float deltaY = e1.getY() - e2.getY();

					// Détection de l'axe le plus important du mouvement entre
					// horizontal et vertical
					if (Math.abs(deltaX) > Math.abs(deltaY)) {
						if (-deltaX > SWIPE_MIN_DISTANCE) {
							showToast("Swipe horizontal gauche -> droite.");
						} else if (deltaX > SWIPE_MIN_DISTANCE) {
							showToast("Swipe horizontal droite -> gauche.");
						}
					} else {
						if (deltaY > SWIPE_MIN_DISTANCE) {
							showToast("Swipe vertical bas -> haut.");
						} else if (-deltaY > SWIPE_MIN_DISTANCE) {
							showToast("Swipe vertical haut -> bas.");
						}
					}
				}
			} catch (Exception e) {
				Log.e(Sandbox.TAG, "Failed to handle swipe", e);
			}
			return false;
		}

		private void showToast(String message) {
			toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
			toast.show();
		}
	}

	private GestureDetector gestureDetector;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		gestureDetector = new GestureDetector(this, new MyGestureDetector());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			Intent intent = new Intent("org.openintents.action.SHOW_ABOUT_DIALOG");
			startActivityForResult(intent, 0);
			return true;
		}
		return false;
	}

	@Override
	public boolean onSearchRequested() {
		// List installed applications
		PackageManager pm = this.getPackageManager();

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		List<ResolveInfo> resolvInfos = pm.queryIntentActivities(mainIntent, 0);
		Collections.sort(resolvInfos, new ResolveInfo.DisplayNameComparator(pm));

		for (ResolveInfo resolvInfo : resolvInfos) {
			Log.i(TAG, resolvInfo.activityInfo.applicationInfo.packageName + "/" + resolvInfo.activityInfo.name);
		}

		return false;
	}

}