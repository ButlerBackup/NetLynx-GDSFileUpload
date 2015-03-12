package com.netlynxtech.gdsfileupload.service;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.io.File;

public class MediaScannerService extends WakefulIntentService {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public MediaScannerService() {
        super("MediaScannerService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        try {
            if (intent.hasExtra("file") && intent.hasExtra("image")) {
                String type = "image/jpeg";
                if (!intent.getBooleanExtra("image", true)) {
                    type = "video/mp4";
                }
                File file = new File(intent.getStringExtra("file"));
                if (file.exists()) {
                    MediaScannerConnection.scanFile(MediaScannerService.this, new String[]{file.getAbsolutePath().toString()}, new String[]{type}, new MediaScannerConnection.MediaScannerConnectionClient() {
                        @Override
                        public void onMediaScannerConnected() {
                        }

                        @Override
                        public void onScanCompleted(String s, Uri uri) {
                            Log.e("MediaScannerService", s);
                            stopSelf();
                        }
                    });
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }
}
