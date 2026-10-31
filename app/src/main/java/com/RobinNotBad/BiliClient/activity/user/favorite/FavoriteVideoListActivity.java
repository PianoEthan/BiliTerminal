package com.RobinNotBad.BiliClient.activity.user.favorite;

import android.os.Bundle;

import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.video.VideoCardAdapter;
import com.RobinNotBad.BiliClient.api.FavoriteApi;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;

import java.util.ArrayList;

public class FavoriteVideoListActivity extends RefreshListActivity {

    private long mediaId;
    private final ArrayList<VideoCard> videoList = new ArrayList<>();
    private VideoCardAdapter videoCardAdapter;
    private boolean busy;
    private boolean destroyed;
    private boolean firstPageLoaded;
    private long longClickAid = -1;
    private long longClickTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaId = getIntent().getLongExtra("mediaId", 0);
        String name = getIntent().getStringExtra("name");
        setPageName(name == null ? "收藏夹" : name);
        if (mediaId <= 0) {
            setRefreshing(false);
            MsgUtil.showMsg("收藏夹参数无效，请返回重试");
            finish();
            return;
        }
        videoCardAdapter = new VideoCardAdapter(this, videoList);
        videoCardAdapter.setOnLongClickListener(this::handleLongClick);
        setAdapter(videoCardAdapter);
        setOnRefreshListener(this::refreshVideos);
        setOnLoadMoreListener(this::loadMore);
        refreshVideos();
    }

    private void refreshVideos() {
        if (busy) return;
        page = 1;
        setBottom(false);
        longClickAid = -1;
        loadVideos(1, true);
    }

    private void loadMore(int nextPage) {
        if (busy) {
            page = Math.max(1, nextPage - 1);
            return;
        }
        if (!firstPageLoaded) {
            page = 1;
            loadVideos(1, true);
        } else {
            loadVideos(nextPage, false);
        }
    }

    @SuppressWarnings("NotifyDataSetChanged")
    private void loadVideos(int requestedPage, boolean refresh) {
        busy = true;
        setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                var result = FavoriteApi.getFolderVideos(mediaId, requestedPage);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    if (refresh) {
                        videoList.clear();
                        videoList.addAll(result.videos);
                        videoCardAdapter.notifyDataSetChanged();
                        firstPageLoaded = true;
                    } else {
                        int start = videoList.size();
                        videoList.addAll(result.videos);
                        videoCardAdapter.notifyItemRangeInserted(start, result.videos.size());
                    }
                    page = requestedPage;
                    setBottom(!result.hasMore);
                    busy = false;
                    setRefreshing(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    page = refresh ? 1 : Math.max(1, requestedPage - 1);
                    if (refresh) firstPageLoaded = false;
                    busy = false;
                    if (e instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
                        MsgUtil.showMsgLong(e.getMessage());
                    } else {
                        report(e);
                    }
                    setRefreshing(false);
                });
            }
        });
    }

    private void handleLongClick(int position) {
        if (busy || position < 0 || position >= videoList.size()) return;
        VideoCard video = videoList.get(position);
        long aid = video.aid;
        long timestamp = System.currentTimeMillis();
        if (longClickAid != aid || timestamp - longClickTimestamp >= 4000) {
            longClickAid = aid;
            longClickTimestamp = timestamp;
            MsgUtil.showMsg("再次长按删除");
            return;
        }
        longClickAid = -1;
        busy = true;
        swipeRefreshLayout.setEnabled(false);
        setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                int result = FavoriteApi.deleteFavorite(aid, mediaId);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    if (result == 0) {
                        int index = videoList.indexOf(video);
                        if (index < 0) {
                            for (int i = 0; i < videoList.size(); i++) {
                                if (videoList.get(i).aid == aid) {
                                    index = i;
                                    break;
                                }
                            }
                        }
                        if (index >= 0) {
                            videoList.remove(index);
                            videoCardAdapter.notifyItemRemoved(index);
                            videoCardAdapter.notifyItemRangeChanged(index, videoList.size() - index);
                        }
                        MsgUtil.showMsg("删除成功");
                    } else {
                        MsgUtil.showMsg("删除失败，错误码：" + result);
                    }
                    finishDelete();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    if (e instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
                        MsgUtil.showMsgLong(e.getMessage());
                    } else {
                        report(e);
                    }
                    finishDelete();
                });
            }
        });
    }

    private void finishDelete() {
        busy = false;
        swipeRefreshLayout.setEnabled(true);
        setRefreshing(false);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }
}
