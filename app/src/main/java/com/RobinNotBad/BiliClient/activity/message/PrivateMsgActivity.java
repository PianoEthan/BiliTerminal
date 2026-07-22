package com.RobinNotBad.BiliClient.activity.message;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.adapter.message.PrivateMsgAdapter;
import com.RobinNotBad.BiliClient.api.PrivateMsgApi;
import com.RobinNotBad.BiliClient.model.PrivateMessage;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PrivateMsgActivity extends BaseActivity {
    JSONObject allMsg = new JSONObject();
    List<PrivateMessage> list = Collections.synchronizedList(new ArrayList<>());
    JSONArray emoteArray = new JSONArray();
    RecyclerView msgView;
    EditText contentEt;
    ImageButton sendBtn;
    View layout_input;
    PrivateMsgAdapter adapter;
    long uid;
    boolean isLoadingMore = false;
    Timer refreshTimer, animTimer;

    boolean animVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_msg);

        msgView = findViewById(R.id.msg_view);
        contentEt = findViewById(R.id.msg_input_et);
        sendBtn = findViewById(R.id.send_btn);
        layout_input = findViewById(R.id.layout_input);

        Intent intent = getIntent();
        uid = intent.getLongExtra("uid", 114514);
        Log.e("", String.valueOf(uid));

        MsgUtil.showMsg("私信有可能被拦截\n尽量不要用终端发私信喵");

        CenterThreadPool.run(() -> {
            try {
                allMsg = PrivateMsgApi.getPrivateMsg(uid, 50, 0, 0);
                if (allMsg == null || allMsg.length() == 0) {
                    runOnUiThread(() -> MsgUtil.showMsg("无法加载私信，请检查网络或登录状态"));
                    return;
                }
                list = PrivateMsgApi.getPrivateMsgList(allMsg);
                Collections.reverse(list);
                emoteArray = PrivateMsgApi.getEmoteJsonArray(allMsg);
                adapter = new PrivateMsgAdapter(list, emoteArray, this);
                
                // PiliPlus 式自动标记已读
                if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.PRIVATE_MSG_AUTO_READ_ENABLE, true)) {
                    try {
                        PrivateMsgApi.updateAck(uid, 1, 0);
                    } catch (Exception e) {
                        Log.e("PrivateMsgActivity", "标记已读失败", e);
                    }
                }
                
                runOnUiThread(() -> {
                    CustomLinearManager manager = new CustomLinearManager(this);
                    manager.setStackFromEnd(true);
                    msgView.setLayoutManager(manager);
                    msgView.setAdapter(adapter);
                    msgView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                            super.onScrollStateChanged(recyclerView, newState);
                            switch (newState) {
                                case RecyclerView.SCROLL_STATE_DRAGGING:
                                    if (!recyclerView.canScrollVertically(-1) && !isLoadingMore) {
                                        loadMore();
                                        Log.e("", "滑动到顶部，开始刷新");
                                    }
                                    break;
                                case RecyclerView.SCROLL_STATE_IDLE:
                                    if (!animVisible) {
                                        if (animTimer != null) animTimer.cancel();
                                        animTimer = new Timer();
                                        animTimer.schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                runOnUiThread(() -> layout_input.startAnimation(getViewAnimation(layout_input, true, true)));
                                                layout_input.postDelayed(() -> animVisible = true, 200);
                                            }
                                        }, 500);
                                    }
                                    break;
                            }
                        }

                        @Override
                        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            if (animVisible && recyclerView.canScrollVertically(0) && dy != 0) {
                                animVisible = false;
                                layout_input.startAnimation(getViewAnimation(layout_input, false, false));
                            }
                        }
                    });

                    refreshTimer = new Timer();
                    refreshTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    }, 15000, 15000);
                });
            } catch (Exception e) {
                runOnUiThread(() -> MsgUtil.err(e));
            }
        });

        sendBtn.setOnClickListener(view -> CenterThreadPool.run(() -> {
            try {
                String content = contentEt.getText().toString().trim();
                if (!content.isEmpty()) {
                    runOnUiThread(() -> contentEt.setText(""));
                    // PiliPlus 格式：content 包裹为 JSON
                    JSONObject result = PrivateMsgApi.sendMsg(
                            SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0),
                            uid,
                            PrivateMessage.TYPE_TEXT,
                            System.currentTimeMillis() / 1000,
                            "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}");
                    runOnUiThread(() -> {
                        if (result.optInt("code", -1) == 0) {
                            MsgUtil.showMsg("发送成功");
                            refresh();
                        } else {
                            String msg = result.optString("message", result.optString("msg", "发送失败"));
                            MsgUtil.showMsg("发送失败：" + msg);
                        }
                    });
                } else {
                    runOnUiThread(() -> MsgUtil.showMsg("你还木有输入喵~"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> MsgUtil.showMsg("发送失败：" + e.getMessage()));
            }
        }));
    }
    //1在上面0在下面

    private TranslateAnimation getViewAnimation(View view, boolean show_or_hide, boolean up_or_down) {
        int height = view.getMeasuredHeight() + 2;
        TranslateAnimation anim;
        anim = new TranslateAnimation(0, 0,
                (show_or_hide ? (up_or_down ? height : -height) : 0),
                (show_or_hide ? 0 : (up_or_down ? -height : height)));
        anim.setDuration(200);
        AccelerateDecelerateInterpolator i = new AccelerateDecelerateInterpolator();
        anim.setInterpolator(i);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (show_or_hide) view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!show_or_hide) view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return anim;
    }

    @Override
    protected void onDestroy() {
        if (refreshTimer != null) refreshTimer.cancel();
        refreshTimer = null;
        super.onDestroy();
    }

    private void refresh() {
        CenterThreadPool.run(() -> {
            try {
                if (list.isEmpty()) return;
                int oldListSize = list.size();
                long lastSeqno = list.get(list.size() - 1).msgSeqno;
                JSONObject msgResult = PrivateMsgApi.getPrivateMsg(uid, 50, lastSeqno, 0);
                ArrayList<PrivateMessage> newList = PrivateMsgApi.getPrivateMsgList(msgResult);
                if (!newList.isEmpty()) {
                    for (int i = 0; i < PrivateMsgApi.getEmoteJsonArray(msgResult).length(); ++i) {
                        JSONObject emote = PrivateMsgApi.getEmoteJsonArray(msgResult).getJSONObject(i);
                        emoteArray.put(emote);
                    }
                    Collections.reverse(newList);
                    runOnUiThread(() -> {
                        for (PrivateMessage msg : newList) {
                            list.add(msg);
                            adapter.notifyItemInserted(list.size() - 1);
                        }
                        adapter.notifyItemRangeChanged(oldListSize - 1, list.size());
                        msgView.smoothScrollToPosition(list.size() - 1);
                    });
                }
            } catch (Exception e) {
                Log.e("PrivateMsgActivity", "refresh error", e);
            }
        });
    }

    @SuppressLint("SuspiciousIndentation")
    private void loadMore() {
        isLoadingMore = true;
        MsgUtil.showMsg("加载更多中...");
        CenterThreadPool.run(() -> {
            try {
                int hasMore = allMsg.optInt("has_more", 0);
                if (hasMore == 1 && !list.isEmpty()) {
                    allMsg = PrivateMsgApi.getPrivateMsg(uid, 15, 0, list.get(0).msgSeqno);
                    Log.e("", allMsg.toString());
                    ArrayList<PrivateMessage> newList = PrivateMsgApi.getPrivateMsgList(allMsg);
                    Collections.reverse(newList);

                    for (int i = 0; i < PrivateMsgApi.getEmoteJsonArray(allMsg).length(); ++i) {
                        JSONObject emote = PrivateMsgApi.getEmoteJsonArray(allMsg).getJSONObject(i);
                        emoteArray.put(emote);
                    }

                    runOnUiThread(() -> {
                        adapter.addItem(newList);
                        MsgUtil.showMsg("已加载更多消息！");
                    });
                } else {
                    runOnUiThread(() -> MsgUtil.showMsg("没有更多消息了"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> MsgUtil.showMsg("加载失败：" + e.getMessage()));
            } finally {
                isLoadingMore = false;
            }
        });
    }
}
