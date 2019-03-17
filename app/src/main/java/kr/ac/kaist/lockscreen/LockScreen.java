package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
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

import static kr.ac.kaist.lockscreen.Adapters.GridAdapter.ADD_NEW_ITEM_TAG;
import static kr.ac.kaist.lockscreen.DatabaseHelper.ACTIVITIES;
import static kr.ac.kaist.lockscreen.DatabaseHelper.LOCATIONS;

public class LockScreen extends AppCompatActivity {
    public static final String TAG = "LockScreen";

    //region UI variables
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

    private final String addTxt = "Add new";
    private final int addIcon = R.drawable.ic_add;
    //endregion

    //region Variables
    private boolean isService = false; // 서비스 중인 확인용
    private boolean isStop = false;
    private boolean ratio_flag = false;
    private int difference_time;
    private String isFocusing = "-1";
    private boolean isHomeBtnPressed = true;

    List<String> titlesLocations;
    List<Integer> iconsLocations;
    List<String> titlesActivity;
    List<Integer> iconsActivity;

    Map<String, Integer> durationsLocations;
    Map<String, Integer> durationsActivity;
    //endregion

    private SharedPreferences sharedPref = null;
    private SharedPreferences.Editor sharedPrefEditor = null;
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

        initUIVars();
        initLocations(); //init all available location
        initActivities(); //init all available activities

        isFocusing = "-1";

        //Service
        intentService = new Intent(this, CountService.class);
        startService(intentService);

