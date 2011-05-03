package org.decat.tig.net;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			lastModified = extractLastModifiedHeader(response);
			HttpEntity responseEntity = response.getEntity();
			byte[] bytes = new byte[(int) responseEntity.getContentLength()];

			readResponse(responseEntity, bytes);

			storeResource(context, filename, bytes);
			Log.i(TIG.TAG, "Successfully downloaded resource");
		} catch (Exception e) {
			Log.e(TIG.TAG, "Could not download and save resources", e);
		}
		return lastModified;
	}

	private static void storeResource(ContextWrapper context, String filename, byte[] bytes) throws FileNotFoundException, IOException {
		FileOutputStream fos = null;
		try {
			fos = context.openFileOutput(filename, Activity.MODE_WORLD_WRITEABLE);
			fos.write(bytes);
			fos.flush();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	private static void readResponse(HttpEntity responseEntity, byte[] bytes) throws IOException {
		InputStream content = null;
		try {
			content = responseEntity.getContent();
			int read = 0;
			while (read < bytes.length) {
				read = read + content.read(bytes, read, bytes.length);
			}
		} finally {
			if (content != null) {
				content.close();
			}
		}
	}

	public static final SimpleDateFormat rfc822DateFormats[] = new SimpleDateFormat[] { new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z"), new SimpleDateFormat("EEE, d MMM yy HH:mm z"),
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"), new SimpleDateFormat("EEE, d MMM yyyy HH:mm z"), new SimpleDateFormat("d MMM yy HH:mm z"), new SimpleDateFormat("d MMM yy HH:mm:ss z"),
			new SimpleDateFormat("d MMM yyyy HH:mm z"), new SimpleDateFormat("d MMM yyyy HH:mm:ss z"), };

	private static String extractLastModifiedHeader(HttpResponse response) {
		String lastModified = null;
		if (response.containsHeader(ResourceDownloader.HTTP_HEADER_LAST_MODIFIED)) {
			lastModified = response.getHeaders(ResourceDownloader.HTTP_HEADER_LAST_MODIFIED)[0].getValue();
			// lastModified = parseRfc822Date(value);
		}
		return lastModified;
	}

	private static Date parseRfc822Date(Date lastModified, String value) {
		Date date = null;
		for (SimpleDateFormat sdt : rfc822DateFormats) {
			try {
				date = sdt.parse(value);
				break;
			} catch (Exception e) {
				Log.e(TIG.TAG, "Failed to retrieve and convert 'Last-Modified' header", e);
			}
		}
		return date;
	}
}
