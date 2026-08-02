package com.RobinNotBad.BiliClient.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.listener.OnItemClickListener;
import com.RobinNotBad.BiliClient.model.ViewPoint;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ViewPointAdapter extends RecyclerView.Adapter<ViewPointAdapter.ViewPointHolder> {
    private List<ViewPoint> viewPoints;
    public OnItemClickListener listener;
    private int currentPosition = -1;
    private int lastCurrentIndex = -1;

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<ViewPoint> viewPoints) {
        this.viewPoints = viewPoints;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateCurrentPosition(int positionInSeconds) {
        if (this.currentPosition == positionInSeconds) {
            return;
        }
        
        this.currentPosition = positionInSeconds;
        
        int newCurrentIndex = getCurrentSegmentIndex();
        if (newCurrentIndex != lastCurrentIndex) {
            int oldIndex = lastCurrentIndex;
            lastCurrentIndex = newCurrentIndex;
            
            if (oldIndex != -1 && oldIndex < getItemCount()) {
                notifyItemChanged(oldIndex);
            }
            if (newCurrentIndex != -1 && newCurrentIndex < getItemCount()) {
                notifyItemChanged(newCurrentIndex);
            }
        }
    }
    
    private int getCurrentSegmentIndex() {
        if (viewPoints == null || currentPosition < 0) {
            return -1;
        }
        for (int i = 0; i < viewPoints.size(); i++) {
            ViewPoint vp = viewPoints.get(i);
            if (currentPosition >= vp.from && currentPosition < vp.to) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public ViewPointHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cell_episode, parent, false);
        return new ViewPointHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPointHolder holder, int position) {
        if (position < 0 || viewPoints == null || position >= viewPoints.size())
            return;
        if (listener != null) {
            holder.listener = listener;
        }
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return viewPoints != null ? viewPoints.size() : 0;
    }

    public class ViewPointHolder extends RecyclerView.ViewHolder {
        private OnItemClickListener listener;
        private final MaterialButton button;

        public ViewPointHolder(View view) {
            super(view);
            button = itemView.findViewById(R.id.btn);
        }

        void bind(int currentIndex) {
            if (currentIndex < 0 || viewPoints == null || currentIndex >= viewPoints.size())
                return;
            ViewPoint viewPoint = viewPoints.get(currentIndex);
            String timeStr = StringUtil.toTime(viewPoint.from) + "-" + StringUtil.toTime(viewPoint.to);
            String displayText = viewPoint.content + "\n" + timeStr;
            button.setText(displayText);
            
            boolean isCurrent = currentPosition >= viewPoint.from && currentPosition < viewPoint.to;
            
            com.RobinNotBad.BiliClient.theme.ThemePalette tp = com.RobinNotBad.BiliClient.theme.ThemeManager.palette();
            if (isCurrent) {
                button.setTextColor(tp.buttonTint);
                ViewCompat.setBackgroundTintList(button, com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tp.selectedText, tp.selectedText));
            } else {
                button.setTextColor(tp.selectedText);
                ViewCompat.setBackgroundTintList(button, com.RobinNotBad.BiliClient.theme.ThemeCompat.pressedStateList(tp.buttonTint, tp.buttonTint));
            }
            
            button.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(currentIndex);
                }
            });
        }
    }
}

