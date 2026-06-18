package com.RobinNotBad.BiliClient.activity.user;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.api.DynamicApi;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PublishOpusActivity extends InstanceActivity {

    private TextInputEditText inputTitle, inputContent;
    private MaterialButton btnPublish, btnPickImage;
    private SwitchMaterial swImage;
    private ImageView previewImage;
    private TextView tipImage;
    private LinearLayout layoutImage;

    private boolean imageUploadEnabled = false;
    private byte[] selectedImageBytes = null;
    private String selectedImageName = "image.jpg";
    private String selectedImageMime = "image/jpeg";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    try {
                        Uri uri = result;
                        InputStream is = getContentResolver().openInputStream(uri);
                        if (is != null) {
                            Bitmap bmp = BitmapFactory.decodeStream(is);
                            is.close();

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                            selectedImageBytes = baos.toByteArray();

                            // 尝试获取文件名和 MIME
                            String mime = getContentResolver().getType(uri);
                            if (mime != null) selectedImageMime = mime;
                            String path = uri.getLastPathSegment();
                            if (path != null) selectedImageName = path;

                            previewImage.setImageBitmap(bmp);
                            previewImage.setVisibility(View.VISIBLE);
                            MsgUtil.showMsg("图片已选择");
                        }
                    } catch (Exception e) {
                        MsgUtil.showMsg("读取图片失败: " + e.getMessage());
                        Log.e("PublishOpus", "pick image error", e);
                    }
                }
            }
    );

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asyncInflate(R.layout.activity_publish_opus, (layoutView, resId) -> {
            inputTitle = findViewById(R.id.input_title);
            inputContent = findViewById(R.id.input_content);
            btnPublish = findViewById(R.id.btn_publish);
            btnPickImage = findViewById(R.id.btn_pick_image);
            swImage = findViewById(R.id.sw_image);
            previewImage = findViewById(R.id.preview_image);
            tipImage = findViewById(R.id.tip_image);
            layoutImage = findViewById(R.id.layout_image);

            // 检查实验室开关
            imageUploadEnabled = SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.LAB_OPUS_IMAGE_UPLOAD, false);

            if (!imageUploadEnabled) {
                swImage.setEnabled(false);
                swImage.setChecked(false);
                tipImage.setVisibility(View.VISIBLE);
            } else {
                tipImage.setVisibility(View.GONE);
                swImage.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) {
                        layoutImage.setVisibility(View.VISIBLE);
                    } else {
                        layoutImage.setVisibility(View.GONE);
                        selectedImageBytes = null;
                        previewImage.setVisibility(View.GONE);
                    }
                });
            }

            btnPickImage.setOnClickListener(v -> {
                try {
                    pickImageLauncher.launch("image/*");
                } catch (Exception e) {
                    MsgUtil.showMsg("无法打开图片选择器");
                }
            });

            btnPublish.setOnClickListener(v -> {
                String title = inputTitle.getText().toString().trim();
                String content = inputContent.getText().toString().trim();

                if (content.isEmpty()) {
                    MsgUtil.showMsg("请输入内容喵～");
                    return;
                }

                btnPublish.setEnabled(false);
                btnPublish.setText("发布中...");

                CenterThreadPool.run(() -> {
                    try {
                        List<DynamicApi.ImageUploadResult> images = null;

                        // 如果开启了图片上传且选择了图片
                        if (imageUploadEnabled && swImage.isChecked() && selectedImageBytes != null) {
                            runOnUiThread(() -> MsgUtil.showMsg("正在上传图片..."));

                            DynamicApi.ImageUploadResult uploadResult = DynamicApi.uploadImage(
                                    selectedImageBytes, selectedImageName, selectedImageMime, "daily");
                            if (uploadResult == null) {
                                runOnUiThread(() -> {
                                    MsgUtil.showMsg("图片上传失败喵～");
                                    btnPublish.setEnabled(true);
                                    btnPublish.setText("发布动态");
                                });
                                return;
                            }

                            images = new ArrayList<>();
                            images.add(uploadResult);
                            runOnUiThread(() -> MsgUtil.showMsg("图片上传成功，正在发布..."));
                        }

                        long dynId = DynamicApi.publishOpus(title, content, images);

                        runOnUiThread(() -> {
                            btnPublish.setEnabled(true);
                            btnPublish.setText("发布动态");
                            if (dynId > 0) {
                                MsgUtil.showMsg("发布成功！动态ID: " + dynId);
                                inputTitle.setText("");
                                inputContent.setText("");
                                selectedImageBytes = null;
                                previewImage.setVisibility(View.GONE);
                                if (swImage.isChecked()) swImage.setChecked(false);
                            } else {
                                MsgUtil.showMsg("发布失败喵～");
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            MsgUtil.showMsg("发布失败: " + e.getMessage());
                            btnPublish.setEnabled(true);
                            btnPublish.setText("发布动态");
                            Log.e("PublishOpus", "error", e);
                        });
                    }
                });
            });
        });
    }
}
