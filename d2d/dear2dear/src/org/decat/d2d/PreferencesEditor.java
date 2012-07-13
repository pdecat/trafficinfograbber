/**
 * Copyright (C) 2010-2012 Patrick Decat
 *
 * dear2dear is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dear2dear is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dear2dear.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.d2d;

import java.util.HashMap;
import java.util.Map;

import org.decat.d2d.Preference.PreferenceType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PreferencesEditor extends Activity {
	public static final String EXTRA_ID = "id";
	public static final String EXTRA_KEY = "key";
	public static final String EXTRA_VALUE = "value";

	protected static final int ACTIVITY_REQUEST_CONTACT_PICK = 0;

	private PreferencesHelper preferencesHelper;
	private SharedPreferences sharedPreferences;

	private Map<String, View> inputViews;
	private HashMap<String, Object> preferencesValues;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = getSharedPreferences(dear2dear.class.getSimpleName(), Context.MODE_PRIVATE);
		preferencesHelper = new PreferencesHelper(sharedPreferences);

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		final TextView tv = new TextView(this);
		tv.setText(getString(R.string.selectYourMessagesAndContacts));
		ll.addView(tv);

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			String sd = pi.applicationInfo.sourceDir;
			if (sd == null || sd.indexOf("/data/app") != 0) {
				dear2dear.showToast(this, getString(R.string.notificationOnBootNeedsInternalMemoryInstallLocation));
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Preference[] preferences = preferencesHelper.preferences;
		inputViews = new HashMap<String, View>();
		preferencesValues = new HashMap<String, Object>();
		for (int i = 0; i < preferences.length; i++) {
			Preference preference = preferences[i];
			PreferenceType preferenceType = preference.type;
			final String key = preference.key;
			Log.d(dear2dear.TAG, "Loading preference " + key);

			Object value = null;
			View view = null;
			switch (preferenceType) {
			case TYPE_CONTACT_VALUE:
				value = sharedPreferences.getString(key, null);
				// Do not create any view, handled with next case
				Log.d(dear2dear.TAG, "No view created for contact value preference " + key);
				break;

			case TYPE_CONTACT:
				// TODO: Add label
				Button btn = new Button(this);
				view = btn;
				value = sharedPreferences.getString(key, null);
				String btnLabel = (String) (value == null ? getString(R.string.selectContactText, getString(preference.labelResourceId)) : preferencesValues.get(key + PreferencesHelper.VALUE_SUFFIX));
				btn.setText(btnLabel);
				btn.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View v) {
						selectContact(key);
					}
				});
				ll.addView(btn);

				Log.d(dear2dear.TAG, "Created button view for contact preference " + key);
				break;

			case TYPE_STRING:
				// TODO: Add label
				EditText editText = new EditText(this);
				view = editText;
				value = sharedPreferences.getString(key, null);
				editText.setText((CharSequence) value);
				ll.addView(editText);
				Log.d(dear2dear.TAG, "Created edittext view for preference " + key);
				break;

			case TYPE_BOOLEAN:
				CheckBox checkBox = new CheckBox(this);
				view = checkBox;
				checkBox.setText((CharSequence) getString(preference.labelResourceId));
				value = sharedPreferences.getBoolean(key, true);
				checkBox.setChecked((Boolean) value);
				ll.addView(checkBox);
				Log.d(dear2dear.TAG, "Created checkbox view for preference " + key);
				break;

			default:
				Log.w(dear2dear.TAG, "Unknown preference type " + key);
			}

			preferencesValues.put(key, value);
			preference.view = view;
			inputViews.put(key, view);
		}

		setContentView(ll);
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences.Editor ed = sharedPreferences.edit();
		Preference[] preferences = preferencesHelper.preferences;

		for (int i = 0; i < preferences.length; i++) {
			Preference preference = preferences[i];
			PreferenceType preferenceType = preference.type;
			String key = preference.key;

			switch (preferenceType) {
			case TYPE_CONTACT_VALUE:
			case TYPE_CONTACT:
				Object value = preferencesValues.get(key);
				if (value != null) {
					Log.d(dear2dear.TAG, "Stored contact preference " + key);
					ed.putString(key, (String) value);
				}
				break;

			case TYPE_STRING:
				Log.d(dear2dear.TAG, "Stored string preference " + key);
				Editable text = ((EditText) preference.view).getText();
				ed.putString(key, text.toString());
				break;

			case TYPE_BOOLEAN:
				Log.d(dear2dear.TAG, "Stored boolean preference " + key);
				ed.putBoolean(key, ((CheckBox) preference.view).isChecked());
				break;

			default:
				Log.w(dear2dear.TAG, "Unknown preference type " + key);
			}
		}
		ed.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		StringBuilder sb = new StringBuilder();

		switch (requestCode) {
		case ACTIVITY_REQUEST_CONTACT_PICK:
			sb.append("Back from picking contact with resultCode=");
			sb.append(resultCode);
			if (resultCode == RESULT_OK) {
				String dataString = data.getDataString();
				String key = data.getStringExtra(EXTRA_KEY);
				String value = data.getStringExtra(EXTRA_VALUE);

				sb.append(", dataString=");
				sb.append(dataString);
				sb.append(", id=");
				sb.append(data.getLongExtra(EXTRA_ID, -1));
				sb.append(", key=");
				sb.append(key);
				sb.append(", value=");
				sb.append(value);

				// Store returned contact information in preference values
				preferencesValues.put(key, dataString);
				preferencesValues.put(key + PreferencesHelper.VALUE_SUFFIX, value);
				((Button) inputViews.get(key)).setText(value);
			}
			Log.d(dear2dear.TAG, sb.toString());
			break;
		default:
			Log.w(dear2dear.TAG, "Unknown activity request code " + requestCode);
		}
	}

	private void selectContact(String key) {
		Intent intent = new Intent(this, ContactSelector.class);
		intent.putExtra(EXTRA_KEY, key);
		startActivityForResult(intent, ACTIVITY_REQUEST_CONTACT_PICK);
	}
}