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

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.decat.tig.net.ResourceDownloader;
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
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class TIG extends Activity {
    private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;
	private static final int ACTIVITY_REQUEST_PREFERENCES_EDITOR = 3;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	public static final String TAG = "TIG";

	private static final String FILENAME_IDF_BACKGROUND = "fond_IDF.jpg";
	private static final String FILENAME_IDF_TRAFFIC = "segment_IDF.gif";

	private static final String URL_SYTADIN = "http://www.sytadin.fr";
	private static final String URL_INFOTRAFIC = "http://www.infotrafic.com";

	private static final String URL_LIVE_TRAFFIC_IDF_BACKGROUND_BASE = URL_SYTADIN + "/fonds/";
	private static final String URL_LIVE_TRAFFIC_IDF_STATE_BASE = URL_SYTADIN + "/raster/";

	private final Map<Integer, WebviewSettings> availableWebviews = new HashMap<Integer, WebviewSettings>();

	private SharedPreferences sharedPreferences;

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
		// Request progress bar feature
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		// Clear webview databases
		clearDatabase("webview.db");
		clearDatabase("webviewCache.db");
	}

	@AfterViews
	public void setup() {
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

		// Show progress bar
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		// Load preferences
		sharedPreferences = getPreferences(this);

		// Retrieve screen density and aspect ratio
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		width = metrics.widthPixels;
		height = metrics.heightPixels;
		Log.i(TIG.TAG, "Screen width is " + width + ", and height is " + height);

		// Setup Ads
		AdView adView = (AdView) findViewById(R.id.adview);
		AdRequest adRequest = new AdRequest();
		adView.loadAd(adRequest);

		// Initialize webview settings
		initializeWebviewSettings();

		// Set default view then show it
		showViewById(R.id.liveTraffic);

		// Show preferences editor if first run of this version
		String appVersion = getAppVersion(this);
		String installedAppVersion = getInstalledAppVersion(this);
		if (!appVersion.equals(installedAppVersion)) {
			Log.i(TAG, "New application version: " + appVersion + " (previous: " + installedAppVersion + ")");
			setInstalledAppVersion(this, appVersion);
			showToast(getString(R.string.newVersion));
			showPreferencesEditor(true);
		} else {
			Log.i(TAG, "Application version: " + appVersion);
		}

	}

	private void initializeWebviewSettings() {
		boolean useHD = sharedPreferences.getBoolean(PreferencesHelper.USE_HD, true);

		if (availableWebviews.isEmpty()) {
			availableWebviews.put(R.id.liveTrafficLite, new WebviewSettings(getString(R.string.liveTrafficLite), "file:///android_asset/tig.html", 197, 81, 385, 298));
			availableWebviews.put(R.id.quickStats, new WebviewSettings(getString(R.string.quickStats), URL_SYTADIN + "/opencms/sites/sytadin/sys/elements/iframe-direct.jsp.html", 1, 10, 173, 276));
			availableWebviews.put(R.id.closedAtNight, new WebviewSettings(getString(R.string.closedAtNight), URL_SYTADIN + "/opencms/opencms/sys/fermetures.jsp", 0, 0, 595, 539));
			availableWebviews.put(R.id.trafficCollisions, new WebviewSettings(getString(R.string.trafficCollisions), URL_INFOTRAFIC + "/route.php?region=IDF&link=accidents.php", 136, 135, 697, 548));
		}

		if (useHD) {
			availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), URL_SYTADIN + "/opencms/sites/sytadin/sys/raster_fs.jsp.html", 361 + 34, 145 + 24, 690 + 34,
					633 + 24));
		} else {
			availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), URL_SYTADIN + "/opencms/sites/sytadin/sys/raster.jsp.html", 237, 108, 424, 316));
		}

	}

	private void clearDatabase(String database) {
		if (this.deleteDatabase(database)) {
			Log.i(TIG.TAG, "Cleared " + database + " database.");
		}
	}

	public static SharedPreferences getPreferences(Context context) {
		return context.getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	public static boolean getBooleanPreferenceValue(Context context, String preferenceKey) {
		return getPreferences(context).getBoolean(preferenceKey, true);
	}

	private String getAppVersion(Context context) {
		return context.getString(R.string.app_version);
	}

	private String getInstalledAppVersion(Context context) {
		return sharedPreferences.getString(PreferencesHelper.INSTALLED_VERSION, "FIRST_RUN");
	}

	private void setInstalledAppVersion(Context context, String appVersion) {
		Editor edit = sharedPreferences.edit();
		edit.putString(PreferencesHelper.INSTALLED_VERSION, appVersion);
		edit.commit();
	}

	private void updateRefreshButtonVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.SHOW_REFRESH_BUTTON);

		View refreshButton = findViewById(R.id.refreshButton);
		boolean preferenceShowRefreshButton = refreshButton.getVisibility() == View.VISIBLE;
		if (value != preferenceShowRefreshButton) {
			if (value) {
				// Show refresh button as set in preferences
				refreshButton.setVisibility(View.VISIBLE);
			} else {
				refreshButton.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDayNightSwitchButtonVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.SHOW_DAY_NIGHT_SWITCH_BUTTON);

		View dayNightSwitchButton = findViewById(R.id.dayNightSwitchButton);
		boolean preferenceShowDayNightSwitchButton = dayNightSwitchButton.getVisibility() == View.VISIBLE;
		if (value != preferenceShowDayNightSwitchButton) {
			if (value) {
				// Show refresh button as set in preferences
				dayNightSwitchButton.setVisibility(View.VISIBLE);
			} else {
				dayNightSwitchButton.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateQuitButtonVisibility(Context context) {
		// Get current value
		boolean value = getBooleanPreferenceValue(context, PreferencesHelper.SHOW_QUIT_BUTTON);

		View quitButton = findViewById(R.id.quitButton);
		boolean preferenceShowQuitButton = quitButton.getVisibility() == View.VISIBLE;
		if (value != preferenceShowQuitButton) {
			if (value) {
				// Show refresh button as set in preferences
				quitButton.setVisibility(View.VISIBLE);
			} else {
				quitButton.setVisibility(View.INVISIBLE);
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
		super.onResume();

		// Refresh webview settings
		initializeWebviewSettings();

		// Update notification shortcut visibility
		updateNotificationShortcutVisibility(this);

		// Update orientation forcing
		updateOrientationForcing(this);

		// Update Refresh button visibility
		updateRefreshButtonVisibility(this);

		// Update Day Night Switch button visibility
		updateDayNightSwitchButtonVisibility(this);

		// Update Quit button visibility
		updateQuitButtonVisibility(this);

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
		String lastModified = null;

		switch (viewId) {
			case 0x102002c: // android.R.id.home
				refreshCurrentView();
				return true;

			case R.id.liveTrafficLite:
				lastModified = prepareLiveTrafficLite();
			case R.id.liveTraffic:
			case R.id.quickStats:
			case R.id.closedAtNight:
			case R.id.trafficCollisions:
				// Store view ID
				currentViewId = viewId;

				// Load Webview
				loadUrlInWebview(availableWebviews.get(viewId), lastModified);
				return true;

			case R.id.sytadinWebsite:
				launchWebsite(URL_SYTADIN);
				return true;
			case R.id.infotraficWebsite:
				launchWebsite(URL_INFOTRAFIC);
			case R.id.preferences:
				showPreferencesEditor(false);
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

	public static void showToast(Context context, String message) {
		final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	private void showToast(String message) {
		showToast(this, message);
	}

	private void showPreferencesEditor(boolean resetDefaults) {
		Intent intent = new Intent(this, PreferencesEditor.class);
		intent.putExtra(PreferencesEditor.ACTIVITY_PREFERENCES_EDITOR_RESET_DEFAULTS, resetDefaults);
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

	private void loadUrlInWebview(WebviewSettings settings, String lastModified) {
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
		webViewClient.setInitialScale(scale);
		webViewClient.setOffset(xoffset, yoffset);
		webViewClient.setTitle(settings.title);
		webViewClient.setLastModified(lastModified);

		// TODO ?
		// webview.stopLoading();
		// webview.freeMemory();

		webview.loadUrl(settings.url);
	}

	protected String cacheResource(ContextWrapper context, String filename, String baseUrl, boolean useCache) {
		File file = context.getFileStreamPath(filename);

		if (file.exists()) {
			if (useCache) {
				Log.i(TAG, "Resource '" + filename + "' already cached in '" + getFilesDir().getAbsolutePath() + "'");
				return new Date(file.lastModified()).toString();
			}
			file.delete();
			Log.i(TAG, "Deleted cached ressource '" + filename + "' from '" + getFilesDir().getAbsolutePath() + "'");
		}

		return ResourceDownloader.downloadFile(context, baseUrl + filename, filename);
	}

	private String prepareLiveTrafficLite() {
		this.setProgress(0);

		// Cache resources
		cacheResource(this, FILENAME_IDF_BACKGROUND, URL_LIVE_TRAFFIC_IDF_BACKGROUND_BASE, true);

		this.setProgress(25);

		String lastModified = cacheResource(this, FILENAME_IDF_TRAFFIC, URL_LIVE_TRAFFIC_IDF_STATE_BASE, false);

		this.setProgress(50);

		return lastModified;
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
		refreshCurrentView();
	}

	public void quit(View v) {
		cancelNotification(this);
		preferenceNotificationShortcut = false;
		finish();
	}

	public void dayNightSwitch(View v) {
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
