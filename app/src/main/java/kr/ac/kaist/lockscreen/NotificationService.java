package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;


public class NotificationService extends NotificationListenerService {

    public static final String TAG = "NotificationService";
    Context context;
    private DatabaseHelper db;
    private SharedPreferences sharedPref = null;
    private SharedPreferences.Editor sharedPrefEditor = null;

    private Intent intentService;

    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();
        db = new DatabaseHelper(this); // init database

        sharedPref = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        //Service
        intentService = new Intent(this, CountService.class);

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
            //State Type 1 -> notification click
            Calendar calStart = Calendar.getInstance();
            Calendar calEnd = Calendar.getInstance();
            long start_time = sharedPref.getLong("data_start_timestamp", -1);
            long end_time = System.currentTimeMillis();
            long duration = end_time - start_time;
            calStart.setTimeInMillis(start_time);
            calEnd.setTimeInMillis(end_time);

            boolean isInserted = db.insertRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), (int) (duration / 1000), (short) 1, 0, "", 0, "", "");

            if (isInserted) {
                Toast.makeText(getApplicationContext(), "State saved", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Failed to save", Toast.LENGTH_SHORT).show();

            stopService(intentService);
            startService(intentService);
            sharedPrefEditor.putInt("FocusMode", 0);
            sharedPrefEditor.apply();
        }

    }
}