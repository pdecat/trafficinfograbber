package org.decat.sandbox;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.view.View;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;

public class SelectContact extends ExpandableListActivity implements OnChildClickListener {
	public class ExpandableContactsListAdapter extends SimpleCursorTreeAdapter {
		public ExpandableContactsListAdapter(Cursor cursor, Context context, int groupLayout, int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			// Given the group, we return a cursor for all the children within
			// that group

			// Return a cursor that points to this contact's phone numbers
			Uri.Builder builder = People.CONTENT_URI.buildUpon();
			ContentUris.appendId(builder, groupCursor.getLong(groupIdColumnIndex));
			builder.appendEncodedPath(People.Phones.CONTENT_DIRECTORY);
			Uri phoneNumbersUri = builder.build();

			return managedQuery(phoneNumbersUri, CHILD_PROJECTION, null, null, null);
		}
	}

	private static final String ID_COLUMN = People._ID;

	private static final String GROUP_COLUMN = People.DISPLAY_NAME;

	private static final String CHILD_COLUMN = People.NUMBER;

	private static final String[] GROUP_FROM = new String[] {
		GROUP_COLUMN
	};

	private static final String[] CHILDREN_FROM = new String[] {
		CHILD_COLUMN
	};

	private static final int[] TO_LAYOUT_FIELD = new int[] {
		android.R.id.text1
	};

	private static final String[] GROUP_PROJECTION = new String[] {
			ID_COLUMN, GROUP_COLUMN
	};

	private static final String CHILD_PROJECTION[] = new String[] {
			People.Phones._ID, CHILD_COLUMN
	};

	private int groupIdColumnIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Query for people
		Cursor groupCursor = managedQuery(People.CONTENT_URI, GROUP_PROJECTION, null, null, null);

		// Cache the ID column index
		this.groupIdColumnIndex = groupCursor.getColumnIndexOrThrow(ID_COLUMN);

		// Set up our adapter
		setListAdapter(new ExpandableContactsListAdapter(groupCursor, this, android.R.layout.simple_expandable_list_item_1, android.R.layout.simple_expandable_list_item_1, GROUP_FROM,
				TO_LAYOUT_FIELD, CHILDREN_FROM, TO_LAYOUT_FIELD));
	}

	@Override
	public boolean onChildClick(android.widget.ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		AlertDialog dialog = new AlertDialog.Builder(SelectContact.this).setMessage(((TextView) v).getText().toString()).setPositiveButton("OK", null).create();
		dialog.show();
		return true;
	}
}