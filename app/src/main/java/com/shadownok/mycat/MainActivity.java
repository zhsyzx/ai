package com.shadownok.mycat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int PERMISSION_CODE = 22;

    //continuity
    public static final int MODE_CONTINUITY = 0;
    public static final int MODE_WAKEUP = 1;
    public static String CUSTOM_WAKEUP = Environment.getExternalStorageDirectory().getPath() + File.separator + "WakeUp.bin";
    public static String CUSTOM_WAKEUP_EFFECT = Environment.getExternalStorageDirectory().getPath() + File.separator + "WakeUp.mp3";

    private Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("选择对话模式");

        initPermission();
        initMode();

        findViewById(R.id.text_continuity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(MODE_CONTINUITY);
                initMode();
                startWakeUpService();
                Toast.makeText(MainActivity.this, "如果无法停止小爱，请关闭屏幕再打开", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.text_wakeup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(MODE_WAKEUP);
                initMode();
                startWakeUpService();
            }
        });

        findViewById(R.id.text_choose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 200);
                ArrayList<String> strings = LogUtil.getInstance().getMsgs();
                String string = "";
                for (String s:strings){
                    string +=s+"\n";
                }
                ((TextView)findViewById(R.id.text_log)).setText(string);
            }
        }, 200);
    }

    private void initMode() {
        int mode = getMode();
        findViewById(R.id.text_continuity).setBackgroundColor(Color.parseColor(mode == MODE_CONTINUITY ? "#DDDDDD" : "#FFFFFF"));
        findViewById(R.id.text_wakeup).setBackgroundColor(Color.parseColor(mode == MODE_WAKEUP ? "#DDDDDD" : "#FFFFFF"));
    }

    private void setMode(int mode) {
        SharedPreferences sharedPreferences = getSharedPreferences("MODE", MODE_PRIVATE);
        sharedPreferences.edit().putInt("mode", mode).commit();
    }

    private int getMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("MODE", MODE_PRIVATE);
        return sharedPreferences.getInt("mode", MODE_WAKEUP);
    }

    private void startWakeUpService() {
        Intent intent = new Intent(this, WakeUpService.class);
        stopService(intent);
        intent.putExtra("mode", getMode());
        startService(intent);
        Toast.makeText(this, "^_^，小爱同学", Toast.LENGTH_SHORT).show();
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            requestPermissions(toApplyList.toArray(tmpList), PERMISSION_CODE);
        } else {
            startWakeUpService();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
        if (PERMISSION_CODE == requestCode) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    initPermission();
                    return;
                }
            }

            startWakeUpService();

        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        moveTaskToBack(false);
    }
}
