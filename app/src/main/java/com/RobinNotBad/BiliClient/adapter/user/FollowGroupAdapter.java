package com.RobinNotBad.BiliClient.adapter.user;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.user.info.UserInfoActivity;
import com.RobinNotBad.BiliClient.model.FollowTag;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP = 0;
    private static final int TYPE_USER = 1;

    final Context context;
    final List<GroupItem> groupList;
    final Map<Integer, Boolean> expandedMap;
    private OnGroupExpandListener expandListener;

    public interface OnGroupExpandListener {
        void onGroupExpand(int tagid);
    }

    public FollowGroupAdapter(Context context) {
        this.context = context;
        this.groupList = new ArrayList<>();
        this.expandedMap = new HashMap<>();
    }

    public void setOnGroupExpandListener(OnGroupExpandListener listener) {
        this.expandListener = listener;
    }

    public void addGroup(FollowTag tag, List<UserInfo> users) {
        groupList.add(new GroupItem(tag, users));
        expandedMap.put(tag.tagid, false);
    }

    public void updateGroupUsers(int tagid, List<UserInfo> users) {
        for (GroupItem group : groupList) {
            if (group.tag.tagid == tagid) {
                group.users.clear();
                group.users.addAll(users);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void addGroupUsers(int tagid, List<UserInfo> users) {
        for (GroupItem group : groupList) {
            if (group.tag.tagid == tagid) {
                group.users.addAll(users);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void toggleGroup(int tagid) {
        Boolean expanded = expandedMap.get(tagid);
        if (expanded != null) {
            boolean newExpanded = !expanded;
            expandedMap.put(tagid, newExpanded);
            
            if (newExpanded) {
                for (GroupItem group : groupList) {
                    if (group.tag.tagid == tagid && group.users.isEmpty() && expandListener != null) {
                        expandListener.onGroupExpand(tagid);
                        break;
                    }
                }
            }
            
            int groupPosition = -1;
            int currentPos = 0;
            for (GroupItem group : groupList) {
                if (group.tag.tagid == tagid) {
                    groupPosition = currentPos;
                    break;
                }
                currentPos++;
                Boolean isExpanded = expandedMap.get(group.tag.tagid);
                if (isExpanded != null && isExpanded) {
                    currentPos += group.users.size();
                }
            }
            
            if (groupPosition >= 0) {
                notifyItemChanged(groupPosition);
                if (newExpanded) {
                    GroupItem group = null;
                    for (GroupItem g : groupList) {
                        if (g.tag.tagid == tagid) {
                            group = g;
                            break;
                        }
                    }
                    if (group != null && !group.users.isEmpty()) {
                        notifyItemRangeInserted(groupPosition + 1, group.users.size());
                    }
                } else {
                    GroupItem group = null;
                    for (GroupItem g : groupList) {
                        if (g.tag.tagid == tagid) {
                            group = g;
                            break;
                        }
                    }
                    if (group != null && !group.users.isEmpty()) {
                        notifyItemRangeRemoved(groupPosition + 1, group.users.size());
                    }
                }
            } else {
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        int currentPos = 0;
        
        for (GroupItem group : groupList) {
            if (currentPos == position) {
                return TYPE_GROUP;
            }
            currentPos++;
            
            Boolean expanded = expandedMap.get(group.tag.tagid);
            if (expanded != null && expanded) {
                int userCount = group.users.size();
                if (position >= currentPos && position < currentPos + userCount) {
                    return TYPE_USER;
                }
                currentPos += userCount;
            }
        }
        
        return TYPE_GROUP;
    }

    private GroupItem getGroupForPosition(int position) {
        int currentPos = 0;
        for (GroupItem group : groupList) {
            if (currentPos == position) {
                return group;
            }
            currentPos++;
            
            Boolean expanded = expandedMap.get(group.tag.tagid);
            if (expanded != null && expanded) {
                int userCount = group.users.size();
                if (position >= currentPos && position < currentPos + userCount) {
                    return group;
                }
                currentPos += userCount;
            }
        }
        if (!groupList.isEmpty()) {
            return groupList.get(groupList.size() - 1);
        }
        return null;
    }

    private UserInfo getUserForPosition(int position) {
        int currentPos = 0;
        for (GroupItem group : groupList) {
            currentPos++;
            
            Boolean expanded = expandedMap.get(group.tag.tagid);
            if (expanded != null && expanded) {
                int userCount = group.users.size();
                if (position >= currentPos && position < currentPos + userCount) {
                    int userIndex = position - currentPos;
                    return group.users.get(userIndex);
                }
                currentPos += userCount;
            }
        }
        return null;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GROUP) {
            View view = LayoutInflater.from(context).inflate(R.layout.cell_follow_group, parent, false);
            return new GroupHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.cell_user_list, parent, false);
            return new UserHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= getItemCount())
            return;

        if (holder instanceof GroupHolder) {
            GroupItem group = getGroupForPosition(position);
            if (group != null) {
                GroupHolder groupHolder = (GroupHolder) holder;
                groupHolder.groupName.setText(group.tag.name);
                groupHolder.groupCount.setText(group.tag.count + " 位成员");
                
                Boolean expanded = expandedMap.get(group.tag.tagid);
                float targetRotation = (expanded != null && expanded) ? 90f : 0f;
                animateRotation(groupHolder.expandIcon, targetRotation);
                
                groupHolder.itemView.setOnClickListener(view -> toggleGroup(group.tag.tagid));
            }
        } else if (holder instanceof UserHolder) {
            UserInfo user = getUserForPosition(position);
            if (user != null) {
                UserHolder userHolder = (UserHolder) holder;

                userHolder.name.setText(user.name);
                if (user.vip_nickname_color != null && !user.vip_nickname_color.isEmpty()) {
                    try {
                        userHolder.name.setTextColor(Color.parseColor(user.vip_nickname_color));
                    } catch (IllegalArgumentException e) {
                        userHolder.name.setTextColor(com.RobinNotBad.BiliClient.theme.ThemeManager.palette().textPrimary);
                    }
                }
                userHolder.desc.setText(user.sign);

                if (user.avatar == null || user.avatar.isEmpty()) {
                    userHolder.avatar.setVisibility(View.GONE);
                    userHolder.desc.setSingleLine(false);
                } else {
                    Glide.with(BiliTerminal.context).asDrawable().load(GlideUtil.url(user.avatar))
                            .transition(GlideUtil.getTransitionOptions())
                            .placeholder(R.mipmap.akari)
                            .apply(RequestOptions.circleCropTransform())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(userHolder.avatar);
                    userHolder.avatar.setVisibility(View.VISIBLE);
                    userHolder.desc.setSingleLine(true);
                }

                if (user.mid != -1) {
                    userHolder.itemView.setOnClickListener(view -> {
                        Intent intent = new Intent()
                                .setClass(context, UserInfoActivity.class)
                                .putExtra("mid", user.mid);
                        context.startActivity(intent);
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = groupList.size();
        for (GroupItem group : groupList) {
            Boolean expanded = expandedMap.get(group.tag.tagid);
            if (expanded != null && expanded) {
                count += group.users.size();
            }
        }
        return count;
    }

    public static class GroupHolder extends RecyclerView.ViewHolder {
        final TextView groupName;
        final TextView groupCount;
        final ImageView expandIcon;

        public GroupHolder(@NonNull View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.groupName);
            groupCount = itemView.findViewById(R.id.groupCount);
            expandIcon = itemView.findViewById(R.id.expandIcon);
        }
    }

    public static class UserHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView desc;
        final ImageView avatar;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.userName);
            desc = itemView.findViewById(R.id.userDesc);
            avatar = itemView.findViewById(R.id.userAvatar);
        }
    }

    private void animateRotation(ImageView imageView, float targetRotation) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(imageView, "rotation", imageView.getRotation(), targetRotation);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private static class GroupItem {
        final FollowTag tag;
        final List<UserInfo> users;

        GroupItem(FollowTag tag, List<UserInfo> users) {
            this.tag = tag;
            this.users = users;
        }
    }
}

