/**
 * 
 */
package org.decat.sandbox;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
	/**
	 * 
	 */
	private final Sandbox sandbox;
	private static final int SWIPE_MIN_DISTANCE = 60;
	private static final int SWIPE_THRESHOLD_VELOCITY = 100;

	public MyGestureDetector(Sandbox sandbox) {
		this.sandbox = sandbox;
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
						this.sandbox.showToast("Swipe horizontal gauche -> droite.");
					} else if (deltaX > SWIPE_MIN_DISTANCE) {
						this.sandbox.showToast("Swipe horizontal droite -> gauche.");
					}
				} else {
					if (deltaY > SWIPE_MIN_DISTANCE) {
						this.sandbox.showToast("Swipe vertical bas -> haut.");
					} else if (-deltaY > SWIPE_MIN_DISTANCE) {
						this.sandbox.showToast("Swipe vertical haut -> bas.");
					}
				}
			}
		} catch (Exception e) {
			Log.e(Sandbox.TAG, "Failed to handle swipe", e);
		}
		return false;
	}

}