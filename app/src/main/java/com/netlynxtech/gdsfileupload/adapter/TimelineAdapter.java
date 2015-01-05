package com.netlynxtech.gdsfileupload.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.netlynxtech.gdsfileupload.FullScreenImageActivity;
import com.netlynxtech.gdsfileupload.R;
import com.netlynxtech.gdsfileupload.classes.Timeline;

import net.koofr.android.timeago.TimeAgo;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TimelineAdapter extends BaseAdapter {
    Context context;
    ArrayList<Timeline> data;
    private static LayoutInflater inflater = null;

    public TimelineAdapter(Context context, ArrayList<Timeline> data) {
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
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
            byte[] decodedString = Base64.decode(t.getImage(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.ivTimelineImage.setImageBitmap(decodedByte);
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
