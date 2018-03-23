package com.shadownok.mycat;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class WakeUpService extends Service implements EventListener {
    public WakeUpService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private EventManager wakeup;
    private Handler handler = new Handler();
    private int mode = 1;
    private boolean isWakeUp = false;



    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.getInstance().e("WakeUpService", "onCreate: 后台服务开启");
        File f1 = new File(MainActivity.CUSTOM_WAKEUP);
        if (!f1.exists()) {
            LogUtil.getInstance().e("WakeUpService", "onCreate: 自定义唤醒文件WakeUp.bin不存在，使用默认唤醒词");
        } else {
            LogUtil.getInstance().e("WakeUpService", "onCreate: 发现自定义唤醒文件WakeUp.bin，使用自定义唤醒词");
        }
        f1 = new File(MainActivity.CUSTOM_WAKEUP_EFFECT);
        if (!f1.exists()) {
            LogUtil.getInstance().e("WakeUpService", "onCreate: 自定义唤醒音效文件WakeUp.mp3不存在，使用默认唤醒音效");
        } else {
            LogUtil.getInstance().e("WakeUpService", "onCreate: 发现自定义唤醒音效文件WakeUp.mp3，使用自定义唤醒音效");
        }
        f1 = null;
        wakeup = EventManagerFactory.create(this, "wp");
        wakeup.registerListener(this); //  EventListener 中 onEvent方法
        AudioFocusWatcher.getInstance().setListener(new AudioFocusWatcher.FocusListener() {
            @Override
            public void canGetFocus() {
                if (mode == 0) {
                    LogUtil.getInstance().e("WakeUpService", "canGetFocus: 连续对话模式，开启小爱");
                    wakeup();
                }
            }

            @Override
            public void canWakeUp() {
                if (mode == 0) {
                    LogUtil.getInstance().e("WakeUpService", "canWakeUp: 连续对话模式，等待唤醒");
                    start();
                }
            }

            @Override
            public void stopWakeUp() {
                stop(false);
            }

            @Override
            public boolean isWakeUp() {
                return isWakeUp;
            }

            @Override
            public int getMode() {
                return mode;
            }
        });
        start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null) {
            mode = intent.getExtras().getInt("mode");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeup.send(SpeechConstant.WAKEUP_STOP, "{}", null, 0, 0);
        LogUtil.getInstance().e("WakeUpService", "onDestroy: 后台服务结束，停止唤醒，释放MIC");
    }

    private String getWakeupApp() {
        SharedPreferences sharedPreferences = getSharedPreferences("WAKEUP", MODE_PRIVATE);
        return sharedPreferences.getString("pkg", "com.miui.voiceassist");
    }

    private void wakeup() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AudioFocusWatcher.getInstance().setSleep(false);
                String packageName = getWakeupApp();

                PackageManager packageManager = WakeUpService.this.getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }, 100);

    }

    /**
     * 测试参数填在这里
     */
    private void start() {
        LogUtil.getInstance().e("WakeUpService", "start: 开始等待唤醒");
        Map<String, Object> params = new TreeMap<String, Object>();

        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);


        if (!new File(MainActivity.CUSTOM_WAKEUP).exists()) {
            params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
        } else {
            params.put(SpeechConstant.WP_WORDS_FILE, MainActivity.CUSTOM_WAKEUP);

        }
        // "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下

        String json = null; // 这里可以替换成你需要测试的json
        json = new JSONObject(params).toString();
        wakeup.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);
        printLog("输入参数：" + json);
    }

    private void stop(boolean isError) {
        LogUtil.getInstance().e("WakeUpService", "stop: 停止唤醒，释放MIC");
        wakeup.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0); //
        if (mode == 1 || isError) {
            LogUtil.getInstance().e("WakeUpService", "stop: 唤醒对话模式，等待重新开启唤醒");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LogUtil.getInstance().e("WakeUpService", "run: 唤醒对话模式，开启唤醒");
                    start();
                }
            }, 4000);
        }

    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logTxt = "name: " + name;
        if (params != null && !params.isEmpty()) {
            logTxt += " ;params :" + params;
        } else if (data != null) {
            logTxt += " ;data length=" + data.length;
        }
        printLog(logTxt);

        if ("wp.ready".equals(name)) {
            isWakeUp = true;
            LogUtil.getInstance().e("WakeUpService", "onEvent: 唤醒已就绪，MIC已被本程序占用");
        }

        if ("wp.data".equals(name)) {
            playSound();
            LogUtil.getInstance().e("WakeUpService", "onEvent: 收到唤醒");
            wakeup();
            stop(false);
        }

        if ("wp.error".equals(name)) {

            isWakeUp = false;
            LogUtil.getInstance().e("WakeUpService", "onEvent: 唤醒异常，重试");
            LogUtil.getInstance().e("WakeUpService", "onEvent: " + getErrorMsg(params));
            stop(true);
        }

        if ("wp.exit".equals(name)) {
            isWakeUp = false;
            LogUtil.getInstance().e("WakeUpService", "onEvent: 唤醒已停止，MIC已释放");
        }
    }

    private void printLog(String text) {
        text += "\n";
        Log.i(getClass().getName(), text);
    }

    private SoundPool sp;
    private int soundId = -1;

    private void playSound() {
        if (sp == null) {
            try {
                sp = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
                AssetFileDescriptor fileDescriptor = getAssets().openFd("wakeup_start.mp3");
                File file = new File(MainActivity.CUSTOM_WAKEUP_EFFECT);
                if (file.exists()) {
                    soundId = sp.load(file.getPath(), 0);
                } else {
                    soundId = sp.load(fileDescriptor, 0);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        sp.play(soundId, 1, 1, 0, 0 ,1);


//        try {
//            AssetFileDescriptor fileDescriptor = getAssets().openFd("wakeup_start.mp3");
////            MediaPlayer mp = MediaPlayer.create(this, R.raw.wakeup_start);
//            MediaPlayer mp = new MediaPlayer();
//            mp.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
//            mp.reset();
//            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                @Override
//                public void onPrepared(MediaPlayer mp) {
//                    mp.start();
//                }
//            });
//            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    mp.reset();
//                    mp.release();
//                    mp = null;
//                }
//            });
//            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//                @Override
//                public boolean onError(MediaPlayer mp, int what, int extra) {
//                    Log.e("WakeUpService", "onError: " + what + ", " + extra);
//                    return true;
//                }
//            });
//
//            mp.prepareAsync();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    private String getErrorMsg(String s){
        try {
            JSONObject jsonObject = new JSONObject(s);
            String code = jsonObject.getString("errorCode");
            String msg = jsonObject.getString("errorDesc");
            return "code -> " +code + ", msg -> " +msg;
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

    }
}
