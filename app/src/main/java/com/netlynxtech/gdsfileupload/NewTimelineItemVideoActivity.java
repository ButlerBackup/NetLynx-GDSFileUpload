package com.netlynxtech.gdsfileupload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationInfo;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;
import com.netcompss.loader.LoadJNI;
import com.netlynxtech.gdsfileupload.apiclasses.SubmitMessage;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;
import com.netlynxtech.gdsfileupload.service.UploadVideoService;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class NewTimelineItemVideoActivity extends ActionBarActivity {
    @InjectView(R.id.ivNewTimelineVideo)
    VideoView ivNewTimelineVideo;

    @InjectView(R.id.etDescription)
    EditText etDescription;

    @InjectView(R.id.tvGetLocation)
    TextView tvGetLocation;

    @InjectView(R.id.bRefreshLocation)
    Button bRefreshLocation;

    String videoFileName = "";

    File videoFile;
    LocationInfo currentLocation;
    boolean isResendingVideo = false;
    String locationName = "";
    Timeline timelineResent;
    uploadVideo mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_new_item_video_timeline);
        ButterKnife.inject(NewTimelineItemVideoActivity.this);
        if (getIntent().hasExtra(Consts.VIDEO_CAMERA_PASS_EXTRAS)) {
            videoFileName = getIntent().getStringExtra(Consts.VIDEO_CAMERA_PASS_EXTRAS);
            Uri uriPath = Uri.parse(videoFileName);
            videoFile = new File(uriPath.getPath());
            Log.e("FILENAME", uriPath.getPath());
            loadVideoFile();
        } else if (getIntent().hasExtra(Consts.TIMELINE_ITEM_SELECTED_FROM_MAINACTIVITY)) {
            timelineResent = (Timeline) getIntent().getSerializableExtra(Consts.TIMELINE_ITEM_SELECTED_FROM_MAINACTIVITY);
            videoFileName = timelineResent.getVideo();
            videoFile = new File(new Utils(NewTimelineItemVideoActivity.this).createFolder(), videoFileName);
            isResendingVideo = true;
            loadVideoFile();
        } else if (getIntent().hasExtra(Consts.VIDEO_CAMERA_PASS_EXTRAS_PURE)) {
            String currentTime = System.currentTimeMillis() + "";
            videoFileName = getIntent().getStringExtra(Consts.VIDEO_CAMERA_PASS_EXTRAS_PURE);
            Uri uriPath = Uri.parse(videoFileName);
            File tempFile = new File(uriPath.getPath());
            Log.e("FILENAME", uriPath.getPath());
            File destination = new File(new Utils(NewTimelineItemVideoActivity.this).createFolder(), currentTime);
            try {
                FileUtils.copyFile(tempFile, destination);
                videoFile = new File(new Utils(NewTimelineItemVideoActivity.this).createFolder(), currentTime);
                videoFileName = videoFile.getName();
                loadVideoFile();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(NewTimelineItemVideoActivity.this, "Failed to copy file to GDSFolder", Toast.LENGTH_SHORT).show();
            }
        } else {
            finish();
        }
        if (!isResendingVideo) {
            tvGetLocation.setText(Consts.LOCATION_LOADING);
            LocationLibrary.forceLocationUpdate(NewTimelineItemVideoActivity.this);
            refreshLocation(new LocationInfo(NewTimelineItemVideoActivity.this));
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
                    currentLocation = new LocationInfo(NewTimelineItemVideoActivity.this);
                }
                currentLocation.lastLat = Float.parseFloat(timelineResent.getLocationLat());
                currentLocation.lastLong = Float.parseFloat(timelineResent.getLocationLong());
            }
        }
    }

    private void loadVideoFile() {
        if (videoFile.exists()) {
            Log.e("File Size", videoFile.length() + "");
            Log.e("File Directory", videoFile.getAbsolutePath().toString());
            ivNewTimelineVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });
            ivNewTimelineVideo.setVideoURI(Uri.parse(videoFile.getAbsolutePath().toString()));
            ivNewTimelineVideo.start();
        } else {
            Toast.makeText(NewTimelineItemVideoActivity.this, "No video found", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.tvGetLocation)
    public void refreshLocation() {
        Log.e("Refreshing", "Refreshing location");
        LocationLibrary.forceLocationUpdate(NewTimelineItemVideoActivity.this);
        refreshLocation(new LocationInfo(NewTimelineItemVideoActivity.this));
        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
        registerReceiver(lftBroadcastReceiver, lftIntentFilter);
    }

    @OnClick(R.id.bRefreshLocation)
    public void refresh() {
        Log.e("Refreshing", "Refreshing location");
        LocationLibrary.forceLocationUpdate(NewTimelineItemVideoActivity.this);
        refreshLocation(new LocationInfo(NewTimelineItemVideoActivity.this));
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
            long fileSizeInBytes = videoFile.length();
            long fileSizeInKB = fileSizeInBytes / 1024;
            long fileSizeInMB = fileSizeInKB / 1024;
            if (fileSizeInMB <= 99999999) {
                if (etDescription.getText().toString().length() > 400) {
                    Toast.makeText(NewTimelineItemVideoActivity.this, "Description is more than 400 characters", Toast.LENGTH_LONG).show();
                } else {
                    Intent i = new Intent(NewTimelineItemVideoActivity.this, UploadVideoService.class);
                    if (etDescription.getText().toString() != null && etDescription.getText().toString().trim().length() > 0) {
                        i.putExtra("message", etDescription.getText().toString().trim());
                    } else {
                        i.putExtra("message", "");
                    }
                    i.putExtra("locationName", locationName);
                    if (currentLocation != null) {
                        i.putExtra("locationLat", Float.toString(currentLocation.lastLat));
                        i.putExtra("locationLong", Float.toString(currentLocation.lastLong));
                    } else {
                        i.putExtra("locationLat", "");
                        i.putExtra("locationLong", "");
                    }
                    i.putExtra("file", videoFile.getAbsoluteFile().toString());
                    Toast.makeText(NewTimelineItemVideoActivity.this, "Video will be processed in the background. You will be notified of any changes", Toast.LENGTH_LONG).show();
                    //startService(i);
                    WakefulIntentService.sendWakefulWork(NewTimelineItemVideoActivity.this, i);
                    finish();
                    //new uploadVideo().execute();
                }
            } else {
                Toast.makeText(NewTimelineItemVideoActivity.this, "File is larger than 2 MB. Size : " + fileSizeInMB + "mb", Toast.LENGTH_LONG).show();
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
            NewTimelineItemVideoActivity.this.runOnUiThread(new Runnable() {
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
                    Geocoder geocoder = new Geocoder(NewTimelineItemVideoActivity.this, Locale.getDefault());
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

    private class uploadVideo extends AsyncTask<Void, Void, Void> {
        MaterialDialog dialog;
        WebAPIOutput res;
        String videoString;

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            NewTimelineItemVideoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        if (res != null) {
                            if (res.getStatusCode() == 1) {
                                SQLFunctions sql = new SQLFunctions(NewTimelineItemVideoActivity.this);
                                sql.open();
                                Timeline t = new Timeline();
                                t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                                t.setMessage(etDescription.getText().toString().trim());
                                t.setImage("");
                                t.setVideo(videoFile.getName().toString());
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
                                Toast.makeText(NewTimelineItemVideoActivity.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("Result", "There were no response from server");
                            Toast.makeText(NewTimelineItemVideoActivity.this, "There were no response from server", Toast.LENGTH_LONG).show();
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

                String compressVideoTime = System.currentTimeMillis() + "_compressed.mp4";
                LoadJNI vk = new LoadJNI();
                try {
                    String workFolder = getApplicationContext().getFilesDir().getAbsolutePath();
                    String[] complexCommand = {"ffmpeg", "-y", "-i", videoFile.getAbsolutePath().toString(), "-strict", "experimental", "-s", "640x480", "-r", "25", "-vcodec", "mpeg4", "-b", "512k", "-ab", "48000", "-ac", "2", "-ar", "22050", "/sdcard/gdsupload/" + compressVideoTime};
                    // -r = fps
                    // vcodec = video codec
                    //ar = audio sample frequency
                    vk.run(complexCommand, workFolder, getApplicationContext());
                    Log.i("test", "ffmpeg4android finished successfully");
                } catch (Throwable e) {
                    Log.e("test", "vk run exception.", e);
                }
                //videoFile.delete();
                videoFile = new File("/sdcard/gdsupload/" + compressVideoTime);
                Utils u = new Utils(NewTimelineItemVideoActivity.this);
                videoString = u.convertVideoToString(videoFile);
                SubmitMessage m;
                if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                    locationName = "";
                } else {
                    locationName = locationName.replace("null", "").trim();
                }
                if (currentLocation != null) {
                    m = new SubmitMessage(u.getUnique(), etDescription.getText().toString().trim(), videoFile.getName(), videoString, Float.toString(currentLocation.lastLat), Float.toString(currentLocation.lastLong), locationName);
                } else {
                    m = new SubmitMessage(u.getUnique(), etDescription.getText().toString().trim(), videoFile.getName(), videoString, "", "", locationName);
                }
                try {
                    res = MainApplication.apiService.uploadContentWithMessage(m);
                    if (!isResendingVideo && res != null) {
                        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath().toString(), MediaStore.Video.Thumbnails.MINI_KIND);
                        new Utils(NewTimelineItemVideoActivity.this).saveImageToFolder(thumbnail, videoFile.getName().toString() + "_thumbnail");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(NewTimelineItemVideoActivity.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new MaterialDialog.Builder(NewTimelineItemVideoActivity.this)
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
