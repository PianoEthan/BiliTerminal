package com.RobinNotBad.BiliClient.activity.dynamic.send;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.EmoteActivity;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.adapter.dynamic.DynamicHolder;
import com.RobinNotBad.BiliClient.adapter.video.VideoCardHolder;
import com.RobinNotBad.BiliClient.api.EmoteApi;
import com.RobinNotBad.BiliClient.model.Dynamic;
import com.RobinNotBad.BiliClient.model.VideoInfo;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.TerminalContext;
import com.google.android.material.card.MaterialCardView;

/**
 * 发送动态输入Activity，直接copy的WriteReplyActivity
 * 换成了ActivityResult
 * （我并不怎么会写）
 */
public class SendDynamicActivity extends BaseActivity {

    EditText editText;

    private final ActivityResultLauncher<Intent> emoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {
        int code = result.getResultCode();
        Intent data = result.getData();
        if (code == RESULT_OK && data != null && data.hasExtra("text")) {
            editText.append(data.getStringExtra("text"));
        }
    });

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asyncInflate(R.layout.activity_send_dynamic, (layoutView, resId) -> {

            if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
                setResult(RESULT_CANCELED);
                finish();
                MsgUtil.showMsg("还没有登录喵~");
            }

            editText = findViewById(R.id.editText);
            MaterialCardView send = findViewById(R.id.send);

            FrameLayout extraCard = findViewById(R.id.forwardCard);
            VideoInfo video = null;
            Dynamic forward = null;
            if (TerminalContext.getInstance().getForwardContent() instanceof VideoInfo) {
                video = (VideoInfo) TerminalContext.getInstance().getForwardContent();
            } else {
                forward = (Dynamic) TerminalContext.getInstance().getForwardContent();
            }
            if (forward != null) {
                View childCard = View.inflate(this, R.layout.cell_dynamic, extraCard);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.applyContent(childCard);
                DynamicHolder holder = new DynamicHolder(childCard, this, false);
                holder.showDynamic(this, forward, false);
            } else if (video != null) {
                View videoCard = LayoutInflater.from(this).inflate(R.layout.cell_video_list, extraCard);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.applyContent(videoCard);
                VideoCardHolder holder = new VideoCardHolder(videoCard);
                holder.showVideoCard(video.toCard(), this);
            }

            send.setOnClickListener(view -> {
                // 不了解遂直接保留cookie刷新判断了
                if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.cookie_refresh, true)) {
                    String text = editText.getText().toString();
                    Intent result = new Intent();
                    // 原神级的传数据
                    Bundle bundle = SendDynamicActivity.this.getIntent().getExtras();
                    if (bundle != null) result.putExtras(bundle);
                    result.putExtra("text", text);
                    setResult(RESULT_OK, result);
                    finish();
                } else
                    MsgUtil.showDialog("无法发送", "上一次的Cookie刷新失败了，\n您可能需要重新登录以进行敏感操作", -1);
            });

            findViewById(R.id.emote).setOnClickListener(view ->
                    emoteLauncher.launch(new Intent(this, EmoteActivity.class).putExtra("from", EmoteApi.BUSINESS_DYNAMIC)));
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        TerminalContext.getInstance().setForwardContent(null);
    }
}