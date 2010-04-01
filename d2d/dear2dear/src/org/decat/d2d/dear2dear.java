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

package org.decat.d2d;

import java.util.List;

import org.decat.d2d.Preference.PreferenceGroup;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class dear2dear extends Activity {
	public static final String TAG = "D2D";

	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	private TextView tv;

	private Button buttons[];

	private Toast toast;

	private SharedPreferences sharedPreferences;
	private PreferencesHelper preferencesHolder;

	protected String messageStepChoice;
	protected String destinationStepChoice;
	protected String mediaStepChoice;

	private void showToast(String message) {
		toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = getPreferences(Context.MODE_PRIVATE);
		preferencesHolder = new PreferencesHelper(sharedPreferences);

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		tv = new TextView(this);
		tv.setTextSize(40);
		ll.addView(tv);

		buttons = new Button[3];
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = new Button(this);
			ll.addView(buttons[i]);
		}

		setContentView(ll);

		showNotificationShortcut();
	}

	private void showNotificationShortcut() {
		NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, getString(R.string.notificationMessage), System.currentTimeMillis());
		Intent intent = new Intent(this, dear2dear.class);
		notification.setLatestEventInfo(this, getString(R.string.app_name) + " " + getString(R.string.app_version), getString(R.string.notificationLabel), PendingIntent.getActivity(this
				.getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notificationManager.notify(0, notification);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (preferencesHolder.getString(preferencesHolder.preferences[0]) == null) {
			String message = "Please proceed with configuration first...";
			Log.i(dear2dear.TAG, message);
			showToast(message);
			showPreferencesEditor();
		} else {
			startFromScratch();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.preferences, 0, R.string.preferences);
		menu.add(Menu.NONE, R.id.about, 1, R.string.about);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			showPreferencesEditor();
			return true;
		case R.id.about:
			showAbout();
			return true;
		}
		return false;
	}

	private void showPreferencesEditor() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_PREFERENCE);
		intent.setComponent(new ComponentName(this.getPackageName(), PreferencesEditor.class.getName()));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void showAbout() {
		Intent intent = new Intent(ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG);
		int activityRequest = ACTIVITY_REQUEST_OI_ABOUT_LAUNCH;

		try {
			PackageManager pm = getPackageManager();
			if (pm.queryIntentActivities(intent, 0).size() == 0) {
				String message = "Requires 'OI About' to show about dialog. Searching Android Market for it...";
				Log.i(dear2dear.TAG, message);
				showToast(message);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:org.openintents.about"));
				activityRequest = ACTIVITY_REQUEST_OI_ABOUT_INSTALL;
			}

			startActivityForResult(intent, activityRequest);
		} catch (Exception e) {
			String message = "Failed to start activity for intent " + intent.toString();
			Log.e(dear2dear.TAG, message, e);
			showToast(message);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_REQUEST_OI_ABOUT_LAUNCH:
			if (resultCode == RESULT_OK) {
				Log.d(dear2dear.TAG, "Back from OI About");
			}
			break;
		case ACTIVITY_REQUEST_OI_ABOUT_INSTALL:
			if (resultCode == RESULT_CANCELED) {
				Log.d(dear2dear.TAG, "Back from Android Market");
				showAbout();
			}
			break;
		default:
			Log.w(dear2dear.TAG, "Unknow activity request code " + requestCode);
		}
	}

	private void startFromScratch() {
		tv.setText(getString(R.string.send) + " " + getString(R.string.q_to_whom));
		List<Preference> contacts = preferencesHolder.getPreferencesByGroup(PreferenceGroup.GROUP_CONTACTS);
		for (int i = 0; i < 3; i++) {
			Preference contact = contacts.get(i);
			String optionValue = preferencesHolder.getString(contact);
			String optionLabel = ContactHelper.getContactNameFromUriString(this, optionValue);
			destinationStepOption(buttons[i], optionLabel, optionValue);
		}
	}

	private void destinationStepOption(final Button btn, final String optionLabel, final String optionValue) {
		if (optionValue == null) {
			btn.setVisibility(View.INVISIBLE);
		} else {
			btn.setVisibility(View.VISIBLE);
			btn.setText(optionLabel);
			btn.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					destinationStepChoice = optionValue;
					tv.setText(getString(R.string.send) + " \"" + destinationStepChoice + "\" " + getString(R.string.q_what));

					List<Preference> messages = preferencesHolder.getPreferencesByGroup(PreferenceGroup.GROUP_MESSAGES);
					for (int i = 0; i < 3; i++) {
						Preference message = messages.get(i);
						String optionValue = preferencesHolder.getString(message);
						String optionLabel = optionValue;
						messageStepOption(buttons[i], optionLabel, optionValue);
					}
				}
			});
		}
	}

	private void messageStepOption(final Button btn, final String optionLabel, final String optionValue) {
		if (optionValue == null) {
			btn.setVisibility(View.INVISIBLE);
		} else {
			btn.setVisibility(View.VISIBLE);
			btn.setText(optionLabel);
			btn.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					messageStepChoice = optionValue;
					tv.setText(getString(R.string.send) + " \"" + messageStepChoice + "\"" + " " + getString(R.string.to) + " \"" + destinationStepChoice + "\" " + getString(R.string.q_how));
					mediaStepOption(buttons[0], getString(R.string.sms));
					mediaStepOption(buttons[1], getString(R.string.email));
					buttons[2].setVisibility(View.INVISIBLE);
				}
			});
		}
	}

	private void mediaStepOption(final Button btn, final String option) {
		btn.setText(option);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				mediaStepChoice = option;
				tv.setText(getString(R.string.send) + " \"" + messageStepChoice + "\"" + " " + getString(R.string.to) + " \"" + destinationStepChoice + "\" " + getString(R.string.by) + " \""
						+ mediaStepChoice + "\"");
				fourthStepOption(buttons[0], getString(R.string.send));
				buttons[1].setVisibility(View.INVISIBLE);
			}
		});
	}

	private void fourthStepOption(final Button btn, String option) {
		btn.setText(option);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				btn.setText(getString(R.string.restart));
				btn.setVisibility(View.VISIBLE);
				btn.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View v) {
						startFromScratch();
					}
				});

				showToast(getString(R.string.sending) + " " + mediaStepChoice + " " + getString(R.string.to) + " " + destinationStepChoice);
				if (getString(R.string.sms).equals(mediaStepChoice)) {
					SmsManager sm = SmsManager.getDefault();

					sm.sendTextMessage(ContactHelper.getContactPhoneNumberFromUriString(dear2dear.this, destinationStepChoice), null, messageStepChoice, null, null);
				} else if (getString(R.string.email).equals(mediaStepChoice)) {
					showToast("TODO: Implement email");
					// TODO: handle email
				}
			}
		});
	}

}