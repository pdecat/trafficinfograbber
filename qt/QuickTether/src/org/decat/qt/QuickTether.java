/**
 * Copyright (C) 2010-2012 Patrick Decat
 *
 * QuickTether is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickTether is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QuickTether.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.qt;

import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class QuickTether extends Activity {
	private static final String TAG = "QT";

	private void showToast(String message) {
		final Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	private void logMessage(String message, StringBuilder messageBuilder) {
		Log.i(TAG, message);
		if (messageBuilder.length() > 0) {
			messageBuilder.append("\n");
		}
		messageBuilder.append(message);
	}

	/** Called when the activity is resumed. */
	@Override
	public void onResume() {
		super.onResume();

		StringBuilder messageBuilder = new StringBuilder();

		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		logMessage("QuickTether: ", messageBuilder);

		// Toggle tethering
		try {
			int wifiApState = getWifiApState(wifiManager, messageBuilder);
			WifiConfiguration wifiConfiguration = getWifiApConfiguration(wifiManager, messageBuilder);

			Integer WIFI_AP_STATE_ENABLED = getConstant(wifiManager, "WIFI_AP_STATE_ENABLED");

			if (wifiApState != WIFI_AP_STATE_ENABLED) {
				// Disable Wifi
				setWifiEnabled(messageBuilder, false, wifiManager);

				// Wait 1s
				wait1s();

				// Enable tethering
				setWifiApEnabled(wifiManager, wifiConfiguration, true, messageBuilder);
			} else {
				// Disable tethering
				setWifiApEnabled(wifiManager, wifiConfiguration, false, messageBuilder);

				wait1s();

				// Enable wifi
				setWifiEnabled(messageBuilder, true, wifiManager);
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to toggle tethering.", e);
			logMessage("Failed to toggle tethering:\n" + e.getMessage(), messageBuilder);
		}

		showToast(messageBuilder.toString());

		// Launch tethering settings
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
		startActivity(intent);

		// Finish activity
		finish();
	}

	private Integer getConstant(WifiManager wifiManager, String constant) throws IllegalAccessException, NoSuchFieldException {
		return (Integer) wifiManager.getClass().getField(constant).getInt(wifiManager);
	}

	private void wait1s() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Log.e(TAG, "Failed to wait 1s.", e);
		}
	}

	private Integer getWifiApState(WifiManager wifiManager, StringBuilder messageBuilder) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Integer result = (Integer) wifiManager.getClass().getMethod("getWifiApState").invoke(wifiManager);
		logMessage("getWifiApState()=" + result, messageBuilder);
		return result;
	}

	private void setWifiEnabled(StringBuilder messageBuilder, boolean enable, WifiManager wifiManager) {
		boolean result = wifiManager.setWifiEnabled(enable);
		logMessage("setWifiEnabled(" + enable + ")=" + result, messageBuilder);
	}

	private WifiConfiguration getWifiApConfiguration(WifiManager wifiManager, StringBuilder messageBuilder) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		WifiConfiguration result = (WifiConfiguration) wifiManager.getClass().getMethod("getWifiApConfiguration").invoke(wifiManager);
		Log.d(TAG, "wifiConfiguration=" + result);
		return result;
	}

	private void setWifiApEnabled(WifiManager wifiManager, WifiConfiguration wifiConfiguration, boolean enable, StringBuilder messageBuilder) throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		boolean result = (Boolean) wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(wifiManager, wifiConfiguration, enable);
		logMessage("setWifiApEnabled(" + enable + ")=" + result, messageBuilder);
	}
}
