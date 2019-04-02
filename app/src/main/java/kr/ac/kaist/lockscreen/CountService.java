package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.net.MalformedURLException;
import java.util.Calendar;

import static kr.ac.kaist.lockscreen.App.CHANNEL_1_ID;
import static kr.ac.kaist.lockscreen.App.notification_pass_time_limit;
import static kr.ac.kaist.lockscreen.App.trigger_duration_in_second;

public class CountService extends Service implements SensorEventListener {

    public static final String TAG = "CountService";

    private SharedPreferences sharedPrefModes = null;
    private SharedPreferences.Editor sharedPrefModesEditor = null;
    private SharedPreferences sharedPrefLaterState = null;
    private SharedPreferences.Editor sharedPrefLaterStateEditor = null;
    private BroadcastReceiver mReceiver;

    private DatabaseHelper db;

    private boolean isStop = false;
    private boolean shake_flag;
    private int shake_time = 0;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity

    Notification notificationServiceRunning;

    private final IBinder mBinder = new LocalBinder();

    class LocalBinder extends Binder {
        CountService getService() {
            return CountService.this;
        }
    }

    public CountService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHelper(this); // init database
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("kr.ac.kaist.lockscreen.shake");
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        shake_time = (int) (System.currentTimeMillis() / 1000);
        //Log.d(TAG, "Shake time: " + shake_time);
        shake_flag = true;

        //Log.i("test", "서비스 시작");
        sharedPrefModes = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefModesEditor = sharedPrefModes.edit();

        sharedPrefLaterState = getSharedPreferences("LaterState", Activity.MODE_PRIVATE);
        sharedPrefLaterStateEditor = sharedPrefLaterState.edit();

        sharedPrefModesEditor.putInt("StartService", (int) (System.currentTimeMillis() / 1000));
        sharedPrefModesEditor.apply();

        sharedPrefModesEditor.putInt("Shake", 0);
        sharedPrefModesEditor.apply();

        //Log.d(TAG, "current time: " + String.valueOf((int) (System.currentTimeMillis() / 1000)));
        //Log.d(TAG, "startService time: " + String.valueOf(sharedPrefModes.getInt("StartService", -1)));

        //counter start
        isStop = false;
        Thread counter = new Thread(new Counter());
        counter.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
        //return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI, new Handler());

        //노티바 고정 띄우기
        notificationServiceRunning = new NotificationCompat.Builder(CountService.this, CHANNEL_1_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(1, notificationServiceRunning);

        //Screen receiver로부터 Screen On/OFF event를 받을 수 있음
        if (intent == null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction("kr.ac.kaist.lockscreen.shake");
            mReceiver = new ScreenReceiver();
            registerReceiver(mReceiver, filter);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        mAccelLast = mAccelCurrent;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = mAccelCurrent - mAccelLast;
        mAccel = mAccel * 0.9f + delta;

        if (mAccel > 6 && sharedPrefModes.getInt("Shake", -1) == 1 && sharedPrefModes.getInt("Typing", -1) == 0) {
            sharedPrefModesEditor.putInt("Shake", 0);
            sharedPrefModesEditor.apply();

            sharedPrefModesEditor.putInt("Shaked", 1);
            sharedPrefModesEditor.apply();

            shake_time = (int) (System.currentTimeMillis() / 1000);
            sendBroadcast(new Intent("kr.ac.kaist.lockscreen.shake"));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service termination");
        isStop = true;
        shake_flag = false;
        unregisterReceiver(mReceiver);
    }

    private class Counter implements Runnable {
        //private Handler handler = new Handler();

        @Override
        public void run() {
            int serviceStart_time = sharedPrefModes.getInt("StartService", -1);

            //Log.i(TAG, "StartService2 time: " + String.valueOf(serviceStart_time));

            sharedPrefModesEditor.putInt("Count", 0);
            sharedPrefModesEditor.apply();

            while (!isStop) {
                int currentTime = (int) (System.currentTimeMillis() / 1000);

                //if delayed lockscreen notification is not pressed within 5 minutes then save state Type 2 -> cancel
                long time_difference = (Calendar.getInstance().getTimeInMillis() / 1000) - (sharedPrefLaterState.getLong("Timestamp", -1) / 1000);
                if (sharedPrefLaterState.getInt("Time_Passed", -1) == 0) {
                    if (time_difference > notification_pass_time_limit) {
                        sharedPrefLaterStateEditor.putInt("Time_Passed", 1);
                        sharedPrefLaterStateEditor.apply();


                        Calendar calStart = Calendar.getInstance();
                        Calendar calEnd = Calendar.getInstance();
                        long end_time = sharedPrefLaterState.getLong("Timestamp", -1);
                        calEnd.setTimeInMillis(end_time);
                        calStart.setTimeInMillis(end_time - (sharedPrefLaterState.getInt("Duration", -1) * 1000));
                        Log.e(TAG, "HERE");
                        submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), sharedPrefLaterState.getInt("Duration", -1), (short) 2, "", "", "");
                        sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", sharedPrefModes.getInt("Total_displayed_surveys_cnt", -1) + 1);
                        sharedPrefModesEditor.apply();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if ((currentTime - shake_time) > 5) {
                    sharedPrefModesEditor.putInt("Shake", 1);
                    sharedPrefModesEditor.apply();
                }

                if ((currentTime - serviceStart_time) > trigger_duration_in_second) {
                    if (sharedPrefModes.getInt("FocusMode", -1) != 1) {
                        sharedPrefModesEditor.putInt("FocusMode", 1);
                        sharedPrefModesEditor.apply();
                        sharedPrefModesEditor.putInt("Count", (int) System.currentTimeMillis() / 1000);
                        sharedPrefModesEditor.apply();
                        sharedPrefModesEditor.putLong("data_start_timestamp", System.currentTimeMillis());
                        sharedPrefModesEditor.apply();
                        Log.d(TAG, "Focus mode started");
                    }
                } else {
                    sharedPrefModesEditor.putInt("FocusMode", 0);
                    sharedPrefModesEditor.apply();
                    Log.d(TAG, "Not in Focus mode");
                }
            }
            //Log.i("test", "while 끝남");
        }
    }

    public void submitRawData(long start_time, long end_time, int duration, short type, String location_txt, String activity_txt, String otherESMResponse) {
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


