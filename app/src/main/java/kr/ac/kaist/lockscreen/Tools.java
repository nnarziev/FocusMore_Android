package kr.ac.kaist.lockscreen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Tools {

    public static final String TAG = "TOOLS";
    static final String
            RES_OK = "0",
            RES_FAIL = "1",
            RES_SRV_ERR = "2";

    private static ExecutorService executor = Executors.newCachedThreadPool();

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo;
        if (connectivityManager == null)
            return false;
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static void execute(MyRunnable runnable) {
        disable_touch(runnable.activity);
        executor.execute(runnable);
    }

    static void executeForAutoDataSubmit(MyRunnable runnable) {
        executor.execute(runnable);
    }

    private static void disable_touch(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    static void enable_touch(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    static String post(String _url, JSONObject json_body) throws IOException {
        URL url = new URL(_url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(json_body != null);
        con.setDoInput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.connect();

        if (json_body != null) {
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(convertToUTF8(json_body.toString()));
            wr.flush();
            wr.close();
        }

        int status = con.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            con.disconnect();
            return null;
        } else {
            byte[] buf = new byte[1024];
            int rd;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BufferedInputStream is = new BufferedInputStream(con.getInputStream());
            while ((rd = is.read(buf)) > 0)
                bos.write(buf, 0, rd);
            is.close();
            con.disconnect();
            bos.close();
            return convertFromUTF8(bos.toByteArray());
        }
    }

    private static String convertFromUTF8(byte[] raw) {
        try {
            return new String(raw, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    private static String convertToUTF8(String s) {
        try {
            return new String(s.getBytes("UTF-8"), "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    static boolean isDuplicated(ArrayList<String> itemList, String checkItem) {
        for (String item : itemList)
            if (item.toLowerCase().equals(checkItem.toLowerCase()))
                return true;
        return false;
    }
}

abstract class MyRunnable implements Runnable {
    MyRunnable(Activity activity, Object... args) {
        this.activity = activity;
        this.args = Arrays.copyOf(args, args.length);
    }

    void enableTouch() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Tools.enable_touch(activity);
            }
        });
    }

    Object[] args;
    Activity activity;
}

class NetworkUtil {
    @SuppressLint("NewApi")
    static public void setNetworkPolicy() {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }
}

class PHPRequest {
    public static final String TAG = "PhPRequest";
    private URL url;
    static final String
            SERV_CODE_SIGN_UP = "0",
            SERV_CODE_SIGN_IN = "1",
            SERV_CODE_SHOW_RD = "2",
            SERV_CODE_ADD_RD = "3";

    PHPRequest(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    private String readStream(InputStream in) throws IOException {
        StringBuilder jsonHtml = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = null;

        while ((line = reader.readLine()) != null)
            jsonHtml.append(line);

        reader.close();
        return jsonHtml.toString();
    }

    String PhPtest(final String data1, final String data2, final String data3, final String data4, final String data5, final String data6, final String data7, final String data8, final String data9, final String data10, final String data11) {
        try {
            String postData = "Data1=" + data1 + "&Data2=" + data2 + "&Data3=" + data3 + "&Data4=" + data4 + "&Data5=" + data5 + "&Data6=" + data6 + "&Data7=" + data7 + "&Data8=" + data8 + "&Data9=" + data9 + "&Data10=" + data10 + "&Data11=" + data11;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(postData.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();
            String result = readStream(conn.getInputStream());
            conn.disconnect();
            Log.i("input", postData);
            return result;
        } catch (Exception e) {
            Log.i(TAG, "Request was failed due to Exception: " + e);
            return null;
        }
    }
}

class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    //region Constants
    private static final String DB_NAME = "FocusMore.db";
    private static final String USER_DATA_LOCATIONS_TABLE = "ud_locations";
    private static final String USER_DATA_ACTIVITIES_TABLE = "ud_activities";
    private static final String UD_COL_1 = "ID";
    private static final String UD_COL_2 = "TEXT";
    private static final String UD_COL_3 = "IMG_ID";
    private static final String UD_COL_4 = "BOOKMARK";
    private static final String UD_COL_5 = "ACCUMULATED_DURATION";

    private static final String RAW_DATA_TABLE = "raw_data";
    private static final String RD_COL_1 = "START_TIME";
    private static final String RD_COL_2 = "END_TIME";
    private static final String RD_COL_3 = "DURATION"; //in minutes
    private static final String RD_COL_4 = "TYPE"; // 1~3
    private static final String RD_COL_5 = "LOCATION_IMG";
    private static final String RD_COL_6 = "LOCATION_TXT";
    private static final String RD_COL_7 = "ACTIVITY_IMG";
    private static final String RD_COL_8 = "ACTIVITY_TXT";
    private static final String RD_COL_9 = "DISTRACTION";

    static final short LOCATIONS = 1;
    static final short ACTIVITIES = 2;
    //endregion

    private Context context;

    DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + USER_DATA_LOCATIONS_TABLE + "(" + UD_COL_1 + " INTEGER PRIMARY KEY, " + UD_COL_2 + " TEXT, " + UD_COL_3 + " INTEGER, " + UD_COL_4 + " INTEGER, " + UD_COL_5 + " INTEGER " + ")");
        db.execSQL("create table " + USER_DATA_ACTIVITIES_TABLE + "(" + UD_COL_1 + " INTEGER PRIMARY KEY, " + UD_COL_2 + " TEXT, " + UD_COL_3 + " INTEGER, " + UD_COL_4 + " INTEGER, " + UD_COL_5 + " INTEGER " + ")");
        db.execSQL("create table " + RAW_DATA_TABLE + "(" + RD_COL_1 + " INTEGER PRIMARY KEY, " + RD_COL_2 + " INTEGER, " + RD_COL_3 + " INTEGER, " + RD_COL_4 + " INTEGER, " + RD_COL_5 + " INTEGER, " + RD_COL_6 + " TEXT, " + RD_COL_7 + " INTEGER, " + RD_COL_8 + " TEXT, " + RD_COL_9 + " TEXT " + ")");

        //region Initialize default values in user data DB
        ArrayList<String> locationsTitleDef = new ArrayList<>(Arrays.asList("Home", "School", "Office", "Library", "Cafe", "Restaurant"));
        ArrayList<Integer> locationsIconsDef = new ArrayList<>(Arrays.asList(R.drawable.icon_location_home
                , R.drawable.icon_location_school
                , R.drawable.icon_location_office
                , R.drawable.icon_location_library
                , R.drawable.icon_location_cafe
                , R.drawable.icon_location_restaurant));

        for (int i = 0; i < locationsTitleDef.size(); i++) {
            long itemId = System.currentTimeMillis();
            Log.d(TAG, "TIME: " + itemId);
            boolean isInserted = false;
            isInserted = insertDefaultUserData(db, LOCATIONS, itemId, locationsTitleDef.get(i), locationsIconsDef.get(i), (short) 1, 0);
            if (!isInserted) {
                Log.d(TAG, "Failed to insert default location");
                break;
            }
            try {
                wait(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        ArrayList<String> activitiesTitleDef = new ArrayList<>(Arrays.asList("Study", "Eat", "Work", "Rest", "Sleep", "Workout", "Communicate"));
        ArrayList<Integer> activitiesIconsDef = new ArrayList<>(Arrays.asList(R.drawable.icon_activity_study
                , R.drawable.icon_activity_eat
                , R.drawable.icon_activity_work
                , R.drawable.icon_activity_rest
                , R.drawable.icon_activity_sleep
                , R.drawable.icon_activity_workout
                , R.drawable.icon_activity_communicate));

        for (int i = 0; i < activitiesTitleDef.size(); i++) {
            long itemId = System.currentTimeMillis();
            boolean isInserted = false;
            isInserted = insertDefaultUserData(db, ACTIVITIES, itemId, activitiesTitleDef.get(i), activitiesIconsDef.get(i), (short) 1, 0);
            if (!isInserted) {
                Log.d(TAG, "Failed to insert default activity");
                break;
            }
            try {
                wait(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        //endregion

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + USER_DATA_LOCATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + USER_DATA_ACTIVITIES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + RAW_DATA_TABLE);
        onCreate(db);
    }

    //region UserData db manipulation
    private boolean insertDefaultUserData(SQLiteDatabase db, short insertFor, long id, String text, int img_id, short bookmark, int acc_duration) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UD_COL_1, id);
        contentValues.put(UD_COL_2, text);
        contentValues.put(UD_COL_3, img_id);
        contentValues.put(UD_COL_4, bookmark);
        contentValues.put(UD_COL_5, acc_duration);
        long res = 0;
        if (insertFor == LOCATIONS)
            res = db.insert(USER_DATA_LOCATIONS_TABLE, null, contentValues);
        else if (insertFor == ACTIVITIES)
            res = db.insert(USER_DATA_ACTIVITIES_TABLE, null, contentValues);

        if (res == -1)
            return false;
        else
            return true;
    }

    boolean insertNewUserData(short insertFor, long id, String text, int img_id, short bookmark, int acc_duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(UD_COL_1, id);
        contentValues.put(UD_COL_2, text);
        contentValues.put(UD_COL_3, img_id);
        contentValues.put(UD_COL_4, bookmark);
        contentValues.put(UD_COL_5, acc_duration);
        long res = 0;
        if (insertFor == LOCATIONS)
            res = db.insert(USER_DATA_LOCATIONS_TABLE, null, contentValues);
        else if (insertFor == ACTIVITIES)
            res = db.insert(USER_DATA_ACTIVITIES_TABLE, null, contentValues);

        if (res == -1)
            return false;
        else
            return true;
    }

    Cursor getAllUserData(short getFor) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = null;
        if (getFor == LOCATIONS)
            res = db.rawQuery("select * from " + USER_DATA_LOCATIONS_TABLE + " order by " + UD_COL_5 + " desc;", null);
        else if (getFor == ACTIVITIES)
            res = db.rawQuery("select * from " + USER_DATA_ACTIVITIES_TABLE + " order by " + UD_COL_5 + " desc;", null);
        return res;
    }

    public void clearUserDataTables() {
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("delete from " + USER_DATA_ACTIVITIES_TABLE);
        db.execSQL("delete from " + USER_DATA_LOCATIONS_TABLE);
    }

    boolean updateUserDataTextIcon(short updateFor, long id, String text, int img_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(UD_COL_1, id);
        contentValues.put(UD_COL_2, text);
        contentValues.put(UD_COL_3, img_id);
        if (updateFor == LOCATIONS)
            db.update(USER_DATA_LOCATIONS_TABLE, contentValues, "ID = ?", new String[]{String.valueOf(id)});
        else if (updateFor == ACTIVITIES)
            db.update(USER_DATA_ACTIVITIES_TABLE, contentValues, "ID = ?", new String[]{String.valueOf(id)});

        return true;
    }

    boolean updateUserDataBookmark(short updateFor, long id, short bookmark) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(UD_COL_1, id);
        contentValues.put(UD_COL_4, bookmark);
        if (updateFor == LOCATIONS)
            db.update(USER_DATA_LOCATIONS_TABLE, contentValues, "ID = ?", new String[]{String.valueOf(id)});
        else if (updateFor == ACTIVITIES)
            db.update(USER_DATA_ACTIVITIES_TABLE, contentValues, "ID = ?", new String[]{String.valueOf(id)});

        return true;
    }

    boolean updateUserDataAccDuration(short updateFor, String title, int duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(UD_COL_5, duration);
        if (updateFor == LOCATIONS)
            db.update(USER_DATA_LOCATIONS_TABLE, contentValues, "TEXT = ?", new String[]{title});
        else if (updateFor == ACTIVITIES)
            db.update(USER_DATA_ACTIVITIES_TABLE, contentValues, "TEXT = ?", new String[]{title});

        return true;
    }

    Integer deleteUserData(short deleteFor, long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int res = 0;
        if (deleteFor == LOCATIONS)
            res = db.delete(USER_DATA_LOCATIONS_TABLE, "ID = ?", new String[]{String.valueOf(id)});
        else if (deleteFor == ACTIVITIES)
            res = db.delete(USER_DATA_ACTIVITIES_TABLE, "ID = ?", new String[]{String.valueOf(id)});

        return res;
    }
    //endregion

    //region RawData db manipulation
    boolean insertRawData(long start_time, long end_time, int duration, short type, int location_img_id, String location_txt, int activity_img_id, String activity_txt, String distraction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(RD_COL_1, start_time);
        contentValues.put(RD_COL_2, end_time);
        contentValues.put(RD_COL_3, duration);
        contentValues.put(RD_COL_4, type);
        contentValues.put(RD_COL_5, location_img_id);
        contentValues.put(RD_COL_6, location_txt);
        contentValues.put(RD_COL_7, activity_img_id);
        contentValues.put(RD_COL_8, activity_txt);
        contentValues.put(RD_COL_9, distraction);

        long res = 0;
        res = db.insert(RAW_DATA_TABLE, null, contentValues);

        if (res == -1)
            return false;
        else
            return true;
    }

    Integer deleteRawData(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int res = 0;
        res = db.delete(RAW_DATA_TABLE, "START_TIME = ?", new String[]{String.valueOf(id)});

        return res;
    }

    /*List<String> getRawData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + RAW_DATA_TABLE, null);

        List<String> dataResultList = new ArrayList<>();
        if (res.moveToFirst()) {
            do {
                StringBuffer data = new StringBuffer();
                data.append("Start: " + res.getString(0) + "\n");
                data.append("End: " + res.getString(1) + "\n");
                data.append("Duration(sec): " + res.getString(2) + "\n");
                data.append("Type: " + res.getString(3) + "\n");
                data.append("Location_img: " + res.getString(4) + "\n");
                data.append("Location_txt: " + res.getString(5) + "\n");
                data.append("Activity_img: " + res.getString(6) + "\n");
                data.append("Activity_txt: " + res.getString(7) + "\n");
                data.append("Distract: " + res.getString(8) + "\n");
                dataResultList.add(data.toString());
                //Log.i("result!!",data.toString());
            } while (res.moveToNext());
        }
        return dataResultList;

    }*/

    List<String[]> getRawData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + RAW_DATA_TABLE, null);

        List<String[]> dataResultList = new ArrayList<>();
        if (res.moveToFirst()) {
            do {
                String[] data = new String[9];
                data[0] = res.getString(0);
                data[1] = res.getString(1);
                data[2] = res.getString(2);
                data[3] = res.getString(3);
                data[4] = res.getString(4);
                data[5] = res.getString(5);
                data[6] = res.getString(6);
                data[7] = res.getString(7);
                data[8] = res.getString(8);
                dataResultList.add(data);
            } while (res.moveToNext());
        }
        return dataResultList;

    }

    public void clearRawDataTable() {
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("delete from " + RAW_DATA_TABLE);
    }
    //endregion

    void testDB() {
        SQLiteDatabase db = getReadableDatabase();
    }
}

class ListDataModel {

    private long itemId;
    private boolean bookmark;
    private String text;
    private int iconResource;

    ListDataModel(long itemId, boolean bookmark, String text, int iconResource) {
        this.itemId = itemId;
        this.bookmark = bookmark;
        this.text = text;
        this.iconResource = iconResource;

    }

    long getItemId() {
        return itemId;
    }

    boolean getBookmark() {
        return bookmark;
    }

    public String getText() {
        return text;
    }

    int getIconResource() {
        return iconResource;
    }
}

class HistoryListDataModel {
    private long startTime;
    private long endTimne;
    private int duration;
    private int iconActivity;
    private String textActivity;
    private int iconLocation;
    private String textLocation;

    HistoryListDataModel(long startTimne, long endTimne, int duration, int iconActivity, String textActivity, int iconLocation, String textLocation) {
        this.startTime = startTimne;
        this.endTimne = endTimne;
        this.duration = duration;
        this.iconActivity = iconActivity;
        this.textActivity = textActivity;
        this.iconLocation = iconLocation;
        this.textLocation = textLocation;
    }

    long getStartTime() {
        return startTime;
    }

    long getEndTime() {
        return endTimne;
    }

    int getDuration() {
        return duration;
    }

    int getIconActivity() {
        return iconActivity;
    }

    String getTextActivity() {
        return textActivity;
    }

    int getIconLocation() {
        return iconLocation;
    }

    String getTextLocation() {
        return textLocation;
    }


}

class State {

    //region Constants
    static final short TYPE_1 = 1;
    static final short TYPE_2 = 2;
    static final short TYPE_3 = 3;
    //endregion

    //region Variables
    private String user_id;
    private Calendar start_time;
    private Calendar end_time;
    private int duration;
    private short type;
    private String location;
    private String activity;
    private short distraction;
    //endregion

    public State(String user_id, Calendar start_time, Calendar end_time, int duration, short type, String location, String activity, short distraction) {
        this.user_id = user_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.duration = duration;
        this.type = type;
        this.location = location;
        this.activity = activity;
        this.distraction = distraction;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Calendar getStart_time() {
        return start_time;
    }

    public void setStart_time(Calendar start_time) {
        this.start_time = start_time;
    }

    public Calendar getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Calendar end_time) {
        this.end_time = end_time;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public short getDistraction() {
        return distraction;
    }

    public void setDistraction(short distraction) {
        this.distraction = distraction;
    }
}