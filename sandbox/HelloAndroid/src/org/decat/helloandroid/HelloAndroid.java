package org.decat.helloandroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

public class HelloAndroid extends Activity {
	private class HelloWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	WebView webview;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button buttonOk = (Button) findViewById(R.id.ok);
		buttonOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				Log.i(HelloAndroid.class.getName(), "Ok");
				Toast.makeText(HelloAndroid.this, R.string.hello,
						Toast.LENGTH_SHORT).show();
			}
		});

		final Button buttonCancel = (Button) findViewById(R.id.refresh);
		buttonCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				Log.i(HelloAndroid.class.getName(), "Refresh");
				finish();
			}
		});

		webview = (WebView) findViewById(R.id.webview);
		webview.setWebViewClient(new HelloWebViewClient());
		WebSettings settings = webview.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webview.loadUrl("http://www.google.com");
		webview.reload();
	}
}