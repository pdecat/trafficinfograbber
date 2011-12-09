package org.decat.tig.web;

import org.decat.tig.R;
import org.decat.tig.TIG;
import org.decat.tig.preferences.PreferencesHelper;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.ads.AdView;

public class TIGWebViewClient extends WebViewClient {
	private transient final Activity activity;
	private transient int xScroll;
	private transient int yScroll;
	private transient String lastModified;
	private transient String title;

	public TIGWebViewClient(Activity activity) {
		this.activity = activity;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		view.loadUrl(url);
		return true;
	}

	@Override
	public void onLoadResource(WebView view, String url) {
		setTitle(view, activity.getString(R.string.loading) + " " + title + "...");

		// Show the Ads banner if enabled
		boolean showAds = TIG.getBooleanPreferenceValue(activity, PreferencesHelper.SHOW_ADS);
		if (showAds) {
			view.post(new Runnable() {
				public void run() {
					setAdsVisibility(true);
				}
			});
		}
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		Log.w(TIG.TAG, "Got error " + errorCode + " while loading URL " + failingUrl);
	}

	@Override
	public void onPageFinished(final WebView view, String url) {
		String formattedTitle = title;
		// if (lastModified != null) {
		// try {
		// SimpleDateFormat sdt = new SimpleDateFormat();
		// lastModified = sdt.format(lastModified);
		// } catch (Exception e) {
		// Log.w(TIG.TAG, "Failed to parse last modified date '" + lastModified
		// + "'", e);
		// }
		// formattedTitle += " - " + lastModified;
		// }
		setTitle(view, formattedTitle);
		view.scrollTo(xScroll, yScroll);

		// Hide the Ads banner after some time
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				view.post(new Runnable() {
					public void run() {
						setAdsVisibility(false);
					}
				});
			}
		}).start();
	}

	private void setAdsVisibility(boolean visibility) {
		AdView adView = (AdView) activity.findViewById(R.id.adview);
		adView.setVisibility(visibility ? View.VISIBLE : View.GONE);
	}

	private void setTitle(WebView view, String title) {
		activity.setTitle(title);
	}

	public void setOffset(int x, int y) {
		this.xScroll = x;
		this.yScroll = y;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
}
