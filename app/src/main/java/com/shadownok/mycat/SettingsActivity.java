package com.shadownok.mycat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by linzhili on 18/3/20.
 */

public class SettingsActivity extends Activity {

    List<PackageInfo> packs;

    private void setWakupApp(String pkg) {
        SharedPreferences sharedPreferences = getSharedPreferences("WAKEUP", MODE_PRIVATE);
        sharedPreferences.edit().putString("pkg", pkg).commit();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("选择需要唤醒的助手");
        ListView listView = new ListView(this);
        setContentView(listView);
        packs = getPackageManager().getInstalledPackages(0);//获取安装程序的包名
        Log.e("SettingsActivity", "onCreate: packs " + packs.size());
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);//某个包信息

//            if ((ApplicationInfo.FLAG_SYSTEM & p.applicationInfo.flags) != 0) {
//                continue;
//            }
            //打印：版本好，版本名，包名....
            Log.i("SettingsActivity", p.versionName + ":" + p.packageName);
        }

        listView.setAdapter(new SimpleAdapter(packs, this));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String pkg = packs.get(position).packageName;
                if ("com.shadow.u".equals(pkg)){
                    Toast.makeText(SettingsActivity.this, "你是不是想搞事情", Toast.LENGTH_SHORT).show();
                    return;
                }
                setWakupApp(pkg);
                finish();
            }
        });
    }


    class SimpleAdapter extends BaseAdapter {

        List<PackageInfo> packs;
        Context context;
        PackageManager packageManager;

        public SimpleAdapter(List<PackageInfo> packs, Context context) {
            this.packs = packs;
            this.context = context;
            packageManager = context.getPackageManager();
        }

        @Override
        public int getCount() {
            return packs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String pkgName = packs.get(position).packageName;
            String appName = packs.get(position).applicationInfo.loadLabel(packageManager).toString();
            String text = appName+"\n"+pkgName;
            if (pkgName.equals(appName)){
                text = "框架程序\n"+pkgName;
            }
            TextView textView = new TextView(context);
            textView.setTextColor(Color.BLACK);
            textView.setPadding(20,20,20,20);
            textView.setBackgroundColor(Color.WHITE);
            textView.setTextSize(20);
            textView.setBackgroundResource(R.drawable.btn_clicker);

            textView.setText(text);

            return textView;
        }
    }
}
