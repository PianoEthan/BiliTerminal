package com.RobinNotBad.BiliClient.activity.user.favorite;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.api.FavoriteApi;
import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.google.android.material.card.MaterialCardView;

public class FavoriteFolderEditActivity extends BaseActivity {

    private long mediaId;
    private FavoriteFolder folder;
    private EditText editTitle;
    private EditText editIntro;
    private MaterialCardView btnSave;
    private MaterialCardView btnDelete;
    private boolean busy;
    private boolean destroyed;
    private long deleteClickTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_folder_edit);
        mediaId = getIntent().getLongExtra("mediaId", 0);
        editTitle = findViewById(R.id.editTitle);
        editIntro = findViewById(R.id.editIntro);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnSave.setOnClickListener(v -> {
            if (folder == null) loadFolder();
            else saveFolder();
        });
        btnDelete.setOnClickListener(v -> handleDeleteClick());
        if (mediaId <= 0) {
            updateControls();
            MsgUtil.showMsg("收藏夹参数无效，请返回重试");
            finish();
            return;
        }
        loadFolder();
    }

    private void updateControls() {
        boolean editable = folder != null && !folder.isDefault && !busy;
        editTitle.setEnabled(editable);
        editIntro.setEnabled(editable);
        boolean canSave = !busy && mediaId > 0 && (folder == null || !folder.isDefault);
        btnSave.setEnabled(canSave);
        btnSave.setClickable(canSave);
        btnSave.setAlpha(canSave ? 1f : 0.5f);
        btnDelete.setVisibility(folder != null && !folder.isDefault ? View.VISIBLE : View.GONE);
        btnDelete.setEnabled(editable);
        btnDelete.setClickable(editable);
        btnDelete.setAlpha(editable ? 1f : 0.5f);
    }

    private void loadFolder() {
        if (busy) return;
        busy = true;
        updateControls();
        CenterThreadPool.run(() -> {
            try {
                FavoriteFolder result = FavoriteApi.getFolderInfo(mediaId);
                if (result == null || result.mediaId != mediaId) {
                    throw new java.io.IOException("收藏夹信息不完整");
                }
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    folder = result;
                    editTitle.setText(folder.name);
                    editIntro.setText(folder.intro);
                    busy = false;
                    updateControls();
                    if (folder.isDefault) MsgUtil.showMsg("默认收藏夹不能编辑或删除");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    busy = false;
                    updateControls();
                    if (e instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
                        MsgUtil.showMsgLong(e.getMessage());
                    } else {
                        report(e);
                        MsgUtil.showMsg("收藏夹信息加载失败，点击保存按钮重试加载");
                    }
                });
            }
        });
    }

    private void saveFolder() {
        if (busy || folder == null || folder.isDefault) return;
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            MsgUtil.showMsg("请输入收藏夹名称");
            return;
        }
        String intro = editIntro.getText().toString();
        int privacy = folder.isPrivate ? 1 : 0;
        String cover = folder.cover;
        busy = true;
        deleteClickTimestamp = 0;
        updateControls();
        CenterThreadPool.run(() -> {
            try {
                int result = FavoriteApi.editFolder(mediaId, title, intro, privacy, cover);
                finishOperation(result, "保存");
            } catch (Exception e) {
                failOperation(e);
            }
        });
    }

    private void handleDeleteClick() {
        if (busy || folder == null || folder.isDefault) return;
        long now = System.currentTimeMillis();
        if (deleteClickTimestamp == 0 || now - deleteClickTimestamp >= 3000) {
            deleteClickTimestamp = now;
            MsgUtil.showMsg("再次点击删除按钮确认删除");
            return;
        }
        deleteClickTimestamp = 0;
        busy = true;
        updateControls();
        CenterThreadPool.run(() -> {
            try {
                int result = FavoriteApi.deleteFolder(mediaId);
                finishOperation(result, "删除");
            } catch (Exception e) {
                failOperation(e);
            }
        });
    }

    private void finishOperation(int result, String action) {
        runOnUiThread(() -> {
            if (destroyed || isFinishing()) return;
            busy = false;
            updateControls();
            if (result == 0) {
                MsgUtil.showMsg(action + "成功");
                setResult(RESULT_OK);
                finish();
            } else {
                MsgUtil.showMsg(action + "失败，错误码：" + result);
            }
        });
    }

    private void failOperation(Exception e) {
        runOnUiThread(() -> {
            if (destroyed || isFinishing()) return;
            busy = false;
            updateControls();
            if (e instanceof com.RobinNotBad.BiliClient.api.FavoriteContract.ApiException) {
                MsgUtil.showMsgLong(e.getMessage());
            } else {
                report(e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }
}
