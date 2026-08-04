package com.RobinNotBad.BiliClient.activity.reply;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.EmoteActivity;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.api.EmoteApi;
import com.RobinNotBad.BiliClient.api.ReplyApi;
import com.RobinNotBad.BiliClient.event.ReplyEvent;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WriteReplyActivity extends BaseActivity {

    private static final Map<Integer, String> msgMap = new HashMap<>() {{
        put(-101, "没有登录or登录信息有误？");
        put(-102, "账号被封禁！");
        put(-509, "请求过于频繁！");
        put(12015, "需要评论验证码...？");
        put(12016, "包含敏感内容！");
        put(12025, "字数过多啦QAQ");
        put(12035, "被拉黑了...");
        put(12051, "重复评论，请勿刷屏！");
    }};

    EditText editText;
    private final ActivityResultLauncher<Intent> emoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {
        int code = result.getResultCode();
        Intent data = result.getData();
        if (code == RESULT_OK && data != null && data.hasExtra("text")) {
            editText.append(data.getStringExtra("text"));
        }
    });

    final ArrayList<String> imageList = new ArrayList<>();
    final ArrayList<ReplyApi.UploadImageData> uploadDataList = new ArrayList<>();
    TextView imageText;

    private final ActivityResultLauncher<Intent> imageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {
        int code = result.getResultCode();
        Intent data = result.getData();
        if (code == RESULT_OK && data != null && data.getData() != null) {
            if (imageList.size() >= 9) {
                MsgUtil.showMsg("最多只能添加9张图片喵~");
                return;
            }
            addImage(data.getData());
        }
    });

    boolean sent = false;
    boolean dontKyPlease = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_reply);

        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
            MsgUtil.showMsg("还没有登录喵~");
            finish();
        }

        Intent intent = getIntent();
        long oid = intent.getLongExtra("oid", 0);
        long rpid = intent.getLongExtra("rpid", 0);
        long parent = intent.getLongExtra("parent", 0);
        int replyType = intent.getIntExtra("replyType", ReplyApi.REPLY_TYPE_VIDEO);
        String parentSender = intent.getStringExtra("parentSender");
        int pos = intent.getIntExtra("pos", -1);

        editText = findViewById(R.id.editText);
        MaterialCardView send = findViewById(R.id.send);
        imageText = findViewById(R.id.imageText);

        if (parentSender != null && !parentSender.isEmpty()) {
            editText.setText("回复 @" + parentSender + " :");
            editText.setSelection(editText.getText().length());
        }

        send.setOnClickListener(view -> {
            if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.cookie_refresh, true)) {
                if (!sent) {
                    CenterThreadPool.run(() -> {
                        String text = editText.getText().toString();
                        if (!text.isEmpty() || !imageList.isEmpty()) {
                            if (checkKy(text) && dontKyPlease) {
                                MsgUtil.showDialog("保护措施……", getString(R.string.reply_dont_ky), 15);
                                dontKyPlease = false;
                                return;
                            }
                            try {
                                String pictures = buildPictures();
                                Pair<Integer, Reply> result = ReplyApi.sendReply(oid, rpid, parent, text, replyType, pictures);
                                int resultCode = result.first;
                                Reply resultReply = result.second;

                                sent = true;

                                if (resultCode == 0) {
                                    runOnUiThread(() -> MsgUtil.showMsg("发送成功>w<"));
                                    resultReply.forceDelete = true;
                                    resultReply.pubTime = "刚刚";
                                    synchronized (uploadDataList) {
                                        for (ReplyApi.UploadImageData uploadData : uploadDataList) {
                                            resultReply.pictureList.add(uploadData.image_url);
                                        }
                                    }
                                    EventBus.getDefault().post(new ReplyEvent(1, resultReply, pos, oid));
                                    finish();
                                } else {
                                    String toast_msg = "评论发送失败：\n" + (msgMap.containsKey(resultCode) ? msgMap.get(resultCode) : resultCode);
                                    runOnUiThread(() -> MsgUtil.showMsg(toast_msg));
                                    sent = false;
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> MsgUtil.err(e));
                            }
                        } else runOnUiThread(() -> MsgUtil.showMsg("还没输入内容呢~"));
                    });
                } else MsgUtil.showMsg("正在发送中");
            } else
                MsgUtil.showDialog("无法发送", "上一次的Cookie刷新失败了，\n您可能需要重新登录以进行敏感操作", -1);
        });

        findViewById(R.id.emote).setOnClickListener(view ->
                emoteLauncher.launch(new Intent(this, EmoteActivity.class).putExtra("from", EmoteApi.BUSINESS_REPLY)));

        findViewById(R.id.image).setOnClickListener(view -> {
            if (imageList.size() >= 9) {
                MsgUtil.showMsg("最多只能添加9张图片喵~");
                return;
            }
            Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickIntent.setType("image/*");
            imageLauncher.launch(pickIntent);
        });
    }

    private void addImage(Uri uri) {
        imageList.add(uri.toString());
        updateImageText();
        CenterThreadPool.run(() -> {
            try {
                byte[] compressed = compressImage(uri);
                ReplyApi.UploadImageData data = ReplyApi.uploadReplyImage(compressed, System.currentTimeMillis() + ".jpg").getOrNull();
                if (data == null) {
                    runOnUiThread(() -> {
                        MsgUtil.showMsg("图片上传失败");
                        imageList.remove(uri.toString());
                        updateImageText();
                    });
                    return;
                }
                synchronized (uploadDataList) {
                    uploadDataList.add(data);
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    MsgUtil.showMsg("图片处理失败");
                    imageList.remove(uri.toString());
                    updateImageText();
                });
            }
        });
    }

    private byte[] compressImage(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("无法读取图片");

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        if (bitmap == null) throw new IOException("解码图片失败");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        bitmap.recycle();
        return outputStream.toByteArray();
    }

    private String buildPictures() {
        JsonArray jsonArray = new JsonArray();
        synchronized (uploadDataList) {
            for (ReplyApi.UploadImageData data : uploadDataList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("img_src", data.image_url);
                jsonObject.addProperty("img_width", data.image_width);
                jsonObject.addProperty("img_height", data.image_height);
                jsonObject.addProperty("img_size", data.img_size);
                jsonArray.add(jsonObject);
            }
        }
        return jsonArray.toString();
    }

    private void updateImageText() {
        int count = imageList.size();
        imageText.setText(count == 0 ? getString(R.string.btn_image) : getString(R.string.btn_image) + "(" + count + ")");
    }

    /**
     * P用没有的保护措施
     *
     * @param str 评论文本
     */
    private boolean checkKy(String str) {
        if (str.contains("哔哩终端")) return true;
        if (str.contains("终端")) {
            return str.contains("表") || str.contains("b站") || str.contains("B站") || str.contains("bili") || str.contains("哔");
        }
        return false;
    }
}