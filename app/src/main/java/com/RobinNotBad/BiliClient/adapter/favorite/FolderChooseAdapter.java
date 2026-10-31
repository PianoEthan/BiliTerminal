package com.RobinNotBad.BiliClient.adapter.favorite;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.api.FavoriteApi;
import com.RobinNotBad.BiliClient.model.FavoriteSelectionState;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

//选择收藏夹
//2023-08-28

public class FolderChooseAdapter extends RecyclerView.Adapter<FolderChooseAdapter.FolderHolder> {

    private final Context context;
    private final ArrayList<String> folderList;
    private final ArrayList<Long> mediaIds;
    private final FavoriteSelectionState selection;
    private final long aid;
    private int busyPosition = RecyclerView.NO_POSITION;

    public FolderChooseAdapter(Context context, ArrayList<String> folderList, ArrayList<Long> mediaIds,
                               ArrayList<Boolean> chooseState, long aid) {
        this.context = context;
        this.folderList = new ArrayList<>(folderList);
        this.mediaIds = new ArrayList<>(mediaIds);
        this.selection = new FavoriteSelectionState(chooseState);
        this.aid = aid;
    }

    @NonNull
    @Override
    public FolderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(this.context).inflate(R.layout.cell_choose, parent, false);
        return new FolderHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderHolder holder, int position) {
        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        holder.folder_name.setText(folderList.get(position));
        setCardView(cardView, selection.isSelected(position));
        if (position == busyPosition) {
            cardView.setStrokeColor(com.RobinNotBad.BiliClient.theme.ThemeManager.palette().accentLow);
            cardView.setStrokeWidth(ToolsUtil.dp2px(1f));
        }
        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getAdapterPosition();
            if (isBusy() || currentPosition == RecyclerView.NO_POSITION
                    || currentPosition >= selection.size() || currentPosition >= mediaIds.size()) return;
            final boolean selected = !selection.isSelected(currentPosition);
            final long mediaId = mediaIds.get(currentPosition);
            busyPosition = currentPosition;
            notifyItemChanged(currentPosition);
            try {
                CenterThreadPool.run(() -> {
                    Exception error = null;
                    try {
                        int result = selected ? FavoriteApi.addFavorite(aid, mediaId)
                                : FavoriteApi.deleteFavorite(aid, mediaId);
                        if (result != 0) {
                            throw new com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException(
                                    (selected ? "添加" : "删除") + "失败！错误码：" + result);
                        }
                    } catch (Exception e) {
                        error = e;
                    }
                    final Exception requestError = error;
                    ((Activity) context).runOnUiThread(() -> completeRequest(currentPosition, selected, requestError));
                });
            } catch (Exception e) {
                completeRequest(currentPosition, selected, e);
            }
        });
    }

    private void completeRequest(int position, boolean selected, Exception error) {
        selection.applyResult(position, selected, error == null);
        busyPosition = RecyclerView.NO_POSITION;
        notifyItemChanged(position);
        if (error instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
            MsgUtil.showMsgLong(error.getMessage());
        } else if (error != null) {
            MsgUtil.err(error);
        } else if (selected && SharedPreferencesUtil.getBoolean("fav_single", false)) {
            ((Activity) context).finish();
        }
    }

    @Override
    public int getItemCount() {
        return folderList != null ? folderList.size() : 0;
    }

    public static class FolderHolder extends RecyclerView.ViewHolder {
        final TextView folder_name;

        public FolderHolder(@NonNull View itemView) {
            super(itemView);
            folder_name = itemView.findViewById(R.id.text);
        }
    }

    private void setCardView(MaterialCardView cardView, boolean bool) {
        if (bool) {
            com.RobinNotBad.BiliClient.theme.ThemePalette p =
                    com.RobinNotBad.BiliClient.theme.ThemeManager.palette();
            cardView.setStrokeColor(p.color(0xFBFB8787, p.selected));
            cardView.setStrokeWidth(ToolsUtil.dp2px(1));
        } else {
            cardView.setStrokeColor(com.RobinNotBad.BiliClient.theme.ThemeManager.palette().gray);
            cardView.setStrokeWidth(ToolsUtil.dp2px(0.1f));
        }
    }

    public boolean hasChanges() {
        return selection.hasChanges();
    }

    public boolean isFavorite() {
        return selection.isFavorite();
    }

    public boolean isBusy() {
        return busyPosition != RecyclerView.NO_POSITION;
    }
}
