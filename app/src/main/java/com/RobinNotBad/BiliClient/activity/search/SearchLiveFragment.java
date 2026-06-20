package com.RobinNotBad.BiliClient.activity.search;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.RobinNotBad.BiliClient.adapter.LiveCardAdapter;
import com.RobinNotBad.BiliClient.api.LiveApi;
import com.RobinNotBad.BiliClient.api.SearchApi;
import com.RobinNotBad.BiliClient.model.LiveRoom;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;

import java.util.ArrayList;
import java.util.List;

public class SearchLiveFragment extends SearchFragment {

    private ArrayList<LiveRoom> roomList = new ArrayList<>();
    private LiveCardAdapter liveCardAdapter;

    public SearchLiveFragment() {
    }

    public static SearchLiveFragment newInstance() {
        return new SearchLiveFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roomList = new ArrayList<>();
        liveCardAdapter = new LiveCardAdapter(requireContext(), roomList);
        setAdapter(liveCardAdapter);

        setOnRefreshListener(this::refreshInternal);
        setOnLoadMoreListener(this::continueLoading);
    }

    private void continueLoading(int page) {
        CenterThreadPool.run(() -> {
            Log.e("debug", "加载下一页");
            try {
                com.google.gson.JsonElement result = SearchApi.searchType(keyword, page, "live");
                if (result != null) {
                    if (page == 1) showEmptyView(false);
                    com.google.gson.JsonArray arr = null;
                    if (result.isJsonObject()) {
                        com.google.gson.JsonElement lr = result.getAsJsonObject().get("live_room");
                        if (lr != null && lr.isJsonArray()) arr = lr.getAsJsonArray();
                    } else if (result.isJsonArray()) {
                        arr = result.getAsJsonArray();
                    }

                    List<LiveRoom> list = new ArrayList<>();
                    if (arr != null) {
                        org.json.JSONArray jsonArray = new org.json.JSONArray(arr.toString());
                        list.addAll(LiveApi.analyzeLiveRooms(jsonArray));
                    }
                    if (list.size() == 0) setBottom(true);
                    else CenterThreadPool.runOnUiThread(() -> {
                        int lastSize = roomList.size();
                        roomList.addAll(list);
                        liveCardAdapter.notifyItemRangeInserted(lastSize, roomList.size() - lastSize);
                    });
                } else setBottom(true);
            } catch (Exception e) {
                report(e);
            }
            setRefreshing(false);
            if (bottom && roomList.isEmpty()) {
                showEmptyView(true);
            }
        });
    }

    public void refreshInternal() {
        CenterThreadPool.runOnUiThread(() -> {
            page = 1;
            if (this.liveCardAdapter == null)
                this.liveCardAdapter = new LiveCardAdapter(this.requireContext(), this.roomList);
            int size_old = this.roomList.size();
            this.roomList.clear();
            if (size_old != 0) this.liveCardAdapter.notifyItemRangeRemoved(0, size_old);
            CenterThreadPool.run(() -> continueLoading(page));
        });
    }

}
