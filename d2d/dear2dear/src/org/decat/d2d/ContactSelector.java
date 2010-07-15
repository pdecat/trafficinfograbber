package org.decat.d2d;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ContactSelector extends ListActivity {
	private static final String ID_COLUMN = People._ID;

	private static final String DISPLAY_COLUMN = People.DISPLAY_NAME;

	private static final String[] FROM_COLUMN = new String[] {
		DISPLAY_COLUMN
	};

	private static final int[] TO_LAYOUT_FIELD = new int[] {
		android.R.id.text1
	};

	private static final String[] PROJECTION = new String[] {
			ID_COLUMN,
			DISPLAY_COLUMN,
			People.STARRED
	};

	private String key;

	private CheckBox checkBox;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create layout
		LinearLayout ll = new LinearLayout(this);
		ll.setGravity(Gravity.FILL_VERTICAL);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		checkBox = new CheckBox(this);
		checkBox.setText(getString(R.string.showOnlyFavoriteContactsText));
		checkBox.setChecked(true);
		ll.addView(checkBox);

		// Register this activity as the check box' click listener
		checkBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Refresh the list adapter
				setupListAdapter();
			}
		});

		// Add the required list view
		ListView listView = new ListView(this);
		listView.setId(android.R.id.list);
		ll.addView(listView);

		// Add a view if the list is empty
		TextView textView = new TextView(this);
		textView.setId(android.R.id.empty);
		textView.setText(getString(R.string.emptyContactsListText));
		ll.addView(textView);

		// Set this activity's layout
		setContentView(ll);

		// Get the key of the preference being edited
		key = getIntent().getExtras().getString(PreferencesEditor.EXTRA_KEY);

		// Setup the list adapter
		setupListAdapter();
	}

	private void setupListAdapter() {
		// Query for people
		Cursor cursor;
		if (checkBox.isChecked()) {
			cursor = managedQuery(People.CONTENT_URI, PROJECTION, People.STARRED + "=1", null, DISPLAY_COLUMN);
		} else {
			cursor = managedQuery(People.CONTENT_URI, PROJECTION, null, null, DISPLAY_COLUMN);
		}

		// Set up our adapter
		setListAdapter(new SimpleCursorAdapter(this, android.R.layout.activity_list_item, cursor, FROM_COLUMN, TO_LAYOUT_FIELD));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Extract data
		Cursor cursor = (Cursor) getListAdapter().getItem(position);
		String value = cursor.getString(cursor.getColumnIndexOrThrow(DISPLAY_COLUMN));

		// Show a toast
		dear2dear.showToast(this, getString(R.string.contactSelectedToast, value, id, position));

		// Prepare result for calling activity
		Intent result = new Intent();
		result.setData(Uri.withAppendedPath(People.CONTENT_URI, Long.toString(id)));
		result.putExtra(PreferencesEditor.EXTRA_ID, id);
		result.putExtra(PreferencesEditor.EXTRA_KEY, key);
		result.putExtra(PreferencesEditor.EXTRA_VALUE, value);
		setResult(RESULT_OK, result);

		// Finish this activity
		finish();
	}

	public void onClick(View v) {
		// Refresh the list adapter
		setupListAdapter();
	}
}