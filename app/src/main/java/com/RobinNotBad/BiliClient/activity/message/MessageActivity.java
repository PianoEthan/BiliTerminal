package com.RobinNotBad.BiliClient.activity.message;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.adapter.message.PrivateMsgSessionsAdapter;
import com.RobinNotBad.BiliClient.api.MessageApi;
import com.RobinNotBad.BiliClient.api.PrivateMsgApi;
import com.RobinNotBad.BiliClient.helper.TutorialHelper;
import com.RobinNotBad.BiliClient.model.PrivateMsgSession;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MessageActivity extends InstanceActivity {
    private RecyclerView sessionsView;

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asyncInflate(R.layout.activity_message, (layoutView, resId) -> {
            SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setRefreshing(true);

            MaterialCardView settingBtn = findViewById(R.id.setting_btn);
            settingBtn.setOnClickListener(view -> {
                Intent intent = new Intent(this, MessageSettingsActivity.class);
                startActivity(intent);
            });

            MaterialCardView reply = findViewById(R.id.reply);
            reply.setOnClickListener(view -> {
                Intent intent = new Intent();
                intent.setClass(this, NoticeActivity.class);
                intent.putExtra("type", "reply");
                startActivity(intent);
                ((TextView) findViewById(R.id.reply_text)).setText("回复我的");
            });

            MaterialCardView like = findViewById(R.id.like);
            like.setOnClickListener(view -> {
                Intent intent = new Intent();
                intent.setClass(this, NoticeActivity.class);
                intent.putExtra("type", "like");
                startActivity(intent);
                ((TextView) findViewById(R.id.like_text)).setText("收到的赞");
            });

            MaterialCardView at = findViewById(R.id.at);
            at.setOnClickListener(view -> {
                Intent intent = new Intent();
                intent.setClass(this, NoticeActivity.class);
                intent.putExtra("type", "at");
                startActivity(intent);
                ((TextView) findViewById(R.id.at_text)).setText("@我");
            });

            MaterialCardView system = findViewById(R.id.system);
            system.setOnClickListener(view -> {
                Intent intent = new Intent();
                intent.setClass(this, NoticeActivity.class);
                intent.putExtra("type", "system");
                startActivity(intent);
            });

            sessionsView = findViewById(R.id.sessions_list);
            sessionsView.setNestedScrollingEnabled(false);

            CenterThreadPool.run(() -> {
                try {
                    JSONObject stats = MessageApi.getUnread();
                    // 使用 getNewSessionsList 获取更完整的会话数据
                    ArrayList<PrivateMsgSession> sessionsList = PrivateMsgApi.getNewSessionsList(0, 20, 0, "web");
                    // 如果新接口返回为空，回退到旧接口
                    if (sessionsList.isEmpty()) {
                        sessionsList = PrivateMsgApi.getSessionsList(20);
                    }
                    Collections.sort(sessionsList, (o1, o2) -> {
                        boolean o1Unread = o1.unread > 0;
                        boolean o2Unread = o2.unread > 0;
                        if (o1Unread && !o2Unread) {
                            return -1;
                        } else if (!o1Unread && o2Unread) {
                            return 1;
                        }
                        return 0;
                    });
                    // 使用 session 自带的信息 + 批量用户查询兜底
                    ArrayList<Long> uidList = new ArrayList<>();
                    for (PrivateMsgSession item : sessionsList) {
                        uidList.add(item.talkerUid);
                    }
                    HashMap<Long, UserInfo> userMap = new HashMap<>();
                    try {
                        if (!uidList.isEmpty()) {
                            userMap = PrivateMsgApi.getUsersInfo(uidList);
                        }
                    } catch (Exception ignored) {}

                    PrivateMsgSessionsAdapter adapter = new PrivateMsgSessionsAdapter(this, sessionsList, userMap);
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        try {
                            if (stats != null) {
                                ((TextView) findViewById(R.id.reply_text)).setText("回复我的"
                                        + ((stats.getInt("reply") > 0) ? ("(" + stats.getInt("reply") + "未读)") : ""));
                                ((TextView) findViewById(R.id.like_text)).setText(
                                        "收到的赞" + ((stats.getInt("like") > 0) ? ("(" + stats.getInt("like") + "未读)") : ""));
                                ((TextView) findViewById(R.id.at_text)).setText(
                                        "@我" + ((stats.getInt("at") > 0) ? ("(" + stats.getInt("at") + "未读)") : ""));
                            }
                            sessionsView.setLayoutManager(new CustomLinearManager(this));
                            sessionsView.setAdapter(adapter);
                            SharedPreferencesUtil.putInt(SharedPreferencesUtil.MESSAGE_UPDATE_NUM, 0);
                        } catch (Exception e) {
                            MsgUtil.err(e);
                        }

                        View scrollView = findViewById(R.id.scrollView);
                        scrollView.setFocusable(true);
                        scrollView.setFocusableInTouchMode(true);
                        scrollView.requestFocus();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        MsgUtil.err(e);
                    });
                }
            });

            TutorialHelper.showTutorialList(this, R.array.tutorial_message, 5);
        });
    }
}
