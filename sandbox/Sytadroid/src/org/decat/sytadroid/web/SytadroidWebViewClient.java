package org.decat.sytadroid.web;

import android.app.Activity;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SytadroidWebViewClient extends WebViewClient {
	private transient final Activity activity;
	private transient int xScroll;
	private transient int yScroll;
	private transient String lastModified;
	private transient String title;

	public SytadroidWebViewClient(Activity activity) {
		this.activity = activity;
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
