/**
 * TrafficInfoGrabber
 *
 * Copyright (C) 2010 - 2023 Patrick Decat
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */
package org.decat.tig;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.decat.tig.databinding.MainBinding;
import org.decat.tig.preferences.PreferencesEditor;
import org.decat.tig.preferences.PreferencesHelper;
import org.decat.tig.web.TIGWebChromeClient;
import org.decat.tig.web.TIGWebViewClient;
import org.decat.tig.web.WebviewSettings;

public class TIG extends Activity {
    private MainBinding binding;

    private static final String USER_AGENT_SDK_11_AND_HIGHER = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.4 (KHTML, like Gecko) Chrome/22.0.1229.94 Safari/537.4";
    private static final String RES_BOOLS = "bool";
    private static final String RES_STRINGS = "string";
    private static final String PREF_DEFAULT_SUFFIX = "_DEFAULT";
    private static final int ACTIVITY_REQUEST_PREFERENCES_EDITOR = 3;

    public static final String QUIT = "org.decat.tig.QUIT_ORDER";

    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "DEFAULT";

    // Set up an intent filter to quit TIG on Car Mode exiting
    private IntentFilter quitIntentFilter = new IntentFilter(QUIT);
    // Set up broadcast receiver to quit TIG on Car Mode exiting
    private BroadcastReceiver quitBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            quit(false);
        }
    };

    public static final String TAG = "TIG";

    public static final String FILE_BASE = "file:///android_asset";
    public static final String FILE_LT_HTML = FILE_BASE + "/tig.html";
    public static final String FILE_LTM_HTML = FILE_BASE + "/tig_mob.html";
    public static final String FILE_LLT_HTML = FILE_BASE + "/tig_llt.html";

    public static final String URL_SYTADIN = "http://www.sytadin.fr";
    public static final String URL_SYTADIN_MOBILE = "http://m.sytadin.fr";

    private static final String URL_INFOTRAFIC = "https://www.infotrafic.com";

    private final SparseArray<WebviewSettings> availableWebviews = new SparseArray<WebviewSettings>(5);

    private int width;
    private int height;

    protected WebView webview;

    View nightModeLayer;

    protected TIGWebViewClient webViewClient;

    protected TIGWebChromeClient webChromeClient;

    private static boolean preferenceNotificationShortcut = false;

    // Fields to manage webview state
    private int previousViewId = -1;
    private int currentViewId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "TIG.onCreate: isFinishing=" + isFinishing() + ", savedInstanceState=" + savedInstanceState);

        // Request progress bar feature
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        nightModeLayer = findViewById(R.id.nightModeLayer);

        // Clear webview databases
        clearDatabase("webview.db");
        clearDatabase("webviewCache.db");

        // Initialize web view
        webview = findViewById(R.id.webview);
        webChromeClient = new TIGWebChromeClient(this);
        webViewClient = new TIGWebViewClient(this, webview);

        webview.setWebViewClient(webViewClient);
        webview.setWebChromeClient(webChromeClient);

        // Clear web view history and caches
        webview.clearHistory();
        webview.clearFormData();
        webview.clearCache(true);

        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Create notification channel
        createNotificationChannel();

        // Enable web view debugging
        enableWebContentsDebugging();

        // Needed since API 7
        webviewSetDomStorageEnabled();

        // Needed since API 14
        enableActionBarIcon();

        // Needed since API 16
        webviewAllowUniversalAccessFromFileURLs();

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

    @TargetApi(26)
    private void createNotificationChannel() {
        // Since API 26+, importance level replaces individual priority level and is set on a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use standard notification folding on Android Pie and newer
            Log.i(TAG, "Build.VERSION.SDK_INT (" + Build.VERSION.SDK_INT + ") > Build.VERSION_CODES.O (" + Build.VERSION_CODES.O + ") ? " + String.valueOf(Build.VERSION.SDK_INT > Build.VERSION_CODES.O));
            int importance = Build.VERSION.SDK_INT > Build.VERSION_CODES.O ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_MIN;

            NotificationChannel channel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL_ID, DEFAULT_NOTIFICATION_CHANNEL_ID, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @TargetApi(7)
    private void webviewSetDomStorageEnabled() {
        // Since API 7, we need to call this method to enable the DOM storage API (needed by OpenLayers?)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
            webview.getSettings().setDomStorageEnabled(true);
        }
    }

    @TargetApi(14)
    private void enableActionBarIcon() {
        // Since API 14, we need to call this method to enable action icon interaction
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setHomeButtonEnabled(true);
        }
    }

    @TargetApi(16)
    private void webviewAllowUniversalAccessFromFileURLs() {
        // Since API 16, we need to call this method to allow universal access from file URLs in WebView
        // Otherwise, you'd get "E/Web Console: XMLHttpRequest cannot load http://someurl Origin null is not allowed by Access-Control-Allow-Origin." errors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
    }

    @TargetApi(19)
    private void enableWebContentsDebugging() {
        // Since API 19, we can call this method to allow WebView remote debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
    }

    private void initializeWebviewSettings() {
        if (availableWebviews.size() == 0) {
            availableWebviews.put(R.id.quickStats, new WebviewSettings(getString(R.string.quickStats), URL_SYTADIN + "/sys/barometres_de_la_circulation.jsp.html", 0, 0, 600, 600));
            availableWebviews.put(R.id.closedAtNight, new WebviewSettings(getString(R.string.closedAtNight), URL_SYTADIN + "/sys/fermetures_carte.jsp.html", -1, -1, -1, -1));
            availableWebviews.put(R.id.trafficCollisions, new WebviewSettings(getString(R.string.trafficCollisions), URL_INFOTRAFIC + "/incidents", 136, 135, 697, 548));

            // TODO: refresh the local files from time to time
            availableWebviews.put(R.id.liveTraffic, new WebviewSettings(getString(R.string.liveTraffic), FILE_LT_HTML, -1, -1, -1, -1, false, false));
            availableWebviews.put(R.id.liveTrafficLite, new WebviewSettings(getString(R.string.liveTrafficLite), FILE_LLT_HTML, 291, 140, 683, 713));
            // availableWebviews.put(R.id.liveTrafficMobile, new WebviewSettings(getString(R.string.liveTrafficMobile), FILE_LTM_HTML, -1, -1, -1, -1, false, false));
        }
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

    private static String getAppVersionName() {
        Log.d(TAG, "TIG.getAppVersionName");

        return BuildConfig.VERSION_NAME;
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

        if (value) {
            // Lock orientation as set in preferences
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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
        Intent intent = new Intent(context, TIG.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");

        Notification notification = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVibrate(null)
                .setSmallIcon(R.drawable.icon)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notificationLabel)))
                .setContentTitle(context.getString(R.string.app_name) + " " + getAppVersionName())
                .setContentIntent(PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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

        // Refresh webview if previously loaded view is not liveTraffic on Android 3+ (AJAX based)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || previousViewId != R.id.liveTraffic) {
            refreshCurrentView();
        }
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

        // Store previous value to manage refresh
        previousViewId = currentViewId;
        switch (viewId) {
            case android.R.id.home: // Refresh on android.R.id.home click for Android 3.0+
                // Restore view ID
                viewId = currentViewId;

                // Load Webview
                loadUrlInWebview(availableWebviews.get(viewId));
                return true;

            case R.id.liveTraffic:
            case R.id.liveTrafficLite:
                // case R.id.liveTrafficMobile:
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

    public void doShowToast(String message) {
        final Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
        toast.show();
    }

    public void showToast(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doShowToast(message);
            }
        });
    }

    private void showPreferencesEditor() {
        Intent intent = new Intent(this, PreferencesEditor.class);
        startActivityForResult(intent, ACTIVITY_REQUEST_PREFERENCES_EDITOR);
    }

    private void showAbout() {
        AlertDialog.Builder aboutWindow = new AlertDialog.Builder(this);
        TextView tx = new TextView(this);
        tx.setAutoLinkMask(Linkify.ALL);
        tx.setLinksClickable(true);
        tx.setMovementMethod(LinkMovementMethod.getInstance());
        tx.setGravity(Gravity.CENTER);
        tx.setTextSize(16);
        tx.setText(getString(R.string.app_name).concat(" ").concat(getAppVersionName()).concat("\n\n").concat(getString(R.string.about_description)).concat("\n\n")
                .concat(getString(R.string.about_copyright)).concat("\n\n").concat(getString(R.string.about_contribution)).concat("\n\n").concat(getString(R.string.about_website)));

        // TODO: display @raw/license_short and @raw/recent_changes

        aboutWindow.setIcon(R.drawable.icon);
        aboutWindow.setTitle(R.string.about);
        aboutWindow.setView(tx);

        aboutWindow.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        aboutWindow.show();
    }

    protected void doLoadUrlInWebview(WebviewSettings ws) {
        Log.i(TAG, "Loading '" + ws.title + "' (URL=" + ws.url + ", xmin=" + ws.xmin + ", ymin=" + ws.ymin + ", xmax=" + ws.xmax + ", ymax=" + ws.ymax + ")");
        int scale = 0;
        int xoffset = 0;
        int yoffset = 0;
        if (ws.xmin != -1 && ws.ymin != -1 && ws.xmax != -1 && ws.ymax != -1) {
            int xscale = (int) ((float) width * 100 / (float) (ws.xmax - ws.xmin));
            int yscale = (int) ((float) height * 100 / (float) (ws.ymax - ws.ymin));
            scale = xscale < yscale ? xscale : yscale;
            xoffset = (ws.xmin * scale) / 100;
            yoffset = (ws.ymin * scale) / 100;
            if (xscale < yscale) {
                yoffset = Math.max(yoffset - ((height - ((ws.ymax - ws.ymin) * scale) / 100) / 2), 0);
            } else {
                xoffset = Math.max(xoffset - ((width - ((ws.xmax - ws.xmin) * scale) / 100) / 2), 0);
            }
            Log.d(TAG, "Computed values: xscale=" + xscale + ", yscale=" + yscale + ", scale=" + scale + ", xoffset=" + xoffset + ", yoffset=" + yoffset);
        }

        webViewClient.setParameters(ws.title, scale, xoffset, yoffset);

        // Interrupt previous loading
        webview.stopLoading();

        // Enable/disable controls depending on selected view
        WebSettings settings = webview.getSettings();
        settings.setBuiltInZoomControls(ws.zoomControls);
        webview.setHorizontalScrollBarEnabled(ws.scrollbar);

        webview.loadUrl(ws.url);
    }

    protected void loadUrlInWebview(WebviewSettings ws) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                doLoadUrlInWebview(ws);
            }
        });
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

        nightModeLayer.setVisibility(nightModeLayer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
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
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
           registerReceiver(quitBroadcastReceiver, quitIntentFilter, RECEIVER_NOT_EXPORTED);
       } else {
           registerReceiver(quitBroadcastReceiver, quitIntentFilter);
       }

        resume();

        // Resume webview activity
        webViewClient.resume();

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

        // Pause webview activity
        webViewClient.pause();

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
