package com.RobinNotBad.BiliClient.activity.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.adapter.ThemeListAdapter;
import com.RobinNotBad.BiliClient.theme.BThemeInstaller;
import com.RobinNotBad.BiliClient.theme.ContentTintHelper;
import com.RobinNotBad.BiliClient.theme.ThemeManager;
import com.RobinNotBad.BiliClient.theme.model.InstalledTheme;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** 主题设置：导入/切换/删除 .btheme，深浅切换与三个开关、混合强度 */
public class ThemeSettingsActivity extends BaseActivity {

    private ThemeListAdapter adapter;
    private EditText blendInput;
    private String pendingDeleteId;
    private long pendingDeleteTime;

    @SuppressLint({"MissingInflatedId", "SetTextI18n", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asyncInflate(R.layout.activity_theme_settings, (layoutView, resId) -> {

            adapter = new ThemeListAdapter(this, new ThemeListAdapter.OnRowListener() {
                @Override
                public void onSelect(ThemeListAdapter.ThemeRow row) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        MsgUtil.showMsg("不支持切换主题");
                        return;
                    }
                    ThemeManager.getInstance().setTheme(row.id);
                    MsgUtil.showMsg("已切换主题：" + row.name);
                    refreshList();
                }

                @Override
                public void onLongPress(ThemeListAdapter.ThemeRow row) {
                    if (row.id == null || row.id.isEmpty()) return; // 内置不可删
                    if (com.RobinNotBad.BiliClient.theme.ThemeManager.isPresetId(row.id)) return; // 预设不可删
                    // 沿用设置页"再点一次"惯例：双击长按才真正删除
                    long now = System.currentTimeMillis();
                    if (row.id.equals(pendingDeleteId) && now - pendingDeleteTime < 3000) {
                        pendingDeleteId = null;
                        pendingDeleteTime = 0;
                        BThemeInstaller.uninstall(ThemeSettingsActivity.this, row.id);
                        MsgUtil.showMsg("已删除主题：" + row.name);
                        refreshList();
                    } else {
                        pendingDeleteId = row.id;
                        pendingDeleteTime = now;
                        MsgUtil.showMsg("再长按一次删除【" + row.name + "】");
                    }
                }
            });
            androidx.recyclerview.widget.RecyclerView list = findViewById(R.id.themeList);
            list.setAdapter(adapter);
            list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            refreshList();

            findViewById(R.id.importTheme).setOnClickListener(v -> importTheme());

            EditText pathInput = findViewById(R.id.themePathInput);
            pathInput.setText(FileUtil.getDownloadPath() == null ? "" : FileUtil.getDownloadPath().getPath());
            findViewById(R.id.themePathInstall).setOnClickListener(v -> {
                String path = pathInput.getText().toString().trim();
                if (TextUtils.isEmpty(path)) {
                    MsgUtil.showMsg("请输入主题文件路径");
                    return;
                }
                File file = new File(path);
                if (!file.isFile()) {
                    MsgUtil.showMsg("文件不存在：" + path);
                    return;
                }
                if (!FileUtil.checkStoragePermission()) {
                    FileUtil.requestStoragePermission(ThemeSettingsActivity.this);
                    MsgUtil.showMsg("请先授予存储权限");
                    return;
                }
                CenterThreadPool.run(() -> installFromStream(file.getName(), () -> new FileInputStream(file)));
            });

            SwitchMaterial switchDark = findViewById(R.id.switchDark);
            switchDark.setChecked(ThemeManager.getInstance().isDark());
            switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ThemeManager.getInstance().setDark(isChecked);
                refreshList();
            });

            SwitchMaterial switchBg = findViewById(R.id.switchBg);
            switchBg.setChecked(ThemeManager.getInstance().isBgEnabled());
            switchBg.setOnCheckedChangeListener((buttonView, isChecked) ->
                    ThemeManager.getInstance().setBgEnabled(isChecked));

