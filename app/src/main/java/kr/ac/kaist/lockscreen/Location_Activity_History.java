package kr.ac.kaist.lockscreen;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Location_Activity_History extends Activity {

    public static final String TAG = "LocationActivityHistory";

    DatabaseHelper db;

    ArrayList<HistoryListDataModel> globalDataModels;
    ListView listView;

    private Button[] tabButtons;
    private TextView date;
    private TextView accTime;

    private Calendar currentCal;
    private boolean isDetailed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_history);

        db = new DatabaseHelper(this); //init database
        db.testDB();

        init();
        //initListView(); //init all available items
    }

    public void init() {
        tabButtons = new Button[]{
                findViewById(R.id.tab_summary),
                findViewById(R.id.tab_detailed)
        };


        date = findViewById(R.id.date);
        accTime = findViewById(R.id.accumulated_time);

        isDetailed = false;

        //region Initialize the dataList from server
        globalDataModels = new ArrayList<>();
        setRawData();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //endregion

        currentCal = Calendar.getInstance();
        String curDate = String.format(Locale.ENGLISH, "%d.%d.%d", currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.DAY_OF_MONTH));
        date.setText(curDate);

        tabClicked(tabButtons[0]); //intentionally clicked the "Summary" tab in the beginning


    }

    public void initList() {

        ArrayList<HistoryListDataModel> localDataModels = new ArrayList<>();

        int accMinutes = 0;

        for (HistoryListDataModel item : globalDataModels) {
            Calendar newCal = Calendar.getInstance();
            newCal.setTimeInMillis(item.getStartTime());
            if (newCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) && newCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)) {
                accMinutes += item.getDuration();
                localDataModels.add(item);
            }
        }


        //TODO: handle activity texts duplication
        Map<String, Integer> countMap = new HashMap<>();

        for (HistoryListDataModel item : localDataModels) {
            String txtElement = item.getTextActivity();
            if (countMap.containsKey(txtElement))
                countMap.put(txtElement, countMap.get(txtElement) + 1);
            else
                countMap.put(txtElement, 1);
        }
        Log.d(TAG, countMap.toString() + "");


        int hour;
        int min;
        int sec;

        if (accMinutes > 0) {
            if (accMinutes < 60) {
                sec = accMinutes;
                accTime.setText(String.format(Locale.ENGLISH, "%d%s", sec, getString(R.string.seconds)));
            } else if (accMinutes < 3600) {
                min = accMinutes / 60;
                sec = accMinutes % 60;
                accTime.setText(String.format(Locale.ENGLISH, "%d%s %d%s", min, getString(R.string.min), sec, getString(R.string.seconds)));
                accTime.setText(String.format(Locale.ENGLISH, "%d%s %d%s", min, getString(R.string.min), sec, getString(R.string.seconds)));
            } else {
                hour = accMinutes / 3600;
                min = (accMinutes % 3600) / 60;
                sec = accMinutes % 60;
                accTime.setText(String.format(Locale.ENGLISH, "%d%s %d%s %d%s", hour, getString(R.string.hours), min, getString(R.string.min), sec, getString(R.string.seconds)));
            }
        } else {
            accTime.setText(getString(R.string.no_data));
        }


        Adapters.HistoryListAdapter adapter = new Adapters.HistoryListAdapter(Location_Activity_History.this, localDataModels, isDetailed);
        listView = findViewById(R.id.list);
        listView.setAdapter(adapter); //set initialized adapter

        //region OnItemClick Listener for ListView (implementation of EDIT function)
        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                long viewId = view.getId();
                if (viewId == R.id.edit) {
                    //perform edit.
                    Toast.makeText(Location_Activity_History.this, "Edit clicked", Toast.LENGTH_SHORT).show();
                }
            }
        });*/
        //endregion
    }

    public void setRawData() {
        if (Tools.isNetworkAvailable(this)) {
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_server, getString(R.string.server_ip)),
                    SignInActivity.loginPrefs.getString(SignInActivity.email, null)
            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String email = (String) args[1];
                    PHPRequest request;
                    try {
                        request = new PHPRequest(url);
                        String result = request.PhPtest(PHPRequest.SERV_CODE_SHOW_RD, email, null, null, null, null, null, null, null, null, null);
                        if (result.equals(Tools.RES_FAIL)) {
                            Toast.makeText(activity, "Failed to get data (No such user)", Toast.LENGTH_SHORT).show();
                        } else {
                            JSONArray jsonArray = new JSONArray(result);
                            Log.d(TAG, "Data is received");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                long start_time = Long.parseLong(obj.getString("start_time"));
                                long end_time = Long.parseLong(obj.getString("end_time"));
                                int duration = Integer.parseInt(obj.getString("elapsed_time"));
                                int icon_act = Integer.parseInt(obj.getString("activity_id"));
                                String txt_act = obj.getString("activity");
                                int icon_loc = Integer.parseInt(obj.getString("location_id"));
                                String txt_loc = obj.getString("location");

                                globalDataModels.add(new HistoryListDataModel(start_time, end_time, duration, icon_act, txt_act, icon_loc, txt_loc));
                            }
                        }
                    } catch (MalformedURLException | JSONException e) {
                        e.printStackTrace();
                    }

                    enableTouch();
                }
            });
        } else {
            Toast.makeText(this, "No connection to internet", Toast.LENGTH_SHORT).show();

        }
    }

    public void tabClicked(View view) {

        for (Button button : tabButtons)
            button.setBackgroundResource(R.color.btn_unchecked_color);

        switch (view.getId()) {
            case R.id.tab_summary:
                tabButtons[0].setBackgroundResource(R.color.btn_checked_color);
                findViewById(R.id.activity_place_txt).setVisibility(View.GONE);
                isDetailed = false;
                initList();
                break;
            case R.id.tab_detailed:
                tabButtons[1].setBackgroundResource(R.color.btn_checked_color);
                findViewById(R.id.activity_place_txt).setVisibility(View.VISIBLE);
                isDetailed = true;
                initList();
                break;
            default:
                break;

        }
    }

    public void dayNavigationClicked(View view) {
        String curDate;
        switch (view.getId()) {
            case R.id.btn_prev_day:
                currentCal.add(Calendar.DAY_OF_MONTH, -1);
                curDate = String.format(Locale.ENGLISH, "%d.%d.%d", currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.DAY_OF_MONTH));
                date.setText(curDate);
                initList();
                break;
            case R.id.btn_next_day:
                currentCal.add(Calendar.DAY_OF_MONTH, 1);
                curDate = String.format(Locale.ENGLISH, "%d.%d.%d", currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.DAY_OF_MONTH));
                date.setText(curDate);
                initList();
                break;
            default:
                break;
        }
    }
}
