package com.RobinNotBad.BiliClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;

import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.activity.user.info.UserInfoActivity;
import com.RobinNotBad.BiliClient.api.DynamicApi;
import com.RobinNotBad.BiliClient.api.MessageApi;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.TerminalContext;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class BiliTerminal extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context context;

    public static boolean DPI_FORCE_CHANGE = false;

    private static WeakReference<InstanceActivity> instance = new WeakReference<>(null);

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (context == null) {
            SharedPreferencesUtil.sharedPreferences = getSharedPreferences("default", MODE_PRIVATE);
            context = getFitDisplayContext(this);

            // 主题系统：同步初始化（<10ms，失败全返回内置主题）
            com.RobinNotBad.BiliClient.theme.ThemeManager.init(this);

            ErrorCatch errorCatch = ErrorCatch.getInstance();
            errorCatch.init(context);

            tryHardwareAcceleration();

            boolean debugBuild = isDebugBuild();
            Logu.LOGV_ENABLED = SharedPreferencesUtil.getBoolean("dev_logv", debugBuild);
            Logu.LOGD_ENABLED = SharedPreferencesUtil.getBoolean("dev_logd", debugBuild);
            Logu.LOGI_ENABLED = SharedPreferencesUtil.getBoolean("dev_logi", debugBuild);

            if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.DYNAMIC_UPDATE_CHECK_ENABLE, true) && SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) != 0) {
                CenterThreadPool.run(() -> {
                    try {
                        long updateBaseline = SharedPreferencesUtil.getLong("dynamic_update_baseline", 0);
                        int updateNum = DynamicApi.checkDynamicUpdate("all", updateBaseline);
                        SharedPreferencesUtil.putInt(SharedPreferencesUtil.DYNAMIC_UPDATE_NUM, updateNum);
                    } catch (IOException | JSONException e) {
                        SharedPreferencesUtil.putInt(SharedPreferencesUtil.DYNAMIC_UPDATE_NUM, 0);
                    }
                });
            }

            if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.MESSAGE_UPDATE_CHECK_ENABLE, true) && SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) != 0) {
                CenterThreadPool.run(() -> {
                    try {
                        int messageUnread = MessageApi.checkMessageUnread();
                        int privateMsgUnread = MessageApi.checkPrivateMsgUnread();
                        int totalUnread = messageUnread + privateMsgUnread;
                        SharedPreferencesUtil.putInt(SharedPreferencesUtil.MESSAGE_UPDATE_NUM, totalUnread);
                    } catch (Exception e) {
                        SharedPreferencesUtil.putInt(SharedPreferencesUtil.MESSAGE_UPDATE_NUM, 0);
                    }
                });
            }
        }
    }

    private void tryHardwareAcceleration() {
        // API < 16 只能关硬件加速避免 GLES20Canvas.clipPath 崩溃（虽然这个方案很难绷啊）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        try {
            Class<?> cbInterface = Class.forName("android.app.Application$ActivityLifecycleCallbacks");
            java.lang.reflect.Method register = Application.class.getMethod(
                    "registerActivityLifecycleCallbacks", cbInterface);
            int flagHwAccel = android.view.WindowManager.LayoutParams.class
                    .getField("FLAG_HARDWARE_ACCELERATED").getInt(null);

            Object cb = Proxy.newProxyInstance(cbInterface.getClassLoader(),
                    new Class<?>[]{cbInterface}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method,
                                             Object[] args) {
                            if ("onActivityCreated".equals(method.getName())
                                    && args != null && args.length > 0
                                    && args[0] instanceof Activity) {
                                try {
                                    ((Activity) args[0]).getWindow().setFlags(flagHwAccel, flagHwAccel);
                                } catch (Exception ignored) {
                                }
                            }
                            return null;
                        }
                    });
            register.invoke(this, cb);
        } catch (Exception e) {
            Logu.e("HWAccel", "反射启用硬件加速失败: " + e.getMessage());
        }
    }

    public static void setInstance(InstanceActivity instanceActivity) {
        instance = new WeakReference<>(instanceActivity);
    }

    @Nullable
    public static InstanceActivity getInstanceActivityOnTop() {
        return instance.get();
    }

    /**
     * 重写attachBaseContext方法，用于调整应用内dpi
     * 尝试下这种风格代码是否会导致低版本设备异常
     *
     * @param old The origin context.
     */
    public static Context getFitDisplayContext(Context old) {
        float dpiTimes = SharedPreferencesUtil.getFloat("dpi", 1.0F);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return old;
        if (!DPI_FORCE_CHANGE && dpiTimes == 1.0F) return old;
        try {
            DisplayMetrics displayMetrics = old.getResources().getDisplayMetrics();
            Configuration configuration = old.getResources().getConfiguration();
            configuration.densityDpi = (int) (displayMetrics.densityDpi * dpiTimes);
            return old.createConfigurationContext(configuration);
        } catch (Exception e) {
            //MsgUtil.err(e,old);
            return old;
        }
    }

    public static int getVersion() throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
    }

    public static boolean isDebugBuild() {
        return "debug".equals(BuildConfig.BUILD_TYPE);
    }

    public static void jumpToVideo(Context context, long aid) {
        TerminalContext.getInstance().enterVideoDetailPage(context, aid);
    }

    public static void jumpToVideo(Context context, String bvid) {
        TerminalContext.getInstance().enterVideoDetailPage(context, bvid);
    }

    public static void jumpToArticle(Context context, long cvid) {
        TerminalContext.getInstance().enterArticleDetailPage(context, cvid);
    }

    public static void jumpToUser(Context context, long mid) {
        Intent intent = new Intent();
        intent.setClass(context, UserInfoActivity.class);
        intent.putExtra("mid", mid);
        context.startActivity(intent);
    }

}
