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

import org.decat.d2d.Preference.PreferenceType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PreferencesEditor extends Activity {
	private PreferencesHelper preferencesHolder;
	private SharedPreferences sharedPreferences;
	protected static final int ACTIVITY_REQUEST_CONTACT_PICK = 0;

	private View inputViews[];
	private String[] tempPreferencesValues;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = getSharedPreferences(dear2dear.class.getSimpleName(), Context.MODE_PRIVATE);
		preferencesHolder = new PreferencesHelper(sharedPreferences);

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		final TextView tv = new TextView(this);
		tv.setText("Select your messages and contacts");
		ll.addView(tv);

		Preference[] preferences = preferencesHolder.preferences;
		tempPreferencesValues = new String[preferences.length];
		inputViews = new View[preferences.length];
		for (int i = 0; i < preferences.length; i++) {
			Preference preference = preferences[i];
			PreferenceType preferenceType = preference.type;
			tempPreferencesValues[i] = preferencesHolder.getString(preference);
			View view = null;
			switch (preferenceType) {
			case TYPE_CONTACT:
				// TODO: Add label
				Button btn = new Button(this);
				view = btn;
				String btnLabel = tempPreferencesValues[i] == null ? "Select " + preference.label : ContactHelper.getContactNameFromUriString(this, tempPreferencesValues[i]);
				btn.setText(btnLabel);
				final int btnId = i;
				btn.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View v) {
						Intent intent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);
						startActivityForResult(intent, ACTIVITY_REQUEST_CONTACT_PICK + btnId);
					}
				});
				ll.addView(btn);

				Log.d(dear2dear.TAG, "Loaded contact preference " + preference.key);
				break;
			case TYPE_STRING:
				// TODO: Add label
				EditText editText = new EditText(this);
				view = editText;
				editText.setText((CharSequence) tempPreferencesValues[i]);
				ll.addView(editText);
				Log.d(dear2dear.TAG, "Loaded string preference " + preference.key);
				break;
			default:
				Log.w(dear2dear.TAG, "Unknow preference type " + preference.key);
			}
			inputViews[i] = view;
			preference.view = view;
		}

		setContentView(ll);
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences.Editor ed = sharedPreferences.edit();
		Preference[] preferences = preferencesHolder.preferences;

		for (int i = 0; i < preferences.length; i++) {
			Preference preference = preferences[i];
			PreferenceType preferenceType = preference.type;
			switch (preferenceType) {
			case TYPE_CONTACT:
				if (tempPreferencesValues[i] != null) {
					Log.d(dear2dear.TAG, "Stored contact preference " + preference.key);
					ed.putString(preference.key, tempPreferencesValues[i]);
				}
				break;
			case TYPE_STRING:
				Log.d(dear2dear.TAG, "Stored string preference " + preference.key);
				ed.putString(preference.key, ((EditText) preference.view).getText().toString());
				break;
			default:
				Log.w(dear2dear.TAG, "Unknow preference type " + preference.key);
			}
		}
		ed.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			int btnId = requestCode - ACTIVITY_REQUEST_CONTACT_PICK;
			if (-1 < btnId && btnId < inputViews.length) {
				Log.d(dear2dear.TAG, "Back from picking contact for view id " + btnId);
				tempPreferencesValues[btnId] = data.getDataString();
				((Button) inputViews[btnId]).setText(ContactHelper.getContactNameFromUriString(this, tempPreferencesValues[btnId]));
			}
		} else {
			Log.w(dear2dear.TAG, "Error on activity result=" + resultCode + ", requestCode=" + requestCode);
		}
	}
}