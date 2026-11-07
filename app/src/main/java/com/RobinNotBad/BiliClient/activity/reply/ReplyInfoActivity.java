package com.RobinNotBad.BiliClient.activity.reply;

import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.adapter.ReplyAdapter;
import com.RobinNotBad.BiliClient.api.ReplyApi;
import com.RobinNotBad.BiliClient.api.ReplyPageContract;
import com.RobinNotBad.BiliClient.event.ReplyEvent;
import com.RobinNotBad.BiliClient.model.ContentType;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.model.ReplyPagination;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;

public class ReplyInfoActivity extends BaseActivity {
    private long oid, rpid, upMid;
    private ContentType type;
    private boolean isManager;
    private boolean destroyed;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private ReplyAdapter adapter;
    private final ArrayList<Reply> replies = new ArrayList<>();
    private final ReplyPagination pages = new ReplyPagination();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_refresh);
        oid = getIntent().getLongExtra("oid", 0);
        rpid = getIntent().getLongExtra("rpid", 0);
        upMid = getIntent().getLongExtra("up_mid", -1);
        isManager = getIntent().getBooleanExtra("is_manager", false);
        try {
            type = ContentType.getContentType(getIntent().getIntExtra("type", 1));
            if (oid <= 0 || rpid <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            MsgUtil.showMsg("评论参数无效");
            finish();
            return;
        }
        setPageName("评论详情");
        refreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new CustomLinearManager(this));
        refreshLayout.setOnRefreshListener(this::refresh);
        if (SharedPreferencesUtil.getBoolean("ui_landscape", false)) {
            DisplayMetrics metrics = new DisplayMetrics();
            android.view.Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= 17) display.getRealMetrics(metrics);
            else display.getMetrics(metrics);
            int padding = metrics.widthPixels / 6;
            recyclerView.setPadding(padding, 0, padding, 0);
        }
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                LinearLayoutManager manager = (LinearLayoutManager) view.getLayoutManager();
                if (dy > 0 && manager != null && manager.findLastVisibleItemPosition() >= manager.getItemCount() - 3) {
                    ReplyPagination.Request request = pages.next();
                    if (request != null) load(request);
                }
            }
        });
        refresh();
    }

    private void refresh() {
        load(pages.refresh());
    }

    private void load(ReplyPagination.Request request) {
        refreshLayout.setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                Reply root = request.refresh ? ReplyApi.getRootReply(type, oid, rpid).getOrThrow() : null;
                ReplyApi.ReplyPage result = ReplyApi.getRepliesPage(oid, rpid, request.page, type.getTypeCode(), 0);
                runOnUiThread(() -> {
                    if (destroyed || !pages.accepts(request)) return;
                    if (request.refresh) {
                        replies.clear();
                        replies.add(root);
                    }
                    int start = replies.size();
                    int added = ReplyApi.appendUnique(replies, result.replies);
                    if (adapter == null) {
                        adapter = new ReplyAdapter(this, replies, oid, rpid, type.getTypeCode(), 0, upMid);
                        adapter.isManager = isManager;
                        adapter.isDetail = true;
                        recyclerView.setAdapter(adapter);
                    } else if (request.refresh) {
                        adapter.notifyDataSetChanged();
                    } else if (added > 0) {
                        adapter.notifyItemRangeInserted(start + 1, added);
                    }
                    pages.succeed(request, result.hasMore, added);
                    refreshLayout.setRefreshing(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed || !pages.accepts(request)) return;
                    pages.fail(request);
                    refreshLayout.setRefreshing(false);
                    if (e instanceof ReplyPageContract.ApiException) MsgUtil.showMsgLong(e.getMessage());
                    else report(e);
                });
            }
        });
    }

    @Override
    protected boolean eventBusEnabled() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true, priority = 1)
    public void onEvent(ReplyEvent event) {
        Reply reply = event.getMessage();
        if (event.getOid() != oid || reply == null || reply.root != rpid || adapter == null) return;
        int start = replies.size();
        if (ReplyApi.appendUnique(replies, Collections.singletonList(reply)) > 0) {
            adapter.notifyItemInserted(start + 1);
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        pages.invalidate();
        super.onDestroy();
    }
}
