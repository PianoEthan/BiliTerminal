package com.RobinNotBad.BiliClient.util;

import android.os.Build;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// 反射工具类，以后用这神秘东西大约可以避免一下VFY……

public class CompatUtil {
    @SuppressWarnings("CharsetObjectCanBeUsed")
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    public static Charset getCharsetUTF8() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return StandardCharsets.UTF_8;
        } else {
            return UTF_8;
        }
    }

    public static void setCompoundDrawablesRelative(TextView tv, int start, int top, int end, int bottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    public static void callMethodIfExists(Object obj, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            Method method = obj.getClass().getMethod(methodName, paramTypes);
            method.invoke(obj, args);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            Logu.e("CompatUtil", "反射调用 " + methodName + " 失败: " + e.getMessage());
        }
    }
}
