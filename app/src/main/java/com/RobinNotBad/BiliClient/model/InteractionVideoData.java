package com.RobinNotBad.BiliClient.model;

import java.util.ArrayList;
import java.util.List;

public class InteractionVideoData {
    public String title;
    public long edgeId;
    public List<InteractionStoryNode> storyList;
    public InteractionEdge edges;
    public InteractionPreload preload;
    public List<InteractionHiddenVar> hiddenVars;
    public int isLeaf;
    public int noTutorial;
    public int noBacktracking;
    public int noEvaluation;

    public static class InteractionStoryNode {
        public long nodeId;
        public long edgeId;
        public String title;
        public long cid;
        public long startPos;
        public String cover;
        public int isCurrent;
        public long cursor;
    }

    public static class InteractionEdge {
        public InteractionDimension dimension;
        public List<InteractionQuestion> questions;
        public InteractionSkin skin;
    }

    public static class InteractionDimension {
        public int width;
        public int height;
        public int rotate;
        public String sar;
    }

    public static class InteractionQuestion {
        public long id;
        public int type;
        public long startTimeR;
        public long duration;
        public int pauseVideo;
        public String title;
        public List<InteractionChoice> choices;
        public long fadeInTime;
        public long fadeOutTime;
    }

    public static class InteractionChoice {
        public long id;
        public String platformAction;
        public String nativeAction;
        public String condition;
        public long cid;
        public int x;
        public int y;
        public int textAlign;
        public String option;
        public InteractionAnimation selected;
        public InteractionAnimation submited;
        public int isDefault;
        public int isHidden;
    }

    public static class InteractionAnimation {
        public String type;
        public long duration;
    }

    public static class InteractionSkin {
        public String choiceImage;
        public String titleTextColor;
        public String titleShadowColor;
        public int titleShadowOffsetX;
        public int titleShadowOffsetY;
        public int titleShadowRadius;
        public String progressbarColor;
        public String progressbarShadowColor;
    }

    public static class InteractionPreload {
        public List<InteractionPreloadVideo> video;
    }

    public static class InteractionPreloadVideo {
        public long aid;
        public long cid;
    }

    public static class InteractionHiddenVar {
        public long value;
        public String id;
        public String idV2;
        public int type;
        public int isShow;
        public String name;
        public int skipOverwrite;
    }

    public static class InteractionStoryProgress {
        public long nodeId;
        public long edgeId;
        public String title;
        public long cid;
        public long startPos;
        public String cover;
        public boolean isCurrent;
        public int cursor;
    }
}

