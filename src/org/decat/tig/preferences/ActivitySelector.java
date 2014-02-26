package org.decat.tig.preferences;

/*
 * #%L
 * TrafficInfoGrabber
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2014 Patrick Decat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.ArrayList;

import org.decat.tig.R;
import org.decat.tig.TIG;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;

@EActivity
public class ActivitySelector extends ListActivity implements OnItemClickListener {
	private ProgressDialog dialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TIG.TAG, "ActivitySelector.onCreate");
		super.onCreate(savedInstanceState);

		// Display a dialog as it takes time to fetch activities
		dialog = new ProgressDialog(this);
		dialog.setMessage(getString(R.string.fetching_activities));
		dialog.show();

		// Register this as an item click listener
		getListView().setOnItemClickListener(this);

		// FIXME: Does nothing...
		// Enable list view filtering
		getListView().setTextFilterEnabled(true);

		initializeListAdapter();
	}

	@Background
	protected void initializeListAdapter() {
		Log.d(TIG.TAG, "ActivitySelector.initializeListAdapter");
		// Query activities that are meant to be started from a launcher
		final PackageManager pm = this.getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		ArrayList<ResolveInfo> resolveInfos = (ArrayList<ResolveInfo>) pm.queryIntentActivities(mainIntent, 0);

		// Map these activities' data to the list view
		final ArrayAdapter<ResolveInfo> adapter = new ArrayAdapter<ResolveInfo>(this, R.layout.activity_picker_layout, resolveInfos) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_picker_layout, parent, false);
				}

				final String text = getItem(position).loadLabel(pm).toString();
				((TextView) convertView.findViewById(R.id.title)).setText(text);

				final Drawable drawable = getItem(position).loadIcon(pm);
				((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(drawable);

				return convertView;
			}
		};

		// WARN: This kind of sorting takes time...
		adapter.sort(new ResolveInfo.DisplayNameComparator(pm));

		// Set up our adapter
		setupListAdapter(adapter);
	}

	@UiThread
	protected void setupListAdapter(final ArrayAdapter<ResolveInfo> adapter) {
		Log.d(TIG.TAG, "ActivitySelector.setupListAdapter");
		setListAdapter(adapter);

		dialog.dismiss();
	}

	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Log.d(TIG.TAG, "ActivitySelector.onItemClick: position=" + position);
		ResolveInfo resolveInfo = (ResolveInfo) getListView().getItemAtPosition(position);

		// Prepare result for calling activity
		Intent result = new Intent();
		result.putExtra(PreferencesEditor.EXTRA_RESOLVE_INFO, resolveInfo);
		setResult(RESULT_OK, result);

		// Finish this activity
		finish();
	}
}
