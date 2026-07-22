package com.RobinNotBad.BiliClient.activity.user;

import android.os.Bundle;
import android.util.Log;

import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.video.VideoCardAdapter;
import com.RobinNotBad.BiliClient.api.BangumiApi;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

//追番列表
//2024-06-07

public class FollowingBangumisActivity extends RefreshListActivity {

    private ArrayList<VideoCard> videoList;
    private VideoCardAdapter videoCardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setPageName("追番列表");

        recyclerView.setHasFixedSize(true);

        videoList = new ArrayList<>();

        CenterThreadPool.run(() -> {
            try {
                int result = BangumiApi.getFollowingList(page, videoList);
                if (result != -1) {
                    videoCardAdapter = new VideoCardAdapter(this, videoList);
                    setOnLoadMoreListener(this::continueLoading);
                    setRefreshing(false);
                    setAdapter(videoCardAdapter);

                    if (result == 1) {
                        Log.e("debug", "到底了");
                        setBottom(true);
                    }
                }

            } catch (Exception e) {
                loadFail(e);
            }
        });
    }

    private void continueLoading(int page) {
        CenterThreadPool.run(() -> {
            try {
                List<VideoCard> list = new ArrayList<>();
                int result = BangumiApi.getFollowingList(page, list);
                if (result != -1) {
                    Log.e("debug", "下一页");
                    //将追番列表条目缓存为已追番（用 season_id 作为 key，与详情页 seasonStatus API 一致）
                    for (VideoCard vc : list) {
                        if (vc.aid > 0) SharedPreferencesUtil.putBoolean("bangumi_follow_" + vc.aid, true);
                    }
                    runOnUiThread(() -> {
                        videoList.addAll(list);
                        videoCardAdapter.notifyItemRangeInserted(videoList.size() - list.size(), list.size());
                    });
                    if (result == 1) {
                        Log.e("debug", "到底了");
                        setBottom(true);
                    }
                }
                setRefreshing(false);
            } catch (Exception e) {
                loadFail(e);
            }
        });
    }
}