package kr.ac.kaist.lockscreen;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Adapters {

    //Adapter for GridView of icons+text
    public static class GridAdapter extends BaseAdapter {

        public static int USUAL_ITEM_TAG = 0;
        public static int ADD_NEW_ITEM_TAG = 1;
        private List<Integer> icons;
        private List<String> titles;
        private Context context;
        private LayoutInflater inflater;

        public GridAdapter(Context context, List<Integer> icons, List<String> titles) {
            this.context = context;
            this.titles = titles;
            this.icons = icons;
        }

        @Override
        public int getCount() {
            return titles.size();
        }

        @Override
        public Object getItem(int i) {
            return titles;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            View gridView = view;

            if (view == null) {
                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (inflater != null) {
                    gridView = inflater.inflate(R.layout.button_layout, null);
                }

            }

            ImageView img = gridView.findViewById(R.id.grid_item_img);
            TextView txt = gridView.findViewById(R.id.grid_item_txt);
            img.setImageResource(icons.get(i));
            txt.setText(titles.get(i));

            if (icons.get(i) == R.drawable.ic_add) {
                gridView.setTag(ADD_NEW_ITEM_TAG);
            } else {
                gridView.setTag(USUAL_ITEM_TAG);
            }

            return gridView;
        }
    }

    //Adapter for ListView of items
    public static class ListAdapter extends ArrayAdapter<ListDataModel> {

        public static final String TAG = "ListAdapter";
        private ArrayList<ListDataModel> dataset;
        LayoutInflater inflater;
        Context context;

        private static class ViewHolder {
            CheckBox checkBox;
            ImageView icon;
            TextView text;
            ImageView editItem;
            ImageView deleteItem;
        }

        ListAdapter(Context context, ArrayList<ListDataModel> data) {
            super(context, R.layout.row_item, data);
            this.dataset = data;
            this.context = context;
        }

        private int lastPosition = -1;

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull final ViewGroup parent) {
            // Get the data item for this position
            ListDataModel dataModel = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            ViewHolder viewHolder; // view lookup cache stored in tag

            final View result;

            if (convertView == null) {
                viewHolder = new ViewHolder();
                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.row_item, parent, false);
                viewHolder.icon = convertView.findViewById(R.id.icon_activity);
                viewHolder.text = convertView.findViewById(R.id.choice_text);
                viewHolder.checkBox = convertView.findViewById(R.id.check_box);
                viewHolder.editItem = convertView.findViewById(R.id.edit);
                viewHolder.deleteItem = convertView.findViewById(R.id.delete);

                //region Perform click for needed buttons(solution from :http://www.migapro.com/click-events-listview-gridview/)
                viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((ListView) parent).performItemClick(view, position, 0); // Let the event be handled in onItemClick()
                    }
                });
                viewHolder.editItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((ListView) parent).performItemClick(view, position, 0); // Let the event be handled in onItemClick()
                    }
                });
                viewHolder.deleteItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((ListView) parent).performItemClick(view, position, 0); // Let the event be handled in onItemClick()
                    }
                });
                //endregion

                result = convertView;
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                result = convertView;
            }

            lastPosition = position;

            viewHolder.checkBox.setChecked(dataModel.getBookmark());
            viewHolder.text.setText(dataModel.getText());
            viewHolder.icon.setImageResource(dataModel.getIconResource());
            // Return the completed view to render on screen
            return result;
        }
    }

    //Adapter for GridView of images while customization
    public static class ImagesGridAdapter extends BaseAdapter {
        private Integer[] icons = {
                R.drawable.icon_location_home,
                R.drawable.icon_activity_rest,
                R.drawable.icon_location_cafe,
                R.drawable.icon_activity_communicate,
                R.drawable.icon_location_library,
                R.drawable.icon_activity_eat,
                R.drawable.icon_location_office,
                R.drawable.icon_activity_sleep,
                R.drawable.icon_location_restaurant,
                R.drawable.icon_activity_study,
                R.drawable.icon_location_school,
                R.drawable.icon_activity_workout,
                R.drawable.icon_activity_work
        };

        private Activity activity;

        public ImagesGridAdapter(Activity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            return icons.length;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            //chosenIcon = activity.findViewById(R.id.ic_chosen);

            final ImageView imageView;
            if (view == null) {
                imageView = new ImageView(activity);
                imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
            } else {
                imageView = (ImageView) view;
            }
            imageView.setImageResource(icons[i]);
            imageView.setTag(icons[i]);

            return imageView;
        }
    }

    //Adapter for ListView of history activity
    public static class HistoryListAdapter extends ArrayAdapter<HistoryListDataModel> {

        public static final String TAG = "HistoryListAdapter";
        private ArrayList<HistoryListDataModel> dataset;
        LayoutInflater inflater;
        boolean isDetailed = false;
        Context context;

        private static class ViewHolder {
            LinearLayout detailedViewTimeHidden;
            LinearLayout detailedViewOthersHidden;
            TextView start_time;
            TextView end_time;
            ImageView iconActivity;
            TextView textActivity;
            ImageView iconLocation;
            TextView textLocation;
            TextView duration;
            ImageView edit;
        }

        HistoryListAdapter(Context context, ArrayList<HistoryListDataModel> data, boolean isDetailed) {
            super(context, R.layout.history_row_item, data);
            this.context = context;
            this.isDetailed = isDetailed;
            this.dataset = data;

        }

        private int lastPosition = -1;

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull final ViewGroup parent) {
            // Get the data item for this position
            HistoryListDataModel dataModel = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            HistoryListAdapter.ViewHolder viewHolder; // view lookup cache stored in tag

            final View result;
            if (convertView == null) {
                viewHolder = new HistoryListAdapter.ViewHolder();
                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.history_row_item, parent, false);
                viewHolder.detailedViewTimeHidden = convertView.findViewById(R.id.detailed_view_time);
                viewHolder.detailedViewOthersHidden = convertView.findViewById(R.id.detailed_view_others);
                viewHolder.start_time = convertView.findViewById(R.id.time_start);
                viewHolder.end_time = convertView.findViewById(R.id.time_end);
                viewHolder.duration = convertView.findViewById(R.id.activity_duration);
                viewHolder.iconActivity = convertView.findViewById(R.id.icon_activity);
                viewHolder.textActivity = convertView.findViewById(R.id.text_activity);
                viewHolder.iconLocation = convertView.findViewById(R.id.icon_location);
                viewHolder.textLocation = convertView.findViewById(R.id.text_location);
                viewHolder.edit = convertView.findViewById(R.id.edit);

                result = convertView;
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (HistoryListAdapter.ViewHolder) convertView.getTag();
                result = convertView;
            }

            //region Perform click for needed buttons(solution from :http://www.migapro.com/click-events-listview-gridview/)
            /*viewHolder.edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((ListView) parent).performItemClick(view, position, 0); // Let the event be handled in onItemClick()
                }
            });*/
            //endregion

            lastPosition = position;


            //region Setting duration text
            int hour;
            int min;
            int sec;
            if (dataModel.getDuration() > 0) {
                if (dataModel.getDuration() < 60) {
                    sec = dataModel.getDuration();
                    viewHolder.duration.setText(String.format(Locale.ENGLISH, "00:00:%d", sec));
                } else if (dataModel.getDuration() < 3600) {
                    min = dataModel.getDuration() / 60;
                    sec = dataModel.getDuration() % 60;
                    viewHolder.duration.setText(String.format(Locale.ENGLISH, "00:%d:%d", min, sec));
                } else {
                    hour = dataModel.getDuration() / 3600;
                    min = (dataModel.getDuration() % 3600) / 60;
                    sec = dataModel.getDuration() % 60;
                    viewHolder.duration.setText(String.format(Locale.ENGLISH, "%d:%d:%d", hour, min, sec));
                }
            } else {
                viewHolder.duration.setText("Undefined");
            }
            //endregion


            viewHolder.iconActivity.setImageResource(dataModel.getIconActivity());
            viewHolder.textActivity.setText(dataModel.getTextActivity());
            viewHolder.iconLocation.setImageResource(dataModel.getIconLocation());
            viewHolder.textLocation.setText(dataModel.getTextLocation());


            if (isDetailed) {
                //region Setting start and end time
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(dataModel.getStartTime());
                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(dataModel.getEndTime());

                viewHolder.start_time.setText(String.format(Locale.ENGLISH, "%d:%d", startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE)));
                viewHolder.end_time.setText(String.format(Locale.ENGLISH, "%d:%d", endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE)));
                //endregion

                viewHolder.detailedViewTimeHidden.setVisibility(View.VISIBLE);
                viewHolder.detailedViewOthersHidden.setVisibility(View.VISIBLE);
                viewHolder.duration.setVisibility(View.GONE);
            } else {
                viewHolder.detailedViewTimeHidden.setVisibility(View.GONE);
                viewHolder.detailedViewOthersHidden.setVisibility(View.GONE);
                viewHolder.duration.setVisibility(View.VISIBLE);
            }

            // Return the completed view to render on screen
            return result;
        }
    }

}







