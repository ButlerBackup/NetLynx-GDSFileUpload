package com.netlynxtech.gdsfileupload;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.VideoView;

import com.netlynxtech.gdsfileupload.classes.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;

public class FullScreenImageActivity extends ActionBarActivity {
    @InjectView(R.id.ivImageZoom)
    ImageViewTouch ivImageZoom;

    @InjectView(R.id.vvVideoZoom)
    VideoView vvVideoZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_fullscreen_image);
        ButterKnife.inject(FullScreenImageActivity.this);
        if (getIntent().hasExtra("image")) {
            Picasso.with(FullScreenImageActivity.this).load(new File(new Utils(FullScreenImageActivity.this).createFolder(), getIntent().getStringExtra("image"))).into(ivImageZoom);
            vvVideoZoom.setVisibility(View.GONE);
        } else if (getIntent().hasExtra("video")) {
            ivImageZoom.setVisibility(View.GONE);
            vvVideoZoom.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });
            vvVideoZoom.setVideoURI(Uri.parse(new Utils(FullScreenImageActivity.this).createFolder() + "/" + getIntent().getStringExtra("video")));
            vvVideoZoom.start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