        sharedPref = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        sharedPrefEditor.putInt("Shaked", 0);
        sharedPrefEditor.apply();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                updateThread();
            }

        };
    }

    void initUIVars() {

        //region Initialize UI variables
        txtCurrentTime = findViewById(R.id.current_time);
        txtTimer = findViewById(R.id.timer);
        seekBarQ1 = findViewById(R.id.question_1);  //init SeekBar for answer from question 1
        seekBarQ2 = findViewById(R.id.question_2);
        seekBarQ3 = findViewById(R.id.question_3);
        editTextQ4 = findViewById(R.id.question_4); //init EditText for answer from question 4

        //drawable of icons for arrow (fold/unfold)
        drawableArrowDown = getResources().getIdentifier("ic_more_down", "drawable", getApplicationContext().getPackageName());
        drawableArrowUp = getResources().getIdentifier("ic_more_up", "drawable", getApplicationContext().getPackageName());
        //endregion

    }

    private void updateThread() {
        Calendar c = Calendar.getInstance();
        String sDate = c.get(Calendar.MONTH) + 1 + "월"
                + c.get(Calendar.DAY_OF_MONTH) + "일 "
                + c.get(Calendar.HOUR_OF_DAY) + ":"
                + String.format(Locale.ENGLISH, "%02d", c.get(Calendar.MINUTE));
        txtCurrentTime.setText(sDate);

        int previous_time = sharedPref.getInt("Count", -1); //becomes 1 when in Focus Mode
        int current_time = (int) System.currentTimeMillis() / 1000;
        if (sharedPref.getInt("Shaked", -1) == 0 && sharedPref.getInt("FocusMode", -1) == 1) {
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

    public void initLocations() {
        titlesLocations = new ArrayList<>();
        iconsLocations = new ArrayList<>();
        durationsLocations = new HashMap<>();

        //Init Locations
        Cursor res = db.getAllUserData(LOCATIONS);
        if (res.getCount() == 0) {
            Toast.makeText(this, "No data for Locations yet", Toast.LENGTH_LONG).show();
        }

        while (res.moveToNext()) {
            if (res.getShort(3) == 1) {
                titlesLocations.add(res.getString(1));
                iconsLocations.add(res.getInt(2));
                durationsLocations.put(res.getString(1), res.getInt(4));
            }
        }

        //Only for 'add new' button
        titlesLocations.add(addTxt);
        iconsLocations.add(addIcon);

        //init radio group icons
        rgLocations = findViewById(R.id.rg_locations);
        int indexOfLocations = 0;
        for (; indexOfLocations < rgLocations.getChildCount() - 1; indexOfLocations++) {
            RadioButton rb = ((RadioButton) rgLocations.getChildAt(indexOfLocations));
            rb.setText(titlesLocations.get(0));
            rb.setCompoundDrawablesWithIntrinsicBounds(0, iconsLocations.get(0), 0, 0);
            rb.setTag(iconsLocations.get(0)); //passing image resource id as a tag
            titlesLocations.remove(0);
            iconsLocations.remove(0);
        }

        Adapters.GridAdapter adapterLocations = new Adapters.GridAdapter(LockScreen.this, iconsLocations, titlesLocations);

        final RadioButton moreBtnLocation = findViewById(R.id.location_btn_4);
        imgArrowLocation = findViewById(R.id.arrow_location);

        gvLocations = findViewById(R.id.gv_locations);
        gvLocations.setAdapter(adapterLocations);
        gvLocations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LinearLayout layout = (LinearLayout) view;
                Log.d("CLICKED", "Clicked button: " + ((TextView) layout.getChildAt(1)).getText().toString());

                if ((int) layout.getTag() == ADD_NEW_ITEM_TAG) {
                    isHomeBtnPressed = false;
                    Log.d(TAG, "NEW BUTTON CLICKED");
                    Intent intent = new Intent(LockScreen.this, Location_Activity_List.class);
                    intent.putExtra("itemFor", LOCATIONS);
                    startActivity(intent);
                } else {
                    gvLocations.setVisibility(View.GONE);
                    moreBtnLocation.setText(titlesLocations.get(i));
                    moreBtnLocation.setCompoundDrawablesWithIntrinsicBounds(0, iconsLocations.get(i), 0, 0);
                    moreBtnLocation.setChecked(true);
                    moreBtnLocation.setTag(iconsLocations.get(i)); //passing image resource id as a tag
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
        iconsActivity = new ArrayList<>();
        durationsActivity = new HashMap<>();

        //Init Activities
        Cursor res = db.getAllUserData(ACTIVITIES);
        if (res.getCount() == 0) {
            Toast.makeText(this, "No data for Activities yet", Toast.LENGTH_LONG).show();
        }

        while (res.moveToNext()) {
            if (res.getShort(3) == 1) {
                titlesActivity.add(res.getString(1));
                iconsActivity.add(res.getInt(2));
                durationsActivity.put(res.getString(1), res.getInt(4));
            }
        }

        //Only for add new button
        titlesActivity.add(addTxt);
        iconsActivity.add(addIcon);

        //init radio group icons
        rgActivity = findViewById(R.id.rg_activity);
        int indexOfActivitiy = 0;
        for (; indexOfActivitiy < rgActivity.getChildCount() - 1; indexOfActivitiy++) {
            RadioButton rb = ((RadioButton) rgActivity.getChildAt(indexOfActivitiy));
            rb.setText(titlesActivity.get(0));
            rb.setCompoundDrawablesWithIntrinsicBounds(0, iconsActivity.get(0), 0, 0);
            rb.setTag(iconsActivity.get(0)); //passing image resource id as a tag
            titlesActivity.remove(0);
            iconsActivity.remove(0);
        }

        Adapters.GridAdapter adapterActivity = new Adapters.GridAdapter(LockScreen.this, iconsActivity, titlesActivity);

        final RadioButton moreBtnActivity = findViewById(R.id.activity_btn_4);
        imgArrowActivity = findViewById(R.id.arrow_activity);

        gvActivity = findViewById(R.id.gv_activity);
        gvActivity.setAdapter(adapterActivity);
        gvActivity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LinearLayout layout = (LinearLayout) view;
                Log.d("CLICKED", "Clicked button: " + ((TextView) layout.getChildAt(1)).getText().toString());

                if ((int) layout.getTag() == ADD_NEW_ITEM_TAG) {
                    isHomeBtnPressed = false;
                    Log.d(TAG, "NEW BUTTON CLICKED");
                    Intent intent = new Intent(LockScreen.this, Location_Activity_List.class);
                    intent.putExtra("itemFor", ACTIVITIES);
                    startActivity(intent);
                } else {
                    gvActivity.setVisibility(View.GONE);
                    moreBtnActivity.setText(titlesActivity.get(i));
                    moreBtnActivity.setCompoundDrawablesWithIntrinsicBounds(0, iconsActivity.get(i), 0, 0);
                    moreBtnActivity.setChecked(true);
                    moreBtnActivity.setTag(iconsActivity.get(i)); //passing image resource id as a tag
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

    public void restartServiceAndFinishActivity() {
        //region Init intent going to home screen
        final Intent intentHome = new Intent(Intent.ACTION_MAIN); //태스크의 첫 액티비티로 시작
        intentHome.addCategory(Intent.CATEGORY_HOME);   //홈화면 표시
        intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //새로운 태스크를 생성하여 그 태스크안에서 액티비티 추가
        //endregion

        //region Restart the service and finish this activity
        stopService(intentService);
        startService(intentService);
        startActivity(intentHome); // Start the home activity
        sharedPrefEditor.putInt("FocusMode", 0);
        sharedPrefEditor.apply();
        finish();
        //endregion
    }

    public void cancelClicked(View view) {
        isHomeBtnPressed = false;
        //State Type 2 -> cancel
        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        long start_time = sharedPref.getLong("data_start_timestamp", -1);
        long end_time = System.currentTimeMillis();
        calStart.setTimeInMillis(start_time);
        calEnd.setTimeInMillis(end_time);

        submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 2, 0, "", 0, "", "");

        sharedPrefEditor.putInt("Shaked", 0);
        sharedPrefEditor.apply();
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
        isHomeBtnPressed = false;
        //State Type 3 -> ideal case
        RadioButton chosenLocationRB = findViewById(rgLocations.getCheckedRadioButtonId());
        RadioButton chosenActivityRB = findViewById(rgActivity.getCheckedRadioButtonId());

        if (chosenActivityRB == null || chosenLocationRB == null || chosenActivityRB.getTag() == null || chosenLocationRB.getTag() == null) {
            Toast.makeText(getApplicationContext(), "Please, choose location, activity and disturbance", Toast.LENGTH_LONG).show();
            return;
        }

        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        long start_time = sharedPref.getLong("data_start_timestamp", -1);
        long end_time = System.currentTimeMillis();
        calStart.setTimeInMillis(start_time);
        calEnd.setTimeInMillis(end_time);

        String restResultData = String.format(Locale.ENGLISH, "%d%d%d%s", seekBarQ1.getProgress() + 1, seekBarQ2.getProgress() + 1, seekBarQ3.getProgress() + 1, editTextQ4.getText());
        submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 3, (int) chosenLocationRB.getTag(), chosenLocationRB.getText().toString(), (int) chosenActivityRB.getTag(), chosenActivityRB.getText().toString(), restResultData);

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

    public void submitRawData(long start_time, long end_time, int duration, short type, int location_img_id, String location_txt, int activity_img_id, String activity_txt, String otherESMResponse) {
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
                    location_img_id,
                    location_txt,
                    activity_img_id,
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
                    int location_img_id = (int) args[6];
                    String location_txt = (String) args[7];
                    int activity_img_id = (int) args[8];
                    String activity_txt = (String) args[9];
                    String otherESMResp = (String) args[10];

                    PHPRequest request;
                    try {
                        request = new PHPRequest(url);
                        String result = request.PhPtest(PHPRequest.SERV_CODE_ADD_RD, email, String.valueOf(type), location_txt, String.valueOf(location_img_id), activity_txt, String.valueOf(activity_img_id), String.valueOf(start_time), String.valueOf(end_time), String.valueOf(duration), String.valueOf(otherESMResp));
                        if (result == null) {
                            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_img_id, location_txt, activity_img_id, activity_txt, otherESMResp);
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
            boolean isInserted = db.insertRawData(start_time, end_time, duration, type, location_img_id, location_txt, activity_img_id, activity_txt, otherESMResponse);
            Log.d(TAG, "No connection case");
            if (isInserted) {
                Toast.makeText(getApplicationContext(), "State saved", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Failed to save", Toast.LENGTH_SHORT).show();

            restartServiceAndFinishActivity();

        }

        sharedPrefEditor.putInt("Flag", 0);
        sharedPrefEditor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isHomeBtnPressed = true;
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
        sharedPrefEditor.putInt("OtherApp", 0);
        sharedPrefEditor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        isHomeBtnPressed = true;
        initLocations();
        initActivities();

        isStop = true;

        /*
        editor_typing.putInt("Typing", 1);
        editor_typing.commit();
        */

        sharedPrefEditor.putInt("Shaked", 0);
        sharedPrefEditor.apply();
        //Log.i("resume", "굿굿");

        sharedPrefEditor.putInt("Flag", 1);
        sharedPrefEditor.apply();


    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.i("onStop", "onStop");
        int shaked = sharedPref.getInt("Shaked", -1);

        //Log.i("shaked?",String.valueOf(shaked));
        if (shaked == 1) {
            try {
                //State Type 1 -> movement
                Calendar calStart = Calendar.getInstance();
                Calendar calEnd = Calendar.getInstance();
                long start_time = sharedPref.getLong("data_start_timestamp", -1);
                long end_time = System.currentTimeMillis();
                calStart.setTimeInMillis(start_time);
                calEnd.setTimeInMillis(end_time);

                submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 1, 0, "", 0, "", "");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sharedPrefEditor.putInt("Shaked", 0);
        sharedPrefEditor.apply();

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
        if (isHomeBtnPressed) {
            Log.d(TAG, "Pressed home button!");

            //State Type 2 -> cancel
            Calendar calStart = Calendar.getInstance();
            Calendar calEnd = Calendar.getInstance();
            long start_time = sharedPref.getLong("data_start_timestamp", -1);
            long end_time = System.currentTimeMillis();
            calStart.setTimeInMillis(start_time);
            calEnd.setTimeInMillis(end_time);

            submitRawData(calStart.getTimeInMillis(), calEnd.getTimeInMillis(), difference_time, (short) 2, 0, "", 0, "", "");

            sharedPrefEditor.putInt("Shaked", 0);
            sharedPrefEditor.apply();
        }
        super.onUserLeaveHint();

    }
}