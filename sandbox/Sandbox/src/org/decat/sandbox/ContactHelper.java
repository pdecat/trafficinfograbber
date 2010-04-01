package org.decat.sandbox;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts.People;
import android.util.Log;

public class ContactHelper {
	public static String getContactInformation(Activity activity, String contactUri, String column) {
		String result = null;
		Uri contactData = Uri.parse(contactUri);
		Cursor c = null;
		try {
			c = activity.managedQuery(contactData, null, null, null, null);
			if (c != null && c.moveToFirst()) {
				result = c.getString(c.getColumnIndexOrThrow(column));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		Log.d(Sandbox.TAG, "Retrieved contact '" + contactUri + "' information " + column + "=" + result);

		return result;
	}

	public static String getContactPhoneNumberFromUriString(Activity activity, String contactUri) {
		String result = null;
		String column = People.Phones.NUMBER;
		Uri contactData = Uri.parse(contactUri);
		Cursor c = null;
		try {
			c = activity.managedQuery(contactData, null, People.Phones.TYPE + "=" + People.Phones.TYPE_MOBILE, null, null);
			if (c != null && c.moveToFirst()) {
				result = c.getString(c.getColumnIndexOrThrow(column));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		Log.d(Sandbox.TAG, "Retrieved contact '" + contactUri + "' information " + column + "=" + result);

		return result;
	}

	public static String getContactNameFromUriString(Activity activity, String contactUri) {
		return getContactInformation(activity, contactUri, People.NAME);
	}
}
