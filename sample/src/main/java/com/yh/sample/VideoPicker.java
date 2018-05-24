package com.yh.sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.iceteck.silicompressorr.SiliCompressor;
import com.yh.videorecordpickercompressor.TakeVideoActivity;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;


/**
 * Created at 2018/4/16.
 *
 * @author CListery
 */
public class VideoPicker extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
    public static final String EXTRA_MAX = "max";
    public static final String EXTRA_WITH_CAMERA = "camera";
    public static final String EXTRA_RESULT = "result";
    
    public static final int REQUEST_CODE_CAMERA = 0x120;
    public static final int REQUEST_CODE_CONFIRM = 0x121;
    
    public static final int RESULT_CANCELED = 0x130;
    public static final int RESULT_OK = 0x131;
    
    public static final int DEF_MAX_COUNT = 1;
    
    private int mMax = DEF_MAX_COUNT;
    
    private ArrayList<Folder> mFolders;
    private PopupWindow mFolderPopupWindow;
    private PopupWindow mMaxtipPop;
    private ImageGridAdapter mAdapter;
    private File mVedio;
    
    private TextView mCategoryText;
    private TextView mDone;
    private TextView mTitle;
    
    private LinearLayout llRoot;
    private ProgressDialog mDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        boolean withCamera = intent.getBooleanExtra(EXTRA_WITH_CAMERA, true);
        mMax = intent.getIntExtra(EXTRA_MAX, -1);
        
        setContentView(R.layout.video_picker);
        
        findViewById(R.id.iv_back).setOnClickListener(this);
        mTitle = (TextView) findViewById(R.id.tv_middle_title);
        mDone = (TextView) findViewById(R.id.bt_commit);
        llRoot = (LinearLayout) findViewById(R.id.ll_root);
        mDone.setOnClickListener(this);
        mCategoryText = (TextView) findViewById(R.id.tv_category);
        mCategoryText.setOnClickListener(this);
        mAdapter = new ImageGridAdapter(this, true);
        mAdapter.setShowCamera(withCamera);
        mAdapter.setSingleChoice(mMax == 1);
        GridView gv = (GridView) findViewById(R.id.gv_images);
        gv.setAdapter(mAdapter);
        gv.setOnItemClickListener(this);
        new Loader().execute();
    }
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_back) {
            finish();
            setResult(RESULT_CANCELED);
        } else if (id == R.id.bt_commit) {
            HashSet<Integer> selected = mAdapter.getSelected();
            if (selected.size() == 0) {
                Toast.makeText(this, "请至少选择一段视频.", Toast.LENGTH_SHORT).show();
                return;
            }
            TreeMap<Integer, String> map = new TreeMap<>();
            for (Integer index : selected) {
                map.put(index, (String) mAdapter.getItem(index));
            }
            ArrayList<String> result = new ArrayList<>(map.values());
            Intent intent = new Intent();
            intent.putStringArrayListExtra(EXTRA_RESULT, result);
            setResult(RESULT_OK, intent);
            finish();
        } else if (id == R.id.tv_category) {
            if (mFolderPopupWindow != null && mFolderPopupWindow.isShowing()) {
                mFolderPopupWindow.dismiss();
            } else {
                showFolderList();
            }
        }
    }
    
    private void showFolderList() {
        if (mFolderPopupWindow == null) {
            mFolderPopupWindow = createPopupFolderList();
        }
        mFolderPopupWindow.showAsDropDown(mCategoryText);
    }
    
    private PopupWindow createPopupFolderList() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        FolderAdapter folderAdapter = new FolderAdapter(this);
        folderAdapter.setFolders(mFolders);
        ListView lv = new ListView(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setAdapter(folderAdapter);
        lv.setItemChecked(0, true);
        popupWindow.setContentView(lv);
        popupWindow.setWidth(width);
        popupWindow.setHeight(height);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Folder folder = (Folder) adapterView.getItemAtPosition(i);
                mCategoryText.setText(folder.name);
                mAdapter.setData(folder.files);
                updateFinishButton();
                
                adapterView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFolderPopupWindow.dismiss();
                    }
                }, 100);
            }
        });
        return popupWindow;
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter.isShowCamera()) {
            if (position == 0) {
                startCamera();
                return;
            }
        }
        
        HashSet<Integer> selected = mAdapter.getSelected();
        if (selected.size() == mMax && !selected.contains(position)) {
            showMaxTips();
        } else {
            startShow(position);
        }
        
        if (mMax > 1) {
            updateFinishButton();
        }
    }
    
    private void startShow(int position) {
        String path = (String) mAdapter.getItem(position);
        mVedio = new File(path);
        Intent intent = new Intent(this, TakeVideoActivity.class);
        intent.putExtra(TakeVideoActivity.EXTRA_OPTION_TYPE, TakeVideoActivity.OptionType.SHOW);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, path);
        startActivityForResult(intent, REQUEST_CODE_CONFIRM);
    }
    
    TextView tv_msg;
    
    private void showMaxTips() {
        if (mMaxtipPop == null) {
            View popRoot = getLayoutInflater().inflate(R.layout.pop_max_tips, null, false);
            tv_msg = (TextView) popRoot.findViewById(R.id.tv_msg);
            mMaxtipPop = new PopupWindow(popRoot, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,
                    true);
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMaxtipPop.dismiss();
                }
            };
            popRoot.findViewById(R.id.tv_submit).setOnClickListener(onClickListener);
            popRoot.setOnClickListener(onClickListener);
        }
        tv_msg.setText(String.format(Locale.CHINESE, "你最多只能选择%d段视频", mMax));
        mMaxtipPop.showAtLocation(llRoot, Gravity.CENTER, 0, 0);
    }
    
    private void updateFinishButton() {
        HashSet<Integer> selected = mAdapter.getSelected();
        int count = selected.size();
        if (count == 0) {
            mDone.setEnabled(false);
            mTitle.setText(String.format("选择视频(%s/%s)", count, mMax));
            return;
        }
        
        if (mMax == -1) {
            mTitle.setText(String.format("选择视频(%s)", count));
        } else {
            mTitle.setText(String.format("选择视频(%s/%s)", count, mMax));
        }
        mDone.setEnabled(true);
    }
    
    private void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mVedio = getCameraOutputFile(false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mVedio));
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }
    
    private File getCameraOutputFile(boolean random) {
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            dir = new File(dir, "Camera");
        } else {
            dir = getCacheDir();
        }
        
        String fileName = String.format(Locale.CHINESE,
                "IMG_%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS%1$tL" + (random ? new Random().nextLong() : "") + ".mp4",
                Calendar.getInstance());
        return new File(dir, fileName);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Intent intent = null;
            File output = null;
            switch (requestCode) {
                case REQUEST_CODE_CAMERA:
                    intent = new Intent(this, TakeVideoActivity.class);
                    intent.putExtra(TakeVideoActivity.EXTRA_OPTION_TYPE, TakeVideoActivity.OptionType.SHOW_4_RECORD);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mVedio.getAbsolutePath());
                    startActivityForResult(intent, REQUEST_CODE_CONFIRM);
                    break;
                case REQUEST_CODE_CONFIRM:
                    output = getCameraOutputFile(true);
                    new VideoCompressAsyncTask(this).execute(mVedio.getAbsolutePath(), output.getParent());
                    break;
            }
        } else {
            mVedio = null;
        }
    }
    
    private void showWaitingDialog() {
        Activity act = VideoPicker.this;
        if (null == mDialog) {
            mDialog = new ProgressDialog(act);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setIndeterminate(true);
            mDialog.setMessage("视频处理中...");
        }
        if (!act.isFinishing() && !act.isDestroyed() && null != mDialog && !mDialog.isShowing()) {
            mDialog.show();
        }
    }
    
    private void dismissWaitingDialog() {
        Activity act = VideoPicker.this;
        if (act.isDestroyed() || act.isFinishing()) {
            return;
        }
        if (null != mDialog && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissWaitingDialog();
    }
    
    private class Loader extends AsyncTask<Void, Void, ArrayList<Folder>> {
        @Override
        protected ArrayList<Folder> doInBackground(Void... params) {
            Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.MediaColumns.DATE_ADDED + " desc");
            ArrayList<Folder> folders = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    Folder all = new Folder();
                    all.name = "所有视频";
                    all.path = "/";
                    HashMap<String, Folder> map = new HashMap<>();
                    cursor.moveToFirst();
                    int indexData = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATA);
                    do {
                        String path = cursor.getString(indexData);
                        File checkF = new File(path);
                        if (!checkF.exists() || !checkF.isFile()) {
                            getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    MediaStore.Video.Media.DATA + " = ?", new String[]{path});
                            continue;
                        }
                        
                        all.files.add(path);
                        int index = path.lastIndexOf('/');
                        if (index > 0) {
                            String dir = path.substring(0, index);
                            Folder folder = map.get(dir);
                            if (folder == null) {
                                folder = new Folder();
                                folder.path = dir;
                                index = dir.lastIndexOf('/');
                                String name;
                                if (index == -1) {
                                    name = dir;
                                } else {
                                    name = dir.substring(index + 1);
                                }
                                folder.name = name;
                                map.put(dir, folder);
                            }
                            folder.files.add(path);
                        }
                    } while (cursor.moveToNext());
                    folders = new ArrayList<>(map.size());
                    folders.addAll(map.values());
                    Collections.sort(folders, new Comparator<Folder>() {
                        @Override
                        public int compare(Folder lhs, Folder rhs) {
                            return lhs.path.compareTo(rhs.path);
                        }
                    });
                    folders.add(0, all);
                }
                cursor.close();
            }
            return folders;
        }
        
        @Override
        protected void onPostExecute(ArrayList<Folder> folders) {
            if (folders == null) {
                mFolders = new ArrayList<>();
                mAdapter.setData(new ArrayList<String>());
                return;
            }
            mFolders = folders;
            if (folders.get(0) != null) {
                mAdapter.setData(folders.get(0).files);
            }
        }
    }
    
    private class VideoCompressAsyncTask extends AsyncTask<String, String, String> {
        
        private Context mContext;
        
        public VideoCompressAsyncTask(Context context) {
            mContext = context;
        }
        
        @Override
        protected void onPreExecute() {
            showWaitingDialog();
        }
        
        @Override
        protected String doInBackground(String... paths) {
            String filePath = null;
            try {
                filePath = SiliCompressor.with(mContext).compressVideo(paths[0], paths[1]);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return filePath;
        }
        
        
        @Override
        protected void onPostExecute(String compressedFilePath) {
            dismissWaitingDialog();
            ArrayList<String> result = new ArrayList<>(1);
            result.add(compressedFilePath);
            Intent intent = new Intent();
            intent.putStringArrayListExtra(EXTRA_RESULT, result);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
    
}
