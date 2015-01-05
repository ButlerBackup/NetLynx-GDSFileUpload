package com.netlynxtech.gdsfileupload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mrengineer13.snackbar.SnackBar;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationInfo;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;
import com.netlynxtech.gdsfileupload.apiclasses.SubmitMessage;
import com.netlynxtech.gdsfileupload.classes.MyLocation;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;

import java.io.File;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class NewTimelineItemActivity extends ActionBarActivity {
    @InjectView(R.id.ivNewTimelineImage)
    ImageView ivNewTimelineImage;

    @InjectView(R.id.etDescription)
    EditText etDescription;

    @InjectView(R.id.tvGetLocation)
    TextView tvGetLocation;

    String pictureFileName = "";

    File imgFile;
    Bitmap croppedImage;
    MyLocation myLocation;
    LocationInfo currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        if (getIntent().hasExtra(Consts.NEW_CAMERA_IMAGE_CROP_LIB_PASS_EXTRA)) {
            File dir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            pictureFileName = getIntent().getStringExtra(Consts.NEW_CAMERA_IMAGE_CROP_LIB_PASS_EXTRA);
            Log.e("FILENAME", dir.toString() + "/" + pictureFileName);
            imgFile = new File(dir.toString() + "/" + pictureFileName);
        } else if (getIntent().hasExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS)) {
            pictureFileName = getIntent().getStringExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS);
            Uri uriPath = Uri.parse(pictureFileName);
            imgFile = new File(uriPath.getPath());
            Log.e("FILENAME", uriPath.getPath());
        } else {
            finish();
        }
        setContentView(R.layout.activity_new_item_timeline);
        ButterKnife.inject(NewTimelineItemActivity.this);
        if (imgFile.exists()) {
            Log.e("File Size", imgFile.length() + "");
            croppedImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ivNewTimelineImage.setImageBitmap(croppedImage);
        } else {
            new SnackBar.Builder(NewTimelineItemActivity.this).withMessage("NO IMAGE").withStyle(SnackBar.Style.ALERT).withDuration(SnackBar.LONG_SNACK).show();
        }
        LocationLibrary.forceLocationUpdate(NewTimelineItemActivity.this);
        refreshLocation(new LocationInfo(NewTimelineItemActivity.this));
        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
        registerReceiver(lftBroadcastReceiver, lftIntentFilter);
    }

    @OnClick(R.id.tvGetLocation)
    public void refreshLocation() {
        LocationLibrary.forceLocationUpdate(NewTimelineItemActivity.this);
        refreshLocation(new LocationInfo(NewTimelineItemActivity.this));
        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
        registerReceiver(lftBroadcastReceiver, lftIntentFilter);
    }

    private final BroadcastReceiver lftBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final LocationInfo locationInfo = (LocationInfo) intent.getSerializableExtra(LocationLibraryConstants.LOCATION_BROADCAST_EXTRA_LOCATIONINFO);
            refreshLocation(locationInfo);
        }
    };

    private void refreshLocation(final LocationInfo locationInfo) {
        if (locationInfo.anyLocationDataReceived()) {
            tvGetLocation.setText(locationInfo.lastLat + "|" + locationInfo.lastLong);
            currentLocation = locationInfo;
            if (locationInfo.hasLatestDataBeenBroadcast()) {
                Log.e("UPDATE", "Latest location has been broadcast");
            } else {
                tvGetLocation.setText("Location broadcast pending (last " + LocationInfo.formatTimeAndDay(locationInfo.lastLocationUpdateTimestamp, true) + ")");
            }
        } else {
            tvGetLocation.setText("No locations recorded yet");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_timeline_item_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mRetakePhoto) {
            return true;
        } else if (id == R.id.mUpload) {
            new uploadImage().execute();
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private class uploadImage extends AsyncTask<Void, Void, Void> {
        MaterialDialog dialog;
        WebAPIOutput res;
        String bitmapString, locationName = "";

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            NewTimelineItemActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    if (res != null) {
                        if (res.getStatusCode() == 1) {
                            SQLFunctions sql = new SQLFunctions(NewTimelineItemActivity.this);
                            sql.open();
                            Timeline t = new Timeline();
                            t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                            t.setMessage(etDescription.getText().toString().trim());
                            t.setImage(bitmapString);
                            t.setVideo("");
                            t.setLocation(!locationName.equals("") ? locationName : "");
                            t.setLocationLat(Float.toString(currentLocation.lastLat));
                            t.setLocationLong(Float.toString(currentLocation.lastLong));
                            sql.insertTimelineItem(t);
                            sql.close();
                            finish();
                        } else {
                            new SnackBar.Builder(NewTimelineItemActivity.this).withMessage(res.getStatusDescription()).withStyle(SnackBar.Style.ALERT).withDuration(SnackBar.LONG_SNACK).show();
                        }
                    }
                }
            });
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Utils u = new Utils(NewTimelineItemActivity.this);
                Log.e("File Size", (croppedImage.getRowBytes() * croppedImage.getHeight()) + "");
                bitmapString = u.convertBitmapToString(croppedImage);
                SubmitMessage m;
                if (currentLocation != null) {
                    Geocoder geocoder = new Geocoder(NewTimelineItemActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(currentLocation.lastLat, currentLocation.lastLong, 1);
                    if (addresses.size() > 0) {
                        String cityName = addresses.get(0).getAddressLine(0);
                        String stateName = addresses.get(0).getAddressLine(1);
                        String countryName = addresses.get(0).getAddressLine(2);
                        locationName = cityName + " " + stateName + " " + countryName;
                        Log.e("Location", locationName);
                    }
                    m = new SubmitMessage(u.getUnique(), etDescription.getText().toString().trim(), imgFile.getName() + ".jpg", bitmapString, Float.toString(currentLocation.lastLat), Float.toString(currentLocation.lastLong));
                } else {
                    m = new SubmitMessage(u.getUnique(), etDescription.getText().toString().trim(), imgFile.getName() + ".jpg", bitmapString, "null", "null");
                }

                res = MainApplication.apiService.uploadContentWithMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new MaterialDialog.Builder(NewTimelineItemActivity.this)
                    .title("Uploading..")
                    .cancelable(false)
                    .customView(R.layout.loading, true)
                    .build();
            dialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // myLocation.cancelTimer();
        unregisterReceiver(lftBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // myLocation.cancelTimer();
    }
}
