package org.decat.tig;

/*
 * #%L
 * TrafficInfoGrabber
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2012 Patrick Decat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.decat.tig.preferences.PreferencesEditor;
import org.decat.tig.preferences.PreferencesHelper;
import org.decat.tig.web.TIGWebChromeClient;
import org.decat.tig.web.TIGWebViewClient;
import org.decat.tig.web.WebviewSettings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class TIG extends Activity {
	private static final String USER_AGENT_SDK_11_AND_HIGHER = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.4 (KHTML, like Gecko) Chrome/22.0.1229.94 Safari/537.4";
	private static final String RES_BOOLS = "bool";
	private static final String RES_STRINGS = "string";
	private static final String PREF_DEFAULT_SUFFIX = "_DEFAULT";
	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;
	private static final int ACTIVITY_REQUEST_PREFERENCES_EDITOR = 3;

	public static final String QUIT = "org.decat.tig.QUIT_ORDER";
	// Set up an intent filter to quit TIG on Car Mode exiting
	private IntentFilter quitIntentFilter = new IntentFilter(QUIT);
	// Set up broadcast receiver to quit TIG on Car Mode exiting
	private BroadcastReceiver quitBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			quit(false);
		}
	};

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	public static final String TAG = "TIG";

	public static final String URL_BASE = "http://tig.decat.org";
	public static final String URL_LT_IDF_HTML = URL_BASE + "/tig.html";
	public static final String URL_LLT_FULL_HTML = URL_BASE + "/tig_llt_full.html";
	public static final String URL_LLT_IDF_HTML = URL_BASE + "/tig_llt_idf.html";

	public static final String URL_SYTADIN = "http://www.sytadin.fr";

	private static final String URL_INFOTRAFIC = "http://www.infotrafic.com";

	private final SparseArray<WebviewSettings> availableWebviews = new SparseArray<WebviewSettings>(5);

	private int width;
	private int height;

	@ViewById
	protected WebView webview;

	@ViewById View nightModeLayer;
	
	@Bean
	protected TIGWebViewClient webViewClient;

	@Bean
	protected TIGWebChromeClient webChromeClient;

	private static boolean preferenceNotificationShortcut = false;
	private static boolean preferenceLockOrientation = false;

	// Fields to manage webview state 
	private int previousViewId = -1;
	private int currentViewId;

	@ViewById
	protected View adview;

	// Google Analytics tracker
	public GoogleAnalyticsTracker tracker;

	@AfterInject
	public void init() {
		Log.d(TAG, "TIG.init");

		// Request progress bar feature
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		// Clear webview databases
		clearDatabase("webview.db");
		clearDatabase("webviewCache.db");

		// Initialize the Google Analytics tracker with a 60s dispatch interval
		GoogleAnalyticsTracker.getInstance().startNewSession("UA-8749317-5", 60, this);
	}

	@AfterViews
	public void setup() {
		Log.d(TAG, "TIG.setup");

		// Needed since API 14
		enableActionBarIcon();

		// Needed since API 16
		allowUniversalAccessFromFileURLs();

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// New Traffic view requires SVG which is only available since Honeycomb
			currentViewId = R.id.liveTraffic;

			// Cheat on User-Agent header to avoid being redirected
			webview.getSettings().setUserAgentString(USER_AGENT_SDK_11_AND_HIGHER);
		} else {
			// Default to Light Traffic view on Gingerbread and below
			currentViewId = R.id.liveTrafficLite;
		}

		// Add a long click listener on the quit button to kill the process instead of finishing the activity
		findViewById(R.id.quitButton).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				TIG.this.quit(true);
				return true;
			}
		});

		// Check if first run of this version
		String appVersion = getAppVersionCode();
		String installedAppVersion = getInstalledAppVersion();
		boolean newVersion = !appVersion.equals(installedAppVersion);

		// Initialize preferences with default values
		PreferenceManager.setDefaultValues(this, TIG.class.getSimpleName(), Context.MODE_PRIVATE, R.xml.preferences, newVersion);

		// Show preferences editor if first run of this version
		if (newVersion) {
			Log.i(TAG, "New application version: " + appVersion + " (previous: " + installedAppVersion + ")");
			setInstalledAppVersion(appVersion);

			// Reset Ads preferences
			setDefaultStringPreferenceValue(this, PreferencesHelper.PREF_ADS);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.newVersionShowPreferences, getAppVersionName())).setCancelable(false).setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
					showPreferencesEditor();
				}
			}).setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			Log.i(TAG, "Application version: " + appVersion);
		}

		GoogleAnalyticsTracker googleAnalyticsTracker = GoogleAnalyticsTracker.getInstance();
		googleAnalyticsTracker.setCustomVar(1, "AppVersion", appVersion);
		googleAnalyticsTracker.setCustomVar(2, "Build/Platform", Build.VERSION.RELEASE);
		googleAnalyticsTracker.setCustomVar(3, "Build/Brand", Build.BRAND);
		googleAnalyticsTracker.setCustomVar(4, "Build/Device", Build.DEVICE);
		googleAnalyticsTracker.trackPageView("/tig/setup/");
	}

	@TargetApi(14)
	private void enableActionBarIcon() {
		// Since API 14, we need to call this method to enable action icon interaction
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setHomeButtonEnabled(true);
		}
	}

	@TargetApi(16)
	private void allowUniversalAccessFromFileURLs() {
		// Since API 16, we need to call this method to allow universal access from file URLs in WebView
		// Otherwise, you'd get "E/Web Console: XMLHttpRequest cannot load http://someurl Origin null is not allowed by Access-Control-Allow-Origin." errors
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
		}
	}

	private void initializeWebviewSettings() {
		if (availableWebviews.size() == 0) {
			availableWebviews.put(R.id.quickStats, new WebviewSettings(getString(R.string.quickStats), URL_SYTADIN + "/sys/barometres_de_la_circulation.jsp.html", 0, 0, 600, 600));
			availableWebviews.put(R.id.closedAtNight, new WebviewSettings(getString(R.string.closedAtNight), URL_SYTADIN + "/sys/fermetures_nocturnes.jsp.html", 0, 0, 595, 539));
			availableWebviews.put(R.id.trafficCollisions, new WebviewSettings(getString(R.string.trafficCollisions), URL_INFOTRAFIC + "/route.php?region=IDF&link=accidents.php", 136, 135, 697, 548));
			availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), URL_LT_IDF_HTML, -1, -1, -1, -1));
		}

		// Setup selected map for Light Traffic view
		String urlLltCarto = getPreferences(this).getString(PreferencesHelper.LT_CARTO, URL_LLT_IDF_HTML);
		WebviewSettings ltWebviewSettings;
		if (URL_LLT_FULL_HTML.equals(urlLltCarto)) {
			ltWebviewSettings = new WebviewSettings(getString(R.string.liveTrafficLite), URL_LLT_FULL_HTML, 388, 193, 631, 621);
		} else {
			ltWebviewSettings = new WebviewSettings(getString(R.string.liveTrafficLite), URL_LLT_IDF_HTML, 291, 140, 683, 713);
		}
		availableWebviews.put(R.id.liveTrafficLite, ltWebviewSettings);

	}

	private void clearDatabase(String database) {
		Log.d(TAG, "TIG.clearDatabase");
		try {
			if (this.deleteDatabase(database)) {
				// Recreate the database as it is not properly recreated in some rare cases, producing the following error:
				// I/Database( 1500): sqlite returned: error code = 1802, msg = statement aborts at 3: [DELETE FROM cache]
				// E/AndroidRuntime( 1500): FATAL EXCEPTION: WebViewWorkerThread
				// E/AndroidRuntime( 1500): android.database.sqlite.SQLiteDiskIOException: error code 10: disk I/O error
				// E/AndroidRuntime( 1500):        at android.database.sqlite.SQLiteStatement.native_execute(Native Method)
				// E/AndroidRuntime( 1500):        at android.database.sqlite.SQLiteStatement.execute(SQLiteStatement.java:61)
				// E/AndroidRuntime( 1500):        at android.database.sqlite.SQLiteDatabase.delete(SQLiteDatabase.java:1640)
				// E/AndroidRuntime( 1500):        at android.webkit.WebViewDatabase.clearCache(WebViewDatabase.java:707)
				// E/AndroidRuntime( 1500):        at android.webkit.CacheManager.clearCache(CacheManager.java:582)
				// E/AndroidRuntime( 1500):        at android.webkit.WebViewWorker.handleMessage(WebViewWorker.java:194)
				// E/AndroidRuntime( 1500):        at android.os.Handler.dispatchMessage(Handler.java:99)
				// E/AndroidRuntime( 1500):        at android.os.Looper.loop(Looper.java:130)
				// E/AndroidRuntime( 1500):        at android.os.HandlerThread.run(HandlerThread.java:60)
				SQLiteDatabase db = this.openOrCreateDatabase(database, 0, null);
				db.close();
				Log.i(TAG, "Deleted and recreated " + database + " database.");
			}
		} catch (Throwable t) {
			Log.e(TAG, "Error while deleting and recreating " + database + " database.", t);
		}
	}

	public static SharedPreferences getPreferences(Context context) {
		Log.d(TAG, "TIG.getPreferences");
		return context.getSharedPreferences(TIG.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	public static boolean getBooleanPreferenceValue(Context context, String preferenceKey) {
		int defaultValueId = context.getResources().getIdentifier(preferenceKey + PREF_DEFAULT_SUFFIX, RES_BOOLS, context.getPackageName());

		boolean defaultValue;
		if (defaultValueId != 0) {
			defaultValue = context.getResources().getBoolean(defaultValueId);
		} else {
			defaultValue = true;
		}

		boolean value = getPreferences(context).getBoolean(preferenceKey, defaultValue);
		Log.d(TAG, "TIG.getBooleanPreferenceValue: preferenceKey=" + preferenceKey + ", value=" + value + ", defaultValue=" + defaultValue);

		return value;
	}

	public static String getStringPreferenceValue(Context context, String preferenceKey) {
		String defaultValue = getDefaultStringPreferenceValue(context, preferenceKey);

		String value = getPreferences(context).getString(preferenceKey, defaultValue);
		Log.d(TAG, "TIG.getStringPreferenceValue: preferenceKey=" + preferenceKey + ", value=" + value + ", defaultValue=" + defaultValue);

		return value;
	}

	private static void setDefaultStringPreferenceValue(Context context, String preferenceKey) {
		String defaultValue = getDefaultStringPreferenceValue(context, preferenceKey);

		getPreferences(context).edit().putString(preferenceKey, defaultValue).commit();
		Log.d(TAG, "TIG.setDefaultStringPreferenceValue: preferenceKey=" + preferenceKey + ", defaultValue=" + defaultValue);
	}

	private static String getDefaultStringPreferenceValue(Context context, String preferenceKey) {
		int defaultValueId = context.getResources().getIdentifier(preferenceKey + PREF_DEFAULT_SUFFIX, RES_STRINGS, context.getPackageName());

		String defaultValue;
		if (defaultValueId != 0) {
			defaultValue = context.getResources().getString(defaultValueId);
		} else {
			defaultValue = "";
		}
		return defaultValue;
	}

	private String getAppVersionName() {
		Log.d(TAG, "TIG.getAppVersionName");
		return getString(R.string.app_version);
	}

	private String getAppVersionCode() {
		Log.d(TAG, "TIG.getAppVersionCode");
		String appVersion = "0";
		try {
			appVersion = Integer.toString(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Failed to fetch app version code", e);
		}

		return appVersion;
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
		updateButtonVisibility(context, buttonPreferenceName, buttonId, null);
	}

	private void updateButtonVisibility(Context context, String buttonPreferenceName, int buttonId, Drawable drawable) {
		Log.d(TAG, "TIG.updateButtonVisibility: buttonPreferenceName=" + buttonPreferenceName);

		// Get current value
		boolean value = getBooleanPreferenceValue(context, buttonPreferenceName);

		ImageButton button = (ImageButton) findViewById(buttonId);
		boolean preferenceShowButton = button.getVisibility() == View.VISIBLE;
		if (value != preferenceShowButton) {
			if (value) {
				if (drawable != null) {
					button.setImageDrawable(drawable);
				}

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
		String value = getStringPreferenceValue(context, PreferencesHelper.PREF_ADS);

		if (value != null && getString(R.string.PREF_ADS_NEVER_VALUE).equals(value)) {
			adview.setVisibility(View.GONE);
		} else {
			adview.setVisibility(View.VISIBLE);
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

	private void resume() {
		Log.d(TAG, "TIG.resume");

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

		// Update Third Part App button visibility
		Drawable thirdPartyAppDrawable = null;
		if (getBooleanPreferenceValue(this, PreferencesHelper.SHOW_THIRD_PARTY_APP_BUTTON)) {
			thirdPartyAppDrawable = getThirdPartyAppDrawable();
		}
		updateButtonVisibility(this, PreferencesHelper.SHOW_THIRD_PARTY_APP_BUTTON, R.id.thirdPartyAppButton, thirdPartyAppDrawable);

		// Update Ads visibility
		updateAdsVisibility(this);

		// Refresh webview if previously loaded view is not liveTraffic on Android 3+ (AJAX based)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || previousViewId != R.id.liveTraffic) {
			refreshCurrentView();
		}

		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/resume/");
	}

	private Drawable getThirdPartyAppDrawable() {
		Log.d(TAG, "TIG.getThirdPartyAppDrawable");
		PackageManager pm = this.getPackageManager();
		ComponentName thirdPartyAppComponentName = getThirdPartyAppComponentName();
		Drawable thirdPartyAppDrawable = null;
		if (thirdPartyAppComponentName != null) {
			try {
				thirdPartyAppDrawable = pm.getActivityIcon(thirdPartyAppComponentName);
				thirdPartyAppDrawable.setAlpha(100);
			} catch (NameNotFoundException e) {
				Log.e(TAG, "TIG.getThirdPartyAppDrawable: error while retrieving third party app icon", e);
			}
		}
		return thirdPartyAppDrawable;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "TIG.onOptionsItemSelected");
		return showViewById(item.getItemId()) ? true : super.onOptionsItemSelected(item);
	}

	private boolean showViewById(int viewId) {
		Log.d(TAG, "TIG.showViewById: viewId=" + viewId);
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/showViewById/" + viewId);

		// Store previous value to manage refresh
		previousViewId = currentViewId;
		switch (viewId) {
			case android.R.id.home: // Refresh on android.R.id.home click for Android 3.0+
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
				return true;

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
		Log.d(TAG, "TIG.refreshCurrentView");
		showViewById(currentViewId);
	}

	public void cancelRetryCountDown(View v) {
		webViewClient.cancelRetryCountDown();
	}

	@UiThread
	public void showToast(String message) {
		final Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
	}

	private void showPreferencesEditor() {
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/showPreferencesEditor/");
		Intent intent = new Intent(this, PreferencesEditor.class);
		startActivityForResult(intent, ACTIVITY_REQUEST_PREFERENCES_EDITOR);
	}

	private void showAbout() {
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/showAbout/");

		Intent intent = new Intent(ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG);
		int activityRequest = ACTIVITY_REQUEST_OI_ABOUT_LAUNCH;

		try {
			PackageManager pm = getPackageManager();
			// Check if OI About is installed otherwise request to install it
			if (pm.queryIntentActivities(intent, 0).size() == 0) {
				installOIAbout();
				return;
			}

			startActivityForResult(intent, activityRequest);
		} catch (Exception e) {
			String message = getString(R.string.failed_to_start_activity_for_intent) + intent.toString();
			Log.e(TAG, message, e);
			showToast(message);
		}
	}

	private void installOIAbout() {
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/installOIAbout/");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.install_oi_about).setCancelable(false).setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();

				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:org.openintents.about"));
				try {
					int activityRequest = ACTIVITY_REQUEST_OI_ABOUT_INSTALL;
					Log.i(TAG, "TIG.installOIAbout: searching Android Market for 'OI About'...");
					startActivityForResult(intent, activityRequest);
				} catch (Exception e) {
					String message = getString(R.string.failed_to_start_activity_for_intent, intent.toString());
					Log.e(TAG, message, e);
					showToast(message);
				}
			}
		}).setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/onActivityResult/" + requestCode + "/" + resultCode);
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
		int scale = 0;
		int xoffset = 0;
		int yoffset = 0;
		if (settings.xmin != -1 && settings.ymin != -1 && settings.xmax != -1 && settings.ymax != -1) {
			int xscale = (int) ((float) width * 100 / (float) (settings.xmax - settings.xmin));
			int yscale = (int) ((float) height * 100 / (float) (settings.ymax - settings.ymin));
			scale = xscale < yscale ? xscale : yscale;
			xoffset = (settings.xmin * scale) / 100;
			yoffset = (settings.ymin * scale) / 100;
			if (xscale < yscale) {
				yoffset = Math.max(yoffset - ((height - ((settings.ymax - settings.ymin) * scale) / 100) / 2), 0);
			} else {
				xoffset = Math.max(xoffset - ((width - ((settings.xmax - settings.xmin) * scale) / 100) / 2), 0);
			}
			Log.d(TAG, "Computed values: xscale=" + xscale + ", yscale=" + yscale + ", scale=" + scale + ", xoffset=" + xoffset + ", yoffset=" + yoffset);
		}

		webViewClient.setParameters(settings.title, scale, xoffset, yoffset);

		// Interrupt previous loading
		webview.stopLoading();

		webview.loadUrl(settings.url);
	}

	private void launchWebsite(String url) {
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/launchWebsite/" + url);
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
		return launchThirdPartyApp(null);
	}

	public boolean launchThirdPartyApp(View v) {
		Log.d(TAG, "TIG.launchThirdPartyApp");

		ComponentName thirdPartyAppComponentName = getThirdPartyAppComponentName();

		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/launchThirdPartyApp/" + thirdPartyAppComponentName);

		if (thirdPartyAppComponentName != null) {
			try {
				Intent myIntent = new Intent();
				myIntent.setAction(Intent.ACTION_MAIN);
				myIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				myIntent.setComponent(thirdPartyAppComponentName);
				myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(myIntent);
			} catch (Exception e) {
				String message = getString(R.string.error_while_launching_third_party_activity) + thirdPartyAppComponentName.getPackageName();
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

	private ComponentName getThirdPartyAppComponentName() {
		String thirdPartyApp = getPreferences(this).getString(PreferencesHelper.OTHER_ACTIVITY + PreferencesHelper.VALUE_SUFFIX, null);
		Log.d(TAG, "TIG.getThirdPartyAppComponentName: thirdPartyApp=" + thirdPartyApp);
		if (thirdPartyApp != null) {
			String[] thirdPartyAppSplitted = thirdPartyApp.split("/");
			if (thirdPartyAppSplitted.length == 2) {
				return new ComponentName(thirdPartyAppSplitted[0], thirdPartyAppSplitted[1]);
			}
		}
		return null;
	}

	public void refreshWebview(View v) {
		Log.d(TAG, "TIG.refreshWebview");

		webview.clearCache(true);

		refreshCurrentView();
	}

	public void dayNightSwitch(View v) {
		Log.d(TAG, "TIG.dayNightSwitch");

		nightModeLayer.setVisibility(nightModeLayer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
	}

	public void quit(View v) {
		quit(false);
	}

	public void quit(boolean kill) {
		Log.d(TAG, "TIG.quit: kill=" + kill);
		GoogleAnalyticsTracker.getInstance().trackPageView("/tig/quit/" + kill);

		cancelNotification(this);
		preferenceNotificationShortcut = false;

		// FIXME: Superfluous?
		moveTaskToBack(true);

		if (kill) {
			Log.i(TAG, "TIG.quit: killing self...");
			android.os.Process.killProcess(android.os.Process.myPid());
		} else {
			Log.i(TAG, "TIG.quit: finishing self...");
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "TIG.onCreate: isFinishing=" + isFinishing() + ", savedInstanceState=" + savedInstanceState);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.d(TAG, "TIG.onRestoreInstanceState: isFinishing=" + isFinishing() + ", savedInstanceState=" + savedInstanceState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "TIG.onConfigurationChanged: isFinishing=" + isFinishing() + ", newConfig=" + newConfig);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "TIG.onNewIntent: isFinishing=" + isFinishing() + ", intent=" + intent);
	}

	@Override
	public void onResume() {
		Log.d(TAG, "TIG.onResume: isFinishing=" + isFinishing());

		// Register an intent receiver to quit TIG on Car Mode exiting
		registerReceiver(quitBroadcastReceiver, quitIntentFilter);

		resume();

		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d(TAG, "TIG.onSaveInstanceState: isFinishing=" + isFinishing() + ", savedInstanceState=" + savedInstanceState);
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "TIG.onPause: isFinishing=" + isFinishing());

		// Unregister the intent receiver to do nothing on Car Mode exiting if TIG is not active
		unregisterReceiver(quitBroadcastReceiver);

		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "TIG.onStop: isFinishing=" + isFinishing());
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "TIG.onDestroy: isFinishing=" + isFinishing());
		super.onDestroy();

		// Stop the Google Analytics tracker when it is no longer needed.
		GoogleAnalyticsTracker.getInstance().stopSession();
	}
}
