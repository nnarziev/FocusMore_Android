package kr.ac.kaist.lockscreen;

import android.database.Cursor;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Location_Activity_History extends Activity {

    public static final String TAG = "LocationActivityHistory";

    DatabaseHelper db;

    ArrayList<HistoryListDataModel> dataModels;
    ListView listView;
    private Cursor row_data_res = null;

    private Button[] tabButtons;

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

        tabClicked(tabButtons[0]); //intentionally clicked the "Summary" tab in the beginning

    }

    public void initList(boolean isDetailed) {
        //region Initialize the dataList from local database
        dataModels = new ArrayList<>();

        setRawData();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, dataModels.toString()+"");


        /*TODO: implement a function to get all raw data and init raw_data_res variable
        row_data_res =
        */

        /*if (row_data_res.getCount() == 0) {
            Toast.makeText(this, "No data in DB yet", Toast.LENGTH_LONG).show();
            return;
        }*/

        /*TODO: add all data from row_data_res to dataModels arraylist*/
        //HistoryListDataModel(long startTimne, long endTimne, int duration, int iconActivity, String textActivity, int iconLocation, String textLocation){
        /*dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity0", R.drawable.icon_activity_communicate, "place0"));
        dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity1", R.drawable.icon_activity_communicate, "place1"));
        dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity2", R.drawable.icon_activity_communicate, "place2"));
        dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity3", R.drawable.icon_activity_communicate, "place3"));
        dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity4", R.drawable.icon_activity_communicate, "place4"));
        dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity5", R.drawable.icon_activity_communicate, "place5"));
        dataModels.add(new HistoryListDataModel(12345, 12345, 123, R.drawable.icon_activity_communicate, "actiity6", R.drawable.icon_activity_communicate, "place6"));*/
        //endregion



        Adapters.HistoryListAdapter adapter = new Adapters.HistoryListAdapter(Location_Activity_History.this, dataModels, isDetailed);
        listView = findViewById(R.id.list);
        listView.setAdapter(adapter); //set initialized adapter

        //region OnItemClick Listener for ListView (implementation of EDIT function)
        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                long viewId = view.getId();
                if (viewId == R.id.edit) {
                    //TODO: perform edit.
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
                    PHPRequest request = null;
                    try {
                        request = new PHPRequest(url);
                        String result = request.PhPtest(PHPRequest.SERV_CODE_SHOW_RD, email, null, null, null, null, null, null, null, null, null);
                        if (result.equals(Tools.RES_FAIL)) {
                            Toast.makeText(activity, "Failed to get data (No such user)", Toast.LENGTH_SHORT).show();
                        } else {
                            JSONArray jsonArray = new JSONArray(result);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                long start_time = Long.parseLong(obj.getString("start_time"));
                                long end_time = Long.parseLong(obj.getString("end_time"));
                                int duration = Integer.parseInt(obj.getString("elapsed_time"));
                                int icon_act = Integer.parseInt(obj.getString("activity_id"));
                                String txt_act = obj.getString("activity");
                                int icon_loc = Integer.parseInt(obj.getString("location_id"));
                                String txt_loc = obj.getString("location");

                                dataModels.add(new HistoryListDataModel(start_time, end_time, duration, icon_act, txt_act, icon_loc, txt_loc));
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
                initList(false);
                break;
            case R.id.tab_detailed:
                tabButtons[1].setBackgroundResource(R.color.btn_checked_color);
                findViewById(R.id.activity_place_txt).setVisibility(View.VISIBLE);
                initList(true);
                break;
            default:
                break;

        }
    }

    public void dayNavigationClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_prev_day:
                break;
            case R.id.btn_next_day:
                break;
            default:
                break;
        }
    }
}
