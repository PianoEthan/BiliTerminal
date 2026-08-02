package com.RobinNotBad.BiliClient.activity.base;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.theme.ThemeApplier;

public class BaseFragment extends Fragment {
    public void runOnUiThread(Runnable runnable) {
        if (isAdded()) requireActivity().runOnUiThread(runnable);
    }

    public Context getAppContext() {
        return BiliTerminal.context;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 主题扼点 2：Fragment 视图树染色
        ThemeApplier.applyContent(view);
    }
}
