package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import static kr.ac.kaist.lockscreen.Adapters.GridAdapter.ADD_NEW_ITEM_TAG;
import static kr.ac.kaist.lockscreen.App.notification_pass_time_limit;
import static kr.ac.kaist.lockscreen.DatabaseHelper.ACTIVITIES;
import static kr.ac.kaist.lockscreen.DatabaseHelper.LOCATIONS;

public class LockScreen extends AppCompatActivity {
    public static final String TAG = "LockScreen";

    private TextView txtCurrentTime;
    private TextView txtTimer;
    private RadioGroup rgLocations;
    private RadioGroup rgActivity;
    private GridView gvLocations;
    private GridView gvActivity;
    private SeekBar seekBarQ1;
    private SeekBar seekBarQ2;
    private SeekBar seekBarQ3;
    private EditText editTextQ4;

    private ImageView imgArrowLocation;
    private ImageView imgArrowActivity;

    private int drawableArrowDown;
    private int drawableArrowUp;

    static final String addTxt = "Add new";
    //endregion

    // region Constants
    enum Action {
        ACTION_HOME_CLIKC,
        ACTION_NOTIFICATION_CLIKC,
        ACTION_BUTTON_CLIKC
    }
    // endregion

    //region Variables
    private boolean isService = false; // 서비스 중인 확인용
    private boolean isStop = false;
    private boolean ratio_flag = false;
    private int difference_time;
    private String isFocusing = "-1";
    static Action action;
    private NotificationHelper mNotificationHelper;

    List<String> titlesLocations;
    List<String> titlesActivity;

    Map<String, Integer> durationsLocations;
    Map<String, Integer> durationsActivity;
    //endregion

    private SharedPreferences sharedPrefModes = null;
    private SharedPreferences.Editor sharedPrefModesEditor = null;
    private SharedPreferences sharedPrefLaterState = null;
    private SharedPreferences.Editor sharedPrefLaterStateEditor = null;
    private CountService myService;
    private Intent intentService;
    private Thread myThread = null;
    private static Handler handler;

    private DatabaseHelper db;

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CountService.LocalBinder mb = (CountService.LocalBinder) service;
            myService = mb.getService();
            isService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        //region Making window full sized and to show up when locked
        Window win = getWindow();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        /*
            FLAG_SHOW_WHEN_LOCKED tells Android to display this activity above the Android default lock screen
            FLAG_DISMISS_KEYGUARD is to make Android default lock screen disappear
        */

        //endregion
        db = new DatabaseHelper(this); // init database
        //db.testDB();

        sharedPrefModes = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefModesEditor = sharedPrefModes.edit();

        sharedPrefLaterState = getSharedPreferences("LaterState", Activity.MODE_PRIVATE);
        sharedPrefLaterStateEditor = sharedPrefLaterState.edit();

        mNotificationHelper = new NotificationHelper(this);

        //Service
        intentService = new Intent(this, CountService.class);

        initUIVars();
        initLocations(); //init all available location
        initActivities(); //init all available activities

