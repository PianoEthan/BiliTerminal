package com.RobinNotBad.BiliClient.adapter.message;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.user.info.UserInfoActivity;
import com.RobinNotBad.BiliClient.adapter.video.VideoCardHolder;
import com.RobinNotBad.BiliClient.api.ReplyApi;
import com.RobinNotBad.BiliClient.model.MessageCard;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.RobinNotBad.BiliClient.util.TerminalContext;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.text.SimpleDateFormat;

public class NoticeHolder extends RecyclerView.ViewHolder {
    public final LinearLayout avaterList;
    public final TextView action;
    public final TextView pubdate;
    public final ConstraintLayout extraCard;
    public final View itemView;

    public NoticeHolder(@NonNull View itemView) {
        super(itemView);
        this.itemView = itemView;
        avaterList = itemView.findViewById(R.id.avatar_list);
        action = itemView.findViewById(R.id.action);
        pubdate = itemView.findViewById(R.id.pubdate);
        extraCard = itemView.findViewById(R.id.extraCard);
    }

    @SuppressLint("SetTextI18n")
    public void showMessage(MessageCard message, Context context) {
        avaterList.removeAllViews();
        if (message.user.isEmpty()) avaterList.setVisibility(View.GONE);
        else avaterList.setVisibility(View.VISIBLE);
        for (int i = 0; i < message.user.size(); i++) {
            ImageView imageView = new ImageView(context);
            Glide.with(BiliTerminal.context)
                    .asDrawable()
                    .load(GlideUtil.url(message.user.get(i).avatar))
                    .transition(GlideUtil.getTransitionOptions())
                    .placeholder(R.mipmap.akari)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageView);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ToolsUtil.dp2px(32), ToolsUtil.dp2px(32)));
            imageView.setLeft(ToolsUtil.dp2px(3));
            int finalI = i;
            imageView.setOnClickListener(view1 -> {
                Intent intent = new Intent();
                intent.setClass(context, UserInfoActivity.class);
                intent.putExtra("mid", message.user.get(finalI).mid);
                context.startActivity(intent);
            });
            avaterList.addView(imageView);

            TextView nickView = new TextView(context);
            nickView.setText(message.user.get(i).name);
            nickView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            nickView.setGravity(Gravity.CENTER_VERTICAL);
            nickView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ToolsUtil.dp2px(32)));
            avaterList.addView(nickView);

            //这个View什么都没有，用来当间隔的
            View view = new View(context);
            view.setLayoutParams(new ViewGroup.LayoutParams(ToolsUtil.dp2px(3), ToolsUtil.dp2px(32)));
            avaterList.addView(view);
        }

        if (message.timeStamp != 0) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            pubdate.setText(sdf.format(message.timeStamp * 1000));
        } else pubdate.setText(message.timeDesc);

        action.setText(message.content);
        StringUtil.setCopy(action);

        if (message.videoCard != null) {
            VideoCard childVideoCard = message.videoCard;
            VideoCardHolder holder = new VideoCardHolder(View.inflate(context, R.layout.cell_dynamic_video, extraCard));
            holder.showVideoCard(childVideoCard, context);
            holder.itemView.findViewById(R.id.videoCardView).setOnClickListener(view ->
                    TerminalContext.getInstance().enterVideoDetailPage(context, 0, childVideoCard.bvid)
            );
        }
        if (message.replyInfo != null || message.dynamicInfo != null) {
            Reply childReply = message.replyInfo != null ? message.replyInfo : message.dynamicInfo;
            ReplyCardHolder holder = new ReplyCardHolder(View.inflate(context, R.layout.cell_message_reply, extraCard));
            holder.showReplyCard(childReply);
            holder.itemView.findViewById(R.id.cardView).setOnClickListener(view -> {
                try {
                    // 所有消息类型统一用 contentUri 导航（完全参照 PiliPlus URI 路由）
                    long seekReply = -1;
                    if ("reply".equals(message.itemType) || message.getType == MessageCard.GET_TYPE_REPLY) {
                        seekReply = message.rootId == 0 ? message.sourceId : message.rootId;
                    }
                    navigateByUri(context, message, seekReply);
                } catch (Exception e) {
                    MsgUtil.err("跳转出错？", e);
                }
            });
        }
    }

    /**
     * 用 B站消息的 contentUri 判断内容类型并跳转（完全参照 PiliPlus 的 URI 路由设计）
     * PiliPlus 通过 item.uri 统一识别内容类型：bilibili://video/BVxxx → 视频,
     * bilibili://opus/xxx → 动态, https://t.bilibili.com/xxx → 动态,
     * https://www.bilibili.com/read/cvxxx → 专栏
     */
    private static void navigateByUri(Context context, MessageCard msg, long seekReply) {
        String uri = msg.contentUri;
        if (TextUtils.isEmpty(uri)) uri = "";
        try {
            // 1) 视频 BV 号：bilibili://video/BVxxx 或 /video/BVxxx
            String bvid = null;
            if (uri.contains("/video/BV")) {
                bvid = uri.substring(uri.indexOf("/video/BV") + 7);
                int q = bvid.indexOf('?');
                if (q > 0) bvid = bvid.substring(0, q);
            } else if (uri.contains("bilibili://video/BV")) {
                bvid = uri.substring(uri.indexOf("bilibili://video/BV") + 18);
                int q = bvid.indexOf('?');
                if (q > 0) bvid = bvid.substring(0, q);
            }
            if (!TextUtils.isEmpty(bvid)) {
                TerminalContext.getInstance().enterVideoDetailPage(context, 0, bvid, null, seekReply);
                return;
            }

            // 2) 动态：https://t.bilibili.com/xxx?xxx 或 bilibili://opus/xxx
            if (uri.contains("t.bilibili.com/")) {
                int s = uri.indexOf("t.bilibili.com/") + 15;
                int e = uri.indexOf('?', s);
                if (e < 0) e = uri.length();
                long id = Long.parseLong(uri.substring(s, e));
                TerminalContext.getInstance().enterDynamicDetailPage(context, id, 0, seekReply);
                return;
            }
            if (uri.contains("bilibili://opus/")) {
                int s = uri.indexOf("bilibili://opus/") + 15;
                int e = uri.indexOf('?', s);
                if (e < 0) e = uri.length();
                long id = Long.parseLong(uri.substring(s, e));
                TerminalContext.getInstance().enterDynamicDetailPage(context, id, 0, seekReply);
                return;
            }

            // 3) 专栏：https://www.bilibili.com/read/cvxxx
            if (uri.contains("/read/cv")) {
                int s = uri.indexOf("/read/cv") + 7;
                int e = uri.indexOf('?', s);
                if (e < 0) e = uri.length();
                long id = Long.parseLong(uri.substring(s, e));
                TerminalContext.getInstance().enterArticleDetailPage(context, id, seekReply);
                return;
            }

            // 4) 通用 bilibili:// URI 回退
            if (uri.contains("bilibili://")) {
                // 尝试提取末尾数字 ID
                int lastSlash = uri.lastIndexOf('/');
                if (lastSlash > 0 && lastSlash < uri.length() - 1) {
                    String last = uri.substring(lastSlash + 1);
                    int q = last.indexOf('?');
                    if (q > 0) last = last.substring(0, q);
                    try {
                        long id = Long.parseLong(last);
                        TerminalContext.getInstance().enterDynamicDetailPage(context, id, 0, seekReply);
                        return;
                    } catch (NumberFormatException ignored2) {}
                }
            }
        } catch (Exception e) {
            Logu.e("navigateByUri parse error: " + e.getMessage());
        }

        // URI 解析失败，按 subjectId / sourceId 回退（参照 PiliPlus 容错逻辑）
        if (msg.subjectId > 100000000) {
            TerminalContext.getInstance().enterDynamicDetailPage(context, msg.subjectId, 0, seekReply);
            return;
        }
        if (msg.sourceId > 100000000) {
            TerminalContext.getInstance().enterDynamicDetailPage(context, msg.sourceId, 0, seekReply);
            return;
        }
        // 最后一层回退：如果是视频相关，用 bvid
        if ("video".equals(msg.itemType) && msg.videoCard != null && !TextUtils.isEmpty(msg.videoCard.bvid)) {
            TerminalContext.getInstance().enterVideoDetailPage(context, 0, msg.videoCard.bvid);
            return;
        }
        MsgUtil.showMsg("原内容已失效，无法跳转");
    }

}
