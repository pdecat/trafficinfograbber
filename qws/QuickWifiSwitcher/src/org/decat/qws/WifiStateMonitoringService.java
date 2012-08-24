/**
 * Copyright (C) 2010-2012 Patrick Decat
 *
 * QuickWifiSwitcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickWifiSwitcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QuickWifiSwitcher.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.qws;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
//import android.widget.RemoteViews;

public class WifiStateMonitoringService extends IntentService {
    private static final int NOTIFICATION_ID = 0;

    protected static final String ORG_DECAT_QWS_INTENT_ACTION_UPDATE_SERVICE = "org.decat.qws.intent.action.UPDATE_SERVICE";

    public WifiStateMonitoringService() {
        super("WifiStateMonitoringService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(QuickWifiSwitcher.TAG, "Make the wifi state monitoring service foreground...");
        
        Intent mainIntent = new Intent(this, QuickWifiSwitcher.class);
        mainIntent.setAction("android.intent.action.MAIN");
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);
        
        Intent aboutIntent = new Intent(this, QuickWifiSwitcher.class);
        aboutIntent.setAction(QuickWifiSwitcher.ORG_DECAT_QWS_INTENT_ACTION_SHOW_ABOUT);
        aboutIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent aboutPendingIntent = PendingIntent.getActivity(this, 0, aboutIntent, 0);

        Intent quitIntent = new Intent(this, QuickWifiSwitcher.class);
        quitIntent.setAction(QuickWifiSwitcher.ORG_DECAT_QWS_INTENT_ACTION_QUIT);
        quitIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent quitPendingIntent = PendingIntent.getActivity(this, 0, quitIntent, 0);

//        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
//        contentView.setImageViewResource(R.id.image, R.drawable.icon);
//        contentView.setTextViewText(R.id.title, getString(R.string.app_name) + " " + getString(R.string.app_version));
//        contentView.setTextViewText(R.id.text, getString(R.string.notificationLabel));

        Notification notification = new Notification.Builder(this)
        .setContentIntent(mainPendingIntent)
//        .setContent(contentView)
        .setContentTitle(getString(R.string.app_name) + " " + getString(R.string.app_version))
        .setContentText(getString(R.string.notificationLabel))
        .setSmallIcon(R.drawable.icon)
        .addAction(android.R.drawable.ic_menu_info_details, getString(R.string.about), aboutPendingIntent)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.quit), quitPendingIntent)
        .setTicker(getString(R.string.notificationMessage))
        .build();
        
//        startForeground(NOTIFICATION_ID, notification);
        NotificationManager nm = (NotificationManager ) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.i(QuickWifiSwitcher.TAG, "onHandleIntent : received action: " + action);

        if (ORG_DECAT_QWS_INTENT_ACTION_UPDATE_SERVICE.equals(action)) {
            // Extract status and update notification
            Log.i(QuickWifiSwitcher.TAG, "onHandleIntent : *TODO* extract status and update notification");
        }
    }
    
}
