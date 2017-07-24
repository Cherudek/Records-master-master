package com.example.android.records.data;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by Gregorio on 19/07/2017.
 */

public class MyApplication extends Application {
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}