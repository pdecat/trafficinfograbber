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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
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
	private static final int ACTIVITY_REQUEST_PREFERENCES_EDITOR = 3;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	private TextView tv;

	private Button buttons[];

	private SharedPreferences sharedPreferences;
	private PreferencesHelper preferencesHelper;

	protected String destinationStepChoiceValue;
	protected String destinationStepChoiceLabel;

	protected String messageStepChoice;

	protected String mediaStepChoice;

	private String destinationChoiceDetails;

	private Button restartButton;

	private static boolean notificationShortcut = false;

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	private void showToast(String message) {
		showToast(this, message);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = getPreferences(Context.MODE_PRIVATE);
		preferencesHelper = new PreferencesHelper(sharedPreferences);

		// Check cached names against contacts IDs stored in preferences (issue
		// #11)
		checkCachedNamesAgainstContactIds();

		// Create layout
		LinearLayout ll = new LinearLayout(this);
		ll.setGravity(Gravity.FILL_VERTICAL);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		tv = new TextView(this);
		tv.setTextSize(40);
		ll.addView(tv);

		buttons = new Button[3];
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = new Button(this);
			ll.addView(buttons[i]);
		}

		// Add a restart button
		restartButton = new Button(this);
		restartButton.setText(getString(R.string.restartText));
		restartButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Log.d(dear2dear.TAG, "Restart from scratch");
				startFromScratch();
			}
		});
		ll.addView(restartButton);

		setContentView(ll);
	}

	/**
	 * Check cached names against contacts IDs stored in preferences (issue #11)
	 */
	private void checkCachedNamesAgainstContactIds() {
		boolean cacheValid = true;
		StringBuilder sb = new StringBuilder();

		List<Preference> contacts = preferencesHelper.getPreferencesByGroup(PreferenceGroup.GROUP_CONTACTS);
		for (int i = 0; i < contacts.size(); i++) {
			Preference contact = contacts.get(i);
			String key = contact.key;
			String contactUri = sharedPreferences.getString(key, null);
			String cachedName = sharedPreferences.getString(key + PreferencesHelper.VALUE_SUFFIX, null);
			String currentName = getNameFromUri(contactUri);
			sb.append("\nContact information for ");
			sb.append(key);
			if (cachedName == null || currentName == null || !currentName.equals(cachedName)) {
				cacheValid = false;
				sb.append(" is invalid! (contactUri=");
				showToast(getString(R.string.contactInformationInvalidText, key, contactUri, cachedName, currentName));
			} else {
				sb.append(" is correct (contactUri=");
			}
			sb.append(contactUri);
			sb.append(", cachedName=");
			sb.append(cachedName);
			sb.append(", currentName=");
			sb.append(currentName);
			sb.append(")\n");
		}

		if (!cacheValid) {
			// TODO: launch preferences editor and emphasize invalid contacts
			Log.w(dear2dear.TAG, sb.toString());
		} else {
			Log.i(dear2dear.TAG, sb.toString());
		}
	}

	public static void updateNotificationShortcut(Context context) {
		// Get shared preferences
		SharedPreferences sharedPreferences = context.getSharedPreferences(dear2dear.class.getSimpleName(), Context.MODE_PRIVATE);

		// Get current value
		boolean value = sharedPreferences.getBoolean(PreferencesHelper.NOTIFICATION_SHORTCUT, true);

		if (value != notificationShortcut) {
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (value) {
				Notification notification = new Notification(R.drawable.icon, context.getString(R.string.notificationMessage), System.currentTimeMillis());
				Intent intent = new Intent(context, dear2dear.class);
				notification.setLatestEventInfo(context, context.getString(R.string.app_name) + " " + context.getString(R.string.app_version), context.getString(R.string.notificationLabel),
						PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
				notification.flags |= Notification.FLAG_ONGOING_EVENT;
				notification.flags |= Notification.FLAG_NO_CLEAR;
				notificationManager.notify(0, notification);
			} else {
				notificationManager.cancel(0);
			}
		}

		// Store new value
		notificationShortcut = value;
	}

	@Override
	public void onResume() {
		super.onResume();

		// Update notification shortcut state
		updateNotificationShortcut(this);

		if (sharedPreferences.getString(preferencesHelper.preferences[0].key, null) == null) {
			String message = getString(R.string.pleaseProceedWithConfigurationFirstText);
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
		Intent intent = new Intent(this, PreferencesEditor.class);
		startActivityForResult(intent, ACTIVITY_REQUEST_PREFERENCES_EDITOR);
	}

	private void showAbout() {
		Intent intent = new Intent(ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG);
		int activityRequest = ACTIVITY_REQUEST_OI_ABOUT_LAUNCH;

		try {
			PackageManager pm = getPackageManager();
			if (pm.queryIntentActivities(intent, 0).size() == 0) {
				String message = getString(R.string.requiresOIAboutAndSearchingMarketText);
				Log.i(dear2dear.TAG, message);
				showToast(message);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:org.openintents.about"));
				activityRequest = ACTIVITY_REQUEST_OI_ABOUT_INSTALL;
			}

			startActivityForResult(intent, activityRequest);
		} catch (Exception e) {
			String message = getString(R.string.failedToStartActivityText, intent.toString());
			Log.e(dear2dear.TAG, message, e);
			showToast(message);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_REQUEST_OI_ABOUT_LAUNCH:
			Log.d(dear2dear.TAG, "Back from OI About");
			break;
		case ACTIVITY_REQUEST_OI_ABOUT_INSTALL:
			if (resultCode == RESULT_CANCELED) {
				Log.d(dear2dear.TAG, "Back from Android Market");
				showAbout();
			}
			break;
		case ACTIVITY_REQUEST_PREFERENCES_EDITOR:
			Log.d(dear2dear.TAG, "Back from preferences editor");
			break;

		default:
			Log.w(dear2dear.TAG, "Unknown activity request code " + requestCode);
		}
	}

	private void startFromScratch() {
		// Hide restart button
		restartButton.setVisibility(View.INVISIBLE);

		String message = getString(R.string.sendToWhomText);
		Log.d(dear2dear.TAG, message);
		tv.setText(message);

		List<Preference> contacts = preferencesHelper.getPreferencesByGroup(PreferenceGroup.GROUP_CONTACTS);
		for (int i = 0; i < buttons.length; i++) {
			Preference contact = contacts.get(i);
			String key = contact.key;
			String optionValue = sharedPreferences.getString(key, null);
			String optionLabel = sharedPreferences.getString(key + PreferencesHelper.VALUE_SUFFIX, null);
			destinationStepOption(buttons[i], optionLabel, optionValue);
			Log.d(dear2dear.TAG, "Added destination step option for " + key);
		}
	}

	private void destinationStepOption(final Button btn, final String optionLabel, final String optionValue) {
		if (optionLabel == null || optionValue == null) {
			btn.setVisibility(View.INVISIBLE);
		} else {
			btn.setVisibility(View.VISIBLE);
			btn.setText(optionLabel);
			btn.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					// Show restart button
					restartButton.setVisibility(View.VISIBLE);

					destinationStepChoiceLabel = optionLabel;
					destinationStepChoiceValue = optionValue;

					String message = getString(R.string.sendContactWhatText, destinationStepChoiceLabel);
					Log.d(dear2dear.TAG, message);
					tv.setText(message);

					List<Preference> messages = preferencesHelper.getPreferencesByGroup(PreferenceGroup.GROUP_MESSAGES);
					for (int i = 0; i < buttons.length; i++) {
						Preference preference = messages.get(i);
						String key = preference.key;
						String optionValue = sharedPreferences.getString(key, null);
						String optionLabel = optionValue;
						messageStepOption(buttons[i], optionLabel, optionValue);
						Log.d(dear2dear.TAG, "Added message step option for " + key);
					}
				}
			});
		}
	}

	private void messageStepOption(final Button btn, final String optionLabel, final String optionValue) {
		if (optionLabel == null || optionValue == null) {
			btn.setVisibility(View.INVISIBLE);
		} else {
			btn.setVisibility(View.VISIBLE);
			btn.setText(optionLabel);
			btn.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					messageStepChoice = optionValue;
					String message = getString(R.string.sendMessageToContactHowText, messageStepChoice, destinationStepChoiceLabel);
					Log.d(dear2dear.TAG, message);
					tv.setText(message);
					mediaStepOption(buttons[0], getString(R.string.sms));
					mediaStepOption(buttons[1], getString(R.string.email));
					Log.d(dear2dear.TAG, "Added media steps options");
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
				destinationChoiceDetails = null;
				if (getString(R.string.sms).equals(mediaStepChoice)) {
					destinationChoiceDetails = getPhoneNumberFromUri(destinationStepChoiceValue);
				} else if (getString(R.string.email).equals(mediaStepChoice)) {
					showToast("TODO: Implement email");
					destinationChoiceDetails = "test@example.com";
					// TODO: handle email
					Log.d(dear2dear.TAG, "TODO: Implement email");
				}
				String message = getString(R.string.sendMessageToContactByMediaText, messageStepChoice, destinationStepChoiceLabel, mediaStepChoice, destinationChoiceDetails);
				Log.d(dear2dear.TAG, message.toString());
				tv.setText(message);
				fourthStepOption(buttons[0], getString(R.string.sendText));
				Log.d(dear2dear.TAG, "Added send step option");
				buttons[1].setVisibility(View.INVISIBLE);
			}
		});
	}

	private void fourthStepOption(final Button btn, String option) {
		btn.setText(option);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				buttons[0].setVisibility(View.INVISIBLE);

				String message = getString(R.string.sendingMessageToContactText, messageStepChoice, destinationStepChoiceLabel, mediaStepChoice, destinationChoiceDetails);
				showToast(message);
				if (getString(R.string.sms).equals(mediaStepChoice)) {
					sendSms();
				} else if (getString(R.string.email).equals(mediaStepChoice)) {
					showToast("TODO: Implement email");
					// TODO: handle email
					Log.d(dear2dear.TAG, "TODO: Implement email");
				}
			}

		});
	}

	private String getPhoneNumberFromUri(String contactUri) {
		String number = null;
		Cursor cursor = managedQuery(Uri.parse(contactUri), null, null, null, null);
		int count = cursor.getCount();
		Log.d(dear2dear.TAG, "count=" + count);
		if (count < 1) {
			Log.e(dear2dear.TAG, "No match for" + contactUri);
		} else if (count > 1) {
			Log.e(dear2dear.TAG, "Too many matches for" + contactUri);
		} else {
			int idColumnIndex = cursor.getColumnIndexOrThrow(People._ID);
			Log.d(dear2dear.TAG, "idColumnIndex=" + idColumnIndex);
			int nameColumnIndex = cursor.getColumnIndexOrThrow(People.NAME);
			Log.d(dear2dear.TAG, "nameColumnIndex=" + nameColumnIndex);

			// Go to the first match
			cursor.moveToFirst();

			long id = cursor.getLong(idColumnIndex);
			Log.d(dear2dear.TAG, "id=" + id);
			String name = cursor.getString(nameColumnIndex);
			Log.d(dear2dear.TAG, "Found contact " + name);

			// Return a cursor that points to this contact's phone
			// numbers
			Uri.Builder builder = People.CONTENT_URI.buildUpon();
			ContentUris.appendId(builder, id);
			builder.appendEncodedPath(People.Phones.CONTENT_DIRECTORY);
			Uri phoneNumbersUri = builder.build();

			Cursor phonesCursor = managedQuery(phoneNumbersUri, new String[] {
					People.Phones._ID,
					People.Phones.NUMBER,
					People.Phones.TYPE
			}, People.Phones.TYPE + "=" + People.Phones.TYPE_MOBILE, null, null);
			int phonesCount = phonesCursor.getCount();
			Log.d(dear2dear.TAG, "phonesCount=" + phonesCount);
			phonesCursor.moveToFirst();
			int phoneColumnIndex = phonesCursor.getColumnIndexOrThrow(People.Phones.NUMBER);
			Log.d(dear2dear.TAG, "phoneColumnIndex=" + phoneColumnIndex);
			number = phonesCursor.getString(phoneColumnIndex);
			Log.d(dear2dear.TAG, "Found number " + number);
		}

		return number;
	}

	private String getNameFromUri(String contactUri) {
		String name = null;
		if (contactUri != null) {
			Cursor cursor = managedQuery(Uri.parse(contactUri), null, null, null, null);
			int count = cursor.getCount();
			Log.d(dear2dear.TAG, "count=" + count);
			if (count < 1) {
				Log.e(dear2dear.TAG, "No match for" + contactUri);
			} else if (count > 1) {
				Log.e(dear2dear.TAG, "Too many matches for" + contactUri);
			} else {
				int idColumnIndex = cursor.getColumnIndexOrThrow(People._ID);
				Log.d(dear2dear.TAG, "idColumnIndex=" + idColumnIndex);
				int nameColumnIndex = cursor.getColumnIndexOrThrow(People.NAME);
				Log.d(dear2dear.TAG, "nameColumnIndex=" + nameColumnIndex);

				// Go to the first match
				cursor.moveToFirst();

				long id = cursor.getLong(idColumnIndex);
				Log.d(dear2dear.TAG, "id=" + id);
				name = cursor.getString(nameColumnIndex);
				Log.d(dear2dear.TAG, "Found contact " + name);
			}
		}

		return name;
	}

	private void sendSms() {
		if (destinationChoiceDetails != null) {
			SmsManager.getDefault().sendTextMessage(destinationChoiceDetails, null, messageStepChoice, null, null);

			// Store the SMS into the standard Google SMS app
			StringBuilder sb = new StringBuilder();

			try {
				Uri uri = Uri.parse("content://sms/");
				ContentResolver cr = getContentResolver();
				ContentValues cv = new ContentValues();
				cv.put("thread_id", 0);
				cv.put("body", messageStepChoice);
				cv.put("address", destinationChoiceDetails);
				cv.put("status", -1);
				cv.put("read", "1");
				cv.put("date", System.currentTimeMillis());

				sb.append("SMS to store:\n");
				sb.append(cv.toString());

				cr.insert(uri, cv);
				sb.append("\n\nResult: success");
			} catch (Exception e) {
				sb.append("\n\nResult: failure");
				Log.e(TAG, "Failed to store SMS", e);
			}

			Log.i(TAG, sb.toString());

		} else {
			showToast("Could not find a phone number for " + destinationStepChoiceLabel);
		}
	}
}