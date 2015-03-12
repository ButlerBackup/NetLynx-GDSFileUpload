package com.netlynxtech.gdsfileupload.classes;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.netlynxtech.gdsfileupload.Consts;
import com.netlynxtech.gdsfileupload.R;
import com.securepreferences.SecurePreferences;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Created by Probook2 on 30/12/2014.
 */
public class Utils {

    private ApiService apiService;
    private Context context;

    public Utils(Context con) {
        this.context = con;
    }

    public String getPhoneNumber() {
        if (Consts.DEBUG) {
            return "97307191";
        }
        SecurePreferences sp = new SecurePreferences(context);
        return sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0");
    }

    public void storeSecurePreferenceValue(String key, String value) {
        SecurePreferences sp = new SecurePreferences(context);
        sp.edit().putString(key, value).commit();
    }

    public boolean checkIfRegistered() {
        SecurePreferences sp = new SecurePreferences(context);
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_LOGIN_ID, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_PASSWORD, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_UDID, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_USER_GROUP, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_USER_NAME, "0"));
        if (!sp.getString(Consts.REGISTER_LOGIN_ID, "0").equals("0") && !sp.getString(Consts.REGISTER_USER_GROUP, "0").equals("0") && !sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0").equals("0") && !sp.getString(Consts.REGISTER_USER_NAME, "0").equals("0") && !sp.getString(Consts.REGISTER_PASSWORD, "0").equals("0") && !sp.getString(Consts.REGISTER_UDID, "0").equals("0")) {
            return true;
        }
        return false;
    }

    public void storeUnique(String gcmid) {
        SecurePreferences sp = new SecurePreferences(context);
        sp.edit().putString(Consts.REGISTER_UDID, gcmid).commit();
    }

    public String getUnique() {
        SecurePreferences sp = new SecurePreferences(context);
        if (Consts.DEBUG) {
            sp.edit().putString(Consts.REGISTER_UDID, "1111111").commit();
            return "1111111";
        }
        if (!sp.getString(Consts.REGISTER_UDID, "0").equals("0")) {
            return sp.getString(Consts.REGISTER_UDID, "0");
        }
        return "";
    }

    public String convertVideoToString(File videoFile) {
        try {
            BufferedInputStream in = null;
            in = new BufferedInputStream(new FileInputStream(videoFile));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            long tamano = videoFile.length();
            int iTamano = (int) tamano;
            byte[] b = new byte[iTamano];
            int bytesRead;
            while ((bytesRead = in.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }
            byte[] bb = bos.toByteArray();
            try {
                return Base64.encodeToString(bb, Base64.DEFAULT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String convertBitmapToString(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public String createFolder() {
        String directory = "";
        File folder = new File(Environment.getExternalStorageDirectory() + "/gdsupload");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }

        if (success) {
            directory = Environment.getExternalStorageDirectory() + "/gdsupload";
        }
        return directory;
    }

    public String createThumbnailFolder() {
        String directory = "";
        File folder = new File(Environment.getExternalStorageDirectory() + "/gdsupload/.thumbnails");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            directory = Environment.getExternalStorageDirectory() + "/gdsupload/.thumbnails";
        }
        return directory;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(File file) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath().toString(), options);

        // Calculate inSampleSize
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        options.inSampleSize = calculateInSampleSize(options, imageWidth, imageHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath().toString(), options);
    }

    public void saveImageToFolder(Bitmap bitmap, String name) {
        Log.e("Creating Thumbnail", "Creating Thumbnail");

        FileOutputStream out = null;
        try {
            String dir = createThumbnailFolder();
            if (!dir.equals("")) {
                Log.e("Creating Thumbnail", dir + "/" + name);
                out = new FileOutputStream(dir + "/" + name);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
            } else {
                Log.e("ERROR", "ERROR MAKING THUMBNAIL FOLDER!!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String size(int size) {
        String hrSize = "";
        double m = size / 1024.0;
        DecimalFormat dec = new DecimalFormat("0.00");

        if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else {
            hrSize = dec.format(size).concat(" KB");
        }
        return hrSize;
    }

    public boolean compressVideoMMS() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("pref_video_compress", false);
    }

    public boolean videoFileSizeLimit() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("pref_video_size_limit", false);
    }

    public void showNotification(int id, String title, String content, boolean autoCancel) {
        final Intent emptyIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(false)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setOngoing(autoCancel)
                        .setContentIntent(pendingIntent); //Required on Gingerbread and below
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, mBuilder.build());
    }

    public void updateNotification(int id, String title) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context);
        builder.setContentTitle(title);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    public void cancelNotification(int id) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    public void activateMediaScanner(File file, boolean isImage) {
        String type = "image/jpeg";
        if (!isImage) {
            type = "video/mp4";
        }
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath().toString()}, new String[]{type}, null);
    }

    public Bitmap createResizeBitmap(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath().toString(), options);
        int imageHeight = options.outHeight / 2;
        int imageWidth = options.outWidth / 2;
        return Bitmap.createScaledBitmap(BitmapFactory.decodeFile(file.getAbsolutePath().toString()), imageWidth, imageHeight, true);
    }
}
