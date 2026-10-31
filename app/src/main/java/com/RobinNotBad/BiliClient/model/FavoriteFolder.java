package com.RobinNotBad.BiliClient.model;

import com.google.gson.annotations.SerializedName;

public class FavoriteFolder {
    @SerializedName("id")
    public long id;
    @SerializedName("media_id")
    public long mediaId;
    @SerializedName(value = "title", alternate = {"name"})
    public String name;
    public String cover = "";
    @SerializedName("media_count")
    public int videoCount;
    @SerializedName("max_count")
    public int maxCount;
    public String intro = "";
    public boolean isDefault;
    public boolean isPrivate;

    public FavoriteFolder() {
    }
}
