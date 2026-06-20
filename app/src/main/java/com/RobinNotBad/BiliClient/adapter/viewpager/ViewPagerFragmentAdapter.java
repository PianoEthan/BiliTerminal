package com.RobinNotBad.BiliClient.adapter.viewpager;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//ViewPagerAdapter，适用于各类需要翻页的场景

public class ViewPagerFragmentAdapter extends FragmentStatePagerAdapter {

    private final List<Fragment> fragmentList;
    final FragmentManager fm;
    private final Map<Integer, Fragment> instantiatedFragments = new HashMap<>();

    public ViewPagerFragmentAdapter(@NonNull FragmentManager fm, List<Fragment> fragmentList) {
        super(fm);
        this.fragmentList = fragmentList;
        this.fm = fm;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position < 0 || position >= fragmentList.size()) {
            return new Fragment();
        }
        return fragmentList.get(position);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        instantiatedFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        instantiatedFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getFragment(int position) {
        return instantiatedFragments.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList != null ? fragmentList.size() : 0;
    }
}
