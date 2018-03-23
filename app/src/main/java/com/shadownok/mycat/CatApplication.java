package com.shadownok.mycat;

import android.app.Application;

/**
 * Created by linzhili on 18/3/13.
 */

public class CatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        CrashHandler.getInstance().init(this);
    }

    private static CatApplication instance;

    public static CatApplication getInstance() {

        return instance;
    }
}
