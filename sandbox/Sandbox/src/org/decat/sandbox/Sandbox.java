package org.decat.sandbox;

/*
 **
 **       Copyright (C) 2010-2011 Patrick Decat
 ** 
 **       This file is part of Sandbox.
 **
 **   Sandbox is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   Sandbox is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with Sandbox.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
	private static final int REQUEST_CONTACT = 1;
	private static final int REQUEST_CONTACT_AND_NUMBER = 2;
	private static final int REQUEST_ACTIVITY = 3;
	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";
	public static final String TAG = "Sandbox";
	private GestureDetector gestureDetector;
	private TextView textview;

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	void showToast(String message) {
		showToast(this, message);
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
		case R.id.storeSms:
			storeSms();
			break;
		case R.id.listApplications:
			listApplications();
			break;
		case R.id.showContactDetails:
			showContactDetails();
			break;
		case R.id.selectContact:
			selectContact();
			break;
		case R.id.selectContactAndNumber:
			selectContactAndNumber();
			break;
		case R.id.selectActivity:
			selectActivity();
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

	private void storeSms() {
		// Store a test SMS
		StringBuilder sb = new StringBuilder();

		try {
			Uri uri = Uri.parse("content://sms/");
			ContentResolver cr = getContentResolver();
			ContentValues cv = new ContentValues();
			cv.put("thread_id", 0);
			cv.put("body", "Test body");
			// cv.put("subject", "Test subject");
			// cv.put("person", 0);
			cv.put("address", "0123456789");
			cv.put("status", -1);
			cv.put("read", "1");
			cv.put("service_center", "9876543210");
			cv.put("date", System.currentTimeMillis());

			sb.append("SMS to store:\n");
			sb.append(cv.toString());

			cr.insert(uri, cv);
			sb.append("\n\nResult: success");
		} catch (Exception e) {
			sb.append("\n\nResult: failure");
			Log.e(Sandbox.TAG, "Failed to store SMS", e);
		}

		Log.i(TAG, sb.toString());
		textview.setText(sb);
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

		textview.setText(sb);
	}

	private void showContactDetails() {
		showContactDetails("content://contacts/people/39");
	}

	private void showContactDetails(String contactUri) {
		String[] columns = {
				People.CONTENT_ITEM_TYPE, People.CONTENT_TYPE, People.DISPLAY_NAME, People.PRIMARY_EMAIL_ID, People.PRIMARY_ORGANIZATION_ID, People.PRIMARY_PHONE_ID, People.CUSTOM_RINGTONE,
				People.IM_ACCOUNT, People.LABEL, People.LAST_TIME_CONTACTED, People.NAME, People.NOTES, People.NUMBER, People.NUMBER_KEY, People.SEND_TO_VOICEMAIL, People.STARRED,
				People.TIMES_CONTACTED, People.TYPE,
		};

		StringBuilder sb = new StringBuilder("Contact information for ");
		sb.append("\n");
		sb.append(contactUri);
		sb.append("\n");

		for (int i = 0; i < columns.length; i++) {
			String column = columns[i];
			try {
				String line = column + "=" + ContactHelper.getContactInformation(this, contactUri, column);
				sb.append(line);
				sb.append("\n");
				Log.i(TAG, line);
			} catch (Exception e) {
				Log.e(TAG, "Failed to retrieve contact information " + column, e);
			}
		}

		textview.setText(sb);
	}

	private void selectContact() {
		Intent intent = new Intent(this, ContactSelector.class);
		startActivityForResult(intent, REQUEST_CONTACT);
	}

	private void selectContactAndNumber() {
		Intent intent = new Intent(this, ContactAndNumberSelector.class);
		startActivityForResult(intent, REQUEST_CONTACT_AND_NUMBER);
	}

	private void selectActivity() {
		Intent intent = new Intent(this, ActivitySelector.class);
		startActivityForResult(intent, REQUEST_ACTIVITY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		StringBuilder sb = new StringBuilder();

		switch (requestCode) {
		case REQUEST_CONTACT:
			Log.d(Sandbox.TAG, "Back from picking contact with resultCode=" + resultCode);
			if (resultCode == RESULT_OK) {
				sb.append("Result from picking contact:\n\tdataString=");
				sb.append(data.getDataString());
				sb.append("\n\tid=");
				sb.append(data.getLongExtra("id", -1));
				sb.append("\n\tvalue=");
				sb.append(data.getStringExtra("value"));
			}
			break;
		case REQUEST_CONTACT_AND_NUMBER:
			Log.d(Sandbox.TAG, "Back from picking contact and number with resultCode=" + resultCode);
			if (resultCode == RESULT_OK) {
				sb.append("Result from picking contact:\n\tdataString=");
				sb.append(data.getDataString());
				sb.append("\n\tid=");
				sb.append(data.getLongExtra("id", -1));
				sb.append("\n\tgroupValue=");
				sb.append(data.getStringExtra("group_value"));
				sb.append("\n\tchildValue=");
				sb.append(data.getStringExtra("child_value"));
			}
			break;
		case REQUEST_ACTIVITY:
			Log.d(Sandbox.TAG, "Back from picking activity with resultCode=" + resultCode);
			if (resultCode == RESULT_OK) {
				sb.append("Result from picking contact:\n\tdataString=");
				sb.append(data.getDataString());
				sb.append("\n\tid=");
				sb.append(data.getLongExtra("id", -1));
				sb.append("\n\tvalue=");
				sb.append(data.getStringExtra("value"));
			}
			break;
		default:
			Log.w(Sandbox.TAG, "Unknown activity request code " + requestCode);
		}

		textview.setText(sb);
	}
}