package com.RobinNotBad.BiliClient.adapter.dynamic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.ImageViewerActivity;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.activity.message.PrivateMsgActivity;
import com.RobinNotBad.BiliClient.activity.user.FollowUsersActivity;
import com.RobinNotBad.BiliClient.activity.user.MedalWallActivity;
import com.RobinNotBad.BiliClient.adapter.user.ElectricUserAdapter;
import com.RobinNotBad.BiliClient.api.ElectricApi;
import com.RobinNotBad.BiliClient.api.UserInfoApi;
import com.RobinNotBad.BiliClient.model.Dynamic;
import com.RobinNotBad.BiliClient.model.ElectricPanel;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.ui.widget.RadiusBackgroundSpan;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.GlideUtil;

import com.RobinNotBad.BiliClient.util.CompatUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.RobinNotBad.BiliClient.util.TerminalContext;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//用户信息页专用Adapter 独立出来也是为了做首项不同

public class UserDynamicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final Context context;
    final ArrayList<Dynamic> dynamicList;
    final UserInfo userInfo;

    public UserDynamicAdapter(Context context, ArrayList<Dynamic> dynamicList, UserInfo userInfo) {
        this.context = context;
        this.dynamicList = dynamicList;
        this.userInfo = userInfo;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(context).inflate(R.layout.cell_user_info, parent, false);
            return new UserInfoHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.cell_dynamic, parent, false);
            return new DynamicHolder(view, (BaseActivity) context, false);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DynamicHolder) {
            int realPosition = position - 1;
            if (realPosition < 0 || realPosition >= dynamicList.size())
                return;

            Dynamic dynamic = dynamicList.get(realPosition);
            if (dynamic == null)
                return;

            DynamicHolder dynamicHolder = (DynamicHolder) holder;
            dynamicHolder.showDynamic(context, dynamic, true);

            if (dynamic.dynamic_forward != null) {
                View childCard = dynamicHolder.cell_dynamic_child;
                if (dynamicHolder.childDynamicHolder == null) {
                    dynamicHolder.childDynamicHolder = new DynamicHolder(childCard, (BaseActivity) context, true);
                }
                dynamicHolder.childDynamicHolder.showDynamic(context, dynamic.dynamic_forward, true);
                dynamicHolder.cell_dynamic_child.setVisibility(View.VISIBLE);
            } else {
                dynamicHolder.cell_dynamic_child.setVisibility(View.GONE);
            }

            View.OnLongClickListener onDeleteLongClick = DynamicHolder.getDeleteListener((Activity) context,
                    dynamicList, realPosition, this);
            dynamicHolder.item_dynamic_delete.setOnLongClickListener(onDeleteLongClick);
            if (dynamic.canDelete)
                dynamicHolder.item_dynamic_delete.setVisibility(View.VISIBLE);
        }
        if (holder instanceof UserInfoHolder) {
            ((UserInfoHolder) holder).bind(context, userInfo);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return dynamicList != null ? dynamicList.size() + 1 : 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 ? 0 : 1);
    }

    public static class UserInfoHolder extends RecyclerView.ViewHolder {
        final TextView userName, userFollowings, userLevel, userFans, userMedal, userDesc, userNotice, userOfficial,
                exclusiveTipLabel, liveRoomLabel, electricPanelHeader;
        final MaterialCardView exclusiveTip, liveRoom, electricPanel;
        final ImageView userAvatar, officialIcon;
        final TextView uidTv;
        final MaterialButton followBtn, msgBtn, contractBtn;
        final RecyclerView electricUserList;
        final View electricPanelDivider, divider;

        boolean notice_expand, desc_expand, electric_expand;

        public UserInfoHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userDesc = itemView.findViewById(R.id.userDesc);
            userNotice = itemView.findViewById(R.id.userNotice);
            userLevel = itemView.findViewById(R.id.userLevel);
            userFans = itemView.findViewById(R.id.userFollowers);
            userMedal = itemView.findViewById(R.id.userMedal);
            userFollowings = itemView.findViewById(R.id.userFollowings);
            userOfficial = itemView.findViewById(R.id.userOfficial);
            exclusiveTip = itemView.findViewById(R.id.exclusiveTip);
            exclusiveTipLabel = itemView.findViewById(R.id.exclusiveTipLabel);
            liveRoom = itemView.findViewById(R.id.liveRoom);
            liveRoomLabel = itemView.findViewById(R.id.liveRoomLabel);
            officialIcon = itemView.findViewById(R.id.officialIcon);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            followBtn = itemView.findViewById(R.id.followBtn);
            msgBtn = itemView.findViewById(R.id.msgBtn);
            contractBtn = itemView.findViewById(R.id.contractBtn);
            uidTv = itemView.findViewById(R.id.uidText);
            electricPanel = itemView.findViewById(R.id.electricPanel);
            electricPanelHeader = itemView.findViewById(R.id.electricPanelHeader);
            electricUserList = itemView.findViewById(R.id.electricUserList);
            electricPanelDivider = itemView.findViewById(R.id.electricPanelDivider);
            divider = itemView.findViewById(R.id.divider);
            StringUtil.setCopy(userDesc, userNotice);
        }

        public void setFollowed(boolean followed) {
            msgBtn.setVisibility((followed ? View.VISIBLE : View.GONE));
            followBtn.setBackgroundTintList(ColorStateList
                    .valueOf((followed ? Color.argb(0xDD, 0x26, 0x26, 0x26) : Color.argb(0xFE, 0xF0, 0x5D, 0x8E))));
            followBtn.setText((followed ? "已关注" : "关注"));
        }

        @SuppressLint("SetTextI18n")
        public void bind(Context context, UserInfo userInfo) {
            SpannableStringBuilder lvStr = new SpannableStringBuilder("Lv" + userInfo.level);
            lvStr.setSpan(StringUtil.getLevelBadge(context, userInfo), 0, lvStr.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            if (userInfo.vip_role > 0) {
                LinkedHashMap<Integer, String> vipTypeMap = new LinkedHashMap<>() {
                    {
                        put(1, "月度大会员");
                        put(3, "年度大会员");
                        put(7, "十年大会员");
                        put(15, "百年大会员");
                    }
                };
                lvStr.append("  ").append(vipTypeMap.get(userInfo.vip_role)).append(" ");
                lvStr.setSpan(
                        new RadiusBackgroundSpan(1, (int) context.getResources().getDimension(R.dimen.card_round),
                                com.RobinNotBad.BiliClient.theme.ThemeManager.palette().buttonTint, com.RobinNotBad.BiliClient.theme.ThemeManager.palette().accent),
                        ("Lv" + userInfo.level).length() + 1, lvStr.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
            this.userLevel.setText(lvStr);
            if (!userInfo.vip_nickname_color.isEmpty())
                this.userName.setTextColor(Color.parseColor(userInfo.vip_nickname_color));
            this.userName.setText(userInfo.name);
            this.userDesc.setText(userInfo.sign);
            if (!userInfo.notice.isEmpty())
                this.userNotice.setText(userInfo.notice);
            else
                this.userNotice.setVisibility(View.GONE);
            this.uidTv.setText(String.valueOf(userInfo.mid));
            StringUtil.setCopy(this.uidTv);
            StringUtil.setLink(this.userDesc, this.userNotice);
            this.userFans.setText(StringUtil.toWan(userInfo.fans) + "粉丝");
            this.userFans.setOnClickListener(
                    (view) -> view.getContext().startActivity(new Intent(view.getContext(), FollowUsersActivity.class)
                            .putExtra("mode", 1).putExtra("mid", userInfo.mid)));

            this.userMedal.setOnClickListener(
                    (view) -> view.getContext().startActivity(new Intent(view.getContext(), MedalWallActivity.class)
                            .putExtra("mid", userInfo.mid)));

            this.userFollowings.setText(StringUtil.toWan(userInfo.following) + "关注");
            this.userFollowings.setOnClickListener(
                    (view) -> view.getContext().startActivity(new Intent(view.getContext(), FollowUsersActivity.class)
                            .putExtra("mode", 0).putExtra("mid", userInfo.mid)));

            if (userInfo.official != 0) {
                this.officialIcon.setVisibility(View.VISIBLE);
                this.userOfficial.setVisibility(View.VISIBLE);
                String[] official_signs = {"哔哩哔哩不知名UP主", "哔哩哔哩知名UP主", "哔哩哔哩大V达人", "哔哩哔哩企业认证",
                        "哔哩哔哩组织认证", "哔哩哔哩媒体认证", "哔哩哔哩政府认证", "哔哩哔哩高能主播", "社会不知名人士", "社会知名人士"};
                this.userOfficial.setText(official_signs[userInfo.official]
                        + (userInfo.officialDesc.isEmpty() ? "" : ("\n" + userInfo.officialDesc)));
            } else {
                this.officialIcon.setVisibility(View.GONE);
                this.userOfficial.setVisibility(View.GONE);
            }

            Glide.with(BiliTerminal.context).asDrawable().load(GlideUtil.url(userInfo.avatar))
                    .transition(GlideUtil.getTransitionOptions())
                    .placeholder(R.mipmap.akari)
                    .apply(RequestOptions.circleCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(this.userAvatar);

            this.userAvatar.setOnClickListener(view -> {
                Intent intent = new Intent();
                intent.setClass(context, ImageViewerActivity.class);
                ArrayList<String> imageList = new ArrayList<>();
                imageList.add(userInfo.avatar);
                intent.putExtra("imageList", imageList);
                context.startActivity(intent);
            });

            if (!userInfo.sys_notice.isEmpty()) {
                this.exclusiveTip.setVisibility(View.VISIBLE);
                SpannableString spannableString = new SpannableString("!:" + userInfo.sys_notice);
                Drawable drawable = StringUtil.getDrawable(context, R.drawable.icon_warning);
                drawable.setBounds(0, 0, 30, 30);
                spannableString.setSpan(new ImageSpan(drawable), 0, 2, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                this.exclusiveTipLabel.setText(spannableString);
            } else
                this.exclusiveTip.setVisibility(View.GONE);

            if (userInfo.live_room != null) {
                this.liveRoom.setVisibility(View.VISIBLE);
                this.liveRoomLabel.setText(userInfo.live_room.title);
                this.liveRoom.setOnClickListener(
                        view -> TerminalContext.getInstance().enterLiveDetailPage(context, userInfo.live_room.roomid));
            } else
                this.liveRoom.setVisibility(View.GONE);

            if ((userInfo.mid == SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0))
                    || (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) || (userInfo.mid == 0))
                this.followBtn.setVisibility(View.GONE);
            else
                this.followBtn.setChecked(userInfo.followed);
            this.followBtn.setOnClickListener(btn -> {
                followBtn.setEnabled(false);
                this.setFollowed(!(userInfo.followed));
                CenterThreadPool.run(() -> {
                    try {
                        int result = UserInfoApi.followUser(userInfo.mid, !(userInfo.followed));
                        String msg;
                        if (result == 0) {
                            userInfo.followed = !(userInfo.followed);
                            msg = "操作成功喵~";
                        } else {
                            CenterThreadPool.runOnUiThread(() -> this.setFollowed(userInfo.followed));
                            if (result == 22015)
                                msg = "被B站风控系统拦截了\n（无法解决，详见公告）";
                            else
                                msg = "操作失败（原因未知）：" + result;
                        }
                        MsgUtil.showMsg(msg);
                    } catch (Exception e) {
                        MsgUtil.err(e);
                    }
                    CenterThreadPool.runOnUiThread(() -> followBtn.setEnabled(true));
                });
            });

            this.setFollowed(userInfo.followed);

            this.msgBtn.setOnClickListener(view -> {
                Intent intent = new Intent(context, PrivateMsgActivity.class);
                intent.putExtra("uid", userInfo.mid);
                context.startActivity(intent);
            });

            long currentMid = SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0);
            if (userInfo.mid != currentMid && currentMid != 0 && userInfo.mid != 0 && userInfo.is_follow_display) {
                this.contractBtn.setVisibility(View.VISIBLE);
                ConstraintLayout layout = (ConstraintLayout) itemView;
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(layout);
                constraintSet.connect(R.id.divider, ConstraintSet.TOP, R.id.contractBtn, ConstraintSet.BOTTOM);
                constraintSet.applyTo(layout);
                this.contractBtn.setOnClickListener(view -> {
                    contractBtn.setEnabled(false);
                    CenterThreadPool.run(() -> {
                        try {
                            int result = UserInfoApi.addContract(userInfo.mid);
                            String msg;
                            if (result == 0) {
                                msg = "加入成功";
                            } else if (result == 158001) {
                                msg = "不满足条件";
                            } else {
                                msg = "操作失败：" + result;
                            }
                            MsgUtil.showMsg(msg);
                        } catch (Exception e) {
                            MsgUtil.err(e);
                        }
                        CenterThreadPool.runOnUiThread(() -> contractBtn.setEnabled(true));
                    });
                });
            } else {
                this.contractBtn.setVisibility(View.GONE);
                ConstraintLayout layout = (ConstraintLayout) itemView;
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(layout);
                constraintSet.connect(R.id.divider, ConstraintSet.TOP, R.id.followBtn, ConstraintSet.BOTTOM);
                constraintSet.applyTo(layout);
            }

            this.userDesc.setOnClickListener(view1 -> {
                if (desc_expand)
                    this.userDesc.setMaxLines(2);
                else
                    this.userDesc.setMaxLines(32);
                desc_expand = !desc_expand;
            });

            this.userNotice.setOnClickListener(view1 -> {
                if (notice_expand)
                    this.userNotice.setMaxLines(2);
                else
                    this.userNotice.setMaxLines(32);
                notice_expand = !notice_expand;
            });

            // 加载充电公示数据
            loadElectricPanel(context, userInfo);

        }

        /**
         * 加载充电公示数据
         */
        private void loadElectricPanel(Context context, UserInfo userInfo) {
            this.electricPanel.setVisibility(View.GONE);

            CenterThreadPool.run(() -> {
                try {
                    ElectricPanel panel = ElectricApi.getElectricPanel(userInfo.mid);

                    if (panel != null && panel.hasData()) {
                        CenterThreadPool.runOnUiThread(() -> {
                            this.electricPanel.setVisibility(View.VISIBLE);

                            // 设置头部文本
                            this.electricPanelHeader.setText("充电公示（本月" + panel.count + "人）");

                            // 设置RecyclerView
                            this.electricUserList.setLayoutManager(new LinearLayoutManager(context));
                            ElectricUserAdapter adapter = new ElectricUserAdapter(context, panel.list);
                            this.electricUserList.setAdapter(adapter);

                            // 设置点击展开/收起
                            this.electricPanelHeader.setOnClickListener(v -> {
                                electric_expand = !electric_expand;
                                if (electric_expand) {
                                    this.electricUserList.setVisibility(View.VISIBLE);
                                    this.electricPanelDivider.setVisibility(View.VISIBLE);
                                    CompatUtil.setCompoundDrawablesRelative(
                                            this.electricPanelHeader, 0, 0, R.drawable.arrow_up, 0);
                                } else {
                                    this.electricUserList.setVisibility(View.GONE);
                                    this.electricPanelDivider.setVisibility(View.GONE);
                                    CompatUtil.setCompoundDrawablesRelative(
                                            this.electricPanelHeader, 0, 0, R.drawable.arrow_down, 0);
                                }
                            });
                        });
                    }
                } catch (Exception e) {
                    // 加载失败，不显示充电公示卡片
                    e.printStackTrace();
                }
            });
        }
    }
}
