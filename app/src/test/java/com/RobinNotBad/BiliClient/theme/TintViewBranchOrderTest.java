package com.RobinNotBad.BiliClient.theme;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * tintView 的 instanceof 链必须「子类在前」。SeekBar 是 ProgressBar 的子类、
 * SwitchCompat/Switch/RadioButton 是 Button 的子类，顺序写反时父类分支先 return，
 * 子类分支整段不可达——这种缺陷在真机上只表现为「颜色不对」，主机上无法直接跑视图，
 * 所以用源码顺序把它钉住（反射拿到的类层级来自单元测试的 android.jar）。
 */
public class TintViewBranchOrderTest {

    private static final Pattern BRANCH =
            Pattern.compile("view instanceof ([A-Za-z_][A-Za-z0-9_.]*)");

    private static final String[] CLASS_PREFIXES = {
            "",
            "android.widget.",
            "android.view.",
            "androidx.appcompat.widget.",
            "androidx.swiperefreshlayout.widget.",
            "com.google.android.material.button.",
            "com.google.android.material.card.",
            "com.google.android.material.progressindicator.",
    };

    @Test
    public void noBranchFollowsItsOwnSupertype() throws Exception {
        List<String> chain = branchChain();
        assertTrue("分支链过短，可能没截取到 tintView：" + chain, chain.size() > 8);

        for (int i = 0; i < chain.size(); i++) {
            Class<?> earlier = load(chain.get(i));
            for (int j = i + 1; j < chain.size(); j++) {
                Class<?> later = load(chain.get(j));
                assertFalse(chain.get(i) + " 是 " + chain.get(j) + " 的父类却排在它前面，"
                        + "后者永远不会被执行", earlier.isAssignableFrom(later));
            }
        }
    }

    @Test
    public void compoundButtonAndSeekBarComeBeforeTheirParents() throws Exception {
        List<String> chain = branchChain();
        assertTrue(chain + " 缺少 SeekBar 分支", chain.contains("SeekBar"));
        assertTrue(chain + " 缺少 SwitchCompat 分支", chain.contains("SwitchCompat"));
        assertTrue(chain + " 缺少 RadioButton 分支", chain.contains("RadioButton"));

        assertTrue("SeekBar 必须在 ProgressBar 之前",
                chain.indexOf("SeekBar") < chain.indexOf("ProgressBar"));
        assertTrue("SwitchCompat 必须在 Button 之前",
                chain.indexOf("SwitchCompat") < chain.indexOf("Button"));
        assertTrue("Switch 必须在 Button 之前",
                chain.indexOf("Switch") < chain.indexOf("Button"));
        assertTrue("RadioButton 必须在 Button 之前",
                chain.indexOf("RadioButton") < chain.indexOf("Button"));
    }

    private static List<String> branchChain() throws Exception {
        String src = new String(Files.readAllBytes(sourceFile().toPath()), StandardCharsets.UTF_8);
        int from = src.indexOf("private static void tintView");
        if (from < 0) fail("ThemeApplier.java 里找不到 tintView");
        int next = src.indexOf("\n    private ", from + 1);
        int javadoc = src.indexOf("\n    /**", from + 1);
        int to = next < 0 ? javadoc : (javadoc < 0 ? next : Math.min(next, javadoc));
        if (to < 0) fail("tintView 方法体结尾定位失败");

        Matcher m = BRANCH.matcher(src.substring(from, to));
        List<String> chain = new ArrayList<>();
        while (m.find()) {
            String name = m.group(1);
            int dot = name.lastIndexOf('.');
            chain.add(dot < 0 ? name : name.substring(dot + 1));
        }
        return chain;
    }

    private static Class<?> load(String simpleName) throws Exception {
        for (String prefix : CLASS_PREFIXES) {
            try {
                return Class.forName(prefix + simpleName);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new AssertionError("TintViewBranchOrderTest 需要为 " + simpleName + " 补一条包名前缀");
    }

    private static File sourceFile() {
        String rel = "src/main/java/com/RobinNotBad/BiliClient/theme/ThemeApplier.java";
        File[] candidates = {new File(rel), new File("app/" + rel), new File("../" + rel)};
        for (File f : candidates) {
            if (f.isFile()) return f;
        }
        throw new AssertionError("找不到 ThemeApplier.java，测试的工作目录假设已失效："
                + new File(".").getAbsolutePath());
    }
}
