package org.decat.sandbox;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.Toast;

public class Sandbox extends Activity {
	public static final String TAG = "Sandbox";

	class MyGestureDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 60;
		private static final int SWIPE_MAX_OFF_PATH = 250;
		private static final int SWIPE_THRESHOLD_VELOCITY = 100;
		private Toast toast;
		private Context context;

		public MyGestureDetector() {
			this.context = Sandbox.this;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
					return false;

				if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
					toast = Toast.makeText(context, "Swipe vertical haut -> bas.", Toast.LENGTH_LONG);
					toast.setGravity(Gravity.LEFT | Gravity.TOP, 25, 320);
					toast.show();
					return false;
				} else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
					toast = Toast.makeText(context, "Swipe vertical bas -> haut.", Toast.LENGTH_LONG);
					toast.setGravity(Gravity.LEFT | Gravity.TOP, 25, 320);
					toast.show();
					return false;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					toast = Toast.makeText(context, "Swipe horizontal gauche -> droite.", Toast.LENGTH_LONG);
					toast.setGravity(Gravity.LEFT | Gravity.TOP, 25, 320);
					toast.show();
					return false;
				} else if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					toast = Toast.makeText(context, "Swipe horizontal droite -> gauche.", Toast.LENGTH_LONG);
					toast.setGravity(Gravity.LEFT | Gravity.TOP, 25, 320);
					toast.show();
					return false;
				}
			} catch (Exception e) {
				Log.e(Sandbox.TAG, "Failed to handle swipe", e);
			}
			return false;
		}
	}

	private GestureDetector gestureDetector;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		gestureDetector = new GestureDetector(this, new MyGestureDetector());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

}