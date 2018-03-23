package com.shadownok.mycat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import static com.shadownok.mycat.MainActivity.MODE_WAKEUP;

public class KeepLiveReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent i) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
            Intent intent = new Intent(context, WakeUpService.class);
            intent.putExtra("mode", getMode(context));
            context.startService(intent);
    }

    private int getMode(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("mode", MODE_WAKEUP);
    }
}
