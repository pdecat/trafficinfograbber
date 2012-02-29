/*
 **
 **       Copyright (C) 2010-2011 Patrick Decat
 ** 
 **       This file is part of TrafficInfoGrabber.
 **
 **   TrafficInfoGrabber is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   TrafficInfoGrabber is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with TrafficInfoGrabber.  If not, see <http://www.gnu.org/licenses/>.
 **
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
