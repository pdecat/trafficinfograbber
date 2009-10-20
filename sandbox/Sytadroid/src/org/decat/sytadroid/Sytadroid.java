package org.decat.sytadroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Sytadroid extends Activity {
	private static final String TAG = "Sytadroid";
	private static final String FILENAME_BACKGROUND_IDF = "fond_IDF.jpg";
	private static final String URL_LIVE_TRAFFIC_FOND_IDF = "http://www.sytadin.fr/fonds/fond_IDF.jpg";
	private static final String URL_LIVE_TRAFFIC = "http://www.sytadin.fr/sys/raster.jsp.html";
	private static final String URL_QUICK_STATS = "http://www.sytadin.fr/opencms/sites/sytadin/sys/elements/iframe-direct.jsp.html";
	private static final String URL_CLOSED_AT_NIGHT = "http://www.sytadin.fr/opencms/sites/sytadin/sys/fermetures.jsp.html";
	private static final String URL_TRAFFIC_COLLISIONS_FR = "http://www.infotrafic.com/route.php?region=FRANC&link=accidents.php";
	private static final String URL_TRAFFIC_COLLISIONS_IDF = "http://www.infotrafic.com/route.php?region=IDF&link=accidents.php";

	private class SytadroidWebViewClient extends WebViewClient {
		private transient final int xScroll;
		private transient final int yScroll;

		public SytadroidWebViewClient(int x, int y) {
			this.xScroll = x;
			this.yScroll = y;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			resetTitle(view, url, "Loading...");
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			resetTitle(view, url, null);
			webview.scrollTo(xScroll, yScroll);
		}
	}

	WebView webview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		webview = (WebView) findViewById(R.id.webview);
		WebSettings settings = webview.getSettings();
		settings.setJavaScriptEnabled(true);

		// Cache resources
		cacheResources();

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
		}
		return false;
	}

	private void loadUrlInWebview(String url, int scale, int x, int y) {
		Log.i(TAG, "Loading URL '" + url + "'");
		webview.loadUrl(url);
		webview.setInitialScale(scale);
		webview.setWebViewClient(new SytadroidWebViewClient(x, y));
	}

	private void cacheResources() {
		File file = getFileStreamPath(FILENAME_BACKGROUND_IDF);

		if (file.exists()) {
			Log.i(TAG, "Resources already cached in '" + getFilesDir().getAbsolutePath() + "'");
		} else {
			downloadFile(URL_LIVE_TRAFFIC_FOND_IDF, FILENAME_BACKGROUND_IDF);
		}
	}

	private void downloadFile(String url, String filename) {
		try {
			Log.i(TAG, "Trying to download '" + url + "' to '" + getFilesDir().getAbsolutePath() + "'");
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpEntity responseEntity = client.execute(get).getEntity();
			byte[] bytes = new byte[(int) responseEntity.getContentLength()];

			InputStream content = null;
			try {
				content = responseEntity.getContent();
				int read = 0;
				while (read < bytes.length) {
					read = read + content.read(bytes, read, bytes.length);
				}
			} finally {
				if (content != null) {
					content.close();
				}
			}

			FileOutputStream fos = null;
			try {
				fos = openFileOutput(filename, Activity.MODE_WORLD_WRITEABLE);
				fos.write(bytes);
				fos.flush();
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
			Log.i(TAG, "Successfully downloaded resource");
		} catch (Exception e) {
			Log.e(TAG, "Could not download and save resources", e);
		}
	}

	private void showLiveTrafficLite() {
		loadUrlInWebview("file:///android_asset/sytadroid.html", 200, 400, 100);
	}

	private void showLiveTraffic() {
		loadUrlInWebview(URL_LIVE_TRAFFIC, 200, 100, 100);
	}

	private void showQuickStats() {
		loadUrlInWebview(URL_QUICK_STATS, 150, 0, 0);
	}

	private void showClosedAtNight() {
		loadUrlInWebview(URL_CLOSED_AT_NIGHT, 50, 100, 100);
	}

	private void showTrafficCollisions() {
		loadUrlInWebview(URL_TRAFFIC_COLLISIONS_IDF, 100, 0, 0);
	}

	private void resetTitle(WebView view, String url, String title) {
		if (url == null || title == null) {
			WebHistoryItem item = view.copyBackForwardList().getCurrentItem();
			if (item != null) {
				if (url == null) {
					url = item.getUrl();
				}
				if (title == null) {
					title = item.getTitle();
				}
			}
		}
		setTitle(buildUrlTitle(url, title));
	}

	/**
	 * Builds and returns the page title, which is some combination of the page
	 * URL and title.
	 * 
	 * @param url
	 *            The URL of the site being loaded.
	 * @param title
	 *            The title of the site being loaded.
	 * @return The page title.
	 */
	private String buildUrlTitle(String url, String title) {
		String urlTitle = "";

		if (url != null) {
			String titleUrl = buildTitleUrl(url);

			if (title != null && 0 < title.length()) {
				if (titleUrl != null && 0 < titleUrl.length()) {
					urlTitle = titleUrl + ": " + title;
				} else {
					urlTitle = title;
				}
			} else {
				if (titleUrl != null) {
					urlTitle = titleUrl;
				}
			}
		}

		return urlTitle;
	}

	/**
	 * @param url
	 *            The URL to build a title version of the URL from.
	 * @return The title version of the URL or null if fails. The title version
	 *         of the URL can be either the URL hostname, or the hostname with
	 *         an "https://" prefix (for secure URLs), or an empty string if,
	 *         for example, the URL in question is a file:// URL with no
	 *         hostname.
	 */
	private static String buildTitleUrl(String url) {
		String titleUrl = null;

		if (url != null) {
			try {
				// parse the url string
				URL urlObj = new URL(url);
				if (urlObj != null) {
					titleUrl = "";

					String protocol = urlObj.getProtocol();
					String host = urlObj.getHost();

					if (host != null && 0 < host.length()) {
						titleUrl = host;
						if (protocol != null) {
							// if a secure site, add an "https://" prefix!
							if (protocol.equalsIgnoreCase("https")) {
								titleUrl = protocol + "://" + host;
							}
						}
					}
				}
			} catch (MalformedURLException e) {
			}
		}

		return titleUrl;
	}

}