package com.RobinNotBad.BiliClient.activity.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LogViewerActivity extends BaseActivity {

    private TextView logText;
    private ScrollView scrollView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        logText = findViewById(R.id.log_text);
        scrollView = findViewById(R.id.scroll_view);

        loadLogs();
    }

    private void loadLogs() {
        CenterThreadPool.run(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat -d -t 500");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder log = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.append(line).append("\n");
                }
                bufferedReader.close();
                process.destroy();

                runOnUiThread(() -> {
                    if (log.length() == 0) {
                        logText.setText("暂无日志喵～");
                    } else {
                        logText.setText(log.toString());
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    logText.setText("读取日志失败: " + e.getMessage());
                    MsgUtil.err(e);
                });
            }
        });
    }
}
