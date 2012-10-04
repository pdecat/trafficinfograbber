package org.decat.tig.web;

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

import java.io.File;

import org.decat.tig.R;
import org.decat.tig.TIG;
import org.decat.tig.net.ResourceDownloader;
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
	private static final String URL_LIVE_TRAFFIC_IDF_BACKGROUND_BASE = TIG.URL_SYTADIN + "/fonds/";
	private static final String URL_LIVE_TRAFFIC_IDF_STATE_BASE = TIG.URL_SYTADIN + "/raster/";

	private static final String PATH_TO_FILES = "file:///data/data/org.decat.tig/files/";
	private static final String FILENAME_IDF_BACKGROUND = "fond_IDF.jpg";
	private static final String FILENAME_IDF_TRAFFIC = "segment_IDF.gif";
	private static final String PATH_IDF_BACKGROUND = PATH_TO_FILES + FILENAME_IDF_BACKGROUND;
	private static final String PATH_IDF_TRAFFIC = PATH_TO_FILES + FILENAME_IDF_TRAFFIC;

	private static final int ADS_DISPLAY_DURATION = 5000;
	private static final int PAGE_LOAD_TIMEOUT_MS = 60000;

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
	private int topPaddingPx;

	// Fields to manage ads display
	private boolean showAds;
	private long loadCount = 0;

	// Field to store main URL
	private String mainURL;

	// Fields to manage retry count down
	private View retryCountDown;
	private TextView retryCountDownText;
	private boolean retryCountDownCancelled;
	private String filesDirAbsolutePath;
	private int scaledTopPadding;

	// Field to manage page load timeout
	private boolean pageLoadTimedOut;

	@AfterInject
	protected void initialize() {
		topPaddingPx = (int) Float.parseFloat(activity.getString(R.dimen.html_body_padding_top).replace("px", ""));
		filesDirAbsolutePath = activity.getFilesDir().getAbsolutePath();
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
		Log.w(TIG.TAG, "TIGWebViewClient.onReceivedError: Got error " + description + " (" + errorCode + ") while loading URL " + failingUrl);
		startRetryCountDown();
	}

	@Background
	protected void startRetryCountDown() {
		retryCountDownCancelled = false;

		// Check if another count down is already in progress
		int currentCountDown = getCurrentRetryCountDown();
		Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown: currentCountDown=" + currentCountDown);
		if (currentCountDown > 0) {
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
				Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown: count down cancelled");
				updateRetryCountDown("0");
				setRetryCountDownVisibility(View.INVISIBLE);
				return;
			}
		}

		setRetryCountDownVisibility(View.INVISIBLE);

		// Trigger refresh
		Log.d(TIG.TAG, "TIGWebViewClient.startRetryCountDown: trigger refresh");
		((TIG) activity).refreshCurrentView();
	}

	private int getCurrentRetryCountDown() {
		return Integer.parseInt(retryCountDownText.getText().toString());
	}

	public void cancelRetryCountDown() {
		Log.d(TIG.TAG, "TIGWebViewClient.cancelRetryCountDown");
		retryCountDownCancelled = true;
	}

	@UiThread
	protected void setRetryCountDownVisibility(int visibility) {
		retryCountDown.setVisibility(visibility);
	}

	@UiThread
	protected void updateRetryCountDown(String text) {
		retryCountDownText.setText(text);
	}

	@Background
	protected void startPageLoadTimeout() {
		pageLoadTimedOut = true;

		try {
			Thread.sleep(PAGE_LOAD_TIMEOUT_MS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (!pageLoadTimedOut) {
			Log.d(TIG.TAG, "TIGWebViewClient.startPageLoadTimeout: page load completed in time");
			return;
		}

		// Trigger retry countdown
		Log.d(TIG.TAG, "TIGWebViewClient.startPageLoadTimeout: page load timed out, trigger a refresh");
		TIG.showToast(activity, activity.getString(R.string.page_load_timed));
		((TIG) activity).refreshCurrentView();
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		Log.d(TIG.TAG, "TIGWebViewClient.onPageStarted: url=" + url);

		// Store main URL
		mainURL = url;

		// Clear last modified
		lastModified = null;
	}

	private void doOnPageStarted(WebView view) {
		Log.d(TIG.TAG, "TIGWebViewClient.doOnPageStarted");

		cancelRetryCountDown();

		// Update title
		setTitle(view, activity.getString(R.string.loading) + " " + title + "...");

		// Show ads if checked in preferences
		showAds = TIG.getBooleanPreferenceValue(activity, PreferencesHelper.SHOW_ADS);
		if (showAds) {
			// Increment loadCount
			loadCount++;

			showAds();
		}

		startPageLoadTimeout();
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

		if (TIG.FILENAME_IDF_HTML.equals(mainURL)) {
			if (PATH_IDF_BACKGROUND.equals(url)) {
				// Cache resources
				cacheResource(FILENAME_IDF_BACKGROUND, URL_LIVE_TRAFFIC_IDF_BACKGROUND_BASE, true);
			} else if (PATH_IDF_TRAFFIC.equals(url)) {
				lastModified = cacheResource(FILENAME_IDF_TRAFFIC, URL_LIVE_TRAFFIC_IDF_STATE_BASE, false);
			}
		}

		// Set the scale and scroll
		setScaleAndScroll(view, false);
	}

	private String cacheResource(String filename, String baseUrl, boolean useCache) {
		Log.d(TIG.TAG, "TIGWebViewClient.cacheResource: filename=" + filename + ", baseUrl=" + baseUrl + ", useCache=" + useCache);
		File file = activity.getFileStreamPath(filename);

		if (file.exists()) {
			if (useCache) {
				Log.d(TIG.TAG, "TIGWebViewClient.cacheResource: '" + filename + "' already cached in '" + filesDirAbsolutePath + "'");
				return null;
			}
			file.delete();
			Log.d(TIG.TAG, "TIGWebViewClient.cacheResource: Deleted cached ressource '" + filename + "' from '" + filesDirAbsolutePath + "'");
		}

		return ResourceDownloader.downloadFile(activity, baseUrl + filename, filename);
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		Log.d(TIG.TAG, "TIGWebViewClient.onPageFinished: url=" + url);

		// Cancel time out
		pageLoadTimedOut = false;

		// Update title
		String formattedTitle = title;
		if (lastModified != null) {
			formattedTitle += " - " + lastModified;
		}
		setTitle(view, formattedTitle);

		// Set the scale and scroll
		setScaleAndScroll(view, true);

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

	@UiThread(delay = 100)
	protected void setScaleAndScroll(WebView view, boolean addPadding) {
		view.setInitialScale(initialScale);

		// Add padding to the top of the HTML view to compensate for the overlaid action bar on Android 3.0+
		int yScroll = this.yScroll;
		if (addPadding && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			view.loadUrl("javascript:document.body.style.paddingTop='" + scaledTopPadding + "px'");
			yScroll += scaledTopPadding;
		}
		view.scrollTo(xScroll, yScroll);

		Log.d(TIG.TAG, "TIGWebViewClient.setScaleAndScroll: initialScale=" + initialScale + ", xScroll=" + xScroll + ", yScroll=" + yScroll + ", scaledTopPadding=" + scaledTopPadding);
	}

	@UiThread
	protected void setTitle(WebView view, String title) {
		activity.setTitle(title);
	}

	public void setParameters(String title, int initialScale, int xoffset, int yoffset) {
		this.title = title;
		this.initialScale = initialScale;
		this.xScroll = xoffset;
		this.yScroll = yoffset;

		// Compensate the padding at the top of the HTML view that compensates for the overlaid action bar on Android 3.0+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			scaledTopPadding = (topPaddingPx * 100) / initialScale;
		} else {
			scaledTopPadding = 0;
		}
	}
}
