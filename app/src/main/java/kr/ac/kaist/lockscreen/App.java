package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

public class App extends Application {
    public static final String CHANNEL_1_ID = "service_channel";
    public static final String CHANNEL_2_ID = "later_esm_channel";
    private SharedPreferences sharedPrefModes = null;
    private SharedPreferences.Editor sharedPrefModesEditor = null;

    public static final int trigger_duration_in_second = 180;
    public static final int screen_appear_threshold = 30;
    public static final int notification_pass_time_limit = 300;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();

        sharedPrefModes = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefModesEditor = sharedPrefModes.edit();

        Calendar curDate = Calendar.getInstance();
        sharedPrefModesEditor.putLong("Surveys_cnt_date", curDate.getTimeInMillis());
        sharedPrefModesEditor.apply();
        sharedPrefModesEditor.putInt("Total_responded_surveys_cnt", 0);
        sharedPrefModesEditor.apply();
        sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", 0);
        sharedPrefModesEditor.apply();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "channel 1",
                    NotificationManager.IMPORTANCE_LOW);
            channel1.setDescription("This is notification for service");


            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
        }
    }
}
