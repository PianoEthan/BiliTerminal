package com.RobinNotBad.BiliClient.activity.video.info;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.ImageViewerActivity;
import com.RobinNotBad.BiliClient.activity.base.BaseFragment;
import com.RobinNotBad.BiliClient.activity.dynamic.send.SendDynamicActivity;
import com.RobinNotBad.BiliClient.activity.search.SearchActivity;
import com.RobinNotBad.BiliClient.activity.settings.SettingPlayerChooseActivity;
import com.RobinNotBad.BiliClient.activity.user.WatchLaterActivity;
import com.RobinNotBad.BiliClient.activity.video.MultiPageActivity;
import com.RobinNotBad.BiliClient.activity.video.QualityChooserActivity;
import com.RobinNotBad.BiliClient.activity.video.collection.CollectionInfoActivity;
import com.RobinNotBad.BiliClient.adapter.user.UpListAdapter;
import com.RobinNotBad.BiliClient.api.BangumiApi;
import com.RobinNotBad.BiliClient.api.DynamicApi;
import com.RobinNotBad.BiliClient.api.HistoryApi;
import com.RobinNotBad.BiliClient.api.LikeCoinFavApi;
import com.RobinNotBad.BiliClient.api.PlayerApi;
import com.RobinNotBad.BiliClient.api.VideoInfoApi;
import com.RobinNotBad.BiliClient.api.WatchLaterApi;
import com.RobinNotBad.BiliClient.model.PlayerData;
import com.RobinNotBad.BiliClient.model.VideoInfo;
import com.RobinNotBad.BiliClient.ui.widget.RadiusBackgroundSpan;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.RobinNotBad.BiliClient.util.TerminalContext;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//真正的视频详情页
//2023-07-17

public class VideoInfoFragment extends BaseFragment {

