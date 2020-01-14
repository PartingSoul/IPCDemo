package com.parting_soul.ipcdemo;

import android.app.Application;

/**
 * @author parting_soul
 * @date 2020-01-14
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BinderPool.getInstance(this).connect();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        BinderPool.getInstance(this).disconnect();
    }

}
