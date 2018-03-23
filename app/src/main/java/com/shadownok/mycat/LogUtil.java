package com.shadownok.mycat;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by linzhili on 18/3/21.
 */

public class LogUtil {
    private static LogUtil instance;

    public static LogUtil getInstance() {
        synchronized (LogUtil.class) {
            if (instance == null) {
                instance = new LogUtil();
            }
        }
        return instance;
    }

    private ArrayList<String> msgs = new ArrayList<>();

    public ArrayList<String> getMsgs() {
        return msgs;
    }

    private void addMsg(String msg) {
        if (msgs.size() > 20) {
            msgs.remove(0);
        }
        msgs.add(msg);
    }

    public void e(String tag, String msg) {
        Log.e(tag, msg);
        addMsg(msg);//tag + " : " +
    }
}
