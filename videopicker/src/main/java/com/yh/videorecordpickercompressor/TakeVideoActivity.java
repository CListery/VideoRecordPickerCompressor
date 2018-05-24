package com.yh.videorecordpickercompressor;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.util.Hashtable;

import com.yh.videorecordpickercompressor.videoview.PhoenixVideoView;

public class TakeVideoActivity extends Activity implements PhoenixVideoView.ControlListener {

    private static final String LOG_TAG = "TakeVideoActivity";

    public static final String EXTRA_OPTION_TYPE = "option_type";
    public static final String EXTRA_VIDEO_URI = "extra_video_uri";

    private OptionType mOptionType;
    private String mOutput;
    private String mVideoUri;

    private String mRotation;

    private PhoenixVideoView mVideo;

    public enum OptionType {
        SHOW, SHOW_4_RECORD, JUST_SHOW
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");
        immersiveStickyMode(true);

        Intent intent = getIntent();
        mOptionType = (OptionType) intent.getSerializableExtra(EXTRA_OPTION_TYPE);
        mOutput = intent.getStringExtra(MediaStore.EXTRA_OUTPUT);
        mVideoUri = intent.getStringExtra(EXTRA_VIDEO_URI);
        if (null == mOptionType) {
            finish();
            return;
        }
        if (TextUtils.isEmpty(mOutput) && TextUtils.isEmpty(mVideoUri)) {
            finish();
            return;
        }

        if (!TextUtils.isEmpty(mOutput)) {
            File checkF = new File(mOutput);
            if (!checkF.exists() || !checkF.isFile()) {
                getContentResolver().delete(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + " = ?", new String[]{mOutput});
                finish();
                return;
            }
        }
        loadVideoRotation();

        setContentView(R.layout.activity_take_video);

        initView();
        initListener();
        initData();

    }

    private void loadVideoRotation() {
        String videoPath = TextUtils.isEmpty(mOutput) ? mVideoUri : mOutput;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (videoPath.startsWith("http://") || videoPath.startsWith("https://") || videoPath.startsWith("widevine://")) {
                retriever.setDataSource(videoPath, new Hashtable<String, String>());
            } else {
                retriever.setDataSource(videoPath);
            }
            mRotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            if (mRotation.equals("0") || mRotation.equals("180")) {
                if (!TextUtils.isEmpty(h) && !TextUtils.isEmpty(w)) {
                    int vh = Integer.parseInt(h);
                    int vw = Integer.parseInt(w);
                    if (vh > vw) {
                        mRotation = "90";
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mRotation.equals("0") || mRotation.equals("180")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void immersiveStickyMode(boolean init) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            View v = window.getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (init) {
                window.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.setStatusBarColor(Color.TRANSPARENT);
                }
            }
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void initData() {
        mVideo.setVideoPath(TextUtils.isEmpty(mOutput) ? mVideoUri : mOutput);
        mVideo.seekTo(100);
    }

    private void initView() {
        mVideo = (PhoenixVideoView) findViewById(R.id.preview_video);
        mVideo.register(this);
    }

    private void initListener() {
        mVideo.setControlListener(this);
    }

    @Override
    protected void onResume() {
        immersiveStickyMode(false);
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        if (mVideo != null) {
            mVideo.onResume();
        }
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause");
        if (mVideo != null) {
            mVideo.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        if (mVideo != null) {
            mVideo.onPause();
        }
        super.onDestroy();
    }

    private void notifyVedio(File file) {
        if (OptionType.SHOW_4_RECORD != mOptionType) {
            return;
        }
        // 把文件插入到系统图库
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.TITLE, file.getName());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
    }

    @Override
    public void onCancel() {
        if (OptionType.JUST_SHOW != mOptionType) {
            checkNeedDel();
            setResult(Activity.RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public void onConfirm() {
        if (OptionType.JUST_SHOW != mOptionType) {
            File file = new File(mOutput);
            if (file.exists() && file.isFile()) {
                notifyVedio(file);
                Intent intent = new Intent();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutput);
                setResult(Activity.RESULT_OK, intent);
            }
        }
        finish();
    }


    private void checkNeedDel() {
        if (OptionType.SHOW_4_RECORD == mOptionType) {
            File f = new File(mOutput);
            boolean deleted = false;
            if (f.exists() && f.isFile()) {
                deleted = f.delete();
            }
            if (deleted) {
                getContentResolver().delete(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + "= ?", new String[]{mOutput});
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (null != mVideo && mVideo.isPlaying()) {
            mVideo.onPause();
            mVideo = null;
        }
        if (OptionType.JUST_SHOW != mOptionType) {
            setResult(Activity.RESULT_CANCELED);
        }
        finish();
    }
}
