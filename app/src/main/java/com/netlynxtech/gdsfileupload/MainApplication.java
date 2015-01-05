package com.netlynxtech.gdsfileupload;

import android.app.Application;
import android.util.Log;

import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.netlynxtech.gdsfileupload.classes.ApiService;

import retrofit.RestAdapter;

public class MainApplication extends Application {

    public static ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("MainApplication", "onCreate()");
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(Consts.WEB_API_URL)
                .build();
        apiService = restAdapter.create(ApiService.class);
        LocationLibrary.initialiseLibrary(getBaseContext(), "com.netlynxtech.gdsfileupload");
    }
}
