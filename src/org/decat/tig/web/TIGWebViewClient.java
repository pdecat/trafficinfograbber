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
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.ads.AdView;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.googlecode.androidannotations.annotations.ViewById;

@EBean
public class TIGWebViewClient extends WebViewClient {
	@RootContext
	protected Activity activity;

	private int initialScale;
	private int xScroll;
	private int yScroll;
	private String lastModified;
	private String title;

	@ViewById
	protected AdView adview;

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		Log.w(TIG.TAG, "Got error " + errorCode + " while loading URL " + failingUrl);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		setTitle(view, activity.getString(R.string.loading) + " " + title + "...");
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
	}

	@Override
	public void onPageFinished(final WebView view, String url) {
		String formattedTitle = title;
		if (lastModified != null) {
			formattedTitle += " - " + lastModified;
		}
		setTitle(view, formattedTitle);

		// Set the scale and scroll once
		setScaleAndScroll(view);

		// Set the scale and scroll again after some time because
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

		super.onPageFinished(view, url);
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
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
}
