package com.netlynxtech.gdsfileupload.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
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
import java.util.Random;

public class UploadVideoService extends WakefulIntentService {
    File videoFile;
    String message, locationName, locationLat, locationLong;
    NotificationCompat.Builder mBuilder;
    String dbItemId = "0";
    boolean isResendFailed = false;

    public UploadVideoService() {
        super("UploadVideoService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        WebAPIOutput res = null;
        String videoString;
        Utils u;
        Bitmap thumbnail = null;
        boolean isResending = false;

        int id = new Random().nextInt(1000);
        if (id < 1) {
            id = 1;
        }
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
        if (intent.hasExtra("resend") && intent.getBooleanExtra("resend", false)) {
            isResending = intent.getBooleanExtra("resend", false);
        }
        if (intent.hasExtra("failedResend") && intent.getBooleanExtra("failedResend", false) && intent.hasExtra("id") && intent.getStringExtra("id") != null) {
            isResendFailed = true;
            dbItemId = intent.getStringExtra("id");
        }
        if (intent.hasExtra("file") && intent.hasExtra("message") && intent.hasExtra("locationName") && intent.hasExtra("locationLat") && intent.hasExtra("locationLong")) {
            videoFile = new File(intent.getStringExtra("file"));
            message = intent.getStringExtra("message");
            locationName = intent.getStringExtra("locationName");
            locationLat = intent.getStringExtra("locationLat");
            locationLong = intent.getStringExtra("locationLong");
            String compressVideoTime = videoFile.getName();
            if (!isResending) {
                compressVideoTime = System.currentTimeMillis() + "_compressed.mp4";
            }
            if (!isResendFailed) {
                compressVideoTime = System.currentTimeMillis() + "_compressed.mp4";
            }

            SQLFunctions sql = new SQLFunctions(UploadVideoService.this);
            sql.open();
            if (!isResendFailed) {
                Timeline t = new Timeline();
                t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                t.setMessage(message);
                t.setImage("");
                t.setVideo(compressVideoTime);
                t.setLocation(locationName);
                t.setLocationLat(locationLat);
                t.setLocationLong(locationLong);
                t.setSuccess("2"); // 2 = uploading
                long tempid = sql.insertTimelineItem(t);

                if (tempid > 0) {
                    dbItemId = String.valueOf(tempid);
                } else {
                    Log.e("FAILED", "Unable to insert new data and get Id");
                    stopSelf();
                }
            } else {
                sql.setUploadStatus(dbItemId, "2");
            }
            sql.close();
            showNotification(id, "Uploading video", message, true, null);

            try {
                u = new Utils(UploadVideoService.this);
                updateNotification(id, "Compressing Video");
                Log.e("UploadVideoService", "Compressing video");
                if (!isResending) {
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
                }
                //videoFile.delete();
                updateNotification(id, "Uploading Video");
                videoFile = new File("/sdcard/gdsupload/" + compressVideoTime);
                videoString = u.convertVideoToString(videoFile);
                if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                    locationName = "";
                } else {
                    locationName = locationName.replace("null", "").trim();
                }
                SubmitMessage m = new SubmitMessage(u.getUnique(), message, videoFile.getName(), videoString, locationLat, locationLong, locationName);

                res = MainApplication.apiService.uploadContentWithMessage(m);
                if (res != null) {
                    Log.e("UploadVideoService", "Creating thumbnail");
                    updateNotification(id, "Creating Video Thumbnail");
                    thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath().toString(), MediaStore.Video.Thumbnails.MINI_KIND);
                    new Utils(UploadVideoService.this).saveImageToFolder(thumbnail, videoFile.getName().toString() + "_thumbnail");
                }
            } catch (Exception e) {
                Log.e("ServiceDemo", "Service was interrupted.", e);
            }
            sql = new SQLFunctions(UploadVideoService.this);
            sql.open();
            try {
                new Utils(UploadVideoService.this).cancelNotification(id);
                if (res != null) {
                    if (res.getStatusCode() == 1) {

                        showNotification(id, "Video uploaded!", message, false, thumbnail);
                        sql.setUploadStatus(dbItemId, "1");
                        Intent i = new Intent(UploadVideoService.this, MediaScannerService.class);
                        i.putExtra("file", videoFile.getAbsoluteFile().toString());
                        i.putExtra("image", false);
                        WakefulIntentService.sendWakefulWork(UploadVideoService.this, i);
                        //stopSelf();
                        // show notification
                    } else {
                        sql.setUploadStatus(dbItemId, "0");
                        showNotification(id, res.getStatusDescription(), res.getStatusDescription(), false, null);
                        Toast.makeText(UploadVideoService.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                        //stopSelf();
                    }
                } else {
                    sql.setUploadStatus(dbItemId, "0");
                    Log.e("Result", "There were no response from server");
                    showNotification(id, "There were no response from server", "There were no response from server", false, null);
                    //stopSelf();
                }
            } catch (Exception e) {
                sql.setUploadStatus(dbItemId, "0");
                e.printStackTrace();
                showNotification(id, e.getMessage().toString(), e.getMessage().toString(), false, null);
                //stopSelf();
            }
            sql.close();
        } else {
            Log.e("SERVICE", "NO PARAMETER");
            showNotification(id, "No parameters", "No parameters", false, null);
            //stopSelf();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void showNotification(int id, String title, String content, boolean autoCancel, Bitmap bitmap) {
        final Intent emptyIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(UploadVideoService.this, 0, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(UploadVideoService.this)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(autoCancel)
                .setContentIntent(pendingIntent); //Required on Gingerbread and below
        if (bitmap != null) {
            mBuilder.setLargeIcon(bitmap);
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, mBuilder.build());
    }

    public void updateNotification(int id, String title) {
        mBuilder.setContentTitle(title);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, mBuilder.build());
    }
}
