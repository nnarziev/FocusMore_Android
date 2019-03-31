package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static kr.ac.kaist.lockscreen.App.CHANNEL_1_ID;

public class CountService extends Service implements SensorEventListener {

    public static final String TAG = "CountService";

    private SharedPreferences sharedPref = null;
    private SharedPreferences.Editor sharedPrefEditor = null;
    private BroadcastReceiver mReceiver;

    private boolean isStop = false;
    private int trigger_duration_in_second = 3;
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
        sharedPref = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        sharedPrefEditor.putInt("StartService", (int) (System.currentTimeMillis() / 1000));
        sharedPrefEditor.apply();

        sharedPrefEditor.putInt("Shake", 0);
        sharedPrefEditor.apply();

        //Log.d(TAG, "current time: " + String.valueOf((int) (System.currentTimeMillis() / 1000)));
        //Log.d(TAG, "startService time: " + String.valueOf(sharedPref.getInt("StartService", -1)));

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

        if (mAccel > 6 && sharedPref.getInt("Shake", -1) == 1 && sharedPref.getInt("Typing", -1) == 0) {
            sharedPrefEditor.putInt("Shake", 0);
            sharedPrefEditor.apply();

            sharedPrefEditor.putInt("Shaked", 1);
            sharedPrefEditor.apply();

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
            int serviceStart_time = sharedPref.getInt("StartService", -1);

            //Log.i(TAG, "StartService2 time: " + String.valueOf(serviceStart_time));

            sharedPrefEditor.putInt("Count", 0);
            sharedPrefEditor.apply();

            while (!isStop) {
                int currentTime = (int) (System.currentTimeMillis() / 1000);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if ((currentTime - shake_time) > 5) {
                    sharedPrefEditor.putInt("Shake", 1);
                    sharedPrefEditor.apply();
                }

                if ((currentTime - serviceStart_time) > trigger_duration_in_second) {
                    if (sharedPref.getInt("FocusMode", -1) != 1) {
                        sharedPrefEditor.putInt("FocusMode", 1);
                        sharedPrefEditor.apply();
                        sharedPrefEditor.putInt("Count", (int) System.currentTimeMillis() / 1000);
                        sharedPrefEditor.apply();
                        sharedPrefEditor.putLong("data_start_timestamp", System.currentTimeMillis());
                        sharedPrefEditor.apply();
                        Log.d(TAG, "Focus mode started");
                    }
                } else {
                    sharedPrefEditor.putInt("FocusMode", 0);
                    sharedPrefEditor.apply();
                    Log.d(TAG, "Not in Focus mode");
                }
            }
            //Log.i("test", "while 끝남");
        }
    }
}