    final int RESULT_ADDED = 1;
    final int RESULT_DELETED = -1;
    final ActivityResultLauncher<Intent> favLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            int code = o.getResultCode();
            if (code == RESULT_ADDED) {
                fav.setImageResource(R.drawable.icon_fav_1);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(fav);
            } else if (code == RESULT_DELETED) {
                fav.setImageResource(R.drawable.icon_fav_0);
            }
        }
    });
    private VideoInfo videoInfo;
    // 其实我不会用，也是抄的上面的😡
    final ActivityResultLauncher<Intent> writeDynamicLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            int code = result.getResultCode();
            Intent data = result.getData();
            if (code == RESULT_OK && data != null) {
                String text = data.getStringExtra("text");
                CenterThreadPool.run(() -> {
                    try {
                        long dynId;
                        Map<String, Long> atUids = new HashMap<>();
                        Pattern pattern = Pattern.compile("@(\\S+)\\s");
                        Matcher matcher = pattern.matcher(text);
                        while (matcher.find()) {
                            String matchedString = matcher.group(1);
                            long uid;
                            if ((uid = DynamicApi.mentionAtFindUser(matchedString)) != -1) {
                                atUids.put(matchedString, uid);
                            }
                        }
                        dynId = DynamicApi.relayVideo(text, (atUids.isEmpty() ? null : atUids), videoInfo.aid);

                        if (dynId != -1) MsgUtil.showMsg("转发成功~");
                        else MsgUtil.showMsg("转发失败");

                    } catch (Exception e) {
                        MsgUtil.err(e);
                    }
                });
            }
        }
    });
    private PlayerData playerData;
    private long aid;
    private String bvid;
    private TextView description;
    private TextView tagsText;
    private ImageView like, coin, fav;
    private int coinAdd = 0;
    private boolean desc_expand = false, tags_expand = false;
    private Animation shakeAnimation;
    private Runnable tripleActionRunnable;
    private boolean isTripleInProgress = false;

    public VideoInfoFragment() {
    }


    public static VideoInfoFragment newInstance(long aid, String bvid) {
        Bundle args = new Bundle();
        args.putLong("aid", aid);
        args.putString("bvid", bvid);
        VideoInfoFragment fragment = new VideoInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle == null) {
            MsgUtil.showMsg("视频详情页：数据为空");
            return;
        }
        aid = bundle.getLong("aid");
        bvid = bundle.getString("bvid");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View rootview, Bundle savedInstanceState) {
        super.onViewCreated(rootview, savedInstanceState);

        TerminalContext.getInstance().getVideoInfoByAidOrBvId(aid, bvid).observe(getViewLifecycleOwner(), (videoInfoResult) -> videoInfoResult.onSuccess((videoInfo) -> {
            if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) return;
            this.videoInfo = videoInfo;

            if (videoInfo == null) {
                Activity activity = getActivity();
                if (activity == null) return;
                activity.finish();
                return;
            }

            initView(rootview);
        }).onFailure((error) -> {
        }));
    }

    @SuppressLint("SetTextI18n")
    private void initView(View rootview) {
        if (SharedPreferencesUtil.getBoolean("ui_landscape", false)) {
            WindowManager windowManager = (WindowManager) rootview.getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= 17) display.getRealMetrics(metrics);
            else display.getMetrics(metrics);
            int paddings = metrics.widthPixels / 6;
            rootview.setPadding(paddings, 0, paddings, 0);
        }

        ImageView cover = rootview.findViewById(R.id.img_cover);
        TextView title = rootview.findViewById(R.id.text_title);
        description = rootview.findViewById(R.id.description);
        tagsText = rootview.findViewById(R.id.tags);
        MaterialCardView exclusiveTip = rootview.findViewById(R.id.exclusiveTip);
        RecyclerView up_recyclerView = rootview.findViewById(R.id.up_recyclerView);
        TextView exclusiveTipLabel = rootview.findViewById(R.id.exclusiveTipLabel);
        TextView viewCount = rootview.findViewById(R.id.viewCount);
        TextView timeText = rootview.findViewById(R.id.timeText);
        TextView durationText = rootview.findViewById(R.id.durationText);
        MaterialButton play = rootview.findViewById(R.id.play);
        MaterialButton addWatchlater = rootview.findViewById(R.id.addWatchlater);
        MaterialButton download = rootview.findViewById(R.id.download);
        MaterialButton relay = rootview.findViewById(R.id.relay);
        TextView bvidText = rootview.findViewById(R.id.bvidText);
        TextView danmakuCount = rootview.findViewById(R.id.danmakuCount);
        like = rootview.findViewById(R.id.btn_like);
        coin = rootview.findViewById(R.id.btn_coin);
        fav = rootview.findViewById(R.id.btn_fav);
        TextView likeLabel = rootview.findViewById(R.id.like_label);
        TextView coinLabel = rootview.findViewById(R.id.coin_label);
        TextView favLabel = rootview.findViewById(R.id.fav_label);
        MaterialCardView collectionCard = rootview.findViewById(R.id.collection);

        rootview.setVisibility(View.GONE);


        if (videoInfo.epid != -1) { //不是空的的话就应该跳转到番剧页面了
            Context context = rootview.getContext();
            if (context == null) return;
            CenterThreadPool.run(() -> {
                TerminalContext.getInstance()
                        .enterVideoDetailPage(context, BangumiApi.getMdidFromEpid(videoInfo.epid), null, "media");
                Activity activity = getActivity();
                if (activity == null) return;
                activity.finish();
            });
            return;
        }

        //显示封面
        Glide.with(getAppContext()).asDrawable().load(GlideUtil.url(videoInfo.cover)).placeholder(R.mipmap.placeholder)
                .transition(GlideUtil.getTransitionOptions())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(ToolsUtil.dp2px(4))).sizeMultiplier(0.85f).skipMemoryCache(true).dontAnimate())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(cover);

        //按内容动态取色：封面种子 → 详情页操作按钮/图标临时强调色
        applyContentTint(rootview, GlideUtil.url(videoInfo.cover));

        if (SharedPreferencesUtil.getBoolean("tags_enable", true)) {
            CenterThreadPool.run(() -> {
                try {
                    SpannableStringBuilder tags_spannable
                            = getDescSpan(VideoInfoApi.getTags(videoInfo.aid));

                    runOnUiThread(() -> {
                        tagsText.setMovementMethod(LinkMovementMethod.getInstance());
                        tagsText.setText(tags_spannable.toString());
                        tagsText.setOnClickListener(view1 -> {
                            tags_expand = !tags_expand;
                            if (tags_expand) {
                                tagsText.setMaxLines(233);
                                tagsText.setText(tags_spannable);
                            } else {
                                tagsText.setMaxLines(1);
                                tagsText.setText(tags_spannable.toString());
                            }
                        });
                    });
                } catch (Exception e) {
                    MsgUtil.err(e);
                }
            });
        } else tagsText.setVisibility(View.GONE);

        //点赞投币收藏
        if (videoInfo.stats.coined != 0)
            coin.setImageResource(R.drawable.icon_coin_1);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(coin);
        if (videoInfo.stats.liked)
            like.setImageResource(R.drawable.icon_like_1);
        if (videoInfo.stats.favoured)
            fav.setImageResource(R.drawable.icon_fav_1);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(fav);

        //历史上报
        CenterThreadPool.run(() -> {
            try {
                playerData = videoInfo.toPlayerData(0);
                PlayerApi.getVideo(playerData, false);
                if (playerData == null) return;
                HistoryApi.reportHistory(videoInfo.aid, playerData.cidHistory, playerData.progress / 1000);
            } catch (Exception e) {
                MsgUtil.err(e);
            }
            onFinishLoad();
        });

        //封面
        cover.requestFocus();
        cover.setOnClickListener(view1 -> {
            if (SharedPreferencesUtil.getBoolean("cover_play_enable", true)) playClick();
            else showCover();
        });
        cover.setOnLongClickListener(v -> {
            showCover();
            return true;
        });

        //标题
        title.setText(getTitleSpan());
        StringUtil.setCopy(title, videoInfo.title);

        //争议信息
        if (!videoInfo.argueMsg.isEmpty()) {
            exclusiveTipLabel.setText(videoInfo.argueMsg);
            exclusiveTip.setVisibility(View.VISIBLE);
        }

        //UP主列表
        UpListAdapter adapter = new UpListAdapter(requireContext(), videoInfo.staff);
        up_recyclerView.setHasFixedSize(true);
        up_recyclerView.setLayoutManager(new CustomLinearManager(getContext()));
        up_recyclerView.setAdapter(adapter);

        viewCount.setText(StringUtil.toWan(videoInfo.stats.view));
        likeLabel.setText(StringUtil.toWan(videoInfo.stats.like));
        coinLabel.setText(StringUtil.toWan(videoInfo.stats.coin));
        favLabel.setText(StringUtil.toWan(videoInfo.stats.favorite));

        danmakuCount.setText(String.valueOf(videoInfo.stats.danmaku));
        bvidText.setText(videoInfo.bvid);
        timeText.setText(videoInfo.timeDesc);
        durationText.setText(videoInfo.duration);

        description.setText(videoInfo.description);
        description.setOnClickListener(view1 -> {
            if (desc_expand) description.setMaxLines(3);
            else description.setMaxLines(512);
            desc_expand = !desc_expand;
        });
        StringUtil.setLink(description);
        StringUtil.setAtLink(videoInfo.descAts, description);
        StringUtil.setCopy(description);

        bvidText.setOnLongClickListener(v -> {
            Context context = getContext();
            if (context == null) {
                return true;
            }
            StringUtil.copyText(context, videoInfo.bvid);
            MsgUtil.showMsg("BV号已复制");
            return true;
        });

        //播放
        play.setOnClickListener(view1 -> playClick());
        play.setOnLongClickListener(view1 -> {
            Context context = getContext();
            if (context != null)
                startActivity(new Intent(context, SettingPlayerChooseActivity.class));
            return true;
        });

        //点赞
        rootview.findViewById(R.id.layout_like).setOnClickListener(view1 -> CenterThreadPool.run(() -> {
            if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
                MsgUtil.showMsg("还没有登录喵~");
                return;
            }
            try {
                int result = LikeCoinFavApi.like(videoInfo.aid, (videoInfo.stats.liked ? 2 : 1));
                if (result == 0) {
                    videoInfo.stats.liked = !videoInfo.stats.liked;
                    runOnUiThread(() -> {
                        MsgUtil.showMsg(videoInfo.stats.liked ? "点赞成功" : "取消成功");

                        if (videoInfo.stats.liked)
                            likeLabel.setText(StringUtil.toWan(++videoInfo.stats.like));
                        else likeLabel.setText(StringUtil.toWan(--videoInfo.stats.like));
                        like.setImageResource(videoInfo.stats.liked ? R.drawable.icon_like_1 : R.drawable.icon_like_0);
                    });
                } else if (isAdded()) {
                    String msg = "操作失败：" + result;
                    switch (result) {
                        case -403:
                            msg = "当前请求触发B站风控";
                            break;
                        case 65006:
                            msg = "已经点赞过了喵~";
                            videoInfo.stats.liked = true;
                            runOnUiThread(() -> {
                            like.setImageResource(R.drawable.icon_like_1);
                            com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(like);
                        });
                            break;
                    }
                    String finalMsg = msg;
                    MsgUtil.showMsg(finalMsg);
                }
            } catch (Exception e) {
                MsgUtil.err(e);
            }
        }));

        //投币
        rootview.findViewById(R.id.layout_coin).setOnClickListener(view1 -> CenterThreadPool.run(() -> {
            if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
                MsgUtil.showMsg("还没有登录喵~");
                return;
            }
            if (videoInfo.stats.coined < videoInfo.stats.coin_limit) {
                try {
                    int result = LikeCoinFavApi.coin(videoInfo.aid, 1);
                    if (result == 0) {
                        if (++coinAdd <= 2) videoInfo.stats.coined++;
                        runOnUiThread(() -> {
                            MsgUtil.showMsg("投币成功");
                            coinLabel.setText(StringUtil.toWan(++videoInfo.stats.coin));
                            coin.setImageResource(R.drawable.icon_coin_1);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(coin);
                        });
                    } else if (isAdded()) {
                        String msg = "投币失败：" + result;
                        if (result == -403) {
                            msg = "当前请求触发B站风控";
                        } else if (result == 34002) {
                            msg = "不能给自己投币哦";
                        }
                        String finalMsg = msg;
                        MsgUtil.showMsg(finalMsg);
                    }
                } catch (Exception e) {
                    MsgUtil.err(e);
                }
            } else {
                MsgUtil.showMsg("投币数量到达上限");
            }
        }));

        //收藏
        rootview.findViewById(R.id.layout_fav).setOnClickListener(view1 -> {
            Intent intent = new Intent();
            intent.setClass(requireContext(), AddFavoriteActivity.class);
            intent.putExtra("aid", videoInfo.aid);
            intent.putExtra("bvid", videoInfo.bvid);
            favLauncher.launch(intent);
        });

        //稍后再看
        addWatchlater.setOnClickListener(view1 -> CenterThreadPool.run(() -> {
            try {
                int result = WatchLaterApi.add(videoInfo.aid);
                if (result == 0) MsgUtil.showMsg("添加成功");
                else MsgUtil.showMsg("添加失败，错误码：" + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        addWatchlater.setOnLongClickListener(view1 -> {
            Intent intent = new Intent();
            intent.setClass(requireContext(), WatchLaterActivity.class);
            startActivity(intent);
            return true;
        });

        //下载
        download.setOnClickListener(view1 -> downloadClick());
        download.setOnLongClickListener(v -> {
            CenterThreadPool.run(() -> {
                File downPath = FileUtil.getVideoDownloadPath(videoInfo.title, null);
                FileUtil.deleteFolder(downPath);
                MsgUtil.showMsg("已清除此视频的缓存文件夹");
            });
            return true;
        });

        //转发
        relay.setOnClickListener((view1) -> {
            Intent intent = new Intent();
            intent.setClass(requireContext(), SendDynamicActivity.class);
            writeDynamicLauncher.launch(intent);
        });
        relay.setOnLongClickListener(v -> {
            StringUtil.copyText(requireContext(), "https://www.bilibili.com/" + videoInfo.bvid);
            MsgUtil.showMsg("视频完整链接已复制");
            return true;
        });

        //未登录隐藏按钮
        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
            addWatchlater.setVisibility(View.GONE);
            relay.setVisibility(View.GONE);
        }

        //合集按钮
        if (videoInfo.collection != null) {
            TextView collectionTitle = rootview.findViewById(R.id.collectionText);
            collectionTitle.setText(String.format("合集 · %s", videoInfo.collection.title));
            collectionCard.setOnClickListener((view1) ->
                    startActivity(new Intent(requireContext(), CollectionInfoActivity.class)
                            .putExtra("fromVideo", videoInfo.aid)));
        } else {
            collectionCard.setVisibility(View.GONE);
        }

        // 一键三连
        rootview.findViewById(R.id.layout_like).setOnLongClickListener(v -> {
            if (SharedPreferencesUtil.getBoolean("like_one_triple", true) &&
                    SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) != 0) {

                if (shakeAnimation == null) {
                    shakeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
                    shakeAnimation.setRepeatCount(Animation.INFINITE);
                }

                like.startAnimation(shakeAnimation);
                coin.startAnimation(shakeAnimation);
                fav.startAnimation(shakeAnimation);

                isTripleInProgress = true;
                tripleActionRunnable = () -> {
                    like.clearAnimation();
                    coin.clearAnimation();
                    fav.clearAnimation();

                    CenterThreadPool.run(() -> {
                        try {
                            int code = LikeCoinFavApi.triple(aid);
                            if (code == 0) {
                                coin.setImageResource(R.drawable.icon_coin_1);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(coin);
                                like.setImageResource(R.drawable.icon_like_1);
                                fav.setImageResource(R.drawable.icon_fav_1);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintImage(fav);
                                MsgUtil.showMsg("三连成功");
                            } else MsgUtil.showMsg("三连失败，错误码：" + code);
                        } catch (Exception e) {
                            MsgUtil.err("三连失败", e);
                        }
                    });
                };
                like.postDelayed(tripleActionRunnable, 2000);

                return true;
            }
            return false;
        });

        rootview.findViewById(R.id.layout_like).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                cancelTripleAction();
            }
            return false;
        });
    }

    private void cancelTripleAction() {
        if (isTripleInProgress) {
            isTripleInProgress = false;
            if (tripleActionRunnable != null) {
                like.removeCallbacks(tripleActionRunnable);
            }
            like.clearAnimation();
            coin.clearAnimation();
            fav.clearAnimation();
        }
    }


    private SpannableString getTitleSpan() {
        String string = null;

        //优先级要对
        if (videoInfo.upowerExclusive) string = "充电专属";
        else if (videoInfo.isSteinGate) string = "互动视频";
        else if (videoInfo.is360) string = "全景视频";
        else if (videoInfo.isCooperation) string = "联合投稿";

        if (string == null) return new SpannableString(videoInfo.title);

        SpannableString titleStr = new SpannableString(" " + string + " " + videoInfo.title);
        RadiusBackgroundSpan badgeBG = new RadiusBackgroundSpan(0, (int) getResources().getDimension(R.dimen.card_round), com.RobinNotBad.BiliClient.theme.ThemeManager.palette().buttonTint, com.RobinNotBad.BiliClient.theme.ThemeManager.palette().accent);
        titleStr.setSpan(badgeBG, 0, string.length() + 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return titleStr;
    }

    private SpannableStringBuilder getDescSpan(String tags) {
        SpannableStringBuilder tag_str = new SpannableStringBuilder("标签：");
        for (String str : tags.split("/")) {
            int old_len = tag_str.length();
            tag_str.append(str).append("/");
            tag_str.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(requireContext(), SearchActivity.class);
                    intent.putExtra("keyword", str);
                    requireContext().startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(com.RobinNotBad.BiliClient.theme.ThemeManager.palette().link);
                }
            }, old_len, tag_str.length() - 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return tag_str;
    }

    private void playClick() {
        if (SharedPreferencesUtil.getBoolean("first_play", true)) {
            SharedPreferencesUtil.putBoolean("first_play", false);

            if (SharedPreferencesUtil.getBoolean("cover_play_enable", true))
                MsgUtil.showDialog("播放视频", getString(R.string.tutorial_cover_play_enabled));
            else
                MsgUtil.showDialog("播放视频", getString(R.string.tutorial_cover_play_disabled));

            return;
        }

        Glide.get(getAppContext()).clearMemory();
        //在播放前清除内存缓存，因为手表内存太小了，播放完回来经常把Activity全释放掉
        //...经过测试，还是会释放，但会好很多
        if (videoInfo.pagenames.size() == 1) PlayerApi.startGettingUrl(playerData, videoInfo.cover);
        else
            startActivity(new Intent(requireContext(), MultiPageActivity.class)
                    .putExtra("data", playerData)
                    .putExtra("cover", videoInfo.cover));

        playerData.timeStamp = 0;
    }

    private void downloadClick() {
        if (!FileUtil.checkStoragePermission()) {
            FileUtil.requestStoragePermission(requireActivity());
        } else {
            File downPath = FileUtil.getVideoDownloadPath(videoInfo.title, null);

            if (downPath.exists() && videoInfo.pagenames.size() == 1) {
                File file_sign = new File(downPath, ".DOWNLOADING");
                MsgUtil.showMsg(file_sign.exists() ? "已在下载队列\n如有异常，长按可清空文件" : "已下载完成");
            } else {
                if (videoInfo.pagenames.size() > 1) {
                    Intent intent = new Intent();
                    intent.setClass(requireContext(), MultiPageActivity.class)
                            .putExtra("download", 1)
                            .putExtra("data", playerData);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(requireContext(), QualityChooserActivity.class)
                            .putExtra("page", 0)
                            .putExtra("aid", videoInfo.aid)
                            .putExtra("bvid", videoInfo.bvid)
                    );
                }
            }
        }
    }

    /** 内容动态取色：详情页操作按钮 + 未点亮图标 */
    private void applyContentTint(View root, String coverUrl) {
        com.RobinNotBad.BiliClient.theme.ContentTintHelper.requestTint(getAppContext(), coverUrl, tint -> {
            if (!isAdded() || getView() == null) return;
            View view = getView();
            MaterialButton[] buttons = new MaterialButton[]{
                    view.findViewById(R.id.play),
                    view.findViewById(R.id.addWatchlater),
                    view.findViewById(R.id.download),
                    view.findViewById(R.id.relay)};
            for (MaterialButton button : buttons) {
                if (button == null) continue;
                androidx.core.view.ViewCompat.setBackgroundTintList(button,
                        com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tint.container, tint.container));
                button.setTextColor(tint.onAccent);
            }
            tintOutlineIcon(view, R.id.btn_like, tint);
            tintOutlineIcon(view, R.id.btn_coin, tint);
            tintOutlineIcon(view, R.id.btn_fav, tint);
        });
    }

    /**
     * 给未点亮图标染临时强调色。状态变更走 setImageResource 换 drawable 实例，
     * 不会保留这里的 tint，因此无需（也无法可靠地）比较 ConstantState 判重。
     */
    private void tintOutlineIcon(View root, int viewId, com.RobinNotBad.BiliClient.theme.ContentTintHelper.ContentTint tint) {
        ImageView imageView = root.findViewById(viewId);
        if (imageView == null || imageView.getDrawable() == null) return;
        imageView.clearColorFilter(); // 全局染色可能设过 textPrimary 滤镜，强调色优先
        android.graphics.drawable.Drawable tinted =
                com.RobinNotBad.BiliClient.theme.ThemeCompat.tintDrawable(imageView.getDrawable(), tint.accent);
        if (tinted != imageView.getDrawable()) imageView.setImageDrawable(tinted); // API<21 包装实例需设回
    }

    private void showCover() {
        try {
            Intent intent = new Intent();
            intent.setClass(requireContext(), ImageViewerActivity.class);
            ArrayList<String> imageList = new ArrayList<>();
            imageList.add(videoInfo.cover);
            intent.putExtra("imageList", imageList);
            requireContext().startActivity(intent);
        } catch (Exception ignored) {
        }
    }


    public void onFinishLoad() {
        try {
            Activity activity = getActivity();
            if (activity instanceof VideoInfoActivity) {
                ((VideoInfoActivity) activity).crossFade(getView());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Logu.d("onDestroy");
        cancelTripleAction();
        super.onDestroy();

    }
}