        if (!getIntent().getBooleanExtra("LaterNotification", false)) {
            isFocusing = "-1";

            //Start the service
            startService(intentService);

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    updateThread();
                }

            };
        } else {
            Calendar curTime = Calendar.getInstance();
            long time_difference = (curTime.getTimeInMillis() / 1000) - (sharedPrefLaterState.getLong("Timestamp", -1) / 1000);
            if (time_difference > notification_pass_time_limit) {
                Toast.makeText(this, "5분이 경과되어 설문에 참여할 수 없습니다. 다음에는 꼭 참여 부탁드립니다!", Toast.LENGTH_LONG).show();
                Intent intentHome = new Intent(Intent.ACTION_MAIN); //태스크의 첫 액티비티로 시작
                intentHome.addCategory(Intent.CATEGORY_HOME);   //홈화면 표시
                intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //새로운 태스크를 생성하여 그 태스크안에서 액티비티 추가
                finish();
                startActivity(intentHome); // Start the home activity
            } else {
                stopService(intentService);
                initLaterStateFromNotif();
            }
        }
        sharedPrefModesEditor.putInt("Shaked", 0);
        sharedPrefModesEditor.apply();


    }

    private void initUIVars() {

        //region UI variables
        txtCurrentTime = findViewById(R.id.current_time);
        txtTimer = findViewById(R.id.timer);
        rgLocations = findViewById(R.id.rg_locations);
        gvLocations = findViewById(R.id.gv_locations);
        rgActivity = findViewById(R.id.rg_activity);
        gvActivity = findViewById(R.id.gv_activity);
        seekBarQ1 = findViewById(R.id.question_1);  //init SeekBar for answer from question 1
        seekBarQ2 = findViewById(R.id.question_2);
        seekBarQ3 = findViewById(R.id.question_3);
        editTextQ4 = findViewById(R.id.question_4); //init EditText for answer from question 4

        //drawable of icons for arrow (fold/unfold)
        drawableArrowDown = getResources().getIdentifier("ic_more_down", "drawable", getApplicationContext().getPackageName());
        drawableArrowUp = getResources().getIdentifier("ic_more_up", "drawable", getApplicationContext().getPackageName());
        //endregion

        //region Showing the progress in response rate
        Calendar curDate = Calendar.getInstance();
        Calendar surveysDate = Calendar.getInstance();
        surveysDate.setTimeInMillis(sharedPrefModes.getLong("Surveys_cnt_date", -1));
        if (surveysDate.get(Calendar.DAY_OF_MONTH) != curDate.get(Calendar.DAY_OF_MONTH)) {
            sharedPrefModesEditor.putLong("Surveys_cnt_date", curDate.getTimeInMillis());
            sharedPrefModesEditor.apply();
            sharedPrefModesEditor.putInt("Total_responded_surveys_cnt", 0);
            sharedPrefModesEditor.apply();
            sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", 0);
            sharedPrefModesEditor.apply();
        } else {
            sharedPrefModesEditor.putInt("Total_responded_surveys_cnt", sharedPrefModes.getInt("Total_responded_surveys_cnt", 0));
            sharedPrefModesEditor.apply();
            sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", sharedPrefModes.getInt("Total_displayed_surveys_cnt", 0));
            sharedPrefModesEditor.apply();
        }

        int total_responded = sharedPrefModes.getInt("Total_responded_surveys_cnt", -1);
        int total_displayed = sharedPrefModes.getInt("Total_displayed_surveys_cnt", -1);
        float perc = 0;
        if (total_displayed == 0)
            perc = ((float) total_responded / (total_displayed + 1));
        else
            perc = ((float) total_responded / total_displayed);
        TextView txtSurveyMetrics = findViewById(R.id.txt_survey_metrics);
        txtSurveyMetrics.setText("오늘의 설문 응답률 " + (int) (perc * 100) + "%\n(설문 출력: " + total_displayed + "회, 설문 응답: " + total_responded + "회)");
        //endregion


    }

    private void initLaterStateFromNotif() {
        Toast.makeText(this, "Will init UI", Toast.LENGTH_SHORT).show();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(sharedPrefLaterState.getLong("Timestamp", -1));
        String sDate = c.get(Calendar.MONTH) + 1 + "월"
                + c.get(Calendar.DAY_OF_MONTH) + "일 "
                + c.get(Calendar.HOUR_OF_DAY) + ":"
                + String.format(Locale.ENGLISH, "%02d", c.get(Calendar.MINUTE));
        txtCurrentTime.setText(sDate);

        difference_time = sharedPrefLaterState.getInt("Duration", -1);
        int hour = 0;
        int min = 0;
        int sec = 0;
        if (difference_time > 0) {
            if (difference_time < 60) {
                sec = difference_time;
                txtTimer.setText(String.format(Locale.ENGLISH, "%d%s", sec, getString(R.string.seconds)));
            } else if (difference_time < 3600) {
                min = difference_time / 60;
                sec = difference_time % 60;
                txtTimer.setText(String.format(Locale.ENGLISH, "%d%s %d%s", min, getString(R.string.min), sec, getString(R.string.seconds)));
            } else {
                hour = difference_time / 3600;
                min = (difference_time % 3600) / 60;
                sec = difference_time % 60;
                txtTimer.setText(String.format(Locale.ENGLISH, "%d%s %d%s %d%s", hour, getString(R.string.hours), min, getString(R.string.min), sec, getString(R.string.seconds)));
            }
        } else {
            txtTimer.setText("잠금 모드 해제!");
        }

    }

    public void initLocations() {
        titlesLocations = new ArrayList<>();
        durationsLocations = new HashMap<>();

        //Init Locations
        Cursor res = db.getAllUserData(LOCATIONS);
        if (res.getCount() == 0) {
            Toast.makeText(this, "No data for Locations yet", Toast.LENGTH_LONG).show();
        }

        while (res.moveToNext()) {
            if (res.getShort(2) == 1) {
                titlesLocations.add(res.getString(1));
                durationsLocations.put(res.getString(1), res.getInt(3));
            }
        }

        //Only for 'add new' button
        titlesLocations.add(addTxt);

        //init radio group
        int indexOfLocations = 0;
        for (; indexOfLocations < rgLocations.getChildCount() - 1; indexOfLocations++) {
            RadioButton rb = ((RadioButton) rgLocations.getChildAt(indexOfLocations));
            rb.setText(titlesLocations.get(0));
            titlesLocations.remove(0);
        }

        Adapters.GridAdapter adapterLocations = new Adapters.GridAdapter(LockScreen.this, titlesLocations);

        final RadioButton moreBtnLocation = findViewById(R.id.location_btn_4);
        imgArrowLocation = findViewById(R.id.arrow_location);

        gvLocations.setAdapter(adapterLocations);
        gvLocations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LinearLayout layout = (LinearLayout) view;
                Log.d("CLICKED", "Clicked button: " + ((TextView) layout.getChildAt(0)).getText().toString());

                if ((int) layout.getTag() == ADD_NEW_ITEM_TAG) {
                    action = Action.ACTION_BUTTON_CLIKC;
                    Log.d(TAG, "NEW BUTTON CLICKED");
                    Intent intent = new Intent(LockScreen.this, Location_Activity_List.class);
                    intent.putExtra("itemFor", LOCATIONS);
                    startActivity(intent);
                } else {
                    gvLocations.setVisibility(View.GONE);
                    moreBtnLocation.setText(titlesLocations.get(i));
                    moreBtnLocation.setChecked(true);
                    imgArrowLocation.setImageResource(drawableArrowDown);
                }

            }
        });

        rgLocations.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = rgLocations.findViewById(checkedId);
                int index = rgLocations.indexOfChild(radioButton); // Index of the pressed radio button (0~3)
                // Add logic here
                Log.d(TAG, "Button id: " + index);
                switch (index) {
                    case 0: // 1st radio button
                        gvLocations.setVisibility(View.GONE);
                        imgArrowLocation.setImageResource(drawableArrowDown);
                        break;
                    case 1: // 2nd radio button
                        gvLocations.setVisibility(View.GONE);
                        imgArrowLocation.setImageResource(drawableArrowDown);
                        break;
                    case 2: // 3rd radio button
                        gvLocations.setVisibility(View.GONE);
                        imgArrowLocation.setImageResource(drawableArrowDown);
                        break;
                    case 3: // 4th radio button
                        imgArrowLocation.setImageResource(drawableArrowUp);
                        if (radioButton.getText().equals(getString(R.string.etc))) {
                            radioButton.setChecked(false);
                        } else {
                            radioButton.setChecked(true);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void initActivities() {
        titlesActivity = new ArrayList<>();
        durationsActivity = new HashMap<>();

        //Init Activities
        Cursor res = db.getAllUserData(ACTIVITIES);
        if (res.getCount() == 0) {
            Toast.makeText(this, "No data for Activities yet", Toast.LENGTH_LONG).show();
        }

        while (res.moveToNext()) {
            if (res.getShort(2) == 1) {
                titlesActivity.add(res.getString(1));
                durationsActivity.put(res.getString(1), res.getInt(3));
            }
        }

        //Only for add new button
        titlesActivity.add(addTxt);

        //init radio group
        int indexOfActivitiy = 0;
        for (; indexOfActivitiy < rgActivity.getChildCount() - 1; indexOfActivitiy++) {
            RadioButton rb = ((RadioButton) rgActivity.getChildAt(indexOfActivitiy));
            rb.setText(titlesActivity.get(0));
            titlesActivity.remove(0);
        }

        Adapters.GridAdapter adapterActivity = new Adapters.GridAdapter(LockScreen.this, titlesActivity);

        final RadioButton moreBtnActivity = findViewById(R.id.activity_btn_4);
        imgArrowActivity = findViewById(R.id.arrow_activity);

        gvActivity.setAdapter(adapterActivity);
        gvActivity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LinearLayout layout = (LinearLayout) view;
                Log.d("CLICKED", "Clicked button: " + ((TextView) layout.getChildAt(0)).getText().toString());

                if ((int) layout.getTag() == ADD_NEW_ITEM_TAG) {
                    action = Action.ACTION_BUTTON_CLIKC;
                    ;
                    Log.d(TAG, "NEW BUTTON CLICKED");
                    Intent intent = new Intent(LockScreen.this, Location_Activity_List.class);
                    intent.putExtra("itemFor", ACTIVITIES);
                    startActivity(intent);
                } else {
                    gvActivity.setVisibility(View.GONE);
                    moreBtnActivity.setText(titlesActivity.get(i));
                    moreBtnActivity.setChecked(true);
                    imgArrowActivity.setImageResource(drawableArrowDown);
                }
            }
        });

        rgActivity.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = rgActivity.findViewById(checkedId);
                int index = rgActivity.indexOfChild(radioButton); // Index of the pressed radio button (0~3)
                Log.d(TAG, "Button id: " + index);
                switch (index) {
                    case 0: // 1st radio button
                        gvActivity.setVisibility(View.GONE);
                        imgArrowActivity.setImageResource(drawableArrowDown);
                        break;
                    case 1: // 2nd radio button
                        gvActivity.setVisibility(View.GONE);
                        imgArrowActivity.setImageResource(drawableArrowDown);
                        break;
                    case 2: // 3rd radio button
                        gvActivity.setVisibility(View.GONE);
                        imgArrowActivity.setImageResource(drawableArrowDown);
                        break;
                    case 3: // 4th radio button
                        imgArrowActivity.setImageResource(drawableArrowUp);
                        if (radioButton.getText().equals(getString(R.string.etc))) {
                            radioButton.setChecked(false);
                        } else {
                            radioButton.setChecked(true);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void updateThread() {
        Calendar c = Calendar.getInstance();
        String sDate = c.get(Calendar.MONTH) + 1 + "월"
                + c.get(Calendar.DAY_OF_MONTH) + "일 "
                + c.get(Calendar.HOUR_OF_DAY) + ":"
                + String.format(Locale.ENGLISH, "%02d", c.get(Calendar.MINUTE));
        txtCurrentTime.setText(sDate);

        int previous_time = sharedPrefModes.getInt("Count", -1); //becomes 1 when in Focus Mode
        int current_time = (int) System.currentTimeMillis() / 1000;
        if (sharedPrefModes.getInt("Shaked", -1) == 0 && sharedPrefModes.getInt("FocusMode", -1) == 1) {
            difference_time = current_time - previous_time;
            int hour = 0;
            int min = 0;
            int sec = 0;

            if (difference_time > 0) {
                if (difference_time < 60) {
                    sec = difference_time;
                    txtTimer.setText(String.format(Locale.ENGLISH, "%d%s", sec, getString(R.string.seconds)));
                } else if (difference_time < 3600) {
                    min = difference_time / 60;
                    sec = difference_time % 60;
                    txtTimer.setText(String.format(Locale.ENGLISH, "%d%s %d%s", min, getString(R.string.min), sec, getString(R.string.seconds)));
                } else {
                    hour = difference_time / 3600;
                    min = (difference_time % 3600) / 60;
                    sec = difference_time % 60;
                    txtTimer.setText(String.format(Locale.ENGLISH, "%d%s %d%s %d%s", hour, getString(R.string.hours), min, getString(R.string.min), sec, getString(R.string.seconds)));
                }
            } else {
                txtTimer.setText("잠금 모드 해제!");
            }
        }
    }

    public void restartServiceAndFinishActivity() {

        //region Restart the service
        intentService = new Intent(this, CountService.class);
        stopService(intentService);
        startService(intentService);
        Log.e(TAG, "restartServiceAndFinishActivity: ");
        sharedPrefModesEditor.putInt("FocusMode", 0);
        sharedPrefModesEditor.apply();
        //endregion

        if (action != Action.ACTION_NOTIFICATION_CLIKC) {
            final Intent intentHome = new Intent(Intent.ACTION_MAIN); //태스크의 첫 액티비티로 시작
            intentHome.addCategory(Intent.CATEGORY_HOME);   //홈화면 표시
            intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //새로운 태스크를 생성하여 그 태스크안에서 액티비티 추가
            finish();
            startActivity(intentHome); // Start the home activity
        }

    }

    public void laterClicked(View view) {
        action = Action.ACTION_BUTTON_CLIKC;

        //region Saving later state in shared pref
        Calendar curTime = Calendar.getInstance();
        //if touching of "Later" button is pressed second time after notification opening notification don't save the state (Save only when pressed first time)
        if (!getIntent().getBooleanExtra("LaterNotification", false)) {
            sharedPrefLaterStateEditor.putInt("Duration", difference_time);
            sharedPrefLaterStateEditor.apply();
            sharedPrefLaterStateEditor.putLong("Timestamp", curTime.getTimeInMillis());
            sharedPrefLaterStateEditor.apply();
            sharedPrefLaterStateEditor.putInt("Time_Passed", 0);
            sharedPrefLaterStateEditor.apply();
        }
        //endregion

        NotificationCompat.Builder nb = mNotificationHelper.getChannel2Notification(getString(R.string.app_name), "설문 화면으로 이동 (5분 안에 설문에 참여해 주세요!)");
        mNotificationHelper.getManager().notify(2, nb.build());

        restartServiceAndFinishActivity();
        sharedPrefModesEditor.putInt("Shaked", 0);
        sharedPrefModesEditor.apply();
    }

    public void etcClicked(View view) {
        switch (view.getId()) {
            case R.id.location_btn_4:
                if (gvLocations.getVisibility() == View.GONE) {
                    gvLocations.setVisibility(View.VISIBLE);
                    imgArrowLocation.setImageResource(drawableArrowUp);
                } else {
                    gvLocations.setVisibility(View.GONE);
                    imgArrowLocation.setImageResource(drawableArrowDown);
                }
                break;
            case R.id.activity_btn_4:
                if (gvActivity.getVisibility() == View.GONE) {
                    gvActivity.setVisibility(View.VISIBLE);
                    imgArrowActivity.setImageResource(drawableArrowUp);
                } else {
                    gvActivity.setVisibility(View.GONE);
                    imgArrowActivity.setImageResource(drawableArrowDown);
                }
                break;
            default:
                break;
        }
    }

    public void saveClicked(View view) {
        action = Action.ACTION_BUTTON_CLIKC;

        //State Type 3 -> ideal case
        RadioButton chosenLocationRB = findViewById(rgLocations.getCheckedRadioButtonId());
        RadioButton chosenActivityRB = findViewById(rgActivity.getCheckedRadioButtonId());

        if (chosenActivityRB == null || chosenLocationRB == null) {
            Toast.makeText(getApplicationContext(), "Please, choose location, activity and disturbance", Toast.LENGTH_LONG).show();
            return;
        }

        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        long start_time = sharedPrefModes.getLong("data_start_timestamp", -1);
        long end_time = System.currentTimeMillis();
        calStart.setTimeInMillis(start_time);
        calEnd.setTimeInMillis(end_time);

        String restResultData = String.format(Locale.ENGLISH, "%d%d%d%s", seekBarQ1.getProgress() + 1, seekBarQ2.getProgress() + 1, seekBarQ3.getProgress() + 1, editTextQ4.getText());
        submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 3, chosenLocationRB.getText().toString(), chosenActivityRB.getText().toString(), restResultData);

        sharedPrefLaterStateEditor.putInt("Time_Passed", 1);
        sharedPrefLaterStateEditor.apply();
        sharedPrefModesEditor.putInt("Total_responded_surveys_cnt", sharedPrefModes.getInt("Total_responded_surveys_cnt", -1) + 1);
        sharedPrefModesEditor.apply();
        sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", sharedPrefModes.getInt("Total_displayed_surveys_cnt", -1) + 1);
        sharedPrefModesEditor.apply();

        //region Updating accumulated duration time for location and activity
        for (Map.Entry<String, Integer> entry : durationsActivity.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            Log.d(TAG, key + " = " + value);
            // do stuff
        }
        for (Map.Entry<String, Integer> entry : durationsLocations.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            Log.d(TAG, key + " = " + value);
            // do stuff
        }
        boolean isUpdated;
        int total_duration = durationsLocations.get(chosenLocationRB.getText().toString()) + difference_time;
        isUpdated = db.updateUserDataAccDuration(LOCATIONS, chosenLocationRB.getText().toString(), total_duration);
        if (isUpdated) {
            total_duration = durationsActivity.get(chosenActivityRB.getText().toString()) + difference_time;
            isUpdated = db.updateUserDataAccDuration(ACTIVITIES, chosenActivityRB.getText().toString(), total_duration);
            if (isUpdated)
                Log.d(TAG, "Updated UserData accumulated time");
            else
                Log.d(TAG, "Failed to update UserData accumulated time");
        } else {
            Log.d(TAG, "Failed to update UserData accumulated time");
        }
        //endregion

    }

    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("kr.ac.kaist.lockscreen.CountService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void submitRawData(long start_time, long end_time, int duration, short type, String location_txt, String activity_txt, String otherESMResponse) {
        if (Tools.isNetworkAvailable(this)) {
            Log.d(TAG, "With connection case");
            Tools.execute(new MyRunnable(
                    this,
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
                        Log.e(TAG, "Result: " + result);
                        if (result == null) {
                            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_txt, activity_txt, otherESMResp);
                            Log.d(TAG, "Case when Server is OFF");
                            if (isInserted) {
                                Log.d(TAG, "State saved to local");
                            } else
                                Log.d(TAG, "Failed to save in local");

                            restartServiceAndFinishActivity();
                        } else {
                            switch (result) {
                                case Tools.RES_OK:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LockScreen.this, "Submitted", Toast.LENGTH_SHORT).show();
                                            restartServiceAndFinishActivity();
                                        }
                                    });
                                    break;
                                case Tools.RES_FAIL:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LockScreen.this, "Failed to submit", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    break;
                                case Tools.RES_SRV_ERR:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LockScreen.this, "Failed to sign up. (SERVER SIDE ERROR)", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    break;
                                default:
                                    break;
                            }
                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                    enableTouch();
                }
            });
        } else {
            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_txt, activity_txt, otherESMResponse);
            Log.d(TAG, "No connection case");
            if (isInserted) {
                Log.d(TAG, "State saved");
            } else
                Log.d(TAG, "Failed to save");

            restartServiceAndFinishActivity();

        }
        restartServiceAndFinishActivity();

        sharedPrefModesEditor.putInt("Flag", 0);
        sharedPrefModesEditor.apply();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i("onStart", "onStart");
        myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //sending message to main thread each second
                        handler.sendMessage(handler.obtainMessage());
                        Thread.sleep(1000);
                    } catch (Throwable t) {
                    }
                }
            }
        });
        myThread.start();
        sharedPrefModesEditor.putInt("OtherApp", 0);
        sharedPrefModesEditor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        action = Action.ACTION_HOME_CLIKC;
        initLocations();
        initActivities();

        isStop = true;


        sharedPrefModesEditor.putInt("Typing", 1);
        sharedPrefModesEditor.apply();


        sharedPrefModesEditor.putInt("Shaked", 0);
        sharedPrefModesEditor.apply();
        //Log.i("resume", "굿굿");

        sharedPrefModesEditor.putInt("Flag", 1);
        sharedPrefModesEditor.apply();


    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.i("onStop", "onStop");
        int shaked = sharedPrefModes.getInt("Shaked", -1);

        //Log.i("shaked?",String.valueOf(shaked));
        if (shaked == 1) {
            try {
                //State Type 1 -> movement
                Calendar calStart = Calendar.getInstance();
                Calendar calEnd = Calendar.getInstance();
                long start_time = sharedPrefModes.getLong("data_start_timestamp", -1);
                long end_time = System.currentTimeMillis();
                calStart.setTimeInMillis(start_time);
                calEnd.setTimeInMillis(end_time);

                Log.e(TAG, "onStop: ");
                submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 1, "", "", "");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sharedPrefModesEditor.putInt("Shaked", 0);
        sharedPrefModesEditor.apply();

        isStop = false;
        myThread.interrupt();
    }

    //Blocking back key
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return true;
    }

    @Override
    protected void onUserLeaveHint() {
        Log.e(TAG, "onUserLeaveHint: called");

        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Calendar calStart, calEnd;
                long start_time, end_time;
                switch (action) {
                    case ACTION_HOME_CLIKC:
                        Log.d(TAG, "Pressed home button!");
                        //region Saving later state in shared pref
                        Calendar curTime = Calendar.getInstance();
                        //if touching of "Later" button is pressed second time after notification opening notification don't save the state (Save only when pressed first time)
                        if (!getIntent().getBooleanExtra("LaterNotification", false)) {
                            sharedPrefLaterStateEditor.putInt("Duration", difference_time);
                            sharedPrefLaterStateEditor.apply();
                            sharedPrefLaterStateEditor.putLong("Timestamp", curTime.getTimeInMillis());
                            sharedPrefLaterStateEditor.apply();
                            sharedPrefLaterStateEditor.putInt("Time_Passed", 0);
                            sharedPrefLaterStateEditor.apply();
                        }
                        //endregion

                        NotificationCompat.Builder nb = mNotificationHelper.getChannel2Notification(getString(R.string.app_name), "설문 화면으로 이동 (5분 안에 설문에 참여해 주세요!)");
                        mNotificationHelper.getManager().notify(2, nb.build());

                        restartServiceAndFinishActivity();
                        sharedPrefModesEditor.putInt("Shaked", 0);
                        sharedPrefModesEditor.apply();
                        /*
                        //State Type 2 -> cancel
                        calStart = Calendar.getInstance();
                        calEnd = Calendar.getInstance();
                        start_time = sharedPrefModes.getLong("data_start_timestamp", -1);
                        end_time = System.currentTimeMillis();
                        calStart.setTimeInMillis(start_time);
                        calEnd.setTimeInMillis(end_time);

                        submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 2, "", "", "");
                        sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", sharedPrefModes.getInt("Total_displayed_surveys_cnt", -1) + 1);
                        sharedPrefModesEditor.apply();

                        sharedPrefModesEditor.putInt("Shaked", 0);
                        sharedPrefModesEditor.apply();
                        */
                        break;
                    case ACTION_NOTIFICATION_CLIKC:
                        int flag = sharedPrefModes.getInt("Flag", -1);
                        int focus = sharedPrefModes.getInt("FocusMode", -1);

                        Map<String, ?> allEntries = sharedPrefModes.getAll();
                        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                            Log.d(TAG, "MAP vals: " + entry.getKey() + ": " + entry.getValue().toString());
                        }

                        //Log.d(TAG, "Focus MODE: " + focus + " " + flag);
                        if (focus == 1 && flag == 1) {
                            Log.d(TAG, "FROM LOCK SCREEN");
                            //State Type 1 -> notification click
                            calStart = Calendar.getInstance();
                            calEnd = Calendar.getInstance();
                            start_time = sharedPrefModes.getLong("data_start_timestamp", -1);
                            end_time = System.currentTimeMillis();
                            long duration = end_time - start_time;
                            calStart.setTimeInMillis(start_time);
                            calEnd.setTimeInMillis(end_time);

                            db = new DatabaseHelper(LockScreen.this); //reinit DB
                            Log.e(TAG, "Submitting type 1 after notification was pressed");
                            submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), (int) (duration / 1000), (short) 1, "", "", "");  //submit raw data when notification is clicked
                            sharedPrefModesEditor.putInt("Total_displayed_surveys_cnt", sharedPrefModes.getInt("Total_displayed_surveys_cnt", -1) + 1);
                            sharedPrefModesEditor.apply();
                        } else
                            Log.d(TAG, "FROM ELSE");
                        break;
                    case ACTION_BUTTON_CLIKC:
                        Log.e(TAG, "ACTION_BUTTON_CLIKC: called");
                        break;
                    default:
                        break;
                }
            }
        });

        super.onUserLeaveHint();
    }


}