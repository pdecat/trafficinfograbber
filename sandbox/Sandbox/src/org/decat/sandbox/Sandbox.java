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
		private static final int SWIPE_THRESHOLD_VELOCITY = 100;
		private Toast toast;
		private Context context;

		public MyGestureDetector() {
			this.context = Sandbox.this;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY || Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
					float deltaX = e1.getX() - e2.getX();
					float deltaY = e1.getY() - e2.getY();

					// Détection de l'axe le plus important du mouvement entre
					// horizontal et vertical
					if (Math.abs(deltaX) > Math.abs(deltaY)) {
						if (-deltaX > SWIPE_MIN_DISTANCE) {
							showToast("Swipe horizontal gauche -> droite.");
						} else if (deltaX > SWIPE_MIN_DISTANCE) {
							showToast("Swipe horizontal droite -> gauche.");
						}
					} else {
						if (deltaY > SWIPE_MIN_DISTANCE) {
							showToast("Swipe vertical bas -> haut.");
						} else if (-deltaY > SWIPE_MIN_DISTANCE) {
							showToast("Swipe vertical haut -> bas.");
						}
					}
				}
			} catch (Exception e) {
				Log.e(Sandbox.TAG, "Failed to handle swipe", e);
			}
			return false;
		}

		private void showToast(String message) {
			toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP, 0, 320);
			toast.show();
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