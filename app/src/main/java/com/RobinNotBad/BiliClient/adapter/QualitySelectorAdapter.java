package com.RobinNotBad.BiliClient.adapter;

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
import com.google.android.material.button.MaterialButton;

public class QualitySelectorAdapter extends RecyclerView.Adapter<QualitySelectorAdapter.QualityHolder> {
    private String[] qualityNames;
    private int[] qualityValues;
    public OnItemClickListener listener;
    public int selectedItemIndex = 0;

    @SuppressLint("NotifyDataSetChanged")
    public void setData(String[] qualityNames, int[] qualityValues, int currentQuality) {
        this.qualityNames = qualityNames;
        this.qualityValues = qualityValues;
        this.selectedItemIndex = -1;

        for (int i = 0; i < qualityValues.length; i++) {
            if (qualityValues[i] == currentQuality) {
                this.selectedItemIndex = i;
                break;
            }
        }

        if (this.selectedItemIndex == -1 && qualityValues.length > 0) {
            this.selectedItemIndex = 0;
        }

        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedItemIndex(int selectedItemIndex) {
        int previousSelectedIndex = this.selectedItemIndex;
        this.selectedItemIndex = selectedItemIndex;
        notifyItemChanged(previousSelectedIndex);
        notifyItemChanged(selectedItemIndex);
    }

    @NonNull
    @Override
    public QualityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(parent.getContext(), R.style.Theme_BiliClient);
        View view = LayoutInflater.from(contextWrapper)
                .inflate(R.layout.cell_episode, parent, false);
        return new QualityHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QualityHolder holder, int position) {
        if (position < 0 || qualityNames == null || position >= qualityNames.length)
            return;
        if (listener != null) {
            holder.listener = listener;
        }
        holder.bind(position, selectedItemIndex == position);
    }

    @Override
    public int getItemCount() {
        return qualityNames != null ? qualityNames.length : 0;
    }

    public class QualityHolder extends RecyclerView.ViewHolder {
        private OnItemClickListener listener;
        private final MaterialButton button;

        public QualityHolder(View view) {
            super(view);
            button = itemView.findViewById(R.id.btn);
        }

        void bind(int currentIndex, boolean isSelected) {
            if (currentIndex < 0 || qualityNames == null || currentIndex >= qualityNames.length)
                return;
            button.setText(qualityNames[currentIndex]);
            // 播放器恒为深色场景（整棵树走 applyContentForcedDark），取深色套才不会在浅色主题下把芯片染成白底
            com.RobinNotBad.BiliClient.theme.ThemePalette tp = com.RobinNotBad.BiliClient.theme.ThemeManager.paletteDark();
            if (isSelected) {
                button.setTextColor(tp.buttonTint);
                ViewCompat.setBackgroundTintList(button, com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tp.selected));
            } else {
                button.setTextColor(tp.selectedText);
                ViewCompat.setBackgroundTintList(button, com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tp.buttonTint));
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
