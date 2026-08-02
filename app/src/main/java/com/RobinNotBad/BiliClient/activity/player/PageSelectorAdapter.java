package com.RobinNotBad.BiliClient.activity.player;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.listener.OnItemClickListener;

import java.util.ArrayList;

public class PageSelectorAdapter extends RecyclerView.Adapter<PageSelectorAdapter.Holder> {
    private ArrayList<String> pagenames;
    private int selectedItemIndex = 0;
    private OnItemClickListener listener;

    public PageSelectorAdapter() {
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

    @SuppressLint("NotifyDataSetChanged")
    public void setData(ArrayList<String> pagenames, int currentIndex) {
        this.pagenames = pagenames;
        this.selectedItemIndex = currentIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_page_item, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (position < 0 || pagenames == null || position >= pagenames.size())
            return;
        holder.bind(position, selectedItemIndex == position);
    }

    @Override
    public int getItemCount() {
        return pagenames != null ? pagenames.size() : 0;
    }

    public class Holder extends RecyclerView.ViewHolder {
        private final TextView pageName;

        public Holder(View view) {
            super(view);
            pageName = itemView.findViewById(R.id.page_name);
        }

        void bind(int currentIndex, boolean isSelected) {
            if (currentIndex < 0 || pagenames == null || currentIndex >= pagenames.size())
                return;

            pageName.setText("P" + (currentIndex + 1) + " " + pagenames.get(currentIndex));

            if (isSelected) {
                pageName.setTextColor(com.RobinNotBad.BiliClient.theme.ThemeManager.paletteDark().accent);
                itemView.setBackgroundResource(R.drawable.background_card);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintBackground(itemView);
            } else {
                pageName.setTextColor(com.RobinNotBad.BiliClient.theme.ThemeManager.paletteDark().textPrimary);
                itemView.setBackgroundResource(R.drawable.background_card_borderless);
                com.RobinNotBad.BiliClient.theme.ThemeApplier.retintBackground(itemView);
            }

            itemView.setOnClickListener(v -> {
                setSelectedItemIndex(currentIndex);
                if (listener != null) {
                    listener.onItemClick(currentIndex);
                }
            });
        }
    }
}