            SwitchMaterial switchExtract = findViewById(R.id.switchExtract);
            switchExtract.setChecked(ThemeManager.getInstance().isExtractBgEnabled());
            switchExtract.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferencesUtil.putBoolean(SharedPreferencesUtil.THEME_EXTRACT_BG, isChecked);
                MsgUtil.showMsg(isChecked ? "已开启背景取色（重启生效）" : "已关闭背景取色");
            });

            SwitchMaterial switchTint = findViewById(R.id.switchContentTint);
            switchTint.setChecked(ThemeManager.getInstance().isContentTintEnabled());
            switchTint.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferencesUtil.putBoolean(SharedPreferencesUtil.THEME_CONTENT_TINT, isChecked);
                if (!isChecked) ContentTintHelper.clearCache();
                MsgUtil.showMsg(isChecked ? "已开启内容动态取色" : "已关闭内容动态取色");
            });

            blendInput = findViewById(R.id.blendInput);
            blendInput.setText(String.valueOf(ThemeManager.getInstance().getBlend()));
            findViewById(R.id.blendApply).setOnClickListener(v -> {
                try {
                    int blend = Integer.parseInt(blendInput.getText().toString().trim());
                    ThemeManager.getInstance().setBlend(blend);
                    MsgUtil.showMsg("混合强度已更新");
                    refreshList();
                } catch (NumberFormatException e) {
                    MsgUtil.showMsg("请输入 0-100 的整数");
                }
            });
        });
    }

    private void refreshList() {
        if (adapter == null) return;
        ThemeManager tm = ThemeManager.getInstance();
        List<ThemeListAdapter.ThemeRow> rows = new ArrayList<>();
        rows.add(new ThemeListAdapter.ThemeRow("", "默认（暗黑）", "内置深色主题，逐像素保持原版观感", null,
                TextUtils.isEmpty(tm.getThemeId())));
        for (com.RobinNotBad.BiliClient.theme.ThemeManager.BuiltinPreset preset : com.RobinNotBad.BiliClient.theme.ThemeManager.getBuiltinPresets()) {
            rows.add(new ThemeListAdapter.ThemeRow(preset.id, preset.name,
                    "内置预设 · 种子色 #" + Integer.toHexString(preset.seed).substring(2).toUpperCase(),
                    null, preset.id.equals(tm.getThemeId())));
        }
        for (InstalledTheme installed : BThemeInstaller.listInstalled(this)) {
            rows.add(new ThemeListAdapter.ThemeRow(installed.id,
                    installed.getName() + (installed.getAuthor().isEmpty() ? "" : " · " + installed.getAuthor()),
                    installed.getDescription(),
                    installed.getPreviewFile(),
                    installed.id.equals(tm.getThemeId())));
        }
        adapter.setRows(rows);
    }

    private void importTheme() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "选择主题包"), 1001);
        } else {
            MsgUtil.showMsg("低版本系统请使用下方路径输入导入");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            CenterThreadPool.run(() -> installFromStream("uri",
                    () -> getContentResolver().openInputStream(uri)));
        }
    }

    private interface StreamOpener {
        InputStream open() throws Exception;
    }

    private void installFromStream(String label, StreamOpener opener) {
        try {
            InputStream in = opener.open();
            BThemeInstaller.InstallResult result = BThemeInstaller.install(this, in);
            if (in != null) in.close();
            runOnUiThread(() -> {
                if (result.ok && result.theme != null) {
                    MsgUtil.showMsg("主题安装成功：" + result.theme.getName());
                    // 重复导入幂等：若正在使用则重载并应用
                    ThemeManager tm = ThemeManager.getInstance();
                    if (result.theme.id.equals(tm.getThemeId())) {
                        tm.setTheme(result.theme.id);
                    }
                    refreshList();
                } else {
                    MsgUtil.showMsg(result.error == null ? "主题安装失败" : result.error);
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> MsgUtil.showMsg("主题安装失败：" + e.getMessage()));
        }
    }
}
