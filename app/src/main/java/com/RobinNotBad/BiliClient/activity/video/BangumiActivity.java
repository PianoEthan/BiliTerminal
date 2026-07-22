package com.RobinNotBad.BiliClient.activity.video;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.adapter.TimelineAdapter;
import com.RobinNotBad.BiliClient.adapter.video.VideoCardAdapter;
import com.RobinNotBad.BiliClient.api.BangumiApi;
import com.RobinNotBad.BiliClient.api.BangumiIndexApi;
import com.RobinNotBad.BiliClient.api.TimelineApi;
import com.RobinNotBad.BiliClient.model.Timeline;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.view.ImageAutoLoadScrollListener;

import java.util.ArrayList;
import java.util.List;

//番剧主页：新番 / 追番 / 索引（chip 风格筛选）
//2026-07-22

public class BangumiActivity extends InstanceActivity {

    private static final int TAB_COUNT = 3;
    private TextView[] tabs;
    private SwipeRefreshLayout[] refreshes;
    private RecyclerView[] recyclers;
    private boolean[] loaded;
    private int currentTab;
    private View filterBar;

    //追番
    private int followingPage;
    private boolean followingEnd;
    private List<VideoCard> followingList;
    private VideoCardAdapter followingAdapter;

    //索引
    private int idxSeasonType = 1, idxArea = -1, idxIsFinish = -1, idxSort = 3, idxStyleId = -1, idxPage = 1;
    private boolean idxEnd;
    private List<VideoCard> indexList;
    private VideoCardAdapter indexAdapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bangumi);
        setMenuClick();

        tabs = new TextView[]{findViewById(R.id.tab0), findViewById(R.id.tab1), findViewById(R.id.tab2)};
        refreshes = new SwipeRefreshLayout[]{findViewById(R.id.refresh0), findViewById(R.id.refresh1), findViewById(R.id.refresh2)};
        recyclers = new RecyclerView[]{findViewById(R.id.recycler0), findViewById(R.id.recycler1), findViewById(R.id.recycler2)};
        loaded = new boolean[TAB_COUNT];
        filterBar = findViewById(R.id.filterBar);

        for (int i = 0; i < TAB_COUNT; i++) {
            recyclers[i].setLayoutManager(new CustomLinearManager(this));
            ImageAutoLoadScrollListener.install(recyclers[i]);
            final int idx = i;
            tabs[i].setOnClickListener(v -> switchTab(idx));
        }
        refreshes[0].setOnRefreshListener(this::loadTimeline);
        refreshes[1].setOnRefreshListener(() -> loadFollowing(true));
        refreshes[2].setOnRefreshListener(() -> loadIndex(true));

        followingList = new ArrayList<>();
        recyclers[1].addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy > 0 && !followingEnd && currentTab == 1) {
                    CustomLinearManager lm = (CustomLinearManager) rv.getLayoutManager();
                    if (lm != null && lm.findLastVisibleItemPosition() >= rv.getLayoutManager().getItemCount() - 3)
                        loadFollowing(false);
                }
            }
        });
        indexList = new ArrayList<>();
        recyclers[2].addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy > 0 && !idxEnd && currentTab == 2) {
                    CustomLinearManager lm = (CustomLinearManager) rv.getLayoutManager();
                    if (lm != null && lm.findLastVisibleItemPosition() >= rv.getLayoutManager().getItemCount() - 3)
                        loadIndex(false);
                }
            }
        });
        initFilterChips();
        switchTab(0);
    }

    private void switchTab(int tab) {
        currentTab = tab;
        for (int i = 0; i < TAB_COUNT; i++) {
            tabs[i].setTextColor(i == tab ? 0xFFFB7299 : 0xFF999999);
            refreshes[i].setVisibility(i == tab ? View.VISIBLE : View.GONE);
        }
        filterBar.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        if (!loaded[tab]) {
            if (tab == 0) loadTimeline();
            else if (tab == 1) loadFollowing(true);
            else loadIndex(true);
        }
    }

    // ============== Tab 0: 新番 ==============
    private void loadTimeline() {
        if (isFinishing()) return;
        refreshes[0].setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                List<Timeline.DayInfo> result = TimelineApi.getTimeline("1", 7, 7);
                if (isFinishing()) return;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    TimelineAdapter adapter = new TimelineAdapter(this, new ArrayList<>(result));
                    recyclers[0].setAdapter(adapter);
                    loaded[0] = true;
                    refreshes[0].setRefreshing(false);
                });
            } catch (Exception e) {
                if (!isFinishing()) runOnUiThread(() -> refreshes[0].setRefreshing(false));
            }
        });
    }

    // ============== Tab 1: 追番 ==============
    @SuppressLint("NotifyDataSetChanged")
    private void loadFollowing(boolean reset) {
        if (isFinishing()) return;
        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
            MsgUtil.showMsgLong("未登录，无法获取追番列表");
            refreshes[1].setRefreshing(false);
            loaded[1] = true;
            return;
        }
        if (reset) { followingPage = 1; followingEnd = false; }
        if (followingEnd) { refreshes[1].setRefreshing(false); return; }
        refreshes[1].setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                List<VideoCard> list = new ArrayList<>();
                int r = BangumiApi.getFollowingList(followingPage, list);
                if (isFinishing()) return;
                int np = followingPage + 1;
                boolean end = (r == 1 || list.isEmpty());
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (reset) followingList.clear();
                    followingList.addAll(list);
                    followingPage = np; followingEnd = end;
                    if (followingAdapter == null) {
                        followingAdapter = new VideoCardAdapter(this, followingList);
                        recyclers[1].setAdapter(followingAdapter);
                    } else followingAdapter.notifyDataSetChanged();
                    loaded[1] = true;
                    refreshes[1].setRefreshing(false);
                });
            } catch (Exception e) {
                if (!isFinishing()) runOnUiThread(() -> refreshes[1].setRefreshing(false));
            }
        });
    }

    // ============== Tab 2: 索引（chip 筛选）==============
    private void initFilterChips() {
        setupChipGroup(0, null, R.id.ftTypeAnim, R.id.ftTypeChina);
        setupChipGroup(0, null, R.id.ftStatusAll, R.id.ftStatusOngoing, R.id.ftStatusDone);
        setupChipGroup(0, null, R.id.ftAreaAll, R.id.ftAreaJapan, R.id.ftAreaChina);
        setupChipGroup(1, null, R.id.ftSortMixed, R.id.ftSortHot);
        initStyleChips();
    }

    /**创建风格标签行（热血/搞笑/治愈/催泪/战斗/日常/科幻/恋爱）*/
    private void initStyleChips() {
        //风格标签 ID 映射（基于 B站 PGC index style_id）
        int[] styleIds = {-1, 1, 5, 9, 55, 13, 15, 25, 7}; //全部/热血/搞笑/治愈/催泪/战斗/日常/科幻/恋爱
        String[] styleNames = {"全部风格", "热血", "搞笑", "治愈", "催泪", "战斗", "日常", "科幻", "恋爱"};
        LinearLayout styleRow = findViewById(R.id.styleRow);
        for (int i = 0; i < styleNames.length; i++) {
            final int sid = styleIds[i];
            TextView tv = new TextView(this);
            tv.setText(styleNames[i]);
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);
            int px10 = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);
            int px26 = (int) (26 * getResources().getDisplayMetrics().density + 0.5f);
            tv.setPadding(px10, 0, px10, 0);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, px26));
            tv.setBackgroundResource(R.drawable.sel_filter_chip);
            tv.setSelected(i == 0);
            tv.setTextColor(i == 0 ? 0xFFFFFFFF : 0xFF666666);
            tv.setOnClickListener(v -> {
                idxStyleId = sid;
                for (int j = 0; j < styleRow.getChildCount(); j++) {
                    View child = styleRow.getChildAt(j);
                    child.setSelected(j == styleRow.indexOfChild(v));
                    ((TextView) child).setTextColor(j == styleRow.indexOfChild(v) ? 0xFFFFFFFF : 0xFF666666);
                }
                reloadIndex();
            });
            styleRow.addView(tv);
        }
    }

    /**设置一组互斥的筛选 chip，选中对应项变粉色白字，未选中灰色字 */
    private void setupChipGroup(int defaultIdx, Runnable onAnyClick, int... ids) {
        for (int i = 0; i < ids.length; i++) {
            final int idx = i;
            TextView tv = findViewById(ids[i]);
            tv.setSelected(i == defaultIdx);
            tv.setTextColor(i == defaultIdx ? 0xFFFFFFFF : 0xFF666666);
            tv.setOnClickListener(v -> {
                for (int j = 0; j < ids.length; j++) {
                    TextView t = findViewById(ids[j]);
                    t.setSelected(j == idx);
                    t.setTextColor(j == idx ? 0xFFFFFFFF : 0xFF666666);
                }
                if (onAnyClick != null) onAnyClick.run();
                applyFilters();
            });
        }
    }

    /**根据当前选中的 chip 状态计算筛选参数并重新加载索引 */
    private void applyFilters() {
        idxSeasonType = findViewById(R.id.ftTypeAnim).isSelected() ? 1 : 4;
        idxIsFinish = findViewById(R.id.ftStatusAll).isSelected() ? -1 :
                      findViewById(R.id.ftStatusOngoing).isSelected() ? 0 : 1;
        idxArea = findViewById(R.id.ftAreaAll).isSelected() ? -1 :
                  findViewById(R.id.ftAreaJapan).isSelected() ? 2 : 1;
        idxSort = findViewById(R.id.ftSortHot).isSelected() ? 3 : 0;
        reloadIndex();
    }

    private void reloadIndex() {
        idxPage = 1; idxEnd = false; indexList.clear();
        if (indexAdapter != null) indexAdapter.notifyDataSetChanged();
        loadIndex(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadIndex(boolean reset) {
        if (isFinishing()) return;
        if (reset) { idxPage = 1; idxEnd = false; indexList.clear(); }
        if (idxEnd) { refreshes[2].setRefreshing(false); return; }
        refreshes[2].setRefreshing(true);
        CenterThreadPool.run(() -> {
            try {
                List<VideoCard> list = new ArrayList<>();
                boolean hasNext = BangumiIndexApi.getIndex(idxSeasonType, idxArea, idxIsFinish, idxStyleId, -1, idxSort, idxPage, list);
                if (isFinishing()) return;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    indexList.addAll(list);
                    idxPage++;
                    idxEnd = !hasNext;
                    if (indexAdapter == null) {
                        indexAdapter = new VideoCardAdapter(this, indexList);
                        recyclers[2].setAdapter(indexAdapter);
                    } else indexAdapter.notifyDataSetChanged();
                    loaded[2] = true;
                    refreshes[2].setRefreshing(false);
                });
            } catch (Exception e) {
                if (!isFinishing()) runOnUiThread(() -> refreshes[2].setRefreshing(false));
            }
        });
    }


}
