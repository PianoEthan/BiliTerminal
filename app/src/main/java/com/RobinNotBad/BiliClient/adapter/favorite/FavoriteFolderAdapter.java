package com.RobinNotBad.BiliClient.adapter.favorite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.user.favorite.FavoriteVideoListActivity;
import com.RobinNotBad.BiliClient.activity.user.favorite.FavouriteOpusListActivity;
import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

//收藏夹Adapter

public class FavoriteFolderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final Context context;
    final ArrayList<FavoriteFolder> folderList;
    final long mid;
    private OnLongClickListener onLongClickListener;
    private OnCreateClickListener onCreateClickListener;

    private static final int TYPE_CREATE = 0;
    private static final int TYPE_FOLDER = 1;
    private static final int TYPE_OPUS = 2;

    public interface OnLongClickListener {
        void onLongClick(int position);
    }

    public interface OnCreateClickListener {
        void onCreateClick();
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        this.onLongClickListener = listener;
    }

    public void setOnCreateClickListener(OnCreateClickListener listener) {
        this.onCreateClickListener = listener;
    }

    public FavoriteFolderAdapter(Context context, ArrayList<FavoriteFolder> folderList, long mid) {
        this.context = context;
        this.folderList = folderList;
        this.mid = mid;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_CREATE;
        } else if (position == folderList.size() + 1) {
            return TYPE_OPUS;
        } else {
            return TYPE_FOLDER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CREATE) {
            View view = LayoutInflater.from(this.context).inflate(R.layout.cell_create_folder_button, parent, false);
            return new CreateButtonHolder(view);
        } else {
            View view = LayoutInflater.from(this.context).inflate(R.layout.cell_favorite_folder_list, parent, false);
            return new FavoriteHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (folderList == null)
            return;
        if (holder instanceof CreateButtonHolder) {
            CreateButtonHolder createHolder = (CreateButtonHolder) holder;
            createHolder.itemView.setOnClickListener(v -> {
                if (onCreateClickListener != null) {
                    onCreateClickListener.onCreateClick();
                }
            });
        } else if (holder instanceof FavoriteHolder) {
            FavoriteHolder favoriteHolder = (FavoriteHolder) holder;
            if (position == folderList.size() + 1) {
                favoriteHolder.name.setText("图文收藏夹");
                favoriteHolder.count.setText("");
                Glide.with(BiliTerminal.context).asDrawable()
                        .load(StringUtil.getDrawable(context, R.drawable.article_fav_cover))
                        .transition(GlideUtil.getTransitionOptions())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(ToolsUtil.dp2px(5))))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(favoriteHolder.cover);
                favoriteHolder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, FavouriteOpusListActivity.class);
                    context.startActivity(intent);
                });
                favoriteHolder.itemView.setOnLongClickListener(null);
            } else if (position > 0 && position <= folderList.size()) {
                FavoriteFolder folder = folderList.get(position - 1);
                if (folder == null)
                    return;

                favoriteHolder.name.setText(StringUtil.htmlToString(folder.name));
                favoriteHolder.count.setText(folder.videoCount + "个视频");
                Glide.with(BiliTerminal.context).asDrawable().load(GlideUtil.url(folder.cover))
                        .transition(GlideUtil.getTransitionOptions())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(ToolsUtil.dp2px(5))))
                        .format(DecodeFormat.PREFER_RGB_565)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(favoriteHolder.cover);
                favoriteHolder.itemView.setOnClickListener(view -> {
                    Intent intent = new Intent();
                    intent.setClass(context, FavoriteVideoListActivity.class);
                    intent.putExtra("mediaId", folder.mediaId);
                    intent.putExtra("name", folder.name);
                    context.startActivity(intent);
                });
                favoriteHolder.itemView.setOnLongClickListener(view -> {
                    if (onLongClickListener != null && !folder.isDefault) {
                        onLongClickListener.onLongClick(position - 1);
                    } else if (folder.isDefault) {
                        com.RobinNotBad.BiliClient.util.MsgUtil.showMsg("默认收藏夹不能编辑");
                    }
                    return true;
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return folderList != null ? folderList.size() + 2 : 2;
    }

    public static class FavoriteHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView count;
        final ImageView cover;

        public FavoriteHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_title);
            count = itemView.findViewById(R.id.text_itemcount);
            cover = itemView.findViewById(R.id.img_cover);
        }
    }

    public static class CreateButtonHolder extends RecyclerView.ViewHolder {
        final TextView text;

        public CreateButtonHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
        }
    }
}
