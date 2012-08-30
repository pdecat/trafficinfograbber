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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class WifiStateMonitoringService extends Service {
    private static final int NOTIFICATION_ID = 0;

    protected static final String ORG_DECAT_QWS_INTENT_ACTION_UPDATE_SERVICE =
                    "org.decat.qws.intent.action.UPDATE_SERVICE";

    protected static final String ORG_DECAT_QWS_INTENT_ACTION_QUIT = "org.decat.qws.intent.action.QUIT";

    protected static final String ORG_DECAT_QWS_INTENT_ACTION_SHOW_ABOUT = "org.decat.qws.intent.action.SHOW_ABOUT";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        final Uri data = intent.getData();
        
        new AsyncTask<Context, Void, Void>() {
            protected Void doInBackground(Context... context) {
        Log.i(QuickWifiSwitcher.TAG, "WifiStateMonitoringService.onStartCommand: received action: " + action
                        + " with data: " + data);

        if (ORG_DECAT_QWS_INTENT_ACTION_UPDATE_SERVICE.equals(action)) {
            // Extract status and update notification
            Log.i(QuickWifiSwitcher.TAG,
                            "WifiStateMonitoringService.onStartCommand: *TODO* extract status and update notification");
        } else if (Intent.ACTION_MAIN.equals(action)) {
            Log.i(QuickWifiSwitcher.TAG, "WifiStateMonitoringService.onStartCommand: Nothing to do...");
        } else if (ORG_DECAT_QWS_INTENT_ACTION_SHOW_ABOUT.equals(action)) {
            Log.i(QuickWifiSwitcher.TAG, "WifiStateMonitoringService.onStartCommand: *TODO* show about");
        } else {
            Intent mainIntent = new Intent(context[0], QuickWifiSwitcher.class);
            mainIntent.setAction("android.intent.action.MAIN");
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context[0], 0, mainIntent, 0);

            Intent aboutIntent = new Intent(context[0], QuickWifiSwitcher.class);
            aboutIntent.setAction(ORG_DECAT_QWS_INTENT_ACTION_SHOW_ABOUT);
            aboutIntent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent aboutPendingIntent =
            PendingIntent.getActivity(context[0], 0, aboutIntent, 0);

            Intent quitIntent = new Intent(context[0], QuickWifiSwitcher.class);
            quitIntent.setAction(ORG_DECAT_QWS_INTENT_ACTION_QUIT);
            quitIntent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent quitPendingIntent = PendingIntent.getActivity(context[0], 0, quitIntent, 0);

//            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
//            contentView.setImageViewResource(R.id.image, R.drawable.ic_stat_name);
//            contentView.setTextViewText(R.id.title, getString(R.string.app_name) + " "
//                            + getString(R.string.app_version));
//            contentView.setTextViewText(R.id.text, getString(R.string.notificationLabel));
//            contentView.setOnClickPendingIntent(R.id.image, mainPendingIntent);

            Notification notification = new Notification.Builder(context[0])
             .setContentIntent(mainPendingIntent)
//            .setContent(contentView)
             .setContentTitle("* " +
             getString(R.string.app_name) + " " +
             getString(R.string.app_version))
             .setContentText("* " +
             getString(R.string.notificationLabel))
            .setSmallIcon(R.drawable.ic_stat_name)
    
            // Using addAction prevents the custom view from
            // being used
             .addAction(android.R.drawable.ic_menu_info_details,
             getString(R.string.about), aboutPendingIntent)
             .addAction(android.R.drawable.ic_menu_close_clear_cancel,
             getString(R.string.quit), quitPendingIntent)
            .setTicker(getString(R.string.notificationMessage)).build();

            notification.flags |= Notification.FLAG_NO_CLEAR;

            Log.i(QuickWifiSwitcher.TAG, "WifiStateMonitoringService.onStartCommand: Make the wifi state monitoring service foreground...");
            startForeground(NOTIFICATION_ID, notification);
//             NotificationManager nm = (NotificationManager ) getSystemService(NOTIFICATION_SERVICE);
//             nm.notify(NOTIFICATION_ID, notification);
        }
        return null;
        }
        };

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
