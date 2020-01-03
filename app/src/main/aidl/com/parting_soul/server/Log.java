package com.parting_soul.server;

/**
 * @author parting_soul
 * @date 2019-12-27
 */
public class Log {
    private static final String TAG = "client";

    public static void d(String msg) {
        android.util.Log.e(TAG, Thread.currentThread().getName() + "  " + msg);
    }

}
