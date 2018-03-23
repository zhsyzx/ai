package com.shadownok.mycat;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

/**
 * Created by linzhili on 18/3/20.
 */

public class AudioFocusWatcher implements AudioManager.OnAudioFocusChangeListener {

    public interface FocusListener {
        void canGetFocus();
        void canWakeUp();
        void stopWakeUp();
        boolean isWakeUp();
        int getMode();
    }

    private static final int MAX_NO_RESPONSE_TIMES = 5;
    private static final long MIN_DISTANCE = 5000;

    private AudioManager am;
    private FocusListener listener;
    private long lastGetFocusTime = 0;
    private int noResponseTimes = 0;
    private boolean sleep = true;
    private Handler handler = new Handler();

    private static AudioFocusWatcher instance;

    public static AudioFocusWatcher getInstance() {
        synchronized (AudioFocusWatcher.class) {
            if (instance == null) {
                instance = new AudioFocusWatcher();
            }
        }
        return instance;
    }

    private AudioFocusWatcher() {
        am = (AudioManager) CatApplication.getInstance().getSystemService(Context.AUDIO_SERVICE);
        //am.abandonAudioFocus(this);
        am.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC, // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000);
                if (sleep) {
                    return;
                }
                if (lastGetFocusTime == 0) {
                    return;
                }
                if (System.currentTimeMillis() - lastGetFocusTime > 10*1000) {
                    sleep = true;
                    lastGetFocusTime = 0;
                    if (listener != null && listener.getMode() == MainActivity.MODE_CONTINUITY) {
                        LogUtil.getInstance().e("AudioFocusWatcher", "run: 超过10秒没有获得音频焦点，对话休眠，开始等待唤醒");

                        listener.canWakeUp();
                    }
                    noResponseTimes = 0;
                }
            }
        }, 1000);
    }

    public void setListener(FocusListener focusListener) {
        this.listener = focusListener;
    }

    public void setSleep(boolean sleep) {
        this.sleep = sleep;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
//        LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: " + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_NONE) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_NONE");
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 获得音频焦点");

            if (listener != null && listener.getMode() == MainActivity.MODE_CONTINUITY) {
                LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 获得音频焦点，开始处理连续对话逻辑");
            }
//            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_GAIN");
            if (sleep) {
                if (listener != null && !listener.isWakeUp() && listener.getMode() == MainActivity.MODE_CONTINUITY) {
                    LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 休眠状态获得音频焦点，唤醒未启动，开始启动唤醒");
                    listener.canWakeUp();
                    return;
                }
                LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 休眠状态获得音频焦点，不处理");
                return;
            }
            if (noResponseTimes >= MAX_NO_RESPONSE_TIMES) {
                sleep = true;
                lastGetFocusTime = 0;
                if (listener != null && listener.getMode() == MainActivity.MODE_CONTINUITY) {
                    LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: " + MAX_NO_RESPONSE_TIMES + "次无人应答，对话休眠，开始等待唤醒");
                    listener.canWakeUp();
                }
                noResponseTimes = 0;
                return;
            }
            if (System.currentTimeMillis() - lastGetFocusTime < MIN_DISTANCE) {
                LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 无人应答");
                noResponseTimes++;
            }
            if (listener != null) {
                listener.canGetFocus();
            }

            lastGetFocusTime = System.currentTimeMillis();
            // Resume playback
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_GAIN_TRANSIENT");

            // Stop playback
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");

        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE");

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_LOSS");

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
//            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT");
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 丢失音频焦点");
            if (sleep) {
                LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: 休眠状态丢失音频焦点，停止唤醒，释放MIC");
                if (listener!=null) {
                    listener.stopWakeUp();
                }
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            LogUtil.getInstance().e("AudioFocusWatcher", "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");

        }
    }
}
