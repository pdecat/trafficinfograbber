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

import android.app.Activity;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class TIGWebChromeClient extends WebChromeClient {
    protected Activity context;

    public TIGWebChromeClient(Activity context) {
        this.context = context;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        // Activities and WebViews measure progress with different scales.
        // The progress meter will automatically disappear when we reach 100%
        context.setProgress(progress * 100);
    }
}
