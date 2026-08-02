package com.RobinNotBad.BiliClient.event;

/**
 * 主题变更事件（非粘性）。
 * BaseActivity 收到后调用 recreate() 即时重建；PlayerActivity 等裸 Activity 由各自手动处理。
 */
public class ThemeChangedEvent {
    private final int generation;

    public ThemeChangedEvent(int generation) {
        this.generation = generation;
    }

    public int getGeneration() {
        return generation;
    }
}
