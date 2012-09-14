/**
 * Copyright (C) 2010-2012 Patrick Decat
 *
 * TrafficInfoGrabber is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TrafficInfoGrabber is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TrafficInfoGrabber.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.tig;

import java.util.HashMap;
import java.util.Map;

import org.decat.tig.preferences.PreferencesEditor;
import org.decat.tig.preferences.PreferencesHelper;
import org.decat.tig.web.TIGWebChromeClient;
import org.decat.tig.web.TIGWebViewClient;
import org.decat.tig.web.WebviewSettings;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class TIG extends Activity {
	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;
	private static final int ACTIVITY_REQUEST_PREFERENCES_EDITOR = 3;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	public static final String TAG = "TIG";

	public static final String FILENAME_IDF_HTML = "file:///android_asset/tig.html";

	public static final String URL_SYTADIN = "http://www.sytadin.fr";
	private static final String URL_INFOTRAFIC = "http://www.infotrafic.com";

	private final Map<Integer, WebviewSettings> availableWebviews = new HashMap<Integer, WebviewSettings>();

	private int width;
	private int height;

	@ViewById
	protected WebView webview;

	@Bean
	protected TIGWebViewClient webViewClient;

	@Bean
	protected TIGWebChromeClient webChromeClient;

	private float oldScreenBrightness = 1f;

	private static boolean preferenceNotificationShortcut = false;

	private static boolean preferenceLockOrientation = false;
	private int currentViewId;

	@AfterInject
	public void init() {
		Log.d(TAG, "TIG.init");

		// Request progress bar feature
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		// Clear webview databases
		clearDatabase("webview.db");
		clearDatabase("webviewCache.db");
	}

	@AfterViews
	public void setup() {
		Log.d(TAG, "TIG.setup");

		// Initialize web view
		webview.setWebViewClient(webViewClient);
		webview.setWebChromeClient(webChromeClient);

		// Clear web view history and caches
		webview.clearHistory();
		webview.clearFormData();
		webview.clearCache(true);

		WebSettings settings = webview.getSettings();
		settings.setBuiltInZoomControls(true);
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		// settings.setAppCacheEnabled(false); // New Android SDK v7

		// Show progress bar
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		// Retrieve screen density and aspect ratio
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		width = metrics.widthPixels;
		height = metrics.heightPixels;
		Log.i(TAG, "Screen width is " + width + ", and height is " + height);

		// Set default view
		currentViewId = R.id.liveTraffic;

		// Check if first run of this version
		String appVersion = getAppVersion();
		String installedAppVersion = getInstalledAppVersion();
		boolean newVersion = !appVersion.equals(installedAppVersion);

		// Initialize preferences with default values
		PreferenceManager.setDefaultValues(this, TIG.class.getSimpleName(), Context.MODE_PRIVATE, R.xml.preferences, newVersion);

		// Show preferences editor if first run of this version
		if (newVersion) {
			Log.i(TAG, "New application version: " + appVersion + " (previous: " + installedAppVersion + ")");
			setInstalledAppVersion(appVersion);
			showToast(getString(R.string.newVersion));
			showPreferencesEditor();
		} else {
			Log.i(TAG, "Application version: " + appVersion);
		}

	}

	private void initializeWebviewSettings() {
		boolean useHD = getPreferences(this).getBoolean(PreferencesHelper.USE_HD, true);

		if (availableWebviews.isEmpty()) {
			availableWebviews.put(R.id.liveTrafficLite, new WebviewSettings(getString(R.string.liveTrafficLite), FILENAME_IDF_HTML, 197, 81, 385, 298));
			availableWebviews.put(R.id.quickStats, new WebviewSettings(getString(R.string.quickStats), URL_SYTADIN + "/opencms/sites/sytadin/sys/elements/iframe-direct.jsp.html", 1, 10, 173, 276));
			availableWebviews.put(R.id.closedAtNight, new WebviewSettings(getString(R.string.closedAtNight), URL_SYTADIN + "/opencms/opencms/sys/fermetures.jsp", 0, 0, 595, 539));
			availableWebviews.put(R.id.trafficCollisions, new WebviewSettings(getString(R.string.trafficCollisions), URL_INFOTRAFIC + "/route.php?region=IDF&link=accidents.php", 136, 135, 697, 548));
		}

		if (useHD) {
			availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), URL_SYTADIN + "/opencms/sites/sytadin/sys/raster_fs.jsp.html", 361 + 34, 145 + 24, 690 + 34,
					633 + 24));
		} else {
			availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), URL_SYTADIN + "/opencms/sites/sytadin/sys/raster.jsp.html", 203 + 34, 108 + 24, 390 + 34,
					316 + 24));
		}

	}

	private void clearDatabase(String database) {
		Log.d(TAG, "TIG.clearDatabase");
		if (this.deleteDatabase(database)) {
			Log.i(TAG, "Cleared " + database + " database.");
		}
	}

	public static SharedPreferences getPreferences(Context context) {
		Log.d(TAG, "TIG.getPreferences");
		return context.getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	public static boolean getBooleanPreferenceValue(Context context, String preferenceKey) {
		boolean value = getPreferences(context).getBoolean(preferenceKey, true);
		Log.d(TAG, "TIG.getBooleanPreferenceValue: preferenceKey=" + preferenceKey + ", value=" + value);
		return value;
	}

	private String getAppVersion() {
		Log.d(TAG, "TIG.getAppVersion");
		return getString(R.string.app_version);
	}

	private String getInstalledAppVersion() {
		Log.d(TAG, "TIG.getInstalledAppVersion");
		return getPreferences(this).getString(PreferencesHelper.INSTALLED_VERSION, "FIRST_RUN");
	}

	private void setInstalledAppVersion(String appVersion) {
		Log.d(TAG, "TIG.setInstalledAppVersion: appVersion=" + appVersion);
		Editor edit = getPreferences(this).edit();
		edit.putString(PreferencesHelper.INSTALLED_VERSION, appVersion);
		edit.commit();
	}

	private void updateButtonVisibility(Context context, String buttonPreferenceName, int buttonId) {
		Log.d(TAG, "TIG.updateButtonVisibility: buttonPreferenceName=" + buttonPreferenceName);

		// Get current value
		boolean value = getBooleanPreferenceValue(context, buttonPreferenceName);

		View button = findViewById(buttonId);
		boolean preferenceShowButton = button.getVisibility() == View.VISIBLE;
		if (value != preferenceShowButton) {
			if (value) {
				// Show refresh button as set in preferences
				button.setVisibility(View.VISIBLE);
			} else {
				button.setVisibility(View.INVISIBLE);
			}
		}
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

	private void updateAdsVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.SHOW_ADS);

		if (!value) {
			View adView = findViewById(R.id.adview);
			adView.setVisibility(View.GONE);
		}
	}

	public static void updateNotificationShortcutVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.NOTIFICATION_SHORTCUT);

		if (value != preferenceNotificationShortcut) {
			if (value) {
				triggerNotification(context);
			} else {
				cancelNotification(context);
			}
		}

		// Store new value
		preferenceNotificationShortcut = value;
	}

	private static void triggerNotification(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, context.getString(R.string.notificationMessage), System.currentTimeMillis());
		Intent intent = new Intent(context, TIG_.class);
		intent.setAction("android.intent.action.MAIN");
		intent.addCategory("android.intent.category.LAUNCHER");
		notification.setLatestEventInfo(context, context.getString(R.string.app_name) + " " + context.getString(R.string.app_version), context.getString(R.string.notificationLabel),
				PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notificationManager.notify(0, notification);
	}

	private static void cancelNotification(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}

	@Override
	public void onResume() {
		Log.d(TAG, "TIG.onResume");
		super.onResume();

		// Refresh webview settings
		initializeWebviewSettings();

		// Update notification shortcut visibility
		updateNotificationShortcutVisibility(this);

		// Update orientation forcing
		updateOrientationForcing(this);

		// Update Refresh button visibility
		updateButtonVisibility(this, PreferencesHelper.SHOW_REFRESH_BUTTON, R.id.refreshButton);

		// Update Day Night Switch button visibility
		updateButtonVisibility(this, PreferencesHelper.SHOW_DAY_NIGHT_SWITCH_BUTTON, R.id.dayNightSwitchButton);

		// Update Quit button visibility
		updateButtonVisibility(this, PreferencesHelper.SHOW_QUIT_BUTTON, R.id.quitButton);

		// Update Ads visibility
		updateAdsVisibility(this);

		// Refresh webview
		refreshCurrentView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return showViewById(item.getItemId());
	}

	private boolean showViewById(int viewId) {
		switch (viewId) {
			case 0x102002c: // Refresh on android.R.id.home click for Android 3.0+
				// Restore view ID
				viewId = currentViewId;

				// Load Webview
				loadUrlInWebview(availableWebviews.get(viewId));
				return true;

			case R.id.liveTrafficLite:
			case R.id.liveTraffic:
			case R.id.quickStats:
			case R.id.closedAtNight:
			case R.id.trafficCollisions:
				// Store view ID
				currentViewId = viewId;

				// Load Webview
				loadUrlInWebview(availableWebviews.get(viewId));
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

	public void refreshCurrentView() {
		showViewById(currentViewId);
	}

	public void cancelRetryCountDown(View v) {
		webViewClient.cancelRetryCountDown();
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
				Log.i(TAG, message);
				showToast(message);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:org.openintents.about"));
				activityRequest = ACTIVITY_REQUEST_OI_ABOUT_INSTALL;
			}

			startActivityForResult(intent, activityRequest);
		} catch (Exception e) {
			String message = "Failed to start activity for intent " + intent.toString();
			Log.e(TAG, message, e);
			showToast(message);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: use this...
		switch (requestCode) {
			case ACTIVITY_REQUEST_OI_ABOUT_LAUNCH:
				if (resultCode == RESULT_OK) {
					Log.d(TAG, "Back from OI About");
				}
				break;
			case ACTIVITY_REQUEST_OI_ABOUT_INSTALL:
				if (resultCode == RESULT_CANCELED) {
					Log.d(TAG, "Back from Android Market");
					showAbout();
				}
				break;
			default:
				Log.w(TAG, "Unknown activity request code " + requestCode);
		}
	}

	@UiThread
	protected void loadUrlInWebview(WebviewSettings settings) {
		Log.i(TAG, "Loading '" + settings.title + "' (URL=" + settings.url + ", xmin=" + settings.xmin + ", ymin=" + settings.ymin + ", xmax=" + settings.xmax + ", ymax=" + settings.ymax + ")");
		int xscale = (int) ((float) width * 100 / (float) (settings.xmax - settings.xmin));
		int yscale = (int) ((float) height * 100 / (float) (settings.ymax - settings.ymin));
		int scale = xscale < yscale ? xscale : yscale;
		int xoffset = (settings.xmin * scale) / 100;
		int yoffset = (settings.ymin * scale) / 100;
		if (xscale < yscale) {
			yoffset -= (height - ((settings.ymax - settings.ymin) * scale) / 100) / 2;
		} else {
			xoffset -= (width - ((settings.xmax - settings.xmin) * scale) / 100) / 2;
		}
		Log.d(TAG, "Computed values: xscale=" + xscale + ", yscale=" + yscale + ", scale=" + scale + ", xoffset=" + xoffset + ", yoffset=" + yoffset);

		webViewClient.setParameters(settings.title, scale, xoffset, yoffset);

		// Interrupt previous loading
		webview.stopLoading();

		webview.loadUrl(settings.url);
	}

	private void launchWebsite(String url) {
		try {
			Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(myIntent);
		} catch (Exception e) {
			String message = getString(R.string.error_while_launching_website, url);
			Log.e(TAG, message, e);
			showToast(message);
		}

	}

	@Override
	public boolean onSearchRequested() {
		String otherActivity = getPreferences(this).getString("OTHER_ACTIVITY", null);
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
				String message = getString(R.string.error_while_launching_third_party_activity) + otherComponentName.getPackageName();
				Log.e(TAG, message, e);
				showToast(message);
			}
		} else {
			String message = getString(R.string.no_third_party_activity_set_in_preferences);
			Log.w(TAG, message);
			showToast(message);
		}
		return false;
	}

	public void refreshWebview(View v) {
		Log.d(TAG, "TIG.refreshWebview");

		webview.clearCache(true);

		refreshCurrentView();
	}

	public void quit(View v) {
		Log.d(TAG, "TIG.quit");

		cancelNotification(this);
		preferenceNotificationShortcut = false;
		finish();
	}

	public void dayNightSwitch(View v) {
		Log.d(TAG, "TIG.dayNightSwitch");

		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();

		if (winParams.screenBrightness > 0.5f) {
			oldScreenBrightness = winParams.screenBrightness;
			winParams.screenBrightness = 0.5f;
		} else {
			winParams.screenBrightness = oldScreenBrightness;
		}

		win.setAttributes(winParams);
	}
}
