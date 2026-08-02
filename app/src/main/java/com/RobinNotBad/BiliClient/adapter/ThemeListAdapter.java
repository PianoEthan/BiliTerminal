package com.RobinNotBad.BiliClient.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 主题列表适配器：行 = 内置默认 + 已安装 .btheme */
public class ThemeListAdapter extends RecyclerView.Adapter<ThemeListAdapter.ThemeHolder> {

    public static class ThemeRow {
        public final String id;      // 空 = 内置默认
        public final String name;
        public final String desc;
        public final File preview;
        public boolean selected;

        public ThemeRow(String id, String name, String desc, File preview, boolean selected) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.preview = preview;
            this.selected = selected;
        }
    }

    public interface OnRowListener {
        void onSelect(ThemeRow row);

        void onLongPress(ThemeRow row);
    }

    private final Context context;
    private final List<ThemeRow> rows = new ArrayList<>();
    private final OnRowListener listener;

    public ThemeListAdapter(Context context, OnRowListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRows(List<ThemeRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    public void markSelected(String id) {
        for (ThemeRow row : rows) {
            row.selected = row.id == null ? id == null || id.isEmpty() : row.id.equals(id);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThemeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cell_theme_item, parent, false);
        return new ThemeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeHolder holder, int position) {
        ThemeRow row = rows.get(position);
        holder.name.setText(row.name);
        holder.desc.setText(row.desc);
        holder.radio.setChecked(row.selected);
        if (row.preview != null && row.preview.isFile()) {
            holder.preview.setVisibility(View.VISIBLE);
            Glide.with(context).load(row.preview).override(72, 72).into(holder.preview);
        } else {
            holder.preview.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(row);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(row);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ThemeHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView desc;
        final RadioButton radio;
        final ImageView preview;

        ThemeHolder(View view) {
            super(view);
            name = view.findViewById(R.id.themeName);
            desc = view.findViewById(R.id.themeDesc);
            radio = view.findViewById(R.id.themeSelected);
            preview = view.findViewById(R.id.themePreview);
        }
    }
}
