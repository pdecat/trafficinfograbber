package org.decat.d2d;

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
	private static final String NOT_DEFINED = "NOT_DEFINED";

	public static final String TAG = "D2D";

	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	private TextView tv;
	private Button btn1;
	private Button btn2;
	private Button btn3;

	private Toast toast;

	private SharedPreferences preferences;

	protected String firstStepChoice;
	protected String secondStepChoice;
	protected String thirdStepChoice;

	private void showToast(String message) {
		toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = getPreferences(Context.MODE_PRIVATE);

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		tv = new TextView(this);
		tv.setTextSize(40);
		ll.addView(tv);

		btn1 = new Button(this);
		ll.addView(btn1);
		btn2 = new Button(this);
		ll.addView(btn2);
		btn3 = new Button(this);
		ll.addView(btn3);

		setContentView(ll);

		showNotificationShortcut();
	}

	private void showNotificationShortcut() {
		NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, getString(R.string.notificationLabel), System.currentTimeMillis());
		Intent intent = new Intent(this, dear2dear.class);
		notification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.notificationLabel), PendingIntent.getActivity(this.getBaseContext(), 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT));
		notificationManager.notify(0, notification);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (NOT_DEFINED.equals(preferences.getString(PreferencesEditor.STR_ACTION_1, NOT_DEFINED))) {
			showPreferencesEditor();
		} else {
			startFromScratch();
		}
	}

	private void startFromScratch() {
		tv.setText(getString(R.string.send) + " " + getString(R.string.q_what));
		firstStepOption(btn1, preferences.getString(PreferencesEditor.STR_ACTION_1, ""));
		firstStepOption(btn2, preferences.getString(PreferencesEditor.STR_ACTION_2, ""));
		firstStepOption(btn3, preferences.getString(PreferencesEditor.STR_ACTION_3, ""));
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
		}
	}

	private void firstStepOption(final Button btn, final String option) {
		btn.setText(option);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				firstStepChoice = option;
				tv.setText(getString(R.string.send) + " \"" + firstStepChoice + "\" " + getString(R.string.q_to_whom));
				secondStepOption(btn1, preferences.getString(PreferencesEditor.STR_DESTINATION_1, NOT_DEFINED));
				secondStepOption(btn2, preferences.getString(PreferencesEditor.STR_DESTINATION_2, NOT_DEFINED));
				secondStepOption(btn3, preferences.getString(PreferencesEditor.STR_DESTINATION_3, NOT_DEFINED));
			}
		});
	}

	private void secondStepOption(final Button btn, final String option) {
		btn.setText(option);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				secondStepChoice = option;
				tv.setText(getString(R.string.send) + " \"" + firstStepChoice + "\"" + " " + getString(R.string.to) + " \"" + secondStepChoice + "\" " + getString(R.string.q_how));
				thirdStepOption(btn1, getString(R.string.sms));
				thirdStepOption(btn2, getString(R.string.email));
				btn3.setVisibility(View.INVISIBLE);
			}
		});
	}

	private void thirdStepOption(final Button btn, final String option) {
		btn.setText(option);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				thirdStepChoice = option;
				tv.setText(getString(R.string.send) + " \"" + firstStepChoice + "\"" + " " + getString(R.string.to) + " \"" + secondStepChoice + "\" " + getString(R.string.by) + " \""
						+ thirdStepChoice + "\"");
				fourthStepOption(btn1, getString(R.string.send));
				btn2.setVisibility(View.INVISIBLE);
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

				showToast(getString(R.string.sending) + " " + thirdStepChoice + " " + getString(R.string.to) + " " + secondStepChoice);
				if (getString(R.string.sms).equals(thirdStepChoice)) {
					SmsManager sm = SmsManager.getDefault();

					sm.sendTextMessage(getDestinationAddress(secondStepChoice), null, firstStepChoice, null, null);
				} else if (getString(R.string.email).equals(thirdStepChoice)) {
					showToast("TODO: Implement email");
					// TODO: handle email
				}
			}

			private String getDestinationAddress(String secondStepChoice) {
				String destinationAddress = null;
				if (preferences.getString(PreferencesEditor.STR_DESTINATION_1, NOT_DEFINED).equals(secondStepChoice)) {
					destinationAddress = preferences.getString(PreferencesEditor.STR_DESTINATION_SMS_1, NOT_DEFINED);
				} else if (preferences.getString(PreferencesEditor.STR_DESTINATION_2, NOT_DEFINED).equals(secondStepChoice)) {
					destinationAddress = preferences.getString(PreferencesEditor.STR_DESTINATION_SMS_2, NOT_DEFINED);
				} else if (preferences.getString(PreferencesEditor.STR_DESTINATION_3, NOT_DEFINED).equals(secondStepChoice)) {
					destinationAddress = preferences.getString(PreferencesEditor.STR_DESTINATION_SMS_3, NOT_DEFINED);
				}
				return destinationAddress;
			}
		});
	}
}