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
import org.decat.tig.web.TIGWebViewClient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class TIG extends Activity {
	private static final int ACTIVITY_REQUEST_OI_ABOUT_INSTALL = 1;
	private static final int ACTIVITY_REQUEST_OI_ABOUT_LAUNCH = 2;

	private static final String ORG_OPENINTENTS_ACTION_SHOW_ABOUT_DIALOG = "org.openintents.action.SHOW_ABOUT_DIALOG";

	// Wikango v1.0
	private static final ComponentName OTHER_COMPONENT_NAME_1 = new ComponentName("com.gpsprevent", "com.gpsprevent.ui.StartupActivity");

	// Eklaireur v2.02
	private static final ComponentName OTHER_COMPONENT_NAME_2 = new ComponentName("com.eklaireur.eklandroid", "com.eklaireur.eklandroid.eklaireur");

	// Eklaireur v3.11
	private static final ComponentName OTHER_COMPONENT_NAME_3 = new ComponentName("com.eklaireur.ekldroid", "com.eklaireur.ekldroid.eklaireur");

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

	private WebView webview;

	private TIGWebViewClient webViewClient;

	private Toast toast;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		webview = (WebView) findViewById(R.id.webview);
		webViewClient = new TIGWebViewClient(this);
		webview.setWebViewClient(webViewClient);
		WebSettings settings = webview.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		// Cache resources
		cacheResources(this);

		// Default view
		showLiveTraffic();
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
			launchSytadinWebsite();
			return true;
		case R.id.about:
			showAbout();
			return true;
		}
		return false;
	}

	private void showToast(String message) {
		toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
		toast.show();
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
		}
	}

	private void loadUrlInWebview(String url, int scale, int x, int y, String title) {
		loadUrlInWebview(url, scale, x, y, title, null);
	}

	private void loadUrlInWebview(String url, int scale, int x, int y, String title, String lastModified) {
		Log.i(TAG, "Loading URL '" + url + "'");
		webview.setInitialScale(scale);
		webViewClient.setOffset(x, y);
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
				loadUrlInWebview("file:///android_asset/tig.html", 200, 400, 150, "LLT", lastModified);
			}
		}.start();
	}

	private void showLiveTraffic() {
		loadUrlInWebview(URL_LIVE_TRAFFIC, 200, 480, 220, "LT");
	}

	private void showQuickStats() {
		loadUrlInWebview(URL_QUICK_STATS, 180, 0, 0, "QS");
	}

	private void showClosedAtNight() {
		loadUrlInWebview(URL_CLOSED_AT_NIGHT, 75, 0, 0, "CAT");
	}

	private void showTrafficCollisions() {
		loadUrlInWebview(URL_TRAFFIC_COLLISIONS_IDF, 100, 0, 0, "TC");
	}

	private void launchSytadinWebsite() {
		try {
			Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SYTADIN));
			startActivity(myIntent);
		} catch (Exception e) {
			String message = "Error while launching www.sytadin.fr website";
			Log.e(TIG.TAG, message, e);
			showToast(message);
		}

	}

	@Override
	public boolean onSearchRequested() {
		ComponentName otherComponentName = OTHER_COMPONENT_NAME_1;
		try {
			Intent myIntent = new Intent();
			myIntent.setAction(Intent.ACTION_MAIN);
			myIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			myIntent.setComponent(otherComponentName);
			myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(myIntent);
		} catch (Exception e) {
			String message = "Error while launching third party component " + otherComponentName.getPackageName();
			Log.e(TIG.TAG, message, e);
			showToast(message);
		}
		return false;
	}
}
