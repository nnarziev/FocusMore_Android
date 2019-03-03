package kr.ac.kaist.lockscreen;

import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

import static kr.ac.kaist.lockscreen.DatabaseHelper.ACTIVITIES;
import static kr.ac.kaist.lockscreen.DatabaseHelper.LOCATIONS;

public class Location_Activity_List extends AppCompatActivity {

    public static final String TAG = "Location_Activity_List";

    DatabaseHelper db;

    ArrayList<ListDataModel> dataModels;
    ArrayList<String> userDataTexts;
    ListView listView;
    private int itemFor;
    private Cursor user_data_res = null;
    private int numOfBookmarked = 0;
    private int numOfExisting = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_list);

        //region Making window full sized and to show up when locked
        Window win = getWindow();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        /*
            FLAG_SHOW_WHEN_LOCKED tells Android to display this activity above the Android default lock screen
            FLAG_DISMISS_KEYGUARD is to make Android default lock screen disappear
        */

        //endregion
        db = new DatabaseHelper(this); //init database
        db.testDB();

        itemFor = getIntent().getShortExtra("itemFor", (short) 0); //variable that tells whether this activity is opened from locations or activities list

        initListView(); //init all available items
    }

    public void initListView() {
        //region Initialize the dataList from local database
        dataModels = new ArrayList<>();
        userDataTexts = new ArrayList<>();
        if (itemFor == LOCATIONS)
            user_data_res = db.getAllUserData(LOCATIONS);
        else if (itemFor == ACTIVITIES)
            user_data_res = db.getAllUserData(ACTIVITIES);

        if (user_data_res.getCount() == 0) {
            Toast.makeText(this, "No data in DB yet", Toast.LENGTH_LONG).show();
            return;
        }

        while (user_data_res.moveToNext()) {
            dataModels.add(new ListDataModel(user_data_res.getLong(0), user_data_res.getShort(3) != 0, user_data_res.getString(1), user_data_res.getInt(2)));
            userDataTexts.add(user_data_res.getString(1));
        }
        //endregion

        Adapters.ListAdapter adapter = new Adapters.ListAdapter(Location_Activity_List.this, dataModels);
        listView = findViewById(R.id.list);
        listView.setAdapter(adapter); //set initialized adapter

        //region Initialize the number of existing and bookmarked items in the list
        numOfBookmarked = 0;
        numOfExisting = 0;
        for (int i = 0; i < listView.getCount(); i++) {
            numOfExisting++;
            if (((CheckBox) ((RelativeLayout) listView.getAdapter().getView(i, null, listView)).getChildAt(0)).isChecked())
                numOfBookmarked++;
        }
        //endregion

        //region OnItemClick Listener for ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                long viewId = view.getId();
                if (viewId == R.id.check_box) {
                    //region Perform Bookmark action
                    if (((CheckBox) (view)).isChecked()) {
                        numOfBookmarked++;
                    } else {
                        numOfBookmarked--;
                    }
                    if (numOfExisting > 4 && numOfBookmarked > 4) {
                        boolean isUpdated = false;
                        if (itemFor == LOCATIONS)
                            isUpdated = db.updateUserDataBookmark(LOCATIONS, dataModels.get(position).getItemId(), (short) (((CheckBox) (view)).isChecked() ? 1 : 0));
                        else if (itemFor == ACTIVITIES)
                            isUpdated = db.updateUserDataBookmark(ACTIVITIES, dataModels.get(position).getItemId(), (short) (((CheckBox) (view)).isChecked() ? 1 : 0));

                        if (isUpdated)
                            Log.d(TAG, "Updated");
                        else
                            Log.d(TAG, "Failed to update");
                    } else {
                        Toast.makeText(Location_Activity_List.this, "Please choose at least 4 items", Toast.LENGTH_LONG).show();
                    }
                    //endregion
                } else if (viewId == R.id.edit) {
                    //region Perform Edit action
                    //Start creating a new dialog with user data for edit action
                    final AlertDialog.Builder mBuilder = new AlertDialog.Builder(Location_Activity_List.this);
                    View mView = getLayoutInflater().inflate(R.layout.dialog_add_item, parent, false);

                    final EditText title = mView.findViewById(R.id.title);
                    final ImageView icon = mView.findViewById(R.id.ic_chosen);
                    GridView gvIcons = mView.findViewById(R.id.gv_icons);
                    Button btnCancel = mView.findViewById(R.id.btn_cancel);
                    Button btnUpdate = mView.findViewById(R.id.btn_save);
                    btnUpdate.setText(R.string.update);

                    //get user data from db before editting

                    if (itemFor == LOCATIONS)
                        user_data_res = db.getAllUserData(LOCATIONS);
                    else if (itemFor == ACTIVITIES)
                        user_data_res = db.getAllUserData(ACTIVITIES);

                    if (user_data_res.getCount() == 0) {
                        Toast.makeText(Location_Activity_List.this, "Failed to find user_data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    while (user_data_res.moveToNext()) {
                        if (user_data_res.getLong(0) == dataModels.get(position).getItemId()) {
                            //put user data to fields before editting
                            title.setText(user_data_res.getString(1));
                            icon.setImageResource(user_data_res.getInt(2));
                            icon.setTag(user_data_res.getInt(2));
                            break;
                        }
                    }

                    gvIcons.setAdapter(new Adapters.ImagesGridAdapter(Location_Activity_List.this));
                    gvIcons.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            ImageView imageView = (ImageView) view;
                            icon.setImageResource((int) imageView.getTag());
                            icon.setTag(imageView.getTag());
                        }
                    });

                    mBuilder.setView(mView);
                    final AlertDialog dialog = mBuilder.create();
                    dialog.show();

                    btnUpdate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (title != null && icon != null && !title.getText().toString().isEmpty() && icon.getTag() != null) {
                                boolean isUpdated = false;
                                if (itemFor == LOCATIONS)
                                    isUpdated = db.updateUserDataTextIcon(LOCATIONS, dataModels.get(position).getItemId(), title.getText().toString(), (int) icon.getTag());
                                else if (itemFor == ACTIVITIES)
                                    isUpdated = db.updateUserDataTextIcon(ACTIVITIES, dataModels.get(position).getItemId(), title.getText().toString(), (int) icon.getTag());

                                if (isUpdated)
                                    Toast.makeText(Location_Activity_List.this, "Saved", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(Location_Activity_List.this, "Failed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Location_Activity_List.this, "Add title/icon", Toast.LENGTH_SHORT).show();
                            }

                            dialog.dismiss();
                            initListView();

                        }
                    });

                    btnCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                            initListView();
                        }
                    });
                    //endregion
                } else if (viewId == R.id.delete) {
                    //region Perform Delete action
                    if (numOfExisting > 4) {
                        if (numOfBookmarked > 4) {
                            new AlertDialog.Builder(Location_Activity_List.this)
                                    .setTitle("Title")
                                    .setMessage("Do you really want to delete this item?")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Integer deletedRows = 0;
                                            if (itemFor == LOCATIONS)
                                                deletedRows = db.deleteUserData(LOCATIONS, dataModels.get(position).getItemId());
                                            else if (itemFor == ACTIVITIES)
                                                deletedRows = db.deleteUserData(ACTIVITIES, dataModels.get(position).getItemId());

                                            if (deletedRows > 0) {
                                                Toast.makeText(Location_Activity_List.this, "Deleted", Toast.LENGTH_SHORT).show();
                                                dataModels.remove(position);
                                            } else
                                                Toast.makeText(Location_Activity_List.this, "Not deleted", Toast.LENGTH_SHORT).show();
                                            initListView();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null).show();
                        } else {
                            Toast.makeText(Location_Activity_List.this, "Please check at least 4 items before delete", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        Toast.makeText(Location_Activity_List.this, "Please leave at least 4 items", Toast.LENGTH_LONG).show();
                    }


                    //endregion
                }
            }
        });
        //endregion
    }

    public void onNewItemClick(View view) {
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(Location_Activity_List.this);
        final ViewGroup nullParent = null;
        View mView = getLayoutInflater().inflate(R.layout.dialog_add_item, nullParent);

        final EditText title = mView.findViewById(R.id.title);
        final ImageView icon = mView.findViewById(R.id.ic_chosen);
        GridView gvIcons = mView.findViewById(R.id.gv_icons);
        Button btnCancel = mView.findViewById(R.id.btn_cancel);
        Button btnSave = mView.findViewById(R.id.btn_save);
        if (itemFor == LOCATIONS)
            title.setHint("Write your location");
        else if (itemFor == ACTIVITIES)
            title.setHint("Write your activity");
        else title.setHint("Write a text");

        btnSave.setText(R.string.save);

        gvIcons.setAdapter(new Adapters.ImagesGridAdapter(Location_Activity_List.this));
        gvIcons.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ImageView imageView = (ImageView) view;
                icon.setImageResource((int) imageView.getTag());
                icon.setTag(imageView.getTag());
            }
        });

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (icon != null && !title.getText().toString().isEmpty() && icon.getTag() != null) {
                    long itemId = System.currentTimeMillis();
                    boolean isInserted = false;

                    if (Tools.isDuplicated(userDataTexts, title.getText().toString())) {
                        Toast.makeText(Location_Activity_List.this, "This activity/place exists already", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (itemFor == LOCATIONS)
                        isInserted = db.insertNewUserData(LOCATIONS, itemId, title.getText().toString(), (int) icon.getTag(), (short) 1, 0);
                    else if (itemFor == ACTIVITIES)
                        isInserted = db.insertNewUserData(ACTIVITIES, itemId, title.getText().toString(), (int) icon.getTag(), (short) 1, 0);

                    if (isInserted)
                        Toast.makeText(Location_Activity_List.this, "Saved", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(Location_Activity_List.this, "Failed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Location_Activity_List.this, "Please, add title/icon", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
                initListView();

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                initListView();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        //initListView();
    }
}
