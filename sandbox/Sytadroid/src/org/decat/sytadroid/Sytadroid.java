package org.decat.sytadroid;

import java.io.File;

import org.decat.sytadroid.net.ResourceDownloader;
import org.decat.sytadroid.web.SytadroidWebViewClient;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class Sytadroid extends Activity {
	public static final String TAG = "SyDr";

	private static final String FILENAME_IDF_BACKGROUND = "fond_IDF.jpg";
	private static final String FILENAME_IDF_TRAFFIC = "segment_IDF.gif";
	
	private static final String URL_SYTADIN = "http://www.sytadin.fr";
	private static final String URL_LIVE_TRAFFIC_IDF_BACKGROUND = URL_SYTADIN + "/fonds/" + FILENAME_IDF_BACKGROUND;
	private static final String URL_LIVE_TRAFFIC_IDF_STATE = URL_SYTADIN + "/raster/" + FILENAME_IDF_TRAFFIC;
	private static final String URL_LIVE_TRAFFIC = URL_SYTADIN + "/opencms/sites/sytadin/sys/raster_deg.jsp.html";
	private static final String URL_QUICK_STATS = URL_SYTADIN + "/opencms/sites/sytadin/sys/elements/iframe-direct.jsp.html";
	private static final String URL_CLOSED_AT_NIGHT = URL_SYTADIN + "/opencms/sites/sytadin/sys/fermetures.jsp.html";

	private static final String URL_INFOTRAFIC = "http://www.infotrafic.com";
	private static final String URL_TRAFFIC_COLLISIONS_IDF = URL_INFOTRAFIC + "/route.php?region=IDF&link=accidents.php";

	private WebView webview;

	private SytadroidWebViewClient webViewClient;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		webview = (WebView) findViewById(R.id.webview);
		webViewClient = new SytadroidWebViewClient(this);
		webview.setWebViewClient(webViewClient);
		WebSettings settings = webview.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		// Cache resources
		cacheResources(this);

		// Default view
		showLiveTrafficLite();
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
		}
		return false;
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
				String lastModified = ResourceDownloader.downloadFile(Sytadroid.this, URL_LIVE_TRAFFIC_IDF_STATE, FILENAME_IDF_TRAFFIC);
				loadUrlInWebview("file:///android_asset/sytadroid.html", 200, 400, 150, "LLT", lastModified);
			}
		}.start();
	}

	private void showLiveTraffic() {
		loadUrlInWebview(URL_LIVE_TRAFFIC, 200, 480, 220, "LT");
	}

	private void showQuickStats() {
		loadUrlInWebview(URL_QUICK_STATS, 150, 0, 0, "QS");
	}

	private void showClosedAtNight() {
		loadUrlInWebview(URL_CLOSED_AT_NIGHT, 75, 0, 0, "CAT");
	}

	private void showTrafficCollisions() {
		loadUrlInWebview(URL_TRAFFIC_COLLISIONS_IDF, 100, 0, 0, "TC");
	}

	private void launchSytadinWebsite() {
		Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SYTADIN));
		startActivity(myIntent);
	}
}