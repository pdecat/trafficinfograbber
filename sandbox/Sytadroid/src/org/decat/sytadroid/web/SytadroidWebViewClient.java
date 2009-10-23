package org.decat.sytadroid.web;

import android.app.Activity;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SytadroidWebViewClient extends WebViewClient {
	private transient final Activity activity;
	private transient final int xScroll;
	private transient final int yScroll;
	private transient final String lastModified;
	private transient final String title;

	public SytadroidWebViewClient(Activity activity, int x, int y, String title, String lastModified) {
		this.activity = activity;
		this.xScroll = x;
		this.yScroll = y;
		this.title = title;
		this.lastModified = lastModified;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		view.loadUrl(url);
		return true;
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		setTitle(view, "Loading " + title + "...");
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		String formattedTitle = title;
		if (lastModified != null) {
			// SimpleDateFormat sdt = new SimpleDateFormat();
			// formattedTitle += " - " + sdt.format(lastModified);
			formattedTitle += " - " + lastModified;
		}
		setTitle(view, formattedTitle);
		view.scrollTo(xScroll, yScroll);
	}

	private void setTitle(WebView view, String title) {
		activity.setTitle(title);
	}
}
