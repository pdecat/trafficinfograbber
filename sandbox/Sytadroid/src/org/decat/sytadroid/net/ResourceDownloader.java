package org.decat.sytadroid.net;

import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.decat.sytadroid.Sytadroid;

import android.app.Activity;
import android.content.ContextWrapper;
import android.util.Log;

public class ResourceDownloader {

	public static String downloadFile(ContextWrapper context, String url, String filename) {
		String lastModified = null;
		try {
			Log.i(Sytadroid.TAG, "Trying to download '" + url + "' to '" + context.getFilesDir().getAbsolutePath() + "'");
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			if (response.containsHeader(ResourceDownloader.HTTP_HEADER_LAST_MODIFIED)) {
				lastModified = response.getHeaders(ResourceDownloader.HTTP_HEADER_LAST_MODIFIED)[0].getValue();
			}
			HttpEntity responseEntity = response.getEntity();
			byte[] bytes = new byte[(int) responseEntity.getContentLength()];
	
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
			Log.i(Sytadroid.TAG, "Successfully downloaded resource");
		} catch (Exception e) {
			Log.e(Sytadroid.TAG, "Could not download and save resources", e);
		}
		return lastModified;
	}

	public static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";

}
