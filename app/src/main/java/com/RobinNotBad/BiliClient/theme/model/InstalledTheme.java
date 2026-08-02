package com.RobinNotBad.BiliClient.theme.model;

import java.io.File;

/**
 * 已安装到 filesDir/themes/&lt;id&gt;/ 的主题包。
 */
public class InstalledTheme {
    public final String id;
    public final ThemeManifest manifest;
    public final File dir;

    public InstalledTheme(String id, ThemeManifest manifest, File dir) {
        this.id = id;
        this.manifest = manifest;
        this.dir = dir;
    }

    public String getName() {
        return manifest == null ? id : manifest.getDisplayName();
    }

    public String getDescription() {
        if (manifest == null || manifest.meta == null) return "";
        return manifest.meta.description == null ? "" : manifest.meta.description;
    }

    public String getAuthor() {
        if (manifest == null || manifest.meta == null) return "";
        return manifest.meta.author == null ? "" : manifest.meta.author;
    }

    public File getPreviewFile() {
        if (manifest == null || manifest.preview == null) return null;
        File f = new File(dir, manifest.preview);
        return f.isFile() ? f : null;
    }

    public File getBackgroundFile(boolean dark) {
        if (manifest == null || manifest.background == null) return null;
        String name = dark ? manifest.background.image_dark : manifest.background.image_light;
        if (name == null) name = manifest.background.image;
        if (name == null) return null;
        File f = new File(dir, name);
        return f.isFile() ? f : null;
    }
}
