package com.netlynxtech.gdsfileupload.service;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.IBinder;

import java.io.File;

public class MediaScannerService extends IntentService {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public MediaScannerService() {
        super("MediaScannerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (intent.hasExtra("file") && intent.hasExtra("image")) {
                String type = "image/jpeg";
                if (!intent.getBooleanExtra("image", true)) {
                    type = "video/mp4";
                }
                File file = new File(intent.getStringExtra("file"));
                //MediaScannerConnection.scanFile(MediaScannerService.this, new String[]{file.getAbsolutePath().toString()}, new String[]{type}, null);
                stopSelf();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }
}
