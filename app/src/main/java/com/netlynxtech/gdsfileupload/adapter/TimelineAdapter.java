package com.netlynxtech.gdsfileupload.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.netlynxtech.gdsfileupload.FullScreenImageActivity;
import com.netlynxtech.gdsfileupload.MainActivity;
import com.netlynxtech.gdsfileupload.R;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.squareup.picasso.Picasso;

import net.koofr.android.timeago.TimeAgo;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TimelineAdapter extends BaseAdapter {
    Context context;
    ArrayList<Timeline> data;
    private static LayoutInflater inflater = null;
    private SparseBooleanArray mSelectedItemsIds;

    public TimelineAdapter(Context context, ArrayList<Timeline> data) {
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSelectedItemsIds = new SparseBooleanArray();
    }

    public void remove(Timeline object) {
        SQLFunctions sql = new SQLFunctions(context);
        sql.open();
        sql.deleteTimelineItem(object.getId());
        data.remove(object);
        sql.close();
        notifyDataSetChanged();
    }

    public ArrayList<Timeline> getTimelineData() {
        return data;
    }

    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, value);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Timeline getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    static class ViewHolder {
        @InjectView(R.id.tvLocation)
        TextView tvLocation;
        @InjectView(R.id.tvTime)
        TextView tvTime;
        @InjectView(R.id.tvMessage)
        TextView tvMessage;
        @InjectView(R.id.ivTimelineImage)
        ImageView ivTimelineImage;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.activity_timeline_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        final Timeline t = data.get(i);
        String location = "<no location>";
        if (t.getLocation().equals("")) {
            if (!t.getLocationLat().equals("") && !t.getLocationLong().equals("")) {
                location = t.getLocationLat() + ", " + t.getLocationLong();
            }
        } else {
            location = t.getLocation();
        }
        holder.tvMessage.setText(t.getMessage().equals("") ? "<no message>" : t.getMessage());
        holder.tvLocation.setText(location);
        holder.tvTime.setText(TimeAgo.timeAgo(context, t.getUnixTime()));
        if (!t.getImage().equals("")) {
            File image = new File(new Utils(context).createThumbnailFolder(), t.getImage() + "_thumbnail");
            if (image.exists()) {
                Picasso.with(context).load(image).placeholder(R.drawable.ic_launcher).error(R.drawable.smrt_logo).into(holder.ivTimelineImage);
            } else {
                // Log.e("DOESNT EXIST", "DOESNT EXIST");
            }
            // Log.e("Thumbnail", image.getAbsolutePath().toString());
        }
        holder.ivTimelineImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(context, FullScreenImageActivity.class).putExtra("image", t.getImage()));
            }
        });
        return view;
    }
}
