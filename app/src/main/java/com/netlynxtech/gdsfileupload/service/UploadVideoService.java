package com.netlynxtech.gdsfileupload.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.netcompss.loader.LoadJNI;
import com.netlynxtech.gdsfileupload.Consts;
import com.netlynxtech.gdsfileupload.MainApplication;
import com.netlynxtech.gdsfileupload.R;
import com.netlynxtech.gdsfileupload.apiclasses.SubmitMessage;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;

import java.io.File;

public class UploadVideoService extends IntentService {
    File videoFile;
    String message, locationName, locationLat, locationLong;
    uploadVideo mTask;
    NotificationCompat.Builder mBuilder;

    public UploadVideoService() {
        super("UploadVideoService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!intent.hasExtra("file")) {
            Log.e("Intent", "no file");
        }
        if (!intent.hasExtra("message")) {
            Log.e("Intent", "no message");
        }
        if (!intent.hasExtra("locationName")) {
            Log.e("Intent", "no locationName");
        }
        if (!intent.hasExtra("locationLat")) {
            Log.e("Intent", "no locationLat");
        }
        if (!intent.hasExtra("locationLong")) {
            Log.e("Intent", "no locationLong");
        }


        if (intent.hasExtra("file") && intent.hasExtra("message") && intent.hasExtra("locationName") && intent.hasExtra("locationLat") && intent.hasExtra("locationLong")) {
            videoFile = new File(intent.getStringExtra("file"));
            message = intent.getStringExtra("message");
            locationName = intent.getStringExtra("locationName");
            locationLat = intent.getStringExtra("locationLat");
            locationLong = intent.getStringExtra("locationLong");
            Toast.makeText(UploadVideoService.this, "Video will be processed in the background. You will be notified of any changes", Toast.LENGTH_LONG).show();
            showNotification(Consts.SERVICE_VIDEO_UPLOAD, "Uploading video", message, true);
           /* mTask = null;
            mTask = new uploadVideo();
            mTask.execute();*/
            new Runnable() {
                WebAPIOutput res;
                String videoString;
                Utils u;

                @Override
                public void run() {
                    try {
                        u = new Utils(UploadVideoService.this);
                        try {
                            updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Compressing Video");
                            Log.e("UploadVideoService", "Compressing video");
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
                            Log.e("UploadVideoService", "Done compressing video. Now uploading");

                            //videoFile.delete();
                            updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Uploading Video");
                            videoFile = new File("/sdcard/gdsupload/" + compressVideoTime);
                            Utils u = new Utils(UploadVideoService.this);
                            videoString = u.convertVideoToString(videoFile);
                            SubmitMessage m;
                            if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                                locationName = "";
                            } else {
                                locationName = locationName.replace("null", "").trim();
                            }
                            m = new SubmitMessage(u.getUnique(), message, videoFile.getName(), videoString, locationLat, locationLong, locationName);

                            try {
                                res = MainApplication.apiService.uploadContentWithMessage(m);
                                if (res != null) {
                                    Log.e("UploadVideoService", "Creating thumbnail");

                                    updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Creating Video Thumbnail");
                                    Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath().toString(), MediaStore.Video.Thumbnails.MINI_KIND);
                                    new Utils(UploadVideoService.this).saveImageToFolder(thumbnail, videoFile.getName().toString() + "_thumbnail");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(UploadVideoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("ServiceDemo", "Service was interrupted.", e);
                    }
                    try {
                        new Utils(UploadVideoService.this).cancelNotification(Consts.SERVICE_VIDEO_UPLOAD);
                        if (res != null) {
                            if (res.getStatusCode() == 1) {
                                SQLFunctions sql = new SQLFunctions(UploadVideoService.this);
                                sql.open();
                                Timeline t = new Timeline();
                                t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                                t.setMessage(message);
                                t.setImage("");
                                t.setVideo(videoFile.getName().toString());
                                t.setLocation(locationName);
                                t.setLocationLat(locationLat);
                                t.setLocationLong(locationLong);
                                sql.insertTimelineItem(t);
                                sql.close();
                                showNotification(0, "Video uploaded!", message, false);
                                startService(new Intent(UploadVideoService.this, MediaScannerService.class).putExtra("file", videoFile.getAbsoluteFile().toString()).putExtra("image", false));
                                stopSelf();
                                // show notification
                            } else {
                                showNotification(0, res.getStatusDescription(), res.getStatusDescription(), false);
                                Toast.makeText(UploadVideoService.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("Result", "There were no response from server");
                            Toast.makeText(UploadVideoService.this, "There were no response from server", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(UploadVideoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
                    }
                }
            }.run();
        } else {
            Log.e("SERVICE", "NO PARAMETER");
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class uploadVideo extends AsyncTask<Void, Void, Void> {
        WebAPIOutput res;
        String videoString;
        Utils u;

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                new Utils(UploadVideoService.this).cancelNotification(Consts.SERVICE_VIDEO_UPLOAD);
                if (res != null) {
                    if (res.getStatusCode() == 1) {
                        SQLFunctions sql = new SQLFunctions(UploadVideoService.this);
                        sql.open();
                        Timeline t = new Timeline();
                        t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                        t.setMessage(message);
                        t.setImage("");
                        t.setVideo(videoFile.getName().toString());
                        t.setLocation(locationName);
                        t.setLocationLat(locationLat);
                        t.setLocationLong(locationLong);
                        sql.insertTimelineItem(t);
                        sql.close();
                        showNotification(0, "Video uploaded!", message, false);
                        startService(new Intent(UploadVideoService.this, MediaScannerService.class).putExtra("file", videoFile.getAbsoluteFile().toString()).putExtra("image", false));
                        stopSelf();
                        // show notification
                    } else {
                        showNotification(0, res.getStatusDescription(), res.getStatusDescription(), false);
                        Toast.makeText(UploadVideoService.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("Result", "There were no response from server");
                    Toast.makeText(UploadVideoService.this, "There were no response from server", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(UploadVideoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {
            u = new Utils(UploadVideoService.this);
            try {
                updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Compressing Video");
                Log.e("UploadVideoService", "Compressing video");
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
                Log.e("UploadVideoService", "Done compressing video. Now uploading");

                //videoFile.delete();
                updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Uploading Video");
                videoFile = new File("/sdcard/gdsupload/" + compressVideoTime);
                Utils u = new Utils(UploadVideoService.this);
                videoString = u.convertVideoToString(videoFile);
                SubmitMessage m;
                if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                    locationName = "";
                } else {
                    locationName = locationName.replace("null", "").trim();
                }
                m = new SubmitMessage(u.getUnique(), message, videoFile.getName(), videoString, locationLat, locationLong, locationName);

                try {
                    res = MainApplication.apiService.uploadContentWithMessage(m);
                    if (res != null) {
                        Log.e("UploadVideoService", "Creating thumbnail");

                        updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Creating Video Thumbnail");
                        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath().toString(), MediaStore.Video.Thumbnails.MINI_KIND);
                        new Utils(UploadVideoService.this).saveImageToFolder(thumbnail, videoFile.getName().toString() + "_thumbnail");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(UploadVideoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
            return null;
        }
    }

    public void showNotification(int id, String title, String content, boolean autoCancel) {
        final Intent emptyIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(UploadVideoService.this, 0, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(UploadVideoService.this)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(autoCancel)
                .setContentIntent(pendingIntent); //Required on Gingerbread and below
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, mBuilder.build());
    }

    public void updateNotification(int id, String title) {
        mBuilder.setContentTitle(title);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, mBuilder.build());
    }

    Runnable sleeper = new Runnable() {
        WebAPIOutput res;
        String videoString;
        Utils u;

        @Override
        public void run() {
            try {
                u = new Utils(UploadVideoService.this);
                try {
                    updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Compressing Video");
                    Log.e("UploadVideoService", "Compressing video");
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
                    Log.e("UploadVideoService", "Done compressing video. Now uploading");

                    //videoFile.delete();
                    updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Uploading Video");
                    videoFile = new File("/sdcard/gdsupload/" + compressVideoTime);
                    Utils u = new Utils(UploadVideoService.this);
                    videoString = u.convertVideoToString(videoFile);
                    SubmitMessage m;
                    if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                        locationName = "";
                    } else {
                        locationName = locationName.replace("null", "").trim();
                    }
                    m = new SubmitMessage(u.getUnique(), message, videoFile.getName(), videoString, locationLat, locationLong, locationName);

                    try {
                        res = MainApplication.apiService.uploadContentWithMessage(m);
                        if (res != null) {
                            Log.e("UploadVideoService", "Creating thumbnail");

                            updateNotification(Consts.SERVICE_VIDEO_UPLOAD, "Creating Video Thumbnail");
                            Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath().toString(), MediaStore.Video.Thumbnails.MINI_KIND);
                            new Utils(UploadVideoService.this).saveImageToFolder(thumbnail, videoFile.getName().toString() + "_thumbnail");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(UploadVideoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("ServiceDemo", "Service was interrupted.", e);
            }
            try {
                new Utils(UploadVideoService.this).cancelNotification(Consts.SERVICE_VIDEO_UPLOAD);
                if (res != null) {
                    if (res.getStatusCode() == 1) {
                        SQLFunctions sql = new SQLFunctions(UploadVideoService.this);
                        sql.open();
                        Timeline t = new Timeline();
                        t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                        t.setMessage(message);
                        t.setImage("");
                        t.setVideo(videoFile.getName().toString());
                        t.setLocation(locationName);
                        t.setLocationLat(locationLat);
                        t.setLocationLong(locationLong);
                        sql.insertTimelineItem(t);
                        sql.close();
                        showNotification(0, "Video uploaded!", message, false);
                        startService(new Intent(UploadVideoService.this, MediaScannerService.class).putExtra("file", videoFile.getAbsoluteFile().toString()).putExtra("image", false));
                        stopSelf();
                        // show notification
                    } else {
                        showNotification(0, res.getStatusDescription(), res.getStatusDescription(), false);
                        Toast.makeText(UploadVideoService.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("Result", "There were no response from server");
                    Toast.makeText(UploadVideoService.this, "There were no response from server", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(UploadVideoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
        }
    };
}
