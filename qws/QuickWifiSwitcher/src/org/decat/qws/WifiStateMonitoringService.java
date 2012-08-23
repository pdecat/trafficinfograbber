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
import android.content.Context;
import android.content.Intent;

public class WifiStateMonitoringService extends IntentService {

    public WifiStateMonitoringService(String name) {
        super("WifiStateMonitoringService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        long endTime = System.currentTimeMillis() + 5*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
    }

    private static void updateNotification(Context context) {
        NotificationManager notificationManager =
                        (NotificationManager ) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =
                        new Notification(R.drawable.icon, context.getString(R.string.notificationMessage),
                                        System.currentTimeMillis());
        Intent intent = new Intent(context, QuickWifiSwitcher.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        notification.setLatestEventInfo(context,
                        context.getString(R.string.app_name) + " " + context.getString(R.string.app_version),
                        context.getString(R.string.notificationLabel),
                        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(0, notification);
    }
}
