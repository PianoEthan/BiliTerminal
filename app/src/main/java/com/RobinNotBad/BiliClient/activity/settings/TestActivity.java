package com.RobinNotBad.BiliClient.activity.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.article.OpusInfoActivity;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.activity.settings.login.SpecialLoginActivity;
import com.RobinNotBad.BiliClient.api.ConfInfoApi;
import com.RobinNotBad.BiliClient.api.DynamicApi;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestActivity extends BaseActivity {

    SwitchMaterial sw_wbi, sw_post;
    EditText input_link, input_data, output;
    TextInputEditText input_opus_title, input_opus_content;
    MaterialCardView btn_crash, btn_request, btn_cookies, btn_opus, btn_log, btn_publish_opus;

    @SuppressLint({"MutatingSharedPrefs", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        sw_wbi = findViewById(R.id.switch_wbi);
        sw_post = findViewById(R.id.switch_post);
        input_link = findViewById(R.id.input_link);
        input_data = findViewById(R.id.input_data);
        output = findViewById(R.id.output_json);
        btn_crash = findViewById(R.id.crash);
        btn_opus = findViewById(R.id.opus);
        btn_log = findViewById(R.id.log);
        btn_publish_opus = findViewById(R.id.publish_opus);
        input_opus_title = findViewById(R.id.input_opus_title);
        input_opus_content = findViewById(R.id.input_opus_content);

        input_link.setText(SharedPreferencesUtil.getString("dev_test_link", ""));

        sw_post.setOnCheckedChangeListener((compoundButton, checked) ->
                input_data.setVisibility(checked ? View.VISIBLE : View.GONE));

        btn_request = findViewById(R.id.request);

        btn_request.setOnClickListener(view -> CenterThreadPool.run(() -> {
            try {
                String url = input_link.getText().toString();
                if (!url.startsWith("https://") && !url.startsWith("http://"))
                    url = "https://" + url;

                if (sw_wbi.isChecked()) url = ConfInfoApi.signWBI(url);

                runOnUiThread(() -> {
                    output.setText("");
                    MsgUtil.showMsg("发出请求！");
                });
                String result;
                if (sw_post.isChecked()) {
                    String data = input_data.getText().toString();
                    result = Objects.requireNonNull(NetWorkUtil.post(url, data).body()).string();
                } else {
                    result = Objects.requireNonNull(NetWorkUtil.get(url).body()).string();
                }

                runOnUiThread(() -> {
                    output.setText(result);
                    MsgUtil.showMsg("请求成功！");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    output.setText(e.toString());
                    MsgUtil.showMsg("请求失败！");
                });
                e.printStackTrace();
            }
        }));

        btn_cookies = findViewById(R.id.cookies);
        btn_cookies.setOnClickListener(view -> {
            Intent intent = new Intent(this, SpecialLoginActivity.class);
            intent.putExtra("login", false);
            startActivity(intent);
        });

        btn_opus.setOnClickListener(v -> startActivity(new Intent(this, OpusInfoActivity.class).putExtra("id", 781871626480254985L)));

        // 一键崩溃测试
        btn_crash.setOnClickListener(v -> {
            throw new RuntimeException("这是一次故意的崩溃测试！如果你看到了这个，说明崩溃功能正常工作喵～");
        });

        // 日志查看
        btn_log.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogViewerActivity.class);
            startActivity(intent);
        });

        // 图文投稿
        btn_publish_opus.setOnClickListener(v -> {
            String title = input_opus_title.getText().toString().trim();
            String content = input_opus_content.getText().toString().trim();

            if (content.isEmpty()) {
                MsgUtil.showMsg("请输入内容喵～");
                return;
            }

            CenterThreadPool.run(() -> {
                try {
                    runOnUiThread(() -> MsgUtil.showMsg("正在生成并上传图片..."));

                    // 生成测试图片
                    Bitmap bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    paint.setColor(Color.parseColor("#FF6B9D"));
                    canvas.drawRect(0, 0, 800, 600, paint);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(50);
                    paint.setTextAlign(Paint.Align.CENTER);
                    String displayText = title.isEmpty() ? content.substring(0, Math.min(content.length(), 15)) : title;
                    canvas.drawText(displayText, 400, 320, paint);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                    byte[] imageBytes = baos.toByteArray();

                    // 上传图片
                    DynamicApi.ImageUploadResult uploadResult = DynamicApi.uploadImage(imageBytes, "test.jpg", "image/jpeg", "daily");
                    if (uploadResult == null) {
                        runOnUiThread(() -> MsgUtil.showMsg("图片上传失败喵～"));
                        return;
                    }

                    runOnUiThread(() -> MsgUtil.showMsg("图片上传成功，正在发布图文..."));

                    // 发布图文
                    List<DynamicApi.ImageUploadResult> images = new ArrayList<>();
                    images.add(uploadResult);

                    long dynId = DynamicApi.publishOpus(title, content, images);

                    if (dynId > 0) {
                        runOnUiThread(() -> MsgUtil.showMsg("图文发布成功！动态ID: " + dynId));
                    } else {
                        runOnUiThread(() -> MsgUtil.showMsg("图文发布失败喵～"));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        MsgUtil.showMsg("图文投稿失败: " + e.getMessage());
                        Log.e("TestActivity", "publishOpus error", e);
                    });
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        SharedPreferencesUtil.putString("dev_test_link", input_link.getText().toString());
        super.onDestroy();
    }
}
