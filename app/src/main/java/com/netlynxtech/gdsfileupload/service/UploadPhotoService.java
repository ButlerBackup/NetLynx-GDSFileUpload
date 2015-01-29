package com.netlynxtech.gdsfileupload.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
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

public class UploadPhotoService extends WakefulIntentService {
    File photoFile;
    String message, locationName, locationLat, locationLong;
    NotificationCompat.Builder mBuilder;
    String dbItemId = "0";
    boolean isResendFailed = false;

    public UploadPhotoService() {
        super("UploadPhotoService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        WebAPIOutput res = null;
        Utils u;
        Bitmap thumbnail = null;
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
        if (intent.hasExtra("failedResend") && intent.getBooleanExtra("failedResend", false) && intent.hasExtra("id") && intent.getStringExtra("id") != null) {
            isResendFailed = true;
            dbItemId = intent.getStringExtra("id");
        }
        if (intent.hasExtra("file") && intent.hasExtra("message") && intent.hasExtra("locationName") && intent.hasExtra("locationLat") && intent.hasExtra("locationLong")) {
            photoFile = new File(intent.getStringExtra("file"));
            message = intent.getStringExtra("message");
            locationName = intent.getStringExtra("locationName");
            locationLat = intent.getStringExtra("locationLat");
            locationLong = intent.getStringExtra("locationLong");
            SQLFunctions sql = new SQLFunctions(UploadPhotoService.this);
            sql.open();
            if (!isResendFailed) { //if this is a new photo
                Timeline t = new Timeline();
                t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                t.setMessage(message);
                t.setImage(photoFile.getName().toString());
                t.setVideo("");
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
            } else { //if resending failed photo
                sql.setUploadStatus(dbItemId, "2");
            }
            sql.close();
            showNotification(id, "Uploading photo", message, true, null);
            try {
                u = new Utils(UploadPhotoService.this);
                updateNotification(id, "Compressing Photo");
                String bitmapString = u.convertBitmapToString(Utils.decodeSampledBitmapFromResource(photoFile));
                Log.e("uploadPhotoService", "Compressing photo");
                Log.e("uploadPhotoService", "Done compressing photo. Now uploading");
                //photoFile.delete();
                updateNotification(id, "Uploading Photo");
                if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                    locationName = "";
                } else {
                    locationName = locationName.replace("null", "").trim();
                }
                SubmitMessage m = new SubmitMessage(u.getUnique(), message, photoFile.getName(), bitmapString, locationLat, locationLong, locationName);
                res = MainApplication.apiService.uploadContentWithMessage(m);
                if (res != null) {
                    Log.e("uploadPhotoService", "Creating thumbnail");
                    updateNotification(id, "Creating Photo Thumbnail");
                    thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(photoFile.getAbsolutePath().toString()), 180, 180);
                    new Utils(UploadPhotoService.this).saveImageToFolder(thumbnail, photoFile.getName().toString() + "_thumbnail");
                }
            } catch (Exception e) {
                Log.e("ServiceDemo", "Service was interrupted.", e);
                e.printStackTrace();
            }
            sql = new SQLFunctions(UploadPhotoService.this);
            sql.open();
            try {
                new Utils(UploadPhotoService.this).cancelNotification(id);
                if (res != null) {
                    if (res.getStatusCode() == 1) {
                        showNotification(id, "Photo uploaded!", message, false, thumbnail);
                        sql.setUploadStatus(dbItemId, "1");
                        Intent i = new Intent(UploadPhotoService.this, MediaScannerService.class);
                        i.putExtra("file", photoFile.getAbsoluteFile().toString());
                        i.putExtra("image", true);
                        WakefulIntentService.sendWakefulWork(UploadPhotoService.this, i);
                        stopSelf();
                        // show notification
                    } else {

                        sql.setUploadStatus(dbItemId, "0");
                        showNotification(id, res.getStatusDescription(), res.getStatusDescription(), false, null);
                        stopSelf();
                    }
                } else {
                    sql.setUploadStatus(dbItemId, "0");
                    Log.e("Result", "There were no response from server");
                    showNotification(id, "There were no response from server", "There were no response from server", false, null);
                    stopSelf();
                }
            } catch (Exception e) {
                e.printStackTrace();
                sql.setUploadStatus(dbItemId, "0");
                showNotification(id, e.getMessage().toString(), e.getMessage().toString(), false, null);
                stopSelf();
            }
            sql.close();
        } else {
            Log.e("SERVICE", "NO PARAMETER");
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void showNotification(int id, String title, String content, boolean autoCancel, Bitmap bitmap) {
        final Intent emptyIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(UploadPhotoService.this, 0, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(UploadPhotoService.this)
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
