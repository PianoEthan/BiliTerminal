package com.RobinNotBad.BiliClient.theme;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

/**
 * minSdk 14 兼容的染色叶函数：
 * API&lt;21 无 android:tint / setBackgroundTintList，统一走 Compat 库。
 */
public class ThemeCompat {

    /** 视图中设置的背景 tint（API21+ 反射安全） */
    public static void setBackgroundTintList(android.view.View view, ColorStateList tint) {
        ViewCompat.setBackgroundTintList(view, tint);
    }

    /**
     * mutate 后着色；空 drawable 返回 null。
     * 返回实际被着色的 drawable——API&lt;21 时 DrawableCompat.wrap 会产生新包装实例，
     * 调用方必须把返回值设回视图（setImageDrawable/setCompoundDrawables/…），
     * 否则着色对视图不可见。API21+ 返回原实例。
     */
    public static Drawable tintDrawable(Drawable drawable, int color) {
        if (drawable == null) return null;
        Drawable wrapped = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(wrapped, color);
        return wrapped;
    }

    /** TextView 复合 drawable 着色（start/top/end/bottom） */
    public static void tintCompoundDrawables(TextView textView, int color) {
        Drawable[] drawables = textView.getCompoundDrawables();
        boolean changed = false;
        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                drawables[i] = tintDrawable(drawables[i], color);
                changed = true;
            }
        }
        if (changed) textView.setCompoundDrawablesWithIntrinsicBounds(
                drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    /** 选中/未选中双态 ColorStateList */
    public static ColorStateList checkedStateList(int normal, int checked) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{android.R.attr.state_selected},
                        new int[]{}
                },
                new int[]{checked, checked, normal});
    }

    /** 按下/默认双态 ColorStateList */
    public static ColorStateList pressedStateList(int normal, int pressed) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_activated},
                        new int[]{}
                },
                new int[]{pressed, pressed, normal});
    }

    /** SeekBar 进度条 + 滑块着色（DrawableCompat，API14 安全；包装实例设回视图） */
    public static void tintSeekBar(SeekBar seekBar, int color) {
        Drawable progress = tintDrawable(seekBar.getProgressDrawable(), color);
        if (progress != null) seekBar.setProgressDrawable(progress);
        if (android.os.Build.VERSION.SDK_INT >= 16 && seekBar.getThumb() != null) {
            Drawable thumb = tintDrawable(seekBar.getThumb(), color);
            if (thumb != null) seekBar.setThumb(thumb);
        }
    }

    /** 把 ARGB 转成主题色并保留 alpha（覆盖用） */
    public static int withAlpha(int argb, int alpha) {
        return ThemePalette.withAlpha(argb, alpha);
    }
}
