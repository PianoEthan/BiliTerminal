package com.RobinNotBad.BiliClient.theme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.material.color.quantize.QuantizerCelebi;
import com.google.material.color.score.Score;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 从位图提取主题种子色（QuantizerCelebi + Score）。
 * 输入位图保持 ≤64px / RGB_565，内存占用 ~8KB。
 */
public class ColorExtractor {

    /** 从内存位图提取；失败返回 0 */
    public static int extractFromBitmap(Bitmap bitmap) {
        if (bitmap == null) return 0;
        try {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w <= 0 || h <= 0) return 0;
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            Map<Integer, Integer> counts = QuantizerCelebi.quantize(pixels, 128);
            if (counts == null || counts.isEmpty()) return 0;
            List<Integer> ranked = Score.score(counts, 4);
            if (ranked == null || ranked.isEmpty()) return 0;
            return ranked.get(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /** 从文件解码 64px 后提取；失败返回 0 */
    public static int extractFromFile(File file) {
        if (file == null || !file.isFile()) return 0;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), opts);
        int sample = 1;
        int width = opts.outWidth;
        int height = opts.outHeight;
        while (width > 64 || height > 64) {
            sample <<= 1;
            width >>= 1;
            height >>= 1;
        }
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(file.getPath(), opts);
            if (bitmap == null) return 0;
            return extractFromBitmap(bitmap);
        } finally {
            if (bitmap != null) bitmap.recycle();
        }
    }
}
