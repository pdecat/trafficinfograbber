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
import android.os.Bundle;
import android.util.Log;

public class QuickWifiSwitcher extends Activity {
	protected static final String TAG = "QWS";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
        Log.i(QuickWifiSwitcher.TAG, "QuickWifiSwitcher.onCreate : Starting the wifi state monitoring service...");

        // Start the wifi state monitoring service 
        Intent intent = new Intent(this, WifiStateMonitoringService.class);
        startService(intent);
        
        // No UI so finish
        Log.i(QuickWifiSwitcher.TAG, "QuickWifiSwitcher.onCreate : No UI, finishing...");
        finish();
	}
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
        Log.i(QuickWifiSwitcher.TAG, "QuickWifiSwitcher.onDestroy...");
	}
}
