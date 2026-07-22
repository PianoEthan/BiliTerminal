package com.RobinNotBad.BiliClient.adapter.video;

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

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.search.SearchActivity;
import com.RobinNotBad.BiliClient.model.HotSearchCard;
import com.RobinNotBad.BiliClient.util.GlideUtil;

import java.util.List;

public class HotSearchAdapter extends RecyclerView.Adapter<HotSearchAdapter.Holder> {

    private final Context context;
    private final List<HotSearchCard> list;

    public HotSearchAdapter(Context context, List<HotSearchCard> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_hot_search, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HotSearchCard item = list.get(position);
        int rank = position + 1;

        holder.rankText.setText(String.valueOf(rank));
        //前三名 B站粉色高亮
        int rankColor = rank <= 3 ? 0xFFFB7299 : 0xFF999999;
        holder.rankText.setTextColor(rankColor);

        holder.keywordText.setText(item.showName);

        if (item.heatScore > 0) {
            holder.heatText.setText(formatHeat(item.heatScore));
        } else {
            holder.heatText.setText("");
        }

        if (item.icon != null && !item.icon.isEmpty()) {
            holder.iconView.setVisibility(View.VISIBLE);
            GlideUtil.request(holder.iconView, item.icon, 2, 0);
        } else {
            holder.iconView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SearchActivity.class);
            intent.putExtra("keyword", item.keyword);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /** 热度值格式化：超过1万显示"x.x万" */
    private String formatHeat(long heat) {
        if (heat >= 10000) {
            return String.format("%.1f万", heat / 10000.0);
        }
        return String.valueOf(heat);
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView rankText, keywordText, heatText;
        ImageView iconView;

        Holder(View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            keywordText = itemView.findViewById(R.id.keywordText);
            heatText = itemView.findViewById(R.id.heatText);
            iconView = itemView.findViewById(R.id.iconView);
        }
    }
}
