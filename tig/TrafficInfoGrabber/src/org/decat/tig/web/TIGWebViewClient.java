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
package org.decat.tig.web;

import org.decat.tig.R;
import org.decat.tig.TIG;
import org.decat.tig.preferences.PreferencesHelper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.ads.AdView;
import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.googlecode.androidannotations.annotations.ViewById;

@EBean
public class TIGWebViewClient extends WebViewClient {
	private static final int ADS_DISPLAY_DURATION = 5000;

	private final class HideAdsJob implements Runnable {
		private final WebView view;
		private long loadCount;

		public HideAdsJob(WebView view, long loadCount) {
			this.view = view;
			this.loadCount = loadCount;
		}

		public void run() {
			try {
				Thread.sleep(ADS_DISPLAY_DURATION);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Hide ads only if no other loading has been triggered since this job was instantiated
			if (loadCount == TIGWebViewClient.this.loadCount) {
				view.post(new Runnable() {
					public void run() {
						setAdsVisibility(false);
					}
				});
			}
		}
	}

	@RootContext
	protected Activity activity;

	@ViewById
	protected AdView adview;

	// Fields to manage the application title
	private String lastModified;
	private String title;

	// Fields to manage zoom and scrolling display
	private int initialScale;
	private int xScroll;
	private int yScroll;

	// Fields to manage ads display
	private long loadCount = 0;
	private Runnable hideAdsJob;

	private int topPaddingPx;

	@AfterInject
	protected void initialize() {
		topPaddingPx = (int) Float.parseFloat(activity.getString(R.dimen.html_body_padding_top).replace("px", ""));
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		super.onReceivedError(view, errorCode, description, failingUrl);

		Log.w(TIG.TAG, "TIGWebViewClient.onReceivedError: Got error " + description + " (" + errorCode + ") while loading URL " + failingUrl);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		super.onPageStarted(view, url, favicon);

		Log.d(TIG.TAG, "TIGWebViewClient.onPageStarted: url=" + url);

		// Update title
		setTitle(view, activity.getString(R.string.loading) + " " + title + "...");

		// Set the scale and scroll once
		setScaleAndScroll(view);

		// Show the Ads banner if enabled
		boolean showAds = TIG.getBooleanPreferenceValue(activity, PreferencesHelper.SHOW_ADS);
		if (showAds) {
			view.post(new Runnable() {
				public void run() {
					setAdsVisibility(true);
				}
			});
		}

		// Increment loadCount and instantiate future job to hide ads
		loadCount++;
		hideAdsJob = new HideAdsJob(view, loadCount);
	}

	@Override
	public void onLoadResource(WebView view, String url) {
		super.onLoadResource(view, url);

		Log.d(TIG.TAG, "TIGWebViewClient.onLoadResource: url=" + url);

		// Set the scale and scroll
		setScaleAndScroll(view);
	}

	@Override
	public void onPageFinished(final WebView view, String url) {
		super.onPageFinished(view, url);

		Log.d(TIG.TAG, "TIGWebViewClient.onPageFinished: url=" + url);

		// Update title
		String formattedTitle = title;
		if (lastModified != null) {
			formattedTitle += " - " + lastModified;
		}
		setTitle(view, formattedTitle);

		// Set the scale and scroll once
		setScaleAndScroll(view);

		// Schedule another zooming again after some time because
		// onPageFinished is called only for main frame.
		// When onPageFinished() is called, the picture rendering may not be
		// done yet.
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				view.post(new Runnable() {
					public void run() {
						setScaleAndScroll(view);
					}
				});
			}
		}).start();

		// Add padding to the top of the HTML view to compensate for the overlaid action bar on Android 3.0+
		if (Integer.parseInt(Build.VERSION.SDK) >= 11) {
			view.loadUrl("javascript:document.body.style.paddingTop='" + topPaddingPx + "px'");
		}

		// Schedule ads hiding
		new Thread(hideAdsJob).start();
	}

	private void setScaleAndScroll(WebView view) {
		view.setInitialScale(initialScale);
		view.scrollTo(xScroll, yScroll);
	}

	private void setAdsVisibility(boolean visibility) {
		adview.setVisibility(visibility ? View.VISIBLE : View.GONE);
	}

	private void setTitle(WebView view, String title) {
		activity.setTitle(title);
	}

	public void setInitialScale(int i) {
		this.initialScale = i;
	}

	public void setOffset(int x, int y) {
		this.xScroll = x;
		this.yScroll = y;

		// Compensate the padding at the top of the HTML view that compensates for the overlaid action bar on Android 3.0+
		if (Integer.parseInt(Build.VERSION.SDK) >= 11) {
			this.yScroll += topPaddingPx;
		}
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
}
