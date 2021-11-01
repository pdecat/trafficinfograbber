/*
 * TrafficInfoGrabber
 *
 * Copyright (C) 2010 - 2021 Patrick Decat
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */
package org.decat.tig.web;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.decat.tig.R;
import org.decat.tig.TIG;

import java.util.concurrent.Executors;

public class TIGWebViewClient extends WebViewClient {
    class TIGWebViewJSI {
        @JavascriptInterface
        public void showToast(String message) {
            Log.d(TIG.TAG, "TIGWebViewJSI.showToast: message=" + message);
            try {
                activity.showToast(message);
            } catch (Throwable t) {
                Log.e(TIG.TAG, "TIGWebViewJSI.showToast", t);
            }
        }

        @JavascriptInterface
        public void updateLastModified(String lastModifiedValue) {
            Log.d(TIG.TAG, "TIGWebViewJSI.updateLastModified: lastModifiedValue=" + lastModifiedValue);
            try {
                TIGWebViewClient.this.updateLastModified(lastModifiedValue);
            } catch (Throwable t) {
                Log.e(TIG.TAG, "TIGWebViewJSI.updateLastModified", t);
            }
        }

        @JavascriptInterface
        public void onLoadResource(String url) {
            Log.d(TIG.TAG, "TIGWebViewJSI.onLoadResource: url=" + url);
            try {
                TIGWebViewClient.this.onLoadResource(webview, url);
            } catch (Throwable t) {
                Log.e(TIG.TAG, "TIGWebViewJSI.onLoadResource", t);
            }
        }
    }

    private static final int PAGE_LOAD_TIMEOUT_MS = 60000;

    protected TIG activity;

    protected WebView webview;

    // Field to manage the application title
    private String title;

    // Field to manage the last modified text
    protected TextView lastModifiedDate;
    protected TextView lastModifiedTime;

    // Fields to manage zoom and scrolling display
    private int initialScale;
    private int xScroll;
    private int yScroll;
    private long scaleAndScrollLastExecution;

    // Field to store main URL
    private String mainURL;

    // Fields to manage retry count down
    protected View retryCountDown;
    protected TextView retryCountDownText;
    private boolean retryCountDownCancelled;

    public TIGWebViewClient(TIG activity, WebView webview) {
        this.activity = activity;
        this.webview = webview;

        // Add a Javascript interface in order to interact with the webview
        webview.addJavascriptInterface(new TIGWebViewJSI(), "TIGAndroid");

        lastModifiedDate = activity.findViewById(R.id.lastModifiedDate);
        lastModifiedTime = activity.findViewById(R.id.lastModifiedTime);

        retryCountDown = activity.findViewById(R.id.retryCountDown);
        retryCountDownText = activity.findViewById(R.id.retryCountDownText);

        // Bind an handler to the UI thread's message queue
        handler = new Handler();
    }

    // Field to manage page load timeout
    private final Runnable pageLoadTimeoutChecker = new Runnable() {
        @Override
        public void run() {
            // Trigger retry countdown
            Log.d(TIG.TAG, "TIGWebViewClient.pageLoadTimeoutChecker.run: page load timed out, trigger a refresh");
            activity.showToast(activity.getString(R.string.page_load_timed));
            activity.refreshCurrentView();
        }
    };

    private Handler handler;

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Log.w(TIG.TAG, "TIGWebViewClient.onReceivedError: Got error " + description + " (" + errorCode + ") while loading URL " + failingUrl);
        view.stopLoading();

        Executors.newSingleThreadExecutor().execute(() -> {
            startRetryCountDown();
        });
    }

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
        activity.refreshCurrentView();
    }

    private int getCurrentRetryCountDown() {
        return Integer.parseInt(retryCountDownText.getText().toString());
    }

    public void cancelRetryCountDown() {
        Log.d(TIG.TAG, "TIGWebViewClient.cancelRetryCountDown");
        retryCountDownCancelled = true;
    }

    protected void setRetryCountDownVisibility(int visibility) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                retryCountDown.setVisibility(visibility);
            }
        });
    }

    protected void updateRetryCountDown(String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                retryCountDownText.setText(text);
            }
        });
    }

    protected void startPageLoadTimeout() {
        Log.d(TIG.TAG, "TIGWebViewClient.startPageLoadTimeout (handler=" + handler + ", pageLoadTimeoutChecker=" + pageLoadTimeoutChecker + ")");
        // Remove previous posts
        cancelPageLoadTimeout();

        // Post a page load timeout checker
        handler.postDelayed(pageLoadTimeoutChecker, PAGE_LOAD_TIMEOUT_MS);
    }

    private void cancelPageLoadTimeout() {
        Log.d(TIG.TAG, "TIGWebViewClient.cancelPageLoadTimeout (handler=" + handler + ", pageLoadTimeoutChecker=" + pageLoadTimeoutChecker + ")");
        handler.removeCallbacksAndMessages(null);
    }

    public void pause() {
        cancelPageLoadTimeout();
        webview.pauseTimers();
    }

    public void resume() {
        webview.resumeTimers();
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        Log.d(TIG.TAG, "TIGWebViewClient.onPageStarted: url=" + url);

        // Store main URL
        mainURL = url;

        // Reset last modified information
        if (mainURL.endsWith(TIG.FILE_LT_HTML) || mainURL.endsWith(TIG.FILE_LTM_HTML)) {
            updateLastModified(activity.getString(R.string.last_update));
        } else {
            // Last modified information is currently only supported with the FILENAME_IDF_HTML file, so clear it for other URLs
            updateLastModified("");
        }

        cancelRetryCountDown();

        // Update title
        setTitle("> " + title + "…");

        Executors.newSingleThreadExecutor().execute(() -> {
            startPageLoadTimeout();
        });

        // Set the scale and scroll
        setScaleAndScroll(view);
    }

    protected void updateLastModified(String lastModifiedValue) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (lastModifiedValue != null && !lastModifiedValue.contains("Données")) {
                    String[] lastModifiedValues = lastModifiedValue.split("\n");
                    lastModifiedDate.setText(lastModifiedValues != null && lastModifiedValues.length > 0 ? lastModifiedValues[0] : "");
                    lastModifiedTime.setText(lastModifiedValues != null && lastModifiedValues.length > 1 ? lastModifiedValues[1] : "");
                } else {
                    lastModifiedDate.setText("Données");
                    lastModifiedTime.setText("indisponibles");
                }
            }
        });
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        Log.d(TIG.TAG, "TIGWebViewClient.onLoadResource: url=" + url + ", mainURL=" + mainURL);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.d(TIG.TAG, "TIGWebViewClient.onPageFinished: url=" + url);

        // Cancel page load timeout
        cancelPageLoadTimeout();

        // Set the scale and scroll
        setScaleAndScroll(view);

        // Update title
        setTitle(title);
    }

    protected void setScaleAndScroll(WebView view) {
        // FIXME: add 100ms delay?
        // Avoid to executes too often
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - scaleAndScrollLastExecution > 500) {
            scaleAndScrollLastExecution = currentTimeMillis;
        } else {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setInitialScale(initialScale);
                view.scrollTo(xScroll, yScroll);

                Log.d(TIG.TAG, "TIGWebViewClient.setScaleAndScroll: initialScale=" + initialScale + ", xScroll=" + xScroll + ", yScroll=" + yScroll);
            }
        });
    }

    protected void setTitle(String title) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setTitle(title);
            }
        });
    }

    public void setParameters(String title, int initialScale, int xoffset, int yoffset) {
        this.title = title;
        this.initialScale = initialScale;
        this.xScroll = xoffset;
        this.yScroll = yoffset;
    }
}
