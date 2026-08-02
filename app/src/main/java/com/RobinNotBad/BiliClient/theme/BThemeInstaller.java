package com.RobinNotBad.BiliClient.theme;

import android.content.Context;
import android.text.TextUtils;

import com.RobinNotBad.BiliClient.theme.model.InstalledTheme;
import com.RobinNotBad.BiliClient.theme.model.ThemeManifest;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.GsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * .btheme 安装器：白名单条目（天然免疫 zip-slip）+ 尺寸上限 + format/id 校验。
 * 解压先落临时目录，再原子替换（deleteFolder + rename）。
 */
public class BThemeInstaller {

    public static final int MAX_PACKAGE_BYTES = 8 * 1024 * 1024;
    public static final int MAX_ENTRIES = 12;
    public static final long MAX_THEME_JSON = 64 * 1024;
    public static final long MAX_PREVIEW = 1024 * 1024;
    public static final long MAX_BACKGROUND = 6 * 1024 * 1024;

    private static final List<String> WHITELIST = Arrays.asList(
            "theme.json", "preview.png",
            "background.png", "background_dark.png", "background_light.png");

    public static final String ERR_TOO_BIG = "主题包过大（>8MB）";
    public static final String ERR_TOO_MANY = "主题包条目过多（>12）";
    public static final String ERR_BAD_ENTRY = "主题包包含非法文件";
    public static final String ERR_FORMAT = "主题版本过新，请升级哔哩终端";
    public static final String ERR_BAD_JSON = "theme.json 缺失或损坏";
    public static final String ERR_BAD_ID = "主题 ID 不合法";

    public static class InstallResult {
        public boolean ok;
        public String error;
        public InstalledTheme theme;
    }

    // ------------------------------------------------------------ 目录

    public static File getThemesDir(Context context) {
        return new File(context.getFilesDir(), "themes");
    }

    /** 校验过 id 后的主题目录；id 不合法返回 null */
    public static File getThemeDir(Context context, String id) {
        if (!isValidId(id)) return null;
        return new File(getThemesDir(context), id);
    }

