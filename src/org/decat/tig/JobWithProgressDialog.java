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
		mypd = ProgressDialog.show(context, context.getString(R.string.downloadingResources), context.getString(R.string.pleaseWait), false);

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
