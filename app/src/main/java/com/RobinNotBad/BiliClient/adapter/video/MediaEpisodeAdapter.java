package com.RobinNotBad.BiliClient.adapter.video;

import android.annotation.SuppressLint;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.listener.OnItemClickListener;
import com.RobinNotBad.BiliClient.model.Bangumi;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MediaEpisodeAdapter extends RecyclerView.Adapter<MediaEpisodeAdapter.EpisodeHolder> {
    private List<Bangumi.Episode> episodeList;

    public OnItemClickListener listener;
    public int selectedItemIndex = 0;
    private boolean useVerticalLayout;

    public MediaEpisodeAdapter() {
    }

    public MediaEpisodeAdapter(boolean useVerticalLayout) {
        this.useVerticalLayout = useVerticalLayout;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedItemIndex(int selectedItemIndex) {
        // Cancel previous selected item and set current item
        int previousSelectedIndex = this.selectedItemIndex;
        this.selectedItemIndex = selectedItemIndex;
        notifyItemChanged(previousSelectedIndex);
        notifyItemChanged(selectedItemIndex);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<Bangumi.Episode> episodeList) {
        this.episodeList = episodeList;
        selectedItemIndex = 0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EpisodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(parent.getContext(), R.style.Theme_BiliClient);
        View view = LayoutInflater.from(contextWrapper)
                .inflate(useVerticalLayout ? R.layout.cell_item_vertical : R.layout.cell_episode, parent, false);
        return new EpisodeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeHolder holder, int position) {
        if (position < 0 || episodeList == null || position >= episodeList.size())
            return;
        if (listener != null) {
            holder.listener = listener;
        }
        holder.bind(position, selectedItemIndex == position);
    }

    @Override
    public int getItemCount() {
        return episodeList != null ? episodeList.size() : 0;
    }

    public class EpisodeHolder extends RecyclerView.ViewHolder {

        private OnItemClickListener listener;
        private final MaterialButton button;

        public EpisodeHolder(View view) {
            super(view);
            button = itemView.findViewById(R.id.btn);
        }

        void bind(int currentIndex, boolean isSelected) {
            if (currentIndex < 0 || episodeList == null || currentIndex >= episodeList.size())
                return;
            button.setText(episodeList.get(currentIndex).title);
            com.RobinNotBad.BiliClient.theme.ThemePalette tp = com.RobinNotBad.BiliClient.theme.ThemeManager.palette();
            if (isSelected) {
                button.setTextColor(tp.buttonTint);
                ViewCompat.setBackgroundTintList(button, com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tp.selectedText, tp.selectedText));
            } else {
                button.setTextColor(tp.selectedText);
                ViewCompat.setBackgroundTintList(button, com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tp.buttonTint, tp.buttonTint));
            }
            button.setOnClickListener(v -> {
                setSelectedItemIndex(currentIndex);
                if (listener != null) {
                    listener.onItemClick(currentIndex);
                }
            });
        }
    }
}
