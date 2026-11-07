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

import com.RobinNotBad.BiliClient.activity.base.RefreshListFragment;
import com.RobinNotBad.BiliClient.adapter.ReplyAdapter;
import com.RobinNotBad.BiliClient.api.ReplyApi;
import com.RobinNotBad.BiliClient.event.ReplyEvent;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;
import androidx.core.util.ObjectsCompat;

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
    private boolean isManager = false;

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

    private final com.RobinNotBad.BiliClient.model.ReplyPagination pages = new com.RobinNotBad.BiliClient.model.ReplyPagination();

    private void continueLoading(int ignored) {
        com.RobinNotBad.BiliClient.model.ReplyPagination.Request request = pages.next();
        if (request != null) loadPage(request);
        else {
            page = Math.max(1, pages.loadedPage());
            if (!pages.hasMore()) setRefreshing(false);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadPage(com.RobinNotBad.BiliClient.model.ReplyPagination.Request request) {
        final long requestedOid = aid;
        final int requestedSort = sort == 3 ? 1 : 0;
        final int requestedType = type;
        setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                ReplyApi.ReplyPage result = ReplyApi.getRepliesPage(requestedOid, 0, request.page, requestedType, requestedSort);
                Reply focused = null;
                if (request.refresh && seek > 0) {
                    boolean found = false;
                    for (Reply reply : result.replies) if (reply.rpid == seek) found = true;
                    if (!found) {
                        try {
                            focused = ReplyApi.getRootReply(com.RobinNotBad.BiliClient.model.ContentType.getContentType(requestedType), requestedOid, seek).getOrNull();
                        } catch (Exception ignored) {
                        }
                    }
                }
                final Reply focus = focused;
                runOnUiThread(() -> {
                    if (getView() == null || !pages.accepts(request)) return;
                    if (request.refresh) replyList.clear();
                    int start = replyList.size();
                    int appended = ReplyApi.appendUnique(replyList, result.replies);
                    if (focus != null) {
                        boolean found = false;
                        for (Reply reply : replyList) if (reply.rpid == focus.rpid) found = true;
                        if (!found) replyList.add(0, focus);
                    }
                    if (replyAdapter == null) {
                        replyAdapter = createReplyAdapter();
                        replyAdapter.isManager = isManager;
                        setOnSortSwitch();
                        setAdapter(replyAdapter);
                    }
                    if (request.refresh && result.count >= 0) count = (int) Math.min(Integer.MAX_VALUE, result.count);
                    replyAdapter.count = count;
                    if (request.refresh) replyAdapter.notifyDataSetChanged();
                    else if (appended > 0) replyAdapter.notifyItemRangeInserted(start + 1, appended);
                    pages.succeed(request, result.hasMore, appended);
                    page = request.page;
                    bottom = !pages.hasMore();
                    setRefreshing(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (getView() == null || !pages.accepts(request)) return;
                    pages.fail(request);
                    page = Math.max(1, pages.loadedPage());
                    setRefreshing(false);
                    if (e instanceof com.RobinNotBad.BiliClient.api.ReplyPageContract.ApiException) MsgUtil.showMsgLong(e.getMessage());
                    else report(e);
                });
            }
        });
    }

    public void notifyReplyInserted(ReplyEvent event) {
        if (event.getOid() != aid || event.getMessage() == null || replyAdapter == null) return;
        runOnUiThread(() -> {
            if (getView() == null || replyAdapter == null) return;
            Reply reply = event.getMessage();
            if (reply.root == 0) {
                for (Reply existing : replyList) if (existing.rpid == reply.rpid) return;
                replyList.add(0, reply);
                replyAdapter.notifyItemInserted(1);
            } else {
                for (int i = 0; i < replyList.size(); i++) {
                    Reply parent = replyList.get(i);
                    if (parent.rpid == reply.root) {
                        if (ReplyApi.appendUnique(parent.childMsgList, java.util.Collections.singletonList(reply)) > 0) parent.childCount++;
                        replyAdapter.notifyItemChanged(i + 1);
                        break;
                    }
                }
            }
        });
    }

    public void refresh(long aid) {
        this.aid = aid;
        if (getView() == null) return;
        page = 1;
        bottom = false;
        loadPage(pages.refresh());
    }

    @Override
    public void onDestroyView() {
        pages.invalidate();
        replyAdapter = null;
        super.onDestroyView();
    }

    private void setOnSortSwitch() {
        replyAdapter.setOnSortSwitchListener(position -> {
            sort = (sort == 2 ? 3 : 2);
            replyAdapter.sort = this.sort;
            refresh(aid);
        });
    }
}