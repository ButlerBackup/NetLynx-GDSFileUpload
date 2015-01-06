package com.netlynxtech.gdsfileupload.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.manuelpeinado.multichoiceadapter.extras.actionbarcompat.MultiChoiceBaseAdapter;
import com.netlynxtech.gdsfileupload.FullScreenImageActivity;
import com.netlynxtech.gdsfileupload.R;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;

import net.koofr.android.timeago.TimeAgo;

import java.util.ArrayList;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TimelineAdapter extends MultiChoiceBaseAdapter {
    Context context;
    ArrayList<Timeline> data;
    private static LayoutInflater inflater = null;

    public TimelineAdapter(Bundle savedInstanceState, Context context, ArrayList<Timeline> data) {
        super(savedInstanceState);
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_timeline_longpress, menu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.menu_discard) {
            discardSelectedItems();
            return true;
        }
        return false;
    }

    private void discardSelectedItems() {
        // http://stackoverflow.com/a/4950905/244576
        Set<Long> selection = getCheckedItems();
        ArrayList<Timeline> items = new ArrayList<>();
        for (long position : selection) {
            items.add(getItem((int) position));
        }
        SQLFunctions sql = new SQLFunctions(context);
        sql.open();
        for (Timeline item : items) {
            sql.deleteTimelineItem(item.getId());
            data.remove(item);
        }
        sql.close();
        finishActionMode();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
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
    public View getViewImpl(int i, View view, ViewGroup parent) {
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
