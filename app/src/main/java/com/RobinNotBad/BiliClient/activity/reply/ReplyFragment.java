package com.RobinNotBad.BiliClient.activity.reply;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.RobinNotBad.BiliClient.activity.base.RefreshListFragment;
import com.RobinNotBad.BiliClient.adapter.ReplyAdapter;
import com.RobinNotBad.BiliClient.api.ReplyApi;
import com.RobinNotBad.BiliClient.event.ReplyEvent;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

//视频下评论页面，评论详情见ReplyInfoActivity
//部分通用代码在VideoReplyAdapter内
//2023-07-22

public class ReplyFragment extends RefreshListFragment {

    private boolean dontload;
    protected long aid, mid;
    protected int sort = 3;
    protected int type;
    protected int count;
    protected ArrayList<Reply> replyList;
    protected ReplyAdapter replyAdapter;
    public int replyType = ReplyApi.REPLY_TYPE_VIDEO;
    private long seek;
    private String pagination = "";
    private boolean isManager = false;
    //游标分页的到底标志：与父类 bottom 配合，到底后阻止继续请求（pagination 会被置空，避免误用空游标重拉第一页导致重复）
    private boolean isEnd = false;
    //已加载评论的 rpid 集合，作为游标分页的兜底去重，防止 B站 API 在游标边界返回重复评论
    private final Set<Long> loadedRpids = new HashSet<>();

