package com.netlynxtech.gdsfileupload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.view.MenuItem;

import com.netlynxtech.gdsfileupload.classes.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;

public class FullScreenImageActivity extends ActionBarActivity {
    @InjectView(R.id.ivImageZoom)
    ImageViewTouch ivImageZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_fullscreen_image);
        ButterKnife.inject(FullScreenImageActivity.this);
        Picasso.with(FullScreenImageActivity.this).load(new File(new Utils(FullScreenImageActivity.this).createFolder(), getIntent().getStringExtra("image"))).into(ivImageZoom);
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
