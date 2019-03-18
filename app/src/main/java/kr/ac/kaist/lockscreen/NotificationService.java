package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.net.MalformedURLException;
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

            db = new DatabaseHelper(this); //reinit DB
            submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), (int) (duration / 1000), (short) 1, 0, "", 0, "", "");

        }

    }

    public void submitRawData(long start_time, long end_time, int duration, short type, int location_img_id, String location_txt, int activity_img_id, String activity_txt, String distraction) {
        if (Tools.isNetworkAvailable(context)) {
            Tools.executeForApplication(new MyApplicationRunnable(
                    (Application) context,
                    getString(R.string.url_server, getString(R.string.server_ip)),
                    SignInActivity.loginPrefs.getString(SignInActivity.email, null),
                    start_time,
                    end_time,
                    duration,
                    type,
                    location_img_id,
                    location_txt,
                    activity_img_id,
                    activity_txt,
                    distraction

            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String email = (String) args[1];
                    long start_time = (long) args[2];
                    long end_time = (long) args[3];
                    int duration = (int) args[4];
                    short type = (short) args[5];
                    int location_img_id = (int) args[6];
                    String location_txt = (String) args[7];
                    int activity_img_id = (int) args[8];
                    String activity_txt = (String) args[9];
                    String distraction = (String) args[10];

                    PHPRequest request;
                    try {
                        request = new PHPRequest(url);
                        String result = request.PhPtest(PHPRequest.SERV_CODE_ADD_RD, email, String.valueOf(type), location_txt, String.valueOf(location_img_id), activity_txt, String.valueOf(activity_img_id), String.valueOf(start_time), String.valueOf(end_time), String.valueOf(duration), String.valueOf(distraction));
                        if (result == null) {
                            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_img_id, location_txt, activity_img_id, activity_txt, distraction);

                            if (isInserted)
                                Log.d(TAG, "State saved to local");
                            else
                                Log.d(TAG, "Failed to save to local");

                            restartServiceAndGoHome();
                        } else {
                            switch (result) {
                                case Tools.RES_OK:
                                    Log.d(TAG, "Submitted");
                                    restartServiceAndGoHome();
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
            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_img_id, location_txt, activity_img_id, activity_txt, distraction);

            if (isInserted) {
                Toast.makeText(getApplicationContext(), "State saved", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Failed to save", Toast.LENGTH_SHORT).show();

            restartServiceAndGoHome();

        }
        sharedPrefEditor.putInt("Flag", 0);
        sharedPrefEditor.apply();
    }

    public void restartServiceAndGoHome() {
        stopService(intentService);
        startService(intentService);
        sharedPrefEditor.putInt("FocusMode", 0);
        sharedPrefEditor.apply();
    }
}