package com.RobinNotBad.BiliClient.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Process;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.api.AppInfoApi;
import com.RobinNotBad.BiliClient.model.ApiResult;
import com.RobinNotBad.BiliClient.service.DownloadService;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.android.material.button.MaterialButton;

public class CatchActivity extends BaseActivity {
    private boolean openStack = false;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catch);

        TextView reason_view = findViewById(R.id.catch_reason);
        TextView stack_view = findViewById(R.id.stack);
        MaterialButton btn_upload = findViewById(R.id.upload_btn);

        Intent intent = getIntent();
        String stack = intent.getStringExtra("stack");

        stack_view.setText(stack);

        findViewById(R.id.exit_btn).setOnClickListener(view -> System.exit(-1));

        SpannableString reason_str = null;

        if (stack != null) {

            if (stack.contains("java.lang.NumberFormatException"))
                reason_str = new SpannableString("可能的崩溃原因：\n数值转换出错");
            else if (stack.contains("java.lang.UnsatisfiedLinkError"))
                reason_str = new SpannableString("可能的崩溃原因：\n外部库加载出错，可能设备太老或修改了安装包");
            else if (stack.contains("org.json.JSONException"))
                reason_str = new SpannableString("可能的崩溃原因：\n数据解析错误");
            else if (stack.contains("java.lang.OutOfMemoryError"))
                reason_str = new SpannableString("可能的崩溃原因：\n内存爆了，这在小内存设备上很正常");
            else
                reason_str = new SpannableString("未知的崩溃原因");

            btn_upload.setVisibility(android.view.View.GONE);

        } else finish();

        findViewById(R.id.restart_btn).setOnClickListener(view -> {
            finish();
            stopService(new Intent(this, DownloadService.class));
            startActivity(new Intent(this, SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Process.killProcess(Process.myPid());
        });

        if (reason_str != null) {
            reason_str.setSpan(new StyleSpan(Typeface.BOLD), 0, 8, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            reason_view.setText(reason_str);
        } else reason_view.setText("未知的崩溃原因");

        stack_view.setOnClickListener(view -> {
            openStack = !openStack;
            if (openStack) stack_view.setMaxLines(200);
            else stack_view.setMaxLines(5);
        });

        StringUtil.setCopy(stack_view);
    }

    @Override
    protected boolean eventBusEnabled() {
        return false;
    }
}
