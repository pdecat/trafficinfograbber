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

import android.app.Activity;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class TIGWebChromeClient extends WebChromeClient {
	private transient final Activity activity;

	public TIGWebChromeClient(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onProgressChanged(WebView view, int progress) {
		// Activities and WebViews measure progress with different scales.
		// The progress meter will automatically disappear when we reach 100%
		activity.setProgress(progress * 100);
	}
}
