package org.decat.tig;

/*
 **
 **       Copyright (C) 2010 Patrick Decat
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

import java.io.File;

import org.decat.tig.net.ResourceDownloader;
import org.decat.tig.preferences.PreferencesEditor;
import org.decat.tig.preferences.PreferencesHelper;
import org.decat.tig.web.TIGWebViewClient;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class TIG extends Activity {
	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;
	private static final int ACTIVITY_REQUEST_PREFERENCES_EDITOR = 3;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	public static final String TAG = "TIG";

	private static final String FILENAME_IDF_BACKGROUND = "fond_IDF.jpg";
	private static final String FILENAME_IDF_TRAFFIC = "segment_IDF.gif";

	private static final String URL_SYTADIN = "http://www.sytadin.fr";
	private static final String URL_LIVE_TRAFFIC_IDF_BACKGROUND = URL_SYTADIN + "/fonds/" + FILENAME_IDF_BACKGROUND;
	private static final String URL_LIVE_TRAFFIC_IDF_STATE = URL_SYTADIN + "/raster/" + FILENAME_IDF_TRAFFIC;
	private static final String URL_LIVE_TRAFFIC = URL_SYTADIN + "/opencms/sites/sytadin/sys/raster.jsp.html";
	private static final String URL_QUICK_STATS = URL_SYTADIN + "/opencms/sites/sytadin/sys/elements/iframe-direct.jsp.html";
	private static final String URL_CLOSED_AT_NIGHT = URL_SYTADIN + "/opencms/opencms/sys/fermetures.jsp";

	private static final String URL_INFOTRAFIC = "http://www.infotrafic.com";
	private static final String URL_TRAFFIC_COLLISIONS_IDF = URL_INFOTRAFIC + "/route.php?region=IDF&link=accidents.php";

	private SharedPreferences sharedPreferences;

	private float zoomFactor;

	private WebView webview;

	private TIGWebViewClient webViewClient;

	private static boolean preferenceNotificationShortcut = false;

	private static boolean preferenceLockOrientation = false;

	private static boolean preferenceShowRefreshButton = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Load preferences
		sharedPreferences = getPreferences(Context.MODE_PRIVATE);

		// Retrieve screen density
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		zoomFactor = metrics.densityDpi / 160.0f;
		Log.i(TIG.TAG, "Screen DPI is " + metrics.densityDpi + ", zoom factor is " + zoomFactor);

		// Initialize view
		webview = (WebView) findViewById(R.id.webview);
		webViewClient = new TIGWebViewClient(this);
		webview.setWebViewClient(webViewClient);
		WebSettings settings = webview.getSettings();
		settings.setBuiltInZoomControls(true);
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		// Cache resources
		cacheResources(this);

		// Default view
		showLiveTraffic();
	}

	private static boolean getBooleanPreferenceValue(Context context, String preferenceKey) {
		// Get shared preferences
		SharedPreferences sharedPreferences = context.getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);

		return sharedPreferences.getBoolean(preferenceKey, true);
	}

	private void updateRefreshButtonVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.SHOW_REFRESH_BUTTON);

		if (value != preferenceShowRefreshButton) {
			View refreshButton = findViewById(R.id.refreshButton);
			if (value) {
				// Show refresh button as set in preferences
				refreshButton.setVisibility(View.VISIBLE);
			} else {
				refreshButton.setVisibility(View.INVISIBLE);
			}
		}

		// Store new value
		preferenceShowRefreshButton = value;
	}

	private void updateOrientationForcing(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.FORCE_PORTRAIT_ORIENTATION);

		if (value != preferenceLockOrientation) {
			if (value) {
				// Lock orientation as set in preferences
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			}
		}

		// Store new value
		preferenceLockOrientation = value;
	}

	public static void updateNotificationShortcutVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.NOTIFICATION_SHORTCUT);

		if (value != preferenceNotificationShortcut) {
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (value) {
				Notification notification = new Notification(R.drawable.icon, context.getString(R.string.notificationMessage), System.currentTimeMillis());
				Intent intent = new Intent(context, TIG.class);
				notification.setLatestEventInfo(context, context.getString(R.string.app_name) + " " + context.getString(R.string.app_version), context.getString(R.string.notificationLabel),
						PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
				notification.flags |= Notification.FLAG_ONGOING_EVENT;
				notification.flags |= Notification.FLAG_NO_CLEAR;
				notificationManager.notify(0, notification);
			} else {
				notificationManager.cancel(0);
			}
		}

		// Store new value
		preferenceNotificationShortcut = value;
	}

	@Override
	public void onResume() {
		super.onResume();

		// Update notification shortcut visibility
		updateNotificationShortcutVisibility(this);

		// Update orientation forcing
		updateOrientationForcing(this);

		// Update refresh button visibility
		updateRefreshButtonVisibility(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.liveTrafficLite:
			showLiveTrafficLite();
			return true;
		case R.id.liveTraffic:
			showLiveTraffic();
			return true;
		case R.id.quickStats:
			showQuickStats();
			return true;
		case R.id.closedAtNight:
			showClosedAtNight();
			return true;
		case R.id.trafficCollisions:
			showTrafficCollisions();
			return true;
		case R.id.sytadinWebsite:
			launchWebsite(URL_SYTADIN);
			return true;
		case R.id.infotraficWebsite:
			launchWebsite(URL_INFOTRAFIC);
		case R.id.preferences:
			showPreferencesEditor();
			return true;
		case R.id.about:
			showAbout();
			return true;
		}
		return false;
	}

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	private void showToast(String message) {
		showToast(this, message);
	}

	private void showPreferencesEditor() {
		Intent intent = new Intent(this, PreferencesEditor.class);
		startActivityForResult(intent, ACTIVITY_REQUEST_PREFERENCES_EDITOR);
	}

	private void showAbout() {
		Intent intent = new Intent(ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG);
		int activityRequest = ACTIVITY_REQUEST_OI_ABOUT_LAUNCH;

		try {
			PackageManager pm = getPackageManager();
			if (pm.queryIntentActivities(intent, 0).size() == 0) {
				String message = "Requires 'OI About' to show about dialog. Searching Android Market for it...";
				Log.i(TIG.TAG, message);
				showToast(message);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:org.openintents.about"));
				activityRequest = ACTIVITY_REQUEST_OI_ABOUT_INSTALL;
			}

			startActivityForResult(intent, activityRequest);
		} catch (Exception e) {
			String message = "Failed to start activity for intent " + intent.toString();
			Log.e(TIG.TAG, message, e);
			showToast(message);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: use this...
		switch (requestCode) {
		case ACTIVITY_REQUEST_OI_ABOUT_LAUNCH:
			if (resultCode == RESULT_OK) {
				Log.d(TIG.TAG, "Back from OI About");
			}
			break;
		case ACTIVITY_REQUEST_OI_ABOUT_INSTALL:
			if (resultCode == RESULT_CANCELED) {
				Log.d(TIG.TAG, "Back from Android Market");
				showAbout();
			}
			break;
		default:
			Log.w(TIG.TAG, "Unknown activity request code " + requestCode);
		}
	}

	private void loadUrlInWebview(String url, int scale, int x, int y, String title) {
		loadUrlInWebview(url, scale, x, y, title, null);
	}

	private void loadUrlInWebview(String url, int scale, int x, int y, String title, String lastModified) {
		Log.i(TAG, "Loading URL '" + url + "'");
		webview.setInitialScale((int) (scale * zoomFactor));
		webViewClient.setOffset((int) (x * zoomFactor), (int) (y * zoomFactor));
		webViewClient.setTitle(title);
		webViewClient.setLastModified(lastModified);
		webview.loadUrl(url);
	}

	private void cacheResources(ContextWrapper context) {
		File file = context.getFileStreamPath(FILENAME_IDF_BACKGROUND);

		if (file.exists()) {
			Log.i(TAG, "Resources already cached in '" + getFilesDir().getAbsolutePath() + "'");
		} else {
			ResourceDownloader.downloadFile(this, URL_LIVE_TRAFFIC_IDF_BACKGROUND, FILENAME_IDF_BACKGROUND);
		}
	}

	private void showLiveTrafficLite() {
		new JobWithProgressDialog(this) {
			@Override
			public void doJob() {
				String lastModified = ResourceDownloader.downloadFile(TIG.this, URL_LIVE_TRAFFIC_IDF_STATE, FILENAME_IDF_TRAFFIC);
				loadUrlInWebview("file:///android_asset/tig.html", 200, 400, 150, getString(R.string.liveTrafficLite), lastModified);
			}
		}.start();
	}

	private void showLiveTraffic() {
		loadUrlInWebview(URL_LIVE_TRAFFIC, 200, 480, 220, getString(R.string.liveTraffic));
	}

	private void showQuickStats() {
		loadUrlInWebview(URL_QUICK_STATS, 180, 0, 0, getString(R.string.quickStats));
	}

	private void showClosedAtNight() {
		loadUrlInWebview(URL_CLOSED_AT_NIGHT, 75, 0, 0, getString(R.string.closedAtNight));
	}

	private void showTrafficCollisions() {
		loadUrlInWebview(URL_TRAFFIC_COLLISIONS_IDF, 100, 0, 0, getString(R.string.trafficCollisions));
	}

	private void launchWebsite(String url) {
		try {
			Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(myIntent);
		} catch (Exception e) {
			String message = "Error while launching " + url + " website";
			Log.e(TIG.TAG, message, e);
			showToast(message);
		}

	}

	@Override
	public boolean onSearchRequested() {
		String otherActivity = sharedPreferences.getString("OTHER_ACTIVITY", null);
		if (otherActivity != null) {
			String[] otherActivitySplitted = otherActivity.split("/");
			ComponentName otherComponentName = new ComponentName(otherActivitySplitted[0], otherActivitySplitted[1]);
			try {
				Intent myIntent = new Intent();
				myIntent.setAction(Intent.ACTION_MAIN);
				myIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				myIntent.setComponent(otherComponentName);
				myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(myIntent);
			} catch (Exception e) {
				String message = "Error while launching third party activity " + otherComponentName.getPackageName();
				Log.e(TIG.TAG, message, e);
				showToast(message);
			}
		} else {
			String message = "No third party activity set in preferences...";
			Log.w(TIG.TAG, message);
			showToast(message);
		}
		return false;
	}

	public void refreshWebview(View v) {
		webview.reload();
	}
}
