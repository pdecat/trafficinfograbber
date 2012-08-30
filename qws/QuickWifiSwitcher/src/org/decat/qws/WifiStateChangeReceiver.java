package org.decat.qws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WifiStateChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(QuickWifiSwitcher.TAG, "WifiStateChangeReceiver.onReceive : Received a android.net.conn.CONNECTIVITY_CHANGE intent...");

        Intent updateIntent = new Intent(context, WifiStateMonitoringService.class);
        updateIntent.setAction(WifiStateMonitoringService.ORG_DECAT_QWS_INTENT_ACTION_UPDATE_SERVICE);
//        context.startService(updateIntent);
    }
}
