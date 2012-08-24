package org.decat.qws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WifiStateChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent updateIntent = new Intent(context, WifiStateMonitoringService.class);
        updateIntent.setAction(WifiStateMonitoringService.ORG_DECAT_QWS_INTENT_ACTION_UPDATE_SERVICE);
        context.startService(updateIntent);
    }
}
