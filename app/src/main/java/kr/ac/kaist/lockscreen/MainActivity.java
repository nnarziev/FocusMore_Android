package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;

import static kr.ac.kaist.lockscreen.DatabaseHelper.ACTIVITIES;
import static kr.ac.kaist.lockscreen.DatabaseHelper.LOCATIONS;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    protected SharedPreferences sharedPref = null;
    protected SharedPreferences.Editor sharedPrefEditor = null;

    private DatabaseHelper myDb;

    ConnectionReceiver receiver;
    IntentFilter intentFilter;

    Intent intentService;

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setActionBar((Toolbar) findViewById(R.id.my_toolbar));
        }

        //init DB
        myDb = new DatabaseHelper(this);
        myDb.testDB();

        //region If the user did not turn the notification listener service on we prompt him to do so
        if (!isNotificationServiceEnabled()) {
            AlertDialog enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }
        //endregion

        //region Registering BroadcastReciever for connectivity changed
        intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        receiver = new ConnectionReceiver();
        intentFilter.addAction(getPackageName() + "android.net.wifi.WIFI_STATE_CHANGED");
        registerReceiver(receiver, intentFilter);
        //endregion

        init();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void init() {
        Button btn_reset_service = findViewById(R.id.resetButton);
        Button btn_esm_history = findViewById(R.id.esmResult);
        Button btn_confirm = findViewById(R.id.confirm);
        Button btn_mng_locations = findViewById(R.id.btn_mng_locations);
        Button btn_mng_activities = findViewById(R.id.btn_mng_activities);
        final EditText txt_input_sec = findViewById(R.id.input_sec);
        final TextView txt_sec = findViewById(R.id.textView2);

        sharedPref = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        int set_duration = sharedPref.getInt("Duration", -1);
        txt_input_sec.setText(String.valueOf(set_duration));

        //락스크린 서비스 실행(카운트도 같이 함)
        intentService = new Intent(this, CountService.class);
        startService(intentService);

        btn_reset_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(intentService);
                startService(intentService);
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int duration = Integer.parseInt(txt_input_sec.getText().toString());
                    sharedPrefEditor.putInt("Duration", duration);
                    sharedPrefEditor.apply();
                    Log.i("결과", String.valueOf(duration));

                    stopService(intentService);
                    startService(intentService);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "올바른 값을 입력해 주세요(1이상 자연수)", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_esm_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Location_Activity_History.class);
                startActivity(intent);
            }
        });

        btn_mng_locations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Location_Activity_List.class);
                intent.putExtra("itemFor", LOCATIONS);
                startActivity(intent);
            }
        });

        btn_mng_activities.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Location_Activity_List.class);
                intent.putExtra("itemFor", ACTIVITIES);
                startActivity(intent);
            }
        });

        sharedPrefEditor.putInt("OtherApp", 0);
        sharedPrefEditor.apply();

        sharedPrefEditor.putInt("Shake", 1);
        sharedPrefEditor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPrefEditor.putInt("Flag", 0);
        sharedPrefEditor.apply();

        //Log.i("Main Activity:",String.valueOf(pref_flag.getInt("Flag",-1)));
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    //뒤로가기 키 막기
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final Intent intent = new Intent(Intent.ACTION_MAIN); //태스크의 첫 액티비티로 시작
        intent.addCategory(Intent.CATEGORY_HOME);   //홈화면 표시
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //새로운 태스크를 생성하여 그 태스크안에서 액티비티 추가
        startActivity(intent);
        return true;
    }

    public void sendEmail() {
        try {
            String[] address = {"nnarziev@gmail.com"};
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "락스크린 실험 결과 입니다 [이름:         ]");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "결과 입니다.");
            shareIntent.putExtra(Intent.EXTRA_EMAIL, address);
            ArrayList<Uri> uris = new ArrayList<Uri>();
            String shareName = new String(getFilesDir().getAbsolutePath() + "/Experiment_result.txt");
            File shareFile = new File(shareName);
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "kr.ac.kaist.lockscreen.fileprovider", shareFile);
            uris.add(contentUri);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String msgStr = "Share...?";
            startActivity(Intent.createChooser(shareIntent, msgStr));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "에러가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }

        /*
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        String[] address = {"kiyeob4416@gmail.com"}; //주소를 넣어두면 미리 주소가 들어가 있다.
        intent.putExtra(Intent.EXTRA_EMAIL, address);
        intent.putExtra(Intent.EXTRA_SUBJECT, "실험");
        intent.putExtra(Intent.EXTRA_TEXT, "보낼 내용");
        //intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:/mnt/sdcard/test.jpg")); //파일 첨부
        startActivity(intent);
        */

    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private AlertDialog buildNotificationServiceAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Notification service");
        alertDialogBuilder.setMessage("Do you want to give access to notification services?");
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return (alertDialogBuilder.create());
    }

    public void logoutClick(MenuItem item) {
        SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
        editor.clear();
        editor.apply();
        stopService(intentService);
        sharedPrefEditor.putInt("FocusMode", 0);
        sharedPrefEditor.apply();

        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }
}
