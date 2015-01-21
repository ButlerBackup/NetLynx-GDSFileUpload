package com.netlynxtech.gdsfileupload.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.netlynxtech.gdsfileupload.Consts;
import com.netlynxtech.gdsfileupload.MainApplication;
import com.netlynxtech.gdsfileupload.R;
import com.netlynxtech.gdsfileupload.apiclasses.SubmitMessage;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;

import java.io.File;

public class UploadPhotoService extends IntentService {
    File photoFile;
    String message, locationName, locationLat, locationLong;
    uploadPhoto mTask;
    NotificationCompat.Builder mBuilder;

    public UploadPhotoService() {
        super("UploadPhotoService");
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
            photoFile = new File(intent.getStringExtra("file"));
            message = intent.getStringExtra("message");
            locationName = intent.getStringExtra("locationName");
            locationLat = intent.getStringExtra("locationLat");
            locationLong = intent.getStringExtra("locationLong");
            //Toast.makeText(uploadPhotoService.this, "Video will be processed in the background. You will be notified of any changes", Toast.LENGTH_LONG).show();
            showNotification(Consts.SERVICE_PHOTO_UPLOAD, "Uploading photo", message, true);
           /* mTask = null;
            mTask = new uploadPhoto();
            mTask.execute();*/
            sleeper.run();
        } else {
            Log.e("SERVICE", "NO PARAMETER");
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class uploadPhoto extends AsyncTask<Void, Void, Void> {
        WebAPIOutput res;
        Utils u;

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                new Utils(UploadPhotoService.this).cancelNotification(Consts.SERVICE_PHOTO_UPLOAD);
                if (res != null) {
                    if (res.getStatusCode() == 1) {
                        SQLFunctions sql = new SQLFunctions(UploadPhotoService.this);
                        sql.open();
                        Timeline t = new Timeline();
                        t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                        t.setMessage(message);
                        t.setImage(photoFile.getName().toString());
                        t.setVideo("");
                        t.setLocation(locationName);
                        t.setLocationLat(locationLat);
                        t.setLocationLong(locationLong);
                        sql.insertTimelineItem(t);
                        sql.close();
                        showNotification(0, "Photo uploaded!", message, false);
                        startService(new Intent(UploadPhotoService.this, MediaScannerService.class).putExtra("file", photoFile.getAbsoluteFile().toString()).putExtra("image", true));
                        stopSelf();
                        // show notification
                    } else {
                        showNotification(0, res.getStatusDescription(), res.getStatusDescription(), false);
                        Toast.makeText(UploadPhotoService.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("Result", "There were no response from server");
                    Toast.makeText(UploadPhotoService.this, "There were no response from server", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(UploadPhotoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {
            u = new Utils(UploadPhotoService.this);
            try {
                updateNotification(Consts.SERVICE_PHOTO_UPLOAD, "Compressing Photo");
                String bitmapString = u.convertBitmapToString(Utils.decodeSampledBitmapFromResource(photoFile));
                Log.e("uploadPhotoService", "Compressing photo");
                Log.e("uploadPhotoService", "Done compressing photo. Now uploading");
                //photoFile.delete();
                updateNotification(Consts.SERVICE_PHOTO_UPLOAD, "Uploading Video");
                Utils u = new Utils(UploadPhotoService.this);
                SubmitMessage m;
                if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                    locationName = "";
                } else {
                    locationName = locationName.replace("null", "").trim();
                }
                m = new SubmitMessage(u.getUnique(), message, photoFile.getName(), bitmapString, locationLat, locationLong, locationName);

                try {
                    res = MainApplication.apiService.uploadContentWithMessage(m);
                    if (res != null) {
                        Log.e("uploadPhotoService", "Creating thumbnail");
                        updateNotification(Consts.SERVICE_PHOTO_UPLOAD, "Creating Photo Thumbnail");
                        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(photoFile.getAbsolutePath().toString()), 180, 180);
                        new Utils(UploadPhotoService.this).saveImageToFolder(thumbnail, photoFile.getName().toString() + "_thumbnail");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(UploadPhotoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
            return null;
        }
    }

    public void showNotification(int id, String title, String content, boolean autoCancel) {
        final Intent emptyIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(UploadPhotoService.this, 0, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(UploadPhotoService.this)
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
        Utils u;

        @Override
        public void run() {
            try {
                u = new Utils(UploadPhotoService.this);
                try {
                    updateNotification(Consts.SERVICE_PHOTO_UPLOAD, "Compressing Photo");
                    String bitmapString = u.convertBitmapToString(Utils.decodeSampledBitmapFromResource(photoFile));
                    Log.e("uploadPhotoService", "Compressing photo");
                    Log.e("uploadPhotoService", "Done compressing photo. Now uploading");
                    //photoFile.delete();
                    updateNotification(Consts.SERVICE_PHOTO_UPLOAD, "Uploading Video");
                    Utils u = new Utils(UploadPhotoService.this);
                    SubmitMessage m;
                    if (locationName.equals(Consts.LOCATION_ERROR) || locationName.equals(Consts.LOCATION_LOADING)) {
                        locationName = "";
                    } else {
                        locationName = locationName.replace("null", "").trim();
                    }
                    m = new SubmitMessage(u.getUnique(), message, photoFile.getName(), bitmapString, locationLat, locationLong, locationName);

                    try {
                        res = MainApplication.apiService.uploadContentWithMessage(m);
                        if (res != null) {
                            Log.e("uploadPhotoService", "Creating thumbnail");
                            updateNotification(Consts.SERVICE_PHOTO_UPLOAD, "Creating Photo Thumbnail");
                            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(photoFile.getAbsolutePath().toString()), 180, 180);
                            new Utils(UploadPhotoService.this).saveImageToFolder(thumbnail, photoFile.getName().toString() + "_thumbnail");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(UploadPhotoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("ServiceDemo", "Service was interrupted.", e);
                e.printStackTrace();
            }
            try {
                new Utils(UploadPhotoService.this).cancelNotification(Consts.SERVICE_PHOTO_UPLOAD);
                if (res != null) {
                    if (res.getStatusCode() == 1) {
                        SQLFunctions sql = new SQLFunctions(UploadPhotoService.this);
                        sql.open();
                        Timeline t = new Timeline();
                        t.setUnixTime((System.currentTimeMillis() / 1000L) + "");
                        t.setMessage(message);
                        t.setImage(photoFile.getName().toString());
                        t.setVideo("");
                        t.setLocation(locationName);
                        t.setLocationLat(locationLat);
                        t.setLocationLong(locationLong);
                        sql.insertTimelineItem(t);
                        sql.close();
                        showNotification(0, "Photo uploaded!", message, false);
                        startService(new Intent(UploadPhotoService.this, MediaScannerService.class).putExtra("file", photoFile.getAbsoluteFile().toString()).putExtra("image", true));
                        stopSelf();
                        // show notification
                    } else {
                        showNotification(0, res.getStatusDescription(), res.getStatusDescription(), false);
                        Toast.makeText(UploadPhotoService.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("Result", "There were no response from server");
                    Toast.makeText(UploadPhotoService.this, "There were no response from server", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(UploadPhotoService.this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
        }
    };
}
