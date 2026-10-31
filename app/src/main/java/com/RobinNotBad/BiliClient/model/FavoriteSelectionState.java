package com.RobinNotBad.BiliClient.model;

import java.util.ArrayList;
import java.util.List;

public final class FavoriteSelectionState {
    private final List<Boolean> initial;
    private final List<Boolean> current;

    public FavoriteSelectionState(List<Boolean> states) {
        initial = new ArrayList<>(states);
        current = new ArrayList<>(states);
    }

    public int size() {
        return current.size();
    }

    public boolean isSelected(int position) {
        return current.get(position);
    }

    public void applyResult(int position, boolean selected, boolean success) {
        if (success) current.set(position, selected);
    }

    public boolean hasChanges() {
        return !initial.equals(current);
    }

    public boolean isFavorite() {
        return current.contains(true);
    }
}
