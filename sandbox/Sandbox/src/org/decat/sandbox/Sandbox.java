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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

public class Sandbox extends Activity {
	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";
	public static final String TAG = "Sandbox";
	private Toast toast;
	private GestureDetector gestureDetector;
	private TextView textview;

	void showToast(String message) {
		toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		textview = (TextView) findViewById(R.id.textview);

		gestureDetector = new GestureDetector(this, new MyGestureDetector(this));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onSearchRequested() {
		Log.i(TAG, "onSearchRequested triggered");

		return false;
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
		case R.id.listApplications:
			listApplications();
			break;
		case R.id.showContactDetails:
			showContactDetails();
			break;
		case R.id.about:
			try {
				Intent intent = new Intent(ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG);
				startActivityForResult(intent, 0);
			} catch (Exception e) {
				String message = "Failed to start activity for intent " + ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG;
				Log.e(Sandbox.TAG, message, e);
				showToast(message);
			}
			break;
		default:
			return false;
		}
		return true;
	}

	private void listApplications() {
		// List installed applications
		StringBuilder sb = new StringBuilder();
		PackageManager pm = this.getPackageManager();

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		List<ResolveInfo> resolvInfos = pm.queryIntentActivities(mainIntent, 0);
		Collections.sort(resolvInfos, new ResolveInfo.DisplayNameComparator(pm));

		for (ResolveInfo resolvInfo : resolvInfos) {
			String line = resolvInfo.activityInfo.applicationInfo.packageName + "/" + resolvInfo.activityInfo.name;
			sb.append(line);
			sb.append("\n");
			Log.i(TAG, line);
		}
	}

	private void showContactDetails() {
		String[] columns = {
				People.CONTENT_ITEM_TYPE, People.CONTENT_TYPE, People.DISPLAY_NAME, People.PRIMARY_EMAIL_ID, People.PRIMARY_ORGANIZATION_ID, People.PRIMARY_PHONE_ID, People.CUSTOM_RINGTONE,
				People.IM_ACCOUNT, People.LABEL, People.LAST_TIME_CONTACTED, People.NAME, People.NOTES, People.NUMBER, People.NUMBER_KEY, People.SEND_TO_VOICEMAIL, People.STARRED,
				People.TIMES_CONTACTED, People.TYPE,
		};

		String contactUri = "content://contacts/people/39";
		StringBuilder sb = new StringBuilder("Contact information for ");
		sb.append(contactUri);
		sb.append("\n");

		for (int i = 0; i < columns.length; i++) {
			String line = columns[i] + "=" + ContactHelper.getContactInformation(this, contactUri, columns[i]);
			sb.append(line);
			sb.append("\n");
			Log.i(TAG, line);
		}

		textview.setText(sb);
	}
}