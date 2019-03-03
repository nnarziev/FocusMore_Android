package kr.ac.kaist.lockscreen;

//화면이 켜졌을 때 ACTION_SCREEN_OFF intent 를 받는다.

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;


public class ScreenReceiver extends BroadcastReceiver {

    public static final String TAG = "ScreenReceiver";
    private DatabaseHelper db;
    protected SharedPreferences sharedPref = null;
    protected SharedPreferences.Editor sharedPrefEditor = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        db = new DatabaseHelper(context);

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

            boolean isInserted = db.insertRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), (int) (duration / 1000), (short) 1, 0, "", 0, "", "");

            if (isInserted) {
                Toast.makeText(context, "State saved", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show();

            sharedPrefEditor.putInt("FocusMode", 0);
            sharedPrefEditor.apply();

            final Intent intentService = new Intent(context, CountService.class);
            context.stopService(intentService);
            context.startService(intentService);

            Intent intent_home = new Intent(Intent.ACTION_MAIN); //태스크의 첫 액티비티로 시작
            intent_home.addCategory(Intent.CATEGORY_HOME);   //홈화면 표시
            intent_home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //새로운 태스크를 생성하여 그 태스크안에서 액티비티 추가
            context.startActivity(intent_home);
        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, CountService.class);
            context.startService(i);
        }
    }
}
