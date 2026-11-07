package com.RobinNotBad.BiliClient.model;

public final class ReplyPagination {
    public static final class Request {
        public final int generation;
        public final int page;
        public final boolean refresh;

        private Request(int generation, int page, boolean refresh) {
            this.generation = generation;
            this.page = page;
            this.refresh = refresh;
        }
    }

    private int generation;
    private int loadedPage;
    private boolean loading;
    private boolean hasMore = true;

    public Request refresh() {
        generation++;
        loading = true;
        return new Request(generation, 1, true);
    }

    public Request next() {
        if (loading || !hasMore) return null;
        loading = true;
        return new Request(generation, loadedPage + 1, loadedPage == 0);
    }

    public boolean accepts(Request request) {
        return loading && request.generation == generation;
    }

    public void succeed(Request request, boolean more, int appended) {
        if (!accepts(request)) return;
        loadedPage = request.page;
        hasMore = more && appended > 0;
        loading = false;
    }

    public void fail(Request request) {
        if (accepts(request)) loading = false;
    }

    public int loadedPage() {
        return loadedPage;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public void invalidate() {
        generation++;
        loading = false;
    }
}
