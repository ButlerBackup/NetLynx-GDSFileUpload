package com.netlynxtech.gdsfileupload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationInfo;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;
import com.netlynxtech.gdsfileupload.apiclasses.SubmitMessage;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class NewTimelineItemPhotoActivity extends ActionBarActivity {
    @InjectView(R.id.ivNewTimelineImage)
    ImageView ivNewTimelineImage;

    @InjectView(R.id.etDescription)
    EditText etDescription;

    @InjectView(R.id.tvGetLocation)
    TextView tvGetLocation;

    @InjectView(R.id.bRefreshLocation)
    Button bRefreshLocation;

    String pictureFileName = "";

    File imgFile;
    Bitmap croppedImage;
    LocationInfo currentLocation;
    boolean isResendingPhoto = false;
    String locationName = "";
    Timeline timelineResent;
    uploadImage mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_new_item_timeline);
        ButterKnife.inject(NewTimelineItemPhotoActivity.this);
        if (getIntent().hasExtra(Consts.IMAGE_CAMERA_PASS_EXTRAS)) {
            pictureFileName = getIntent().getStringExtra(Consts.IMAGE_CAMERA_PASS_EXTRAS);
            imgFile = new File(new Utils(NewTimelineItemPhotoActivity.this).createFolder(), pictureFileName);
            Log.e("FILENAME", imgFile.getAbsolutePath().toString());
            loadImageFile();
        } else if (getIntent().hasExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS)) {
            String currentTime = System.currentTimeMillis() + "";
            pictureFileName = getIntent().getStringExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS);
            Uri uriPath = Uri.parse(pictureFileName);
            File tempFile = new File(uriPath.getPath());
            Log.e("FILENAME", uriPath.getPath());
            File destination = new File(new Utils(NewTimelineItemPhotoActivity.this).createFolder(), currentTime);
            try {
                FileUtils.copyFile(tempFile, destination);
                imgFile = new File(new Utils(NewTimelineItemPhotoActivity.this).createFolder(), currentTime);
                loadImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(NewTimelineItemPhotoActivity.this, "Failed to copy file to GDSFolder", Toast.LENGTH_SHORT).show();
            }
        } else if (getIntent().hasExtra(Consts.TIMELINE_ITEM_SELECTED_FROM_MAINACTIVITY)) {
            timelineResent = (Timeline) getIntent().getSerializableExtra(Consts.TIMELINE_ITEM_SELECTED_FROM_MAINACTIVITY);
            pictureFileName = timelineResent.getImage();
            imgFile = new File(new Utils(NewTimelineItemPhotoActivity.this).createFolder(), pictureFileName);
            isResendingPhoto = true;
            loadImageFile();
        } else {
            finish();
        }
        if (!isResendingPhoto) {
            tvGetLocation.setText(Consts.LOCATION_LOADING);
            LocationLibrary.forceLocationUpdate(NewTimelineItemPhotoActivity.this);
            refreshLocation(new LocationInfo(NewTimelineItemPhotoActivity.this));
            final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
            registerReceiver(lftBroadcastReceiver, lftIntentFilter);
        } else {
            etDescription.setText(timelineResent.getMessage());
            //Log.e("HERE", timelineResent.getLocation().toString());
            //Toast.makeText(NewTimelineItemPhotoActivity.this, timelineResent.getLocation().toString(), Toast.LENGTH_LONG).show();
            if (timelineResent.getLocation() != null && timelineResent.getLocation().length() > 0) {
                locationName = timelineResent.getLocation();
                tvGetLocation.setText(locationName);
            } else {
                tvGetLocation.setText(Consts.LOCATION_ERROR);
            }
            if (timelineResent.getLocationLat() != null && timelineResent.getLocationLat().length() > 0 && timelineResent.getLocationLong() != null && timelineResent.getLocationLong().length() > 0) {
                if (currentLocation == null) {
                    currentLocation = new LocationInfo(NewTimelineItemPhotoActivity.this);
                }
                currentLocation.lastLat = Float.parseFloat(timelineResent.getLocationLat());
                currentLocation.lastLong = Float.parseFloat(timelineResent.getLocationLong());
            }
        }
    }

    private void loadImageFile() {
        if (imgFile.exists()) {
            Log.e("File Size", imgFile.length() + "");
            Log.e("File Directory", imgFile.getAbsolutePath().toString());
            croppedImage = Utils.decodeSampledBitmapFromResource(imgFile);
            ivNewTimelineImage.setImageBitmap(croppedImage);
        } else {
            Toast.makeText(NewTimelineItemPhotoActivity.this, "No image found", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.tvGetLocation)
    public void refreshLocation() {
        Log.e("Refreshing", "Refreshing location");
        LocationLibrary.forceLocationUpdate(NewTimelineItemPhotoActivity.this);
        refreshLocation(new LocationInfo(NewTimelineItemPhotoActivity.this));
        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
        registerReceiver(lftBroadcastReceiver, lftIntentFilter);
    }

    @OnClick(R.id.bRefreshLocation)
    public void refresh() {
        Log.e("Refreshing", "Refreshing location");
        LocationLibrary.forceLocationUpdate(NewTimelineItemPhotoActivity.this);
        refreshLocation(new LocationInfo(NewTimelineItemPhotoActivity.this));
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
            //tvGetLocation.setText(locationInfo.lastLat + ", " + locationInfo.lastLong);
            currentLocation = locationInfo;
            if (locationInfo.hasLatestDataBeenBroadcast()) {
                Log.e("UPDATE", "Latest location has been broadcast");
                new getLocationPlaceName().execute();
            } else {

                // tvGetLocation.setText("Waiting for location.. (last " + LocationInfo.formatTimeAndDay(locationInfo.lastLocationUpdateTimestamp, true) + ")");
            }
        } else {
            tvGetLocation.setText(Consts.LOCATION_ERROR);
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
        if (id == R.id.mUpload) {
            if (etDescription.getText().toString().length() > 400) {
                Toast.makeText(NewTimelineItemPhotoActivity.this, "Description is more than 400 characters", Toast.LENGTH_LONG).show();
            } else {
                mTask = null;
                mTask = new uploadImage();
                mTask.execute();
            }
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private class getLocationPlaceName extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            NewTimelineItemPhotoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvGetLocation.setText(locationName);
                }
            });
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if (currentLocation != null) {
                    Geocoder geocoder = new Geocoder(NewTimelineItemPhotoActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(currentLocation.lastLat, currentLocation.lastLong, 1);
                    if (addresses != null && addresses.size() > 0) {
                        String cityName = addresses.get(0).getAddressLine(0);
                        String stateName = addresses.get(0).getAddressLine(1);
                        String countryName = addresses.get(0).getCountryName();
                        locationName = cityName + " " + stateName + " " + countryName;
                        Log.e("Location", locationName);
                    }
                } else {
                    locationName = Consts.LOCATION_ERROR;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class uploadImage extends AsyncTask<Void, Void, Void> {
        MaterialDialog dialog;
        WebAPIOutput res;
        String bitmapString;

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            NewTimelineItemPhotoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        if (res != null) {
                            if (res.getStatusCode() == 1) {
                                SQLFunctions sql = new SQLFunctions(NewTimelineItemPhotoActivity.this);
                                sql.open();
                                Timeline t = new Timeline();
                                t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                                t.setMessage(etDescription.getText().toString().trim());
                                t.setImage(imgFile.getName().toString());
                                t.setVideo("");
                                t.setLocation(locationName);
                                if (currentLocation == null) {
                                    t.setLocationLat("");
                                    t.setLocationLong("");
                                } else {
                                    t.setLocationLat(Float.toString(currentLocation.lastLat));
                                    t.setLocationLong(Float.toString(currentLocation.lastLong));
                                }
                                sql.insertTimelineItem(t);
                                sql.close();
                                finish();
                            } else {
                                Toast.makeText(NewTimelineItemPhotoActivity.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("Result", "There were no response from server");
                            Toast.makeText(NewTimelineItemPhotoActivity.this, "There were no response from server", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //Toast.makeText(NewTimelineItemPhotoActivity.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Utils u = new Utils(NewTimelineItemPhotoActivity.this);
                bitmapString = u.convertBitmapToString(croppedImage);
                SubmitMessage m;
                if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                    locationName = "";
                } else {
                    locationName = locationName.replace("null", "").trim();
                }
                if (currentLocation != null) {
                    m = new SubmitMessage(u.getUnique(), etDescription.getText().toString().trim(), imgFile.getName() + ".jpg", bitmapString, Float.toString(currentLocation.lastLat), Float.toString(currentLocation.lastLong), locationName);
                } else {
                    m = new SubmitMessage(u.getUnique(), etDescription.getText().toString().trim(), imgFile.getName() + ".jpg", bitmapString, "", "", locationName);
                }
                try {
                    res = MainApplication.apiService.uploadContentWithMessage(m);
                    if (!isResendingPhoto && res != null) {
                        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imgFile.getAbsolutePath().toString()), 180, 180);
                        new Utils(NewTimelineItemPhotoActivity.this).saveImageToFolder(thumbnail, imgFile.getName().toString() + "_thumbnail");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(NewTimelineItemPhotoActivity.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new MaterialDialog.Builder(NewTimelineItemPhotoActivity.this)
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
        try {
            unregisterReceiver(lftBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
