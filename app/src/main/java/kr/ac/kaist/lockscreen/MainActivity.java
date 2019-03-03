package kr.ac.kaist.lockscreen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;

import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static kr.ac.kaist.lockscreen.DatabaseHelper.ACTIVITIES;
import static kr.ac.kaist.lockscreen.DatabaseHelper.LOCATIONS;
import static kr.ac.kaist.lockscreen.SignInActivity.email;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    protected SharedPreferences sharedPref = null;
    protected SharedPreferences.Editor sharedPrefEditor = null;

    protected ListView listView;
    protected String[] results;

    private DatabaseHelper myDb;

    TextView qResult;

    ConnectionReceiver receiver;
    IntentFilter intentFilter;

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    public void init() {
        listView = findViewById(R.id.listView);
        Button btn_reset_service = findViewById(R.id.resetButton);
        Button btn_esm_history = findViewById(R.id.esmResult);
        Button btn_pop_local_db_res = findViewById(R.id.popupResult);
        Button btn_confirm = findViewById(R.id.confirm);
        Button btn_mng_locations = findViewById(R.id.btn_mng_locations);
        Button btn_mng_activities = findViewById(R.id.btn_mng_activities);
        final EditText txt_input_sec = findViewById(R.id.input_sec);
        final TextView txt_sec = findViewById(R.id.textView2);

        qResult = findViewById(R.id.query_result);

        sharedPref = getSharedPreferences("Modes", Activity.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        int set_duration = sharedPref.getInt("Duration", -1);
        txt_input_sec.setText(String.valueOf(set_duration));

        //락스크린 서비스 실행(카운트도 같이 함)
        final Intent intentService = new Intent(this, CountService.class);
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

        btn_pop_local_db_res.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*try {
                    List<String> results_temp = myDb.getRawData();
                    int count = results_temp.size();
                    results = new String[count];
                    results = results_temp.toArray(results);
                    //Date date = new Date(Long.parseLong(results[0].split("\n")[0].split(":")[1]));
                    //results[0] = date.toString();

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, results);
                    listView.setAdapter(adapter);
                    resultView.setText(String.valueOf(count) + "개의 결과가 발견되었습니다.");
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
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

    private class phpDown extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {

            StringBuilder jsonHtml = new StringBuilder();
            String str;
            try {
                // connection url
                URL url = new URL(urls[0]);

                // connection object generation
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // if connected
                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setUseCaches(false);
                    // if code returned
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        for (; ; ) {
                            // read text from web page
                            String line = br.readLine();
                            if (line == null) break;
                            jsonHtml.append(line + "\n");
                        }
                        br.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return jsonHtml.toString();

        }

        protected void onPostExecute(String str) {
            Log.d(TAG, str + "");
        }
    }

}
