package com.RobinNotBad.BiliClient.activity.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingMenuActivity extends BaseActivity {

    private SwitchMaterial menu_popular, menu_hotsearch, menu_live, menu_precious, menu_ranking, menu_timeline;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asyncInflate(R.layout.activity_setting_menu, (layoutView, resId) -> {

            menu_popular = findViewById(R.id.menu_popular);
            menu_popular.setChecked(SharedPreferencesUtil.getBoolean("menu_popular", true));

            menu_hotsearch = findViewById(R.id.menu_hotsearch);
            menu_hotsearch.setChecked(SharedPreferencesUtil.getBoolean("menu_hotsearch", true));

            menu_live = findViewById(R.id.menu_live);
            menu_live.setChecked(SharedPreferencesUtil.getBoolean("menu_live", false));

            menu_precious = findViewById(R.id.menu_precious);
            menu_precious.setChecked(SharedPreferencesUtil.getBoolean("menu_precious", false));

            menu_ranking = findViewById(R.id.menu_ranking);
            menu_ranking.setChecked(SharedPreferencesUtil.getBoolean("menu_ranking", false));

            menu_timeline = findViewById(R.id.menu_timeline);
            menu_timeline.setChecked(SharedPreferencesUtil.getBoolean("menu_timeline", false));

            MaterialButton sort_btn = findViewById(R.id.sort);
            sort_btn.setOnClickListener(view -> {
                Intent intent = new Intent(SettingMenuActivity.this, SortSettingActivity.class);
                startActivity(intent);
            });
        });
    }

    private void save() {
        SharedPreferencesUtil.putBoolean("menu_popular", menu_popular.isChecked());
        SharedPreferencesUtil.putBoolean("menu_hotsearch", menu_hotsearch.isChecked());
        SharedPreferencesUtil.putBoolean("menu_precious", menu_precious.isChecked());
        SharedPreferencesUtil.putBoolean("menu_ranking", menu_ranking.isChecked());
        SharedPreferencesUtil.putBoolean("menu_live", menu_live.isChecked());
        SharedPreferencesUtil.putBoolean("menu_timeline", menu_timeline.isChecked());
    }

    @Override
    protected void onDestroy() {
        save();
        super.onDestroy();
    }
}
