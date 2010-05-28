package org.decat.sandbox;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ContactSelector extends ListActivity implements OnItemClickListener {
	private static final String ID_COLUMN = People._ID;

	private static final String DISPLAY_COLUMN = People.DISPLAY_NAME;

	private static final String[] FROM_COLUMN = new String[] {
		DISPLAY_COLUMN
	};

	private static final int[] TO_LAYOUT_FIELD = new int[] {
		android.R.id.text1
	};

	private static final String[] PROJECTION = new String[] {
			ID_COLUMN, DISPLAY_COLUMN
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Query for people
		Cursor cursor = managedQuery(People.CONTENT_URI, PROJECTION, null, null, DISPLAY_COLUMN);

		// Register this as an item click listener
		getListView().setOnItemClickListener(this);

		// Set up our adapter
		setListAdapter(new SimpleCursorAdapter(this, android.R.layout.activity_list_item, cursor, FROM_COLUMN, TO_LAYOUT_FIELD));
	}

	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		// Extract data
		Cursor cursor = (Cursor) getListAdapter().getItem(position);
		String value = cursor.getString(cursor.getColumnIndexOrThrow(DISPLAY_COLUMN));

		// Show a toast
		StringBuilder sb = new StringBuilder("Selected id=");
		sb.append(id);
		sb.append(", position=");
		sb.append(position);
		sb.append(", value=");
		sb.append(value);
		Sandbox.showToast(this, sb.toString());

		// Prepare result for calling activity
		Intent result = new Intent();
		result.setData(Uri.withAppendedPath(People.CONTENT_URI, Long.toString(id)));
		result.putExtra("id", id);
		result.putExtra("value", value);
		setResult(RESULT_OK, result);

		// Finish this activity
		finish();
	}
}