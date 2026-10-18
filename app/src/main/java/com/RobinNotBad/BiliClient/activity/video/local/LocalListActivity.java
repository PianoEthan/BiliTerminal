package com.RobinNotBad.BiliClient.activity.video.local;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.adapter.video.LocalVideoAdapter;
import com.RobinNotBad.BiliClient.model.LocalVideo;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;

import java.io.File;
import java.util.ArrayList;

//本地列表
//2023-08-07

public class LocalListActivity extends InstanceActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private final ArrayList<LocalVideo> videoList = new ArrayList<>(10);
    private LocalVideoAdapter adapter;
    private TextView emptyTip;

    private int longClickPosition = -1;
    private long longClickTimestamp;

    private boolean started;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_main_refresh);
        setMenuClick();

        recyclerView = findViewById(R.id.recyclerView);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        emptyTip = findViewById(R.id.emptyTip);

        TextView pageName = findViewById(R.id.pageName);
        pageName.setText("缓存");

        if (!FileUtil.checkStoragePermission()) {
            FileUtil.requestStoragePermission(this);
        }

        CenterThreadPool.run(() -> {
            runOnUiThread(() -> swipeRefreshLayout.setRefreshing(true));
            ArrayList<LocalVideo> scanned = scan(FileUtil.getVideoDownloadPath());

            runOnUiThread(() -> {
                videoList.clear();
                videoList.addAll(scanned);
                adapter = new LocalVideoAdapter(this, videoList);

                adapter.setOnLongClickListener(position -> {
                    long timestamp = System.currentTimeMillis();
                    if (longClickPosition == position && timestamp - longClickTimestamp < 4000) {
                        File file = new File(FileUtil.getVideoDownloadPath(), videoList.get(position).title);
                        CenterThreadPool.run(() -> FileUtil.deleteFolder(file));
                        MsgUtil.showMsg("删除成功");
                        videoList.remove(position);
                        adapter.notifyItemRemoved(position + 1);
                        adapter.notifyItemRangeChanged(position + 1, videoList.size() - position);
                        longClickPosition = -1;
                        checkEmpty();
                    } else {
                        longClickPosition = position;
                        longClickTimestamp = timestamp;
                        MsgUtil.showMsg("再次长按删除");
                    }
                });

                recyclerView.setLayoutManager(getLayoutManager());
                recyclerView.setAdapter(adapter);
                swipeRefreshLayout.setRefreshing(false);
                checkEmpty();
                started = true;
            });
        });
    }

    private ArrayList<LocalVideo> scan(File folder) {
        ArrayList<LocalVideo> result = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null)
            return result;

        for (File video : files) {
            if (video.isDirectory()) {
                LocalVideo localVideo = new LocalVideo();
                localVideo.title = video.getName();

                localVideo.cover = (new File(video, "cover.png")).toString();

                localVideo.pageList = new ArrayList<>();
                localVideo.danmakuFileList = new ArrayList<>();
                localVideo.videoFileList = new ArrayList<>();
                localVideo.sizeList = new ArrayList<>();

                File videoFile = new File(video, "video.mp4");
                File audioFile = new File(video, "audio.m4a"); // 仅音频文件
                File danmakuFile = new File(video, "danmaku.xml");

                // 优先检查视频文件，其次检查音频文件
                File mediaFile = videoFile.exists() ? videoFile : (audioFile.exists() ? audioFile : null);

                if (mediaFile != null) {
                    File mark = new File(video, ".DOWNLOADING");
                    if (mark.exists())
                        continue;

                    localVideo.sizeList.add(mediaFile.length());
                    localVideo.videoFileList.add(mediaFile.toString());
                    localVideo.danmakuFileList.add(danmakuFile.toString()); // 单集视频

                    localVideo.calcTotalSize();
                    result.add(localVideo);
                } else {
                    File[] pages = video.listFiles(); // 分页视频
                    if (pages != null) {
                        for (File page : pages) {
                            if (page.isDirectory()) {
                                File mark = new File(page, ".DOWNLOADING");
                                if (mark.exists())
                                    continue;

                                File pageVideoFile = new File(page, "video.mp4");
                                File pageAudioFile = new File(page, "audio.m4a"); // 仅音频文件
                                File pageDanmakuFile = new File(page, "danmaku.xml");

                                // 优先检查视频文件，其次检查音频文件
                                File pageMediaFile = pageVideoFile.exists() ? pageVideoFile
                                        : (pageAudioFile.exists() ? pageAudioFile : null);

                                if (pageMediaFile != null) {
                                    localVideo.pageList.add(page.getName());
                                    localVideo.sizeList.add(pageMediaFile.length());
                                    localVideo.videoFileList.add(pageMediaFile.toString());
                                    localVideo.danmakuFileList.add(pageDanmakuFile.toString());
                                }
                            }
                        }
                        localVideo.calcTotalSize();
                        if (localVideo.videoFileList.size() > 0)
                            result.add(localVideo);
                    }
                }
            }
        }
        return result;
    }

    private void checkEmpty() {
        runOnUiThread(() -> {
            if (videoList.isEmpty() && emptyTip != null) {
                emptyTip.setVisibility(View.VISIBLE);
            } else {
                if (emptyTip != null) {
                    emptyTip.setVisibility(View.GONE);
                }
            }
        });
    }

    public void refresh() {
        if (started) {
            runOnUiThread(() -> swipeRefreshLayout.setRefreshing(true));
            CenterThreadPool.run(() -> {
                ArrayList<LocalVideo> scanned = scan(FileUtil.getVideoDownloadPath());
                runOnUiThread(() -> {
                    videoList.clear();
                    videoList.addAll(scanned);
                    adapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                    checkEmpty();
                });
            });
        }
    }
}