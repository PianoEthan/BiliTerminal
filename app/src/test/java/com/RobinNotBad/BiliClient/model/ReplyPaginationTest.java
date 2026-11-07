package com.RobinNotBad.BiliClient.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;

import com.RobinNotBad.BiliClient.api.ReplyApi;

import static org.junit.Assert.*;

public class ReplyPaginationTest {
    @Test public void failureRetriesSamePageWithoutResettingToFirstPage() {
        ReplyPagination pages = new ReplyPagination();
        ReplyPagination.Request first = pages.refresh();
        pages.succeed(first, true, 3);
        ReplyPagination.Request next = pages.next();
        assertEquals(2, next.page);
        assertNull(pages.next());
        pages.fail(next);
        assertEquals(1, pages.loadedPage());
        assertEquals(2, pages.next().page);
    }

    @Test public void refreshAndSortInvalidateStaleResponses() {
        ReplyPagination pages = new ReplyPagination();
        ReplyPagination.Request stale = pages.refresh();
        ReplyPagination.Request current = pages.refresh();
        assertFalse(pages.accepts(stale));
        pages.succeed(stale, false, 0);
        assertTrue(pages.accepts(current));
        pages.succeed(current, true, 3);
        assertEquals(2, pages.next().page);
    }

    @Test public void firstPageFailureCannotProducePageZero() {
        ReplyPagination pages = new ReplyPagination();
        ReplyPagination.Request first = pages.refresh();
        pages.fail(first);
        assertEquals(1, pages.next().page);
    }

    @Test public void emptyOrDuplicateOnlyPagesStopAutomaticLooping() {
        ReplyPagination pages = new ReplyPagination();
        pages.succeed(pages.refresh(), true, 3);
        pages.succeed(pages.next(), true, 0);
        assertFalse(pages.hasMore());
        assertNull(pages.next());
        pages.succeed(pages.refresh(), true, 4);
        assertTrue(pages.hasMore());
    }

    @Test public void destroyedViewRejectsDelayedResponse() {
        ReplyPagination pages = new ReplyPagination();
        ReplyPagination.Request first = pages.refresh();
        pages.invalidate();
        assertFalse(pages.accepts(first));
    }

    @Test public void repeatedThreeRepliesAreNotAppendedAgain() {
        ArrayList<Reply> target = new ArrayList<>();
        Reply a = new Reply(); a.rpid = 1;
        Reply b = new Reply(); b.rpid = 2;
        Reply c = new Reply(); c.rpid = 3;
        assertEquals(3, ReplyApi.appendUnique(target, Arrays.asList(a, b, c)));
        assertEquals(0, ReplyApi.appendUnique(target, Arrays.asList(a, b, c)));
        Reply d = new Reply(); d.rpid = 4;
        assertEquals(1, ReplyApi.appendUnique(target, Arrays.asList(c, d)));
        assertEquals(4, target.size());
    }
}
