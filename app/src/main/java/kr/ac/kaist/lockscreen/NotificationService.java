package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.net.MalformedURLException;
import java.util.Calendar;

import static kr.ac.kaist.lockscreen.App.notification_pass_time_limit;

public class NotificationService extends NotificationListenerService {

    public static final String TAG = "NotificationService";
    Context context;

    private DatabaseHelper db;
    private SharedPreferences sharedPrefModes = null;
    private SharedPreferences.Editor sharedPrefModesEditor = null;
    private SharedPreferences sharedPrefLaterState = null;
    private SharedPreferences.Editor sharedPrefLaterStateEditor = null;

    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();

        db = new DatabaseHelper(this); // init database

        sharedPrefModes = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefModesEditor = sharedPrefModes.edit();

        sharedPrefLaterState = getSharedPreferences("LaterState", Activity.MODE_PRIVATE);
        sharedPrefLaterStateEditor = sharedPrefLaterState.edit();
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
        //if this removed notification is postponed survey notification (notification id = 2)
        if (sbn.getId() == 2) {
            if (reason == NotificationListenerService.REASON_CANCEL) {

                long time_difference = (Calendar.getInstance().getTimeInMillis() / 1000) - (sharedPrefLaterState.getLong("Timestamp", -1) / 1000);
                if (time_difference <= notification_pass_time_limit) {
                    sharedPrefLaterStateEditor.putInt("Time_Passed", 1);
                    sharedPrefLaterStateEditor.apply();
                    Calendar calStart = Calendar.getInstance();
                    Calendar calEnd = Calendar.getInstance();
                    long end_time = sharedPrefLaterState.getLong("Timestamp", -1);
                    calEnd.setTimeInMillis(end_time);
                    calStart.setTimeInMillis(end_time - (sharedPrefLaterState.getInt("Duration", -1) * 1000));

                    submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), sharedPrefLaterState.getInt("Duration", -1), (short) 2, "", "", "");
                    sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", sharedPrefModes.getInt("Total_displayed_surveys_cnt", -1) + 1);
                    sharedPrefModesEditor.apply();

                }
            }
        }
        //If notification was clicked save the state as type1 and restart the service
        if (reason == NotificationListenerService.REASON_CLICK) {
            if (sbn.getId() != 2)
                LockScreen.action = LockScreen.Action.ACTION_NOTIFICATION_CLIKC;
        }
    }

    public void submitRawData(long start_time, long end_time, int duration, short type, String
            location_txt, String activity_txt, String otherESMResponse) {
        if (Tools.isNetworkAvailable(this)) {
            Log.d(TAG, "With connection case");
            Tools.executeForService(new MyServiceRunnable(
                    getString(R.string.url_server, getString(R.string.server_ip)),
                    SignInActivity.loginPrefs.getString(SignInActivity.email, null),
                    start_time,
                    end_time,
                    duration,
                    type,
                    location_txt,
                    activity_txt,
                    otherESMResponse
            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String email = (String) args[1];
                    long start_time = (long) args[2];
                    long end_time = (long) args[3];
                    int duration = (int) args[4];
                    short type = (short) args[5];
                    String location_txt = (String) args[6];
                    String activity_txt = (String) args[7];
                    String otherESMResp = (String) args[8];

                    PHPRequest request;
                    try {
                        request = new PHPRequest(url);
                        String result = request.PhPtest(PHPRequest.SERV_CODE_ADD_RD, email, String.valueOf(type), location_txt, String.valueOf(""), activity_txt, String.valueOf(""), String.valueOf(start_time), String.valueOf(end_time), String.valueOf(duration), String.valueOf(otherESMResp)); //TODO: remove empty strings for icons
                        if (result == null) {
                            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_txt, activity_txt, otherESMResp);
                            Log.d(TAG, "Case when Server is OFF");
                            if (isInserted) {
                                Log.d(TAG, "State saved to local");
                            } else
                                Log.d(TAG, "Failed to save in local");

                            restartService();
                        } else {
                            switch (result) {
                                case Tools.RES_OK:
                                    Log.d(TAG, "Submitted");
                                    restartService();
                                    break;
                                case Tools.RES_FAIL:
                                    Log.d(TAG, "Failed to submit");
                                    break;
                                case Tools.RES_SRV_ERR:
                                    Log.d(TAG, "Failed to sign up. (SERVER SIDE ERROR)");
                                    break;
                                default:
                                    break;
                            }
                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_txt, activity_txt, otherESMResponse);
            Log.d(TAG, "No connection case");
            if (isInserted) {
                Log.d(TAG, "State saved");
            } else
                Log.d(TAG, "Failed to save");

            restartService();

        }
        restartService();
        sharedPrefModesEditor.putInt("Flag", 0);
        sharedPrefModesEditor.apply();

    }

    public void restartService() {

        //region Restart the service
        Intent intentService = new Intent(this, CountService.class);
        stopService(intentService);
        startService(intentService);
        sharedPrefModesEditor.putInt("FocusMode", 0);
        sharedPrefModesEditor.apply();
        //endregion

    }
}