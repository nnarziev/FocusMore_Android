package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import java.net.MalformedURLException;
import java.util.List;

public class ConnectionReceiver extends BroadcastReceiver {
    public static final String TAG = "ConnectionReceiver";

    private DatabaseHelper myDb;
    private Context context;

    @Override
    public void onReceive(Context con, Intent intent) {

        context = con;
        //init DB
        myDb = new DatabaseHelper(context);
        //myDb.testDB();

        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (Tools.isNetworkAvailable(context)) {
                try {
                    Log.d(TAG, "Network is connected");
                    submitRDFromDB();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Network is changed or reconnected");
            }
        }
    }

    //function to submit the data when internet connection is established
    public void submitRDFromDB() {

        //region Extracting raw data from local db and
        List<String[]> results_temp = myDb.getRawData();
        int count = results_temp.size();

        if (count > 0) {
            for (String[] raw : results_temp) {
                submitRaw(Long.parseLong(raw[0]), Long.parseLong(raw[1]), Integer.parseInt(raw[2]), Short.parseShort(raw[3]), raw[4], raw[5], raw[6]);
            }
        }

        Log.d(TAG, "Count: " + count);
    }

    //function to submit each row from local db and deleting that row after submission from local db
    public void submitRaw(long start_time, long end_time, int duration, short type, String location_txt, String activity_txt, String distraction) {
        Tools.executeForAutoDataSubmit(new MyRunnable(
                (Activity) context,
                context.getString(R.string.url_server, context.getString(R.string.server_ip)),
                SignInActivity.loginPrefs.getString(SignInActivity.email, null),
                start_time,
                end_time,
                duration,
                type,
                location_txt,
                activity_txt,
                distraction
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
                String distraction = (String) args[8];

                PHPRequest request;
                try {
                    request = new PHPRequest(url);
                    String result = request.PhPtest(PHPRequest.SERV_CODE_ADD_RD, email, String.valueOf(type), location_txt, "", activity_txt, "", String.valueOf(start_time), String.valueOf(end_time), String.valueOf(duration), String.valueOf(distraction)); //TODO: remove empty strings for icons
                    if (result != null) {
                        switch (result) {
                            case Tools.RES_OK:
                                Log.d(TAG, "Automatic submission");
                                deleteSubmittedRaw(start_time);
                                break;
                            case Tools.RES_FAIL:
                                Log.d(TAG, "Automatic submission Failed");
                                break;
                            case Tools.RES_SRV_ERR:
                                Log.d(TAG, "Automatic submission Failed (SERVER)");
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
    }

    //function to delete the row from local db by id
    public void deleteSubmittedRaw(long id) {
        int deletedRows;
        deletedRows = myDb.deleteRawData(id);
        if (deletedRows > 0) {
            Log.d(TAG, "Deleted form local DB");
        } else
            Log.d(TAG, "Not deleted from local DB");
    }
}
