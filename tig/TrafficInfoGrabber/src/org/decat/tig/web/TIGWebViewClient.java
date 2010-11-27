package org.decat.tig.web;

import org.decat.tig.R;

import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		String formattedTitle = title;
		if (lastModified != null) {
			/*
			 * try { SimpleDateFormat sdt = new SimpleDateFormat(); lastModified
			 * = sdt.format(lastModified); } catch (Exception e) {
			 * Log.w(TIG.TAG, "Failed to parse last modified date '" +
			 * lastModified + "'", e); }
			 */
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
