package com.RobinNotBad.BiliClient.activity.user.favorite;

import android.content.Intent;
import android.os.Bundle;

import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.favorite.FavoriteFolderAdapter;
import com.RobinNotBad.BiliClient.api.FavoriteApi;
import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;

public class FavoriteFolderListActivity extends RefreshListActivity {

    private long mid;
    private boolean loading;
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPageName("收藏");
        mid = SharedPreferencesUtil.getLong("mid", 0);
        setOnRefreshListener(this::loadFolders);
        loadFolders();
    }

    private void loadFolders() {
        if (loading) return;
        if (mid <= 0) {
            MsgUtil.showMsg("请先登录后查看收藏夹");
            emptyView.setText("请先登录后查看收藏夹");
            showEmptyView();
            setRefreshing(false);
            return;
        }
        loading = true;
        setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                ArrayList<FavoriteFolder> folders = FavoriteApi.getFavoriteFolders(mid);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    FavoriteFolderAdapter adapter = new FavoriteFolderAdapter(this, folders, mid);
                    adapter.setOnCreateClickListener(this::showCreateDialog);
                    adapter.setOnLongClickListener(position -> {
                        if (position < 0 || position >= folders.size()) return;
                        FavoriteFolder folder = folders.get(position);
                        if (folder.mediaId <= 0) {
                            MsgUtil.showMsg("无法获取收藏夹信息，请下拉重试");
                            return;
                        }
                        Intent intent = new Intent(this, FavoriteFolderEditActivity.class);
                        intent.putExtra("mediaId", folder.mediaId);
                        startActivityForResult(intent, 1);
                    });
                    hideEmptyView();
                    setAdapter(adapter);
                    loading = false;
                    setRefreshing(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    loading = false;
                    if (e instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
                        MsgUtil.showMsgLong(e.getMessage());
                    } else {
                        report(e);
                        MsgUtil.showMsg("收藏夹加载失败，请下拉重试");
                    }
                    setRefreshing(false);
                });
            }
        });
    }

    private void showCreateDialog() {
        startActivityForResult(new Intent(this, FavoriteFolderCreateActivity.class), 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            loadFolders();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }
}
