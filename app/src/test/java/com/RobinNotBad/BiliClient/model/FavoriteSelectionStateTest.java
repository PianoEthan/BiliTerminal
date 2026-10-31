package com.RobinNotBad.BiliClient.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FavoriteSelectionStateTest {
    @Test
    public void unchangedExitPreservesInitialSelection() {
        FavoriteSelectionState state = new FavoriteSelectionState(Arrays.asList(true, false));
        assertFalse(state.hasChanges());
        assertTrue(state.isFavorite());
    }

    @Test
    public void initiallyEmptyHasNoDeletion() {
        FavoriteSelectionState state = new FavoriteSelectionState(Collections.emptyList());
        assertFalse(state.hasChanges());
        assertFalse(state.isFavorite());
        FavoriteSelectionState unselected = new FavoriteSelectionState(Arrays.asList(false, false));
        assertFalse(unselected.hasChanges());
        assertFalse(unselected.isFavorite());
    }

    @Test
    public void addThenDeleteReturnsToInitialState() {
        FavoriteSelectionState state = new FavoriteSelectionState(Collections.singletonList(false));
        state.applyResult(0, true, true);
        assertTrue(state.hasChanges());
        assertTrue(state.isFavorite());
        state.applyResult(0, false, true);
        assertFalse(state.hasChanges());
        assertFalse(state.isFavorite());
    }

    @Test
    public void deleteAllThenAddAnotherRemainsFavorite() {
        FavoriteSelectionState state = new FavoriteSelectionState(Arrays.asList(true, true, false));
        state.applyResult(0, false, true);
        state.applyResult(1, false, true);
        assertTrue(state.hasChanges());
        assertFalse(state.isFavorite());
        state.applyResult(2, true, true);
        assertTrue(state.hasChanges());
        assertTrue(state.isFavorite());
    }

    @Test
    public void failedRequestsDoNotChangeSelection() {
        FavoriteSelectionState state = new FavoriteSelectionState(Arrays.asList(true, false));
        state.applyResult(0, false, false);
        state.applyResult(1, true, false);
        assertTrue(state.isSelected(0));
        assertFalse(state.isSelected(1));
        assertTrue(state.isFavorite());
        assertFalse(state.hasChanges());
    }

    @Test
    public void initialStateIsAnIndependentSnapshot() {
        ArrayList<Boolean> input = new ArrayList<>(Collections.singletonList(false));
        FavoriteSelectionState state = new FavoriteSelectionState(input);
        input.set(0, true);
        assertFalse(state.isSelected(0));
        state.applyResult(0, true, true);
        assertTrue(state.hasChanges());
    }
}
