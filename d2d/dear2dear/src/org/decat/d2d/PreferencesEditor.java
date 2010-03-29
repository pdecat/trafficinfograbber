package org.decat.d2d;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PreferencesEditor extends Activity {
	private SharedPreferences preferences;

	// Keys
	protected static final String STR_ACTION_1 = "ACTION_1";
	protected static final String STR_ACTION_2 = "ACTION_2";
	protected static final String STR_ACTION_3 = "ACTION_3";

	protected static final String STR_DESTINATION_1 = "DESTINATION_1";
	protected static final String STR_DESTINATION_2 = "DESTINATION_2";
	protected static final String STR_DESTINATION_3 = "DESTINATION_3";

	protected static final String STR_DESTINATION_SMS_1 = "DESTINATION_SMS_1";
	protected static final String STR_DESTINATION_SMS_2 = "DESTINATION_SMS_2";
	protected static final String STR_DESTINATION_SMS_3 = "DESTINATION_SMS_3";

	protected static final String STR_DESTINATION_EMAIL_1 = "DESTINATION_EMAIL_1";
	protected static final String STR_DESTINATION_EMAIL_2 = "DESTINATION_EMAIL_2";
	protected static final String STR_DESTINATION_EMAIL_3 = "DESTINATION_EMAIL_3";

	private static final String[] STR_OPTIONS = { STR_ACTION_1, STR_ACTION_2, STR_ACTION_3, STR_DESTINATION_1, STR_DESTINATION_2, STR_DESTINATION_3, STR_DESTINATION_SMS_1, STR_DESTINATION_SMS_2,
			STR_DESTINATION_SMS_3, STR_DESTINATION_EMAIL_1, STR_DESTINATION_EMAIL_2, STR_DESTINATION_EMAIL_3 };

	// Default values
	private static final String STR_DEFAULT_ACTION_1 = "ACTION 1";
	private static final String STR_DEFAULT_ACTION_2 = "ACTION 2";
	private static final String STR_DEFAULT_ACTION_3 = "ACTION 3";

	private static final String STR_DEAR_1 = "DEAR 1";
	private static final String STR_DEAR_2 = "DEAR 2";
	private static final String STR_DEAR_3 = "DEAR 3";

	private static final String STR_DEAR_1_ADDR_SMS = "PHONE 1";
	private static final String STR_DEAR_2_ADDR_SMS = "PHONE 2";
	private static final String STR_DEAR_3_ADDR_SMS = "PHONE 3";

	private static final String STR_DEAR_1_ADDR_EMAIL = "EMAIL 1";
	private static final String STR_DEAR_2_ADDR_EMAIL = "EMAIL 2";
	private static final String STR_DEAR_3_ADDR_EMAIL = "EMAIL 3";

	private static final String[] STR_DEFAULT_VALUES = { STR_DEFAULT_ACTION_1, STR_DEFAULT_ACTION_2, STR_DEFAULT_ACTION_3, STR_DEAR_1, STR_DEAR_2, STR_DEAR_3, STR_DEAR_1_ADDR_SMS,
			STR_DEAR_2_ADDR_SMS, STR_DEAR_3_ADDR_SMS, STR_DEAR_1_ADDR_EMAIL, STR_DEAR_2_ADDR_EMAIL, STR_DEAR_3_ADDR_EMAIL };

	private EditText[] editTexts = new EditText[STR_OPTIONS.length];

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = getSharedPreferences(dear2dear.class.getSimpleName(), Context.MODE_PRIVATE);

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(android.widget.LinearLayout.VERTICAL);

		final TextView tv = new TextView(this);
		tv.setText("Set 3 actions, dears, phone numbers and emails");
		ll.addView(tv);

		for (int i = 0; i < STR_OPTIONS.length; i++) {
			editTexts[i] = new EditText(this);
			editTexts[i].setText(preferences.getString(STR_OPTIONS[i], STR_DEFAULT_VALUES[i]));
			ll.addView(editTexts[i]);
		}

		setContentView(ll);
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences.Editor ed = preferences.edit();

		for (int i = 0; i < STR_OPTIONS.length; i++) {
			ed.putString(STR_OPTIONS[i], editTexts[i].getText().toString());
		}
		ed.commit();
	}
}