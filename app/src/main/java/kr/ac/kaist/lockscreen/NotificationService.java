package kr.ac.kaist.lockscreen;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationService extends NotificationListenerService {

    public static final String TAG = "NotificationService";
    Context context;

    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "Notification was posted");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        //If notification was clicked save the state as type1 and restart the service
        if (reason == NotificationListenerService.REASON_CLICK) {
            LockScreen.action = LockScreen.Action.ACTION_NOTIFICATION_CLIKC;
        }
    }
}