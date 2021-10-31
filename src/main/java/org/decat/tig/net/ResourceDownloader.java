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
package org.decat.tig.net;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.decat.tig.TIG;

import android.app.Activity;
import android.content.ContextWrapper;
import android.util.Log;

public class ResourceDownloader {
	private static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";

	public static String downloadFile(ContextWrapper context, String url, String filename) {
		String lastModified = null;
		try {
			Log.i(TIG.TAG, "Trying to download '" + url + "' to '" + context.getFilesDir().getAbsolutePath() + "'");

			HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
			urlConnection.setRequestProperty("Pragma", "no-cache");
			urlConnection.setUseCaches(false);
			try {
				lastModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(urlConnection.getLastModified()), ZoneId.of("GMT")).toString();

				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				storeResource(context, filename, in);
			} finally {
				urlConnection.disconnect();
			}


			Log.i(TIG.TAG, "Successfully downloaded resource");
		} catch (Exception e) {
			Log.e(TIG.TAG, "Could not download and save resources", e);
		}
		return lastModified;
	}

	private static void storeResource(ContextWrapper context, String filename, InputStream in) throws FileNotFoundException, IOException {
		FileOutputStream fout = null;
		try {
			fout = context.openFileOutput(filename, Activity.MODE_WORLD_WRITEABLE);
			int i;
			do {
				i = in.read();
				if (i != -1)
					fout.write(i);
			} while (i != -1);
		} finally {
			fout.close();
		}
	}
}