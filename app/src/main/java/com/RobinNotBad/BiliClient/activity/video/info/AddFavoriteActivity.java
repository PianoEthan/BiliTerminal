package com.RobinNotBad.BiliClient.activity.video.info;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.favorite.FolderChooseAdapter;
import com.RobinNotBad.BiliClient.api.FavoriteApi;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;

//添加收藏
//2023-08-28

public class AddFavoriteActivity extends RefreshListActivity {
    private FolderChooseAdapter adapter;
    private long aid;
    private boolean loading;
    private boolean finishing;
    private static final int RESULT_ADDED = 1;
    private static final int RESULT_DELETED = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPageName("添加收藏");
        Intent intent = getIntent();
        aid = intent.getLongExtra("aid", 0);
        if (aid <= 0) {
            MsgUtil.showMsg("视频信息无效");
            finish();
            return;
        }
        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
            MsgUtil.showMsg("还没有登录喵~");
            finish();
            return;
        }
        setOnRefreshListener(this::loadFolders);
        loadFolders();
    }

    private void loadFolders() {
        if (loading || adapter != null || isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) return;
        loading = true;
        setRefreshing(true);
        swipeRefreshLayout.setEnabled(false);
        try {
            CenterThreadPool.run(() -> {
                ArrayList<String> folderList = new ArrayList<>();
                ArrayList<Boolean> stateList = new ArrayList<>();
                ArrayList<Long> mediaIds = new ArrayList<>();
                try {
                    FavoriteApi.getFavoriteState(aid, folderList, mediaIds, stateList);
                    runOnUiThread(() -> {
                        loading = false;
                        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) return;
                        adapter = new FolderChooseAdapter(this, folderList, mediaIds, stateList, aid);
                        setAdapter(adapter);
                        setRefreshing(false);
                        swipeRefreshLayout.setEnabled(false);
                        if (folderList.isEmpty()) showEmptyView();
                        else hideEmptyView();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> onLoadError(e));
                }
            });
        } catch (Exception e) {
            onLoadError(e);
        }
    }

    private void onLoadError(Exception e) {
        loading = false;
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) return;
        setRefreshing(false);
        swipeRefreshLayout.setEnabled(true);
        if (e instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
            MsgUtil.showMsgLong(e.getMessage());
        } else {
            MsgUtil.err(e);
        }
    }

    @Override
    public void finish() {
        if (adapter != null && adapter.isBusy()) {
            MsgUtil.showMsg("正在更新收藏，请稍候");
            return;
        }
        if (!finishing) {
            finishing = true;
            if (adapter != null && adapter.hasChanges()) {
                setResult(adapter.isFavorite() ? RESULT_ADDED : RESULT_DELETED);
                if (SharedPreferencesUtil.getBoolean("fav_notice", true)) {
                    MsgUtil.showMsg(adapter.isFavorite() ? "更改成功" : "删除成功");
                }
            }
        }
        super.finish();
    }
}