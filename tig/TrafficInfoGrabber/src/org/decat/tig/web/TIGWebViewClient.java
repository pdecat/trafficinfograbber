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
import android.widget.TextView;

import com.google.ads.AdView;
import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

@EBean
public class TIGWebViewClient extends WebViewClient {
	private static final int ADS_DISPLAY_DURATION = 5000;

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

	private int topPaddingPx;

	private boolean showAds;

	// Field to store main URL
	private String mainURL;

	private View retryCountDown;

	private TextView retryCountDownText;

	private boolean retryCountDownCancelled;

	@AfterInject
	protected void initialize() {
		topPaddingPx = (int) Float.parseFloat(activity.getString(R.dimen.html_body_padding_top).replace("px", ""));
	}

	@AfterViews
	protected void initialize2() {
		// I had to move those from initialize to here because findViewById triggers setContentView which then triggers the following AndroidRuntimeException:
		// "requestFeature() must be called before adding content"
		retryCountDown = activity.findViewById(R.id.retryCountDown);
		retryCountDownText = (TextView) activity.findViewById(R.id.retryCountDownText);
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		retryCountDownCancelled = false;
		startRetryCountDown();

		Log.w(TIG.TAG, "TIGWebViewClient.onReceivedError: Got error " + description + " (" + errorCode + ") while loading URL " + failingUrl);
	}

	@Background
	protected void startRetryCountDown() {
		Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown");

		// Check if another count down is already in progress
		String currentCountDown = retryCountDownText.getText().toString();
		if (Integer.parseInt(currentCountDown) > 0) {
			// Abort
			Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown: another count down is already in progress, aborting (" + currentCountDown + "s left).");
			return;
		}

		setRetryCountDownVisibility(View.VISIBLE);

		for (int c = 30; c >= 0; c--) {
			updateRetryCountDown(Integer.toString(c));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (retryCountDownCancelled) {
				Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown: trigger refresh");
				return;
			}
		}

		setRetryCountDownVisibility(View.INVISIBLE);

		// Trigger refresh
		Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown: trigger refresh");
		((TIG) activity).refreshCurrentView();
	}

	public void cancelRetryCountDown() {
		retryCountDownCancelled = true;
		updateRetryCountDown("0");
		setRetryCountDownVisibility(View.INVISIBLE);
	}

	@UiThread
	protected void setRetryCountDownVisibility(int visibility) {
		retryCountDown.setVisibility(visibility);
	}

	@UiThread
	protected void updateRetryCountDown(String text) {
		retryCountDownText.setText(text);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		Log.d(TIG.TAG, "TIGWebViewClient.onPageStarted: url=" + url);

		// Store main URL
		mainURL = url;
	}

	private void doOnPageStarted(WebView view) {
		Log.d(TIG.TAG, "TIGWebViewClient.doOnPageStarted");

		retryCountDownCancelled = false;

		// Update title
		setTitle(view, activity.getString(R.string.loading) + " " + title + "...");

		// Show ads if checked in preferences
		showAds = TIG.getBooleanPreferenceValue(activity, PreferencesHelper.SHOW_ADS);
		if (showAds) {
			// Increment loadCount
			loadCount++;

			showAds();
		}
	}

	@Override
	public void onLoadResource(WebView view, String url) {
		Log.d(TIG.TAG, "TIGWebViewClient.onLoadResource: url=" + url);

		// I've got an issue on my Nexus One (Android 2.3.6) where WebViewClient.onPageStarted() is not called
		// if the URL passed to WebView.loadUrl() does not change from the previous call.
		// On my Galaxy Nexus (Android 4.1.1), this does not happen.
		// Check http://code.google.com/p/android/issues/detail?id=37123
		// Workaround this issue by checking if the URL loaded matches the main URL.
		if (mainURL.equals(url)) {
			doOnPageStarted(view);
		}

		// Set the scale and scroll
		setScaleAndScroll(view);
	}

	@Override
	public void onPageFinished(final WebView view, String url) {
		Log.d(TIG.TAG, "TIGWebViewClient.onPageFinished: url=" + url);

		// Update title
		String formattedTitle = title;
		if (lastModified != null) {
			formattedTitle += " - " + lastModified;
		}
		setTitle(view, formattedTitle);

		// Set the scale and scroll
		setScaleAndScroll(view);

		// Add padding to the top of the HTML view to compensate for the overlaid action bar on Android 3.0+
		if (Build.VERSION.SDK_INT >= 11) {
			view.loadUrl("javascript:document.body.style.paddingTop='" + topPaddingPx + "px'");
		}

		// Hide ads after a short delay
		if (showAds) {
			hideAds(loadCount);
		}
	}

	@UiThread
	protected void showAds() {
		setAdsVisibility(true);
	}

	@UiThread(delay = ADS_DISPLAY_DURATION)
	protected void hideAds(long loadCountBeforeDelay) {
		Log.d(TIG.TAG, "TIGWebViewClient.hideAds: loadCountBeforeDelay=" + loadCountBeforeDelay + ", loadCount=" + loadCount);

		// Hide ads only if no other loading has been triggered since this job was instantiated
		if (loadCountBeforeDelay == TIGWebViewClient.this.loadCount) {
			setAdsVisibility(false);
		}
	}

	private void setAdsVisibility(boolean visibility) {
		adview.setVisibility(visibility ? View.VISIBLE : View.GONE);
	}

	@UiThread
	protected void setScaleAndScroll(WebView view) {
		view.setInitialScale(initialScale);
		view.scrollTo(xScroll, yScroll);
	}

	@UiThread
	protected void setTitle(WebView view, String title) {
		activity.setTitle(title);
	}

	public void setParameters(String title, String lastModified, int initialScale, int xoffset, int yoffset) {
		this.title = title;
		this.lastModified = lastModified;
		this.initialScale = initialScale;
		this.xScroll = xoffset;
		this.yScroll = yoffset;

		// Compensate the padding at the top of the HTML view that compensates for the overlaid action bar on Android 3.0+
		if (Build.VERSION.SDK_INT >= 11) {
			this.yScroll += topPaddingPx;
		}
	}
}