    public static ReplyFragment newInstance(long aid, int type) {
        ReplyFragment fragment = new ReplyFragment();
        Bundle args = new Bundle();
        args.putLong("aid", aid);
        args.putInt("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    public static ReplyFragment newInstance(long aid, int type, boolean dontload) {
        ReplyFragment fragment = new ReplyFragment();
        Bundle args = new Bundle();
        args.putLong("aid", aid);
        args.putInt("type", type);
        args.putBoolean("dontload", dontload);
        fragment.setArguments(args);
        return fragment;
    }


    public static ReplyFragment newInstance(long aid, int type, long seek_rpid) {
        ReplyFragment fragment = new ReplyFragment();
        Bundle args = new Bundle();
        args.putLong("aid", aid);
        args.putInt("type", type);
        args.putLong("seek", seek_rpid);
        fragment.setArguments(args);
        return fragment;
    }

    public static ReplyFragment newInstance(long aid, int type, boolean dontload, long seek_rpid) {
        ReplyFragment fragment = new ReplyFragment();
        Bundle args = new Bundle();
        args.putLong("aid", aid);
        args.putInt("type", type);
        args.putBoolean("dontload", dontload);
        args.putLong("seek", seek_rpid);
        fragment.setArguments(args);
        return fragment;
    }

    public static ReplyFragment newInstance(long aid, int type, int count, long seek_rpid, long up_mid) {
        ReplyFragment fragment = new ReplyFragment();
        Bundle args = new Bundle();
        args.putLong("aid", aid);
        args.putInt("count", count);
        args.putInt("type", type);
        args.putLong("seek", seek_rpid);
        args.putLong("mid", up_mid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            aid = getArguments().getLong("aid", 0);
            count = getArguments().getInt("count", 0);
            type = getArguments().getInt("type", 0);
            replyType = type;
            dontload = getArguments().getBoolean("dontload", false);
            seek = getArguments().getLong("seek", -1);
            mid = getArguments().getLong("mid", -1);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setForceSingleColumn();
        super.onViewCreated(view, savedInstanceState);

        if (SharedPreferencesUtil.getBoolean("ui_landscape", false)) {
            WindowManager windowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= 17) display.getRealMetrics(metrics);
            else display.getMetrics(metrics);
            int paddings = metrics.widthPixels / 6;
            recyclerView.setPadding(paddings, 0, paddings, 0);
        }

        setOnRefreshListener(() -> refresh(aid));
        setOnLoadMoreListener(this::continueLoading);

        Log.e("debug-av号", String.valueOf(aid));

        replyList = new ArrayList<>();

        if (!dontload) refresh(aid);
    }

    public void setManager(Object source) {
        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) return;

        try {
            if (source != null) {
                if (source instanceof List<?>) {
                    List<UserInfo> staffs = (List<UserInfo>) source;
                    for (UserInfo userInfo : staffs) {
                        if (userInfo.mid == SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0)) {
                            isManager = true;
                            break;
                        }
                    }
                } else if (source instanceof UserInfo) {
                    isManager = ((UserInfo) source).mid == SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0);
                }
            }
        } catch (Exception e) {
            MsgUtil.err(e);
        }
    }

    private ReplyAdapter createReplyAdapter() {
        return new ReplyAdapter(requireContext(), replyList, aid, 0, type, sort, mid);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void continueLoading(int page) {
        //到底拦截：已到末页时不再发起请求，避免用空游标重拉第一页导致重复评论
        if (isEnd || bottom) {
            setRefreshing(false);
            return;
        }
        CenterThreadPool.run(() -> {
            try {
                //B站对某些游标值可能只返回 cursor 不返回 replies，这里自动连续加载直到拿到评论或到底，避免用户看到"翻页无反应"
                int emptyPages = 0;
                while (!isEnd && !bottom && emptyPages < 5) {
                    List<Reply> list = new ArrayList<>();
                    ReplyApi.ReplyResult result = ReplyApi.getRepliesLazyWithRetry(aid, 0, pagination, type, sort, list);
                    if (!result.isSuccess()) {
                        //失败时回退 page，使父类页码与游标状态一致；失败不更新游标，允许按原游标重新触发
                        this.page--;
                        runOnUiThread(() -> {
                            setRefreshing(false);
                            showLoadErrorMsg(result);
                        });
                        return;
                    }
                    //仅当本次请求成功时才推进游标
                    this.pagination = result.nextOffset;
                    //兜底去重：过滤掉 rpid 已存在的评论
                    ReplyApi.filterDuplicateReplies(list, loadedRpids);
                    if (result.isEnd()) {
                        isEnd = true;
                        bottom = true;
                    }
                    if (!list.isEmpty()) {
                        final List<Reply> unique = list;
                        runOnUiThread(() -> {
                            replyList.addAll(unique);
                            if (replyAdapter != null)
                                replyAdapter.notifyItemRangeInserted(replyList.size() - unique.size() + 1, unique.size());
                        });
                        setRefreshing(false);
                        return;   //拿到评论，退出等用户继续下滑
                    }
                    //本页无新评论：若未到底则继续加载下一页，连续空页达上限则提示
                    emptyPages++;
                    Logu.d("ReplyFragment", "本页无新评论，自动加载下一页 (empty=" + emptyPages + ")");
                }
                setRefreshing(false);
                if (emptyPages >= 5 && !isEnd) {
                    runOnUiThread(() -> MsgUtil.showMsgLong("暂时没有更多评论了"));
                }
            } catch (Exception e) {
                loadFail(e);
            }
        });
    }

    /**根据 ReplyResult 的状态/API错误码给出用户友好的提示。网络错误时附带详情便于定位。*/
    private void showLoadErrorMsg(ReplyApi.ReplyResult result) {
        if (result.status == ReplyApi.ReplyResult.STATUS_NET_ERROR) {
            //网络/解析失败：WBI 与无签名降级均失败，展示详情便于定位根因
            MsgUtil.showMsgLong("评论加载失败：" + result.message);
        } else if (result.status == ReplyApi.ReplyResult.STATUS_API_ERROR) {
            String tip;
            switch (result.apiCode) {
                case -101: tip = "请先登录后再查看评论"; break;
                case -404: tip = "评论区不存在"; break;
                case 12002: tip = "评论区已关闭"; break;
                case 12014: tip = "评论已被风控，请稍后再试"; break;
                default: tip = "评论加载失败（" + result.apiCode + "）" + (result.message != null && !result.message.isEmpty() ? "：" + result.message : ""); break;
            }
            MsgUtil.showMsgLong(tip);
        }
    }

    public void notifyReplyInserted(ReplyEvent replyEvent) {
        if (replyEvent.getOid() != aid) return;
        Reply reply = replyEvent.getMessage();
        if (reply.root == 0) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager());
            int pos = layoutManager.findFirstCompletelyVisibleItemPosition();
            pos = Math.max(pos, 0);
            replyList.add(pos, reply);
            int finalPos = pos;
            runOnUiThread(() -> {
                replyAdapter.notifyItemInserted(finalPos);
                replyAdapter.notifyItemRangeChanged(finalPos, replyList.size() - finalPos + 1);
                layoutManager.scrollToPositionWithOffset(finalPos + 1, 0);
            });
        } else if (replyEvent.getPos() >= 0) {
            replyList.get(replyEvent.getPos()).childMsgList.add(reply);
            replyList.get(replyEvent.getPos()).childCount++;
            runOnUiThread(() -> replyAdapter.notifyItemChanged(replyEvent.getPos() + 1));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(long aid) {
        //切换/下拉刷新时重置全部分页状态，确保不残留旧数据与旧游标
        pagination = "";
        isEnd = false;
        bottom = false;
        loadedRpids.clear();
        page = 1;
        this.aid = aid;
        setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                List<Reply> list = new ArrayList<>();
                ReplyApi.ReplyResult result = ReplyApi.getRepliesLazyWithRetry(aid, seek, pagination, type, sort, list);
                if (result.isSuccess()) {
                    this.pagination = result.nextOffset;
                    ReplyApi.filterDuplicateReplies(list, loadedRpids);
                    final List<Reply> unique = list;
                    setRefreshing(false);
                    if (isAdded()) {
                        runOnUiThread(() -> {
                            if (!isAdded()) return;
                            if (replyList != null) replyList.clear();
                            else replyList = new ArrayList<>();
                            replyList.addAll(unique);
                            if (replyAdapter == null) {
                                replyAdapter = createReplyAdapter();
                                replyAdapter.count = count;
                                replyAdapter.isManager = isManager;
                                setOnSortSwitch();
                                setAdapter(replyAdapter);
                            } else {
                                replyAdapter.notifyDataSetChanged();
                            }
                        });
                        if (result.isEnd()) {
                            Logu.d("ReplyFragment", "评论到底 type=" + type + " oid=" + aid);
                            isEnd = true;
                            bottom = true;
                        }
                    }
                } else if (isAdded()) {
                    setRefreshing(false);
                    runOnUiThread(() -> showLoadErrorMsg(result));
                }
            } catch (Exception e) {
                loadFail(e);
            }
        });
    }

    private void setOnSortSwitch() {
        replyAdapter.setOnSortSwitchListener(position -> {
            sort = (sort == 2 ? 3 : 2);
            replyAdapter.sort = this.sort;
            refresh(aid);
        });
    }
}