    public static boolean isValidId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9._-]{1,64}$");
    }

    // ------------------------------------------------------------ 查询

    public static InstalledTheme loadInstalled(Context context, String id) {
        File dir = getThemeDir(context, id);
        if (dir == null || !dir.isDirectory()) return null;
        File json = new File(dir, "theme.json");
        if (!json.isFile()) return null;
        String content = FileUtil.readString(json);
        if (TextUtils.isEmpty(content)) return null;
        ThemeManifest manifest = GsonUtil.fromJson(content, ThemeManifest.class);
        if (manifest == null || !manifest.isValid() || !id.equals(manifest.id)) return null;
        return new InstalledTheme(id, manifest, dir);
    }

    public static List<InstalledTheme> listInstalled(Context context) {
        List<InstalledTheme> list = new ArrayList<>();
        File dir = getThemesDir(context);
        File[] children = dir.listFiles();
        if (children == null) return list;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            InstalledTheme theme = loadInstalled(context, child.getName());
            if (theme != null) list.add(theme);
        }
        return list;
    }

    // ------------------------------------------------------------ 安装

    /** 从输入流安装；返回结果（不抛异常，错误信息面向用户） */
    public static InstallResult install(Context context, InputStream input) {
        InstallResult result = new InstallResult();
        File tmpFile = null;
        try {
            // 1. 流 → cache 临时文件（整体 ≤8MB）
            tmpFile = File.createTempFile("btheme", ".zip", context.getCacheDir());
            long total = copyCapped(input, tmpFile, MAX_PACKAGE_BYTES);
            if (total > MAX_PACKAGE_BYTES) {
                result.error = ERR_TOO_BIG;
                return result;
            }

            ZipInputStream zis = new ZipInputStream(new java.io.FileInputStream(tmpFile));
            ThemeManifest manifest = null;
            byte[] themeJson = null;
            String themeJsonName = null;

            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRIES) {
                    result.error = ERR_TOO_MANY;
                    zis.close();
                    return result;
                }
                String name = entry.getName();
                if (!WHITELIST.contains(name)) {
                    result.error = ERR_BAD_ENTRY + "：" + name;
                    zis.close();
                    return result;
                }
                if ("theme.json".equals(name)) {
                    themeJson = readCapped(zis, MAX_THEME_JSON);
                    if (themeJson == null) {
                        result.error = ERR_BAD_JSON;
                        zis.close();
                        return result;
                    }
                    themeJsonName = name;
                }
            }
            zis.close();
            if (themeJson == null) {
                result.error = ERR_BAD_JSON;
                return result;
            }

            // 2. 解析 + format/id 校验
            String json = new String(themeJson, "UTF-8");
            manifest = GsonUtil.fromJson(json, ThemeManifest.class);
            if (manifest == null || manifest.format > ThemeManifest.FORMAT) {
                result.error = ERR_FORMAT;
                return result;
            }
            if (manifest == null || !manifest.isValid()) {
                result.error = ERR_BAD_JSON;
                return result;
            }
            if (!isValidId(manifest.id)) {
                result.error = ERR_BAD_ID;
                return result;
            }
            File finalDir = getThemeDir(context, manifest.id);

            // 3. 解压到临时目录 → 原子替换
            File tmpDir = new File(context.getCacheDir(), "btheme_" + System.currentTimeMillis());
            if (!tmpDir.mkdirs()) {
                result.error = "主题安装失败";
                return result;
            }
            zis = new ZipInputStream(new java.io.FileInputStream(tmpFile));
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals(themeJsonName)) {
                    // theme.json 已在第一遍解析；直接落盘
                    writeFile(new File(tmpDir, name), themeJson);
                    continue;
                }
                long cap = name.endsWith(".png") ? MAX_BACKGROUND : MAX_THEME_JSON;
                if (name.equals("preview.png")) cap = MAX_PREVIEW;
                byte[] data = readCapped(zis, cap);
                if (data == null) {
                    result.error = ERR_BAD_ENTRY + "：" + name + "（过大）";
                    zis.close();
                    FileUtil.deleteFolder(tmpDir);
                    return result;
                }
                writeFile(new File(tmpDir, name), data);
            }
            zis.close();

            // 4. 原子替换（先确保 themes 目录存在）
            File themesDir = getThemesDir(context);
            if (!themesDir.isDirectory() && !themesDir.mkdirs()) {
                FileUtil.deleteFolder(tmpDir);
                result.error = "主题安装失败";
                return result;
            }
            FileUtil.deleteFolder(finalDir);
            if (!tmpDir.renameTo(finalDir)) {
                FileUtil.deleteFolder(tmpDir);
                result.error = "主题安装失败";
                return result;
            }
            result.theme = new InstalledTheme(manifest.id, manifest, finalDir);
            result.ok = true;
            return result;
        } catch (Exception e) {
            result.error = "主题安装失败：" + e.getMessage();
            return result;
        } finally {
            if (tmpFile != null) tmpFile.delete();
        }
    }

    /** 卸载；若正在使用则先回退内置主题。内置预设不可卸载。 */
    public static void uninstall(Context context, String id) {
        if (id == null || ThemeManager.isPresetId(id)) return;
        ThemeManager tm = ThemeManager.getInstance();
        if (tm != null && id.equals(tm.getThemeId())) {
            tm.setTheme("");
        }
        File dir = getThemeDir(context, id);
        if (dir != null) FileUtil.deleteFolder(dir);
    }

    // ------------------------------------------------------------ 工具

    /** 读满整个条目，超上限返回 null */
    private static byte[] readCapped(ZipInputStream zis, long cap) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = zis.read(buffer)) != -1) {
            total += n;
            if (total > cap) return null;
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /** 拷贝流到文件并限制总量；返回实际字节数 */
    private static long copyCapped(InputStream in, File file, long cap) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        try {
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > cap) break;
                out.write(buffer, 0, n);
            }
        } finally {
            out.close();
        }
        return total;
    }

    private static void writeFile(File file, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
