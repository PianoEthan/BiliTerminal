package com.RobinNotBad.BiliClient.activity.video;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.adapter.video.HotSearchAdapter;
import com.RobinNotBad.BiliClient.api.HotSearchApi;
import com.RobinNotBad.BiliClient.model.HotSearchCard;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.view.ImageAutoLoadScrollListener;

import java.util.ArrayList;

//热搜排行榜页面
//2026-07-21

public class HotSearchActivity extends InstanceActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<HotSearchCard> hotList;
    private HotSearchAdapter adapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_main_refresh);
        setMenuClick();

        recyclerView = findViewById(R.id.recyclerView);
        ImageAutoLoadScrollListener.install(recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadHotSearch);

        TextView title = findViewById(R.id.pageName);
        title.setText("热搜");

        loadHotSearch();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadHotSearch() {
        swipeRefreshLayout.setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                ArrayList<HotSearchCard> list = new ArrayList<>();
                boolean success = HotSearchApi.getHotSearch(list);
                if (success) {
                    hotList = list;
                    runOnUiThread(() -> {
                        if (recyclerView.getLayoutManager() == null) {
                            recyclerView.setLayoutManager(new CustomLinearManager(this));
                        }
                        if (adapter == null) {
                            adapter = new HotSearchAdapter(this, hotList);
                            recyclerView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    });
                } else {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        MsgUtil.showMsgLong("获取热搜失败，请稍后重试");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    MsgUtil.showMsgLong("网络异常，请稍后重试");
                });
            }
        });
    }
}
