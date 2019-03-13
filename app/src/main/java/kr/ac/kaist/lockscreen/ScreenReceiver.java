package kr.ac.kaist.lockscreen;

//화면이 켜졌을 때 ACTION_SCREEN_OFF intent 를 받는다.

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.Calendar;


public class ScreenReceiver extends BroadcastReceiver {

    public static final String TAG = "ScreenReceiver";
    private DatabaseHelper db;
    protected SharedPreferences sharedPref = null;
    protected SharedPreferences.Editor sharedPrefEditor = null;
    private Context context;

    @Override
    public void onReceive(Context con, Intent intent) {

        db = new DatabaseHelper(context);
        context = con;

        sharedPref = context.getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();


        int focus = sharedPref.getInt("FocusMode", -1);

        //pref_other = context.getSharedPreferences("OtherApp", Activity.MODE_PRIVATE); //다른 앱(홈화면 포함) 실행 중인가?
        //editor_other = pref_other.edit();


        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            sharedPrefEditor.putInt("Typing", 1);
            sharedPrefEditor.apply();

            if (focus == 1) {
                Log.d(TAG, "The smartphone screen is on (timer expired)");
                Intent i = new Intent(context, LockScreen.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            //SharedPreferences pref_other = context.getSharedPreferences("OtherApp",Context.MODE_PRIVATE);
            int flag = sharedPref.getInt("Flag", -1);
            //int otherApp = pref_other.getInt("OtherApp",-1);

            sharedPrefEditor.putInt("Typing", 0);
            sharedPrefEditor.apply();

            Log.d(TAG, "Smartphone screen is OFF" + String.valueOf(flag));

            if (flag != 1 || focus == 0) { // 만약에 잠금 화면에서 화면이 꺼진 것이라면 reset하지 않는다. 그리고 timer가 trigger되지 않았으면.
                final Intent intentService = new Intent(context, CountService.class);
                sharedPrefEditor.putInt("Flag", 0);
                sharedPrefEditor.apply();
                context.stopService(intentService);
                context.startService(intentService);
            }

            /*
            if(otherApp == 1 && focus == 0){
                final Intent intentService = new Intent(context, CountService.class);
                editor_flag.putInt("Flag",0);
                editor_flag.commit();
                context.stopService(intentService);
                context.startService(intentService);
            }
            editor_other.putInt("OtherApp",0);
            editor_other.commit();
            */
        }

        if (intent.getAction().equals("kr.ac.kaist.lockscreen.shake")) {
            Log.d(TAG, "Shake (movement detected)");

            //State Type 1 -> movement
            Calendar calStart = Calendar.getInstance();
            Calendar calEnd = Calendar.getInstance();
            long start_time = sharedPref.getLong("data_start_timestamp", -1);
            long end_time = System.currentTimeMillis();
            long duration = end_time - start_time;
            calStart.setTimeInMillis(start_time);
            calEnd.setTimeInMillis(end_time);

            db = new DatabaseHelper(context); //reinit DB
            submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), (int) (duration / 1000), (short) 1, (int) 0, "", 0, "", "");

        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, CountService.class);
            context.startService(i);
        }
    }

    public void submitRawData(long start_time, long end_time, int duration, short type, int location_img_id, String location_txt, int activity_img_id, String activity_txt, String distraction) {
        if (Tools.isNetworkAvailable(context)) {
            Tools.executeForService(new MyServiceRunnable(
                    (Service) context,
                    context.getString(R.string.url_server, context.getString(R.string.server_ip)),
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
                        switch (result) {
                            case Tools.RES_OK:
                                Log.d(TAG, "Submitted");
                                restartServiceFinishActivity();
                                break;
                            case Tools.RES_FAIL:
                                Log.d(TAG, "Failed to submi");
                                break;
                            case Tools.RES_SRV_ERR:
                                Log.d(TAG, "Failed to sign up. (SERVER SIDE ERROR)");
                                break;
                            default:
                                break;
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                }
            });
        } else {
            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_img_id, location_txt, activity_img_id, activity_txt, distraction);

            if (isInserted) {
                Toast.makeText(context, "State saved", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show();

            restartServiceFinishActivity();

        }
    }

    public void restartServiceFinishActivity() {

        sharedPrefEditor.putInt("FocusMode", 0);
        sharedPrefEditor.apply();

        final Intent intentService = new Intent(context, CountService.class);
        context.stopService(intentService);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        context.startService(intentService);

        Intent intent = new Intent("finisher");
        context.sendBroadcast(intent);     // send custom broadcast to Finisher activity to finish the activity

    }
}


