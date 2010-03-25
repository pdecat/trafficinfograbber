/**
 * 
 */
package org.decat.tig;

import android.app.ProgressDialog;
import android.content.Context;

abstract class JobWithProgressDialog extends Thread {
	private ProgressDialog mypd;

	/**
	 * @param context
	 */
	JobWithProgressDialog(Context context) {
		mypd = ProgressDialog.show(context, "Downloading resources", "Please wait...", false);

	}

	abstract public void doJob();

	public void run() {
		try {
			doJob();
		} finally {
			// Dismiss the progress dialog
			mypd.dismiss();
		}
	}
}
