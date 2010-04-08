package org.decat.sandbox;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
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
}
