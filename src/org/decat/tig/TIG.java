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
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class TIG extends Activity {
	private static final String RES_BOOLS = "bool";
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

	// public static final String FILENAME_IDF_HTML = "file:///android_asset/tig.html";

	public static final String URL_SYTADIN = "http://www.sytadin.fr";
	private static final String URL_INFOTRAFIC = "http://www.infotrafic.com";

	private final SparseArray<WebviewSettings> availableWebviews = new SparseArray<WebviewSettings>(5);

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

		// Needed since API 14
		enableActionBarIcon();

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
	}

	@TargetApi(14)
	private void enableActionBarIcon() {
		// Since API 14, we need to call this method to enable action icon interaction
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setHomeButtonEnabled(true);
		}
	}

	private void initializeWebviewSettings() {
		if (availableWebviews.size() == 0) {
			availableWebviews.put(R.id.liveTrafficLite, new WebviewSettings(getString(R.string.liveTrafficLite), URL_SYTADIN + "/carto/dynamique/emprises/segment_TOTALE_fs.png", 388, 193, 631, 621));
			availableWebviews.put(R.id.quickStats, new WebviewSettings(getString(R.string.quickStats), URL_SYTADIN + "/sys/barometres_de_la_circulation.jsp.html", 0, 0, 600, 600));
			availableWebviews.put(R.id.closedAtNight, new WebviewSettings(getString(R.string.closedAtNight), URL_SYTADIN + "/sys/fermetures_nocturnes.jsp.html", 0, 0, 595, 539));
			availableWebviews.put(R.id.trafficCollisions, new WebviewSettings(getString(R.string.trafficCollisions), URL_INFOTRAFIC + "/route.php?region=IDF&link=accidents.php", 136, 135, 697, 548));
			availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), URL_SYTADIN, 300, 250, 700, 600));
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

		// Refresh webview
		refreshCurrentView();
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
		return launchThirdPartyApp(null);
	}

	public boolean launchThirdPartyApp(View v) {
		Log.d(TAG, "TIG.launchThirdPartyApp");

		ComponentName thirdPartyAppComponentName = getThirdPartyAppComponentName();

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

	public void quit(View v) {
		quit(false);
	}

	public void quit(boolean kill) {
		Log.d(TAG, "TIG.quit: kill=" + kill);

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
	}
}
