package org.decat.helloandroid;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HelloAndroid extends Activity {
	private final static String URL_LIVE_TRAFFIC = "http://www.sytadin.fr/sys/raster.jsp.html";
	private final static String URL_QUICK_STATS = "http://www.sytadin.fr/opencms/sites/sytadin/sys/elements/iframe-direct.jsp.html";
	private final static String URL_CLOSED_AT_NIGHT = "http://www.sytadin.fr/opencms/sites/sytadin/sys/fermetures.jsp.html";

	private class HelloWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			resetTitle(view);
		}
	}

	WebView webview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		webview = (WebView) findViewById(R.id.webview);
		webview.setWebViewClient(new HelloWebViewClient());
		WebSettings settings = webview.getSettings();
		settings.setJavaScriptEnabled(true);
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
		case R.id.liveTraffic:
			showLiveTraffic();
			return true;
		case R.id.quickStats:
			showQuickStats();
			return true;
		case R.id.closedAtNight:
			showClosedAtNight();
			return true;
		}
		return false;
	}

	private void showLiveTraffic() {
		webview.loadUrl(URL_LIVE_TRAFFIC);
		webview.setInitialScale(200);
		webview.scrollTo(100, 100);
	}

	private void showQuickStats() {
		webview.loadUrl(URL_QUICK_STATS);
		webview.setInitialScale(200);
		webview.scrollTo(0, 0);
	}

	private void showClosedAtNight() {
		webview.loadUrl(URL_CLOSED_AT_NIGHT);
		webview.setInitialScale(200);
		webview.scrollTo(100, 100);
	}

	// Reset the title and the icon based on the given item.
	private void resetTitle(WebView view) {
		WebHistoryItem item = view.copyBackForwardList().getCurrentItem();
		if (item != null) {
			setUrlTitle(item.getUrl(), item.getTitle());
		} else {
			setUrlTitle(null, null);
		}
	}

	/**
	 * Sets a title composed of the URL and the title string.
	 * 
	 * @param url
	 *            The URL of the site being loaded.
	 * @param title
	 *            The title of the site being loaded.
	 */
	private void setUrlTitle(String url, String title) {
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