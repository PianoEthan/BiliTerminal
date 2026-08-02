package com.RobinNotBad.BiliClient.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class HighEnergyProgressBar extends androidx.appcompat.widget.AppCompatSeekBar {
    private float[] highEnergyData;
    private Paint linePaint;
    private Paint fillPaint;
    private int stepSec = 10;
    private boolean showHighEnergy = true;

    public HighEnergyProgressBar(Context context) {
        super(context);
        init();
    }

    public HighEnergyProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HighEnergyProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(com.RobinNotBad.BiliClient.theme.ThemeManager.palette().highEnergyLine);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f * getContext().getResources().getDisplayMetrics().density); // 2dp线宽
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setColor(com.RobinNotBad.BiliClient.theme.ThemeManager.palette().highEnergyFill);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * 设置高能曲线强调色（内容动态取色用；null 忽略）
     *
     * @param line 曲线线色
     * @param fill 曲线填充色
     */
    public void setAccentColors(int line, int fill) {
        if (line != 0) linePaint.setColor(line);
        if (fill != 0) fillPaint.setColor(fill);
        invalidate();
    }

    /**
     * 设置高能数据
     *
     * @param data    高能数据数组，每个值表示该时间段的弹幕密度
     * @param stepSec 采样间隔（秒）
     */
    public void setHighEnergyData(float[] data, int stepSec) {
        this.highEnergyData = data;
        this.stepSec = stepSec;
        invalidate();
    }

    /**
     * 设置是否显示高能进度条
     */
    public void setShowHighEnergy(boolean show) {
        this.showHighEnergy = show;
        invalidate();
    }

    /**
     * 清除高能数据
     */
    public void clearHighEnergyData() {
        this.highEnergyData = null;
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (showHighEnergy && highEnergyData != null && highEnergyData.length > 0) {
            drawHighEnergy(canvas);
        }

        super.onDraw(canvas);
    }

    private void drawHighEnergy(Canvas canvas) {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int max = getMax();

        if (max <= 0 || width <= 0 || height <= 0) {
            return;
        }

        float maxValue = 0;
        for (float value : highEnergyData) {
            if (value > maxValue) {
                maxValue = value;
            }
        }

        if (maxValue <= 0) {
            return;
        }

        float startX = getPaddingLeft();
        float baselineY = getPaddingTop() + height;
        float maxWaveHeight = height * 0.8f;

        Path linePath = new Path();
        Path fillPath = new Path();
        boolean pathStarted = false;

        for (int i = 0; i < highEnergyData.length; i++) {
            int time = i * stepSec * 1000;
            if (time > max)
                break;

            float x = startX + (float) time / max * width;

            float density = highEnergyData[i] / maxValue;
            density = (float) Math.pow(density, 0.7);
            float y = baselineY - maxWaveHeight * density;

            if (!pathStarted) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, baselineY);
                fillPath.lineTo(x, y);
                pathStarted = true;
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        if (pathStarted) {
            int lastIndex = Math.min(highEnergyData.length - 1, (int) (max / (stepSec * 1000f)));
            float lastX = startX + (float) Math.min(lastIndex * stepSec * 1000, max) / max * width;
            fillPath.lineTo(lastX, baselineY);
            fillPath.close();

            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(linePath, linePaint);
        }
    }
}
