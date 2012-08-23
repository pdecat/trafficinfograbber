/**
 * Copyright (C) 2010-2012 Patrick Decat
 *
 * QuickWifiSwitcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickWifiSwitcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QuickWifiSwitcher.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.qws;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class QuickWifiSwitcher extends Activity {
	private static final String TAG = "QWS";

	@Override
	public void onResume() {
		super.onResume();

        Log.i(QuickWifiSwitcher.TAG, "Starting the wifi state monitoring service...");

		// Start the wifi state monitoring service 
		Intent intent = new Intent(this, WifiStateMonitoringService.class);
		startService(intent);

        Log.i(QuickWifiSwitcher.TAG, "Wifi state monitoring service started, finishing...");

		// Finish activity
		finish();
	}
}
