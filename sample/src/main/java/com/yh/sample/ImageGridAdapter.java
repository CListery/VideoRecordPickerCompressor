package com.yh.sample;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.HashSet;
import java.util.List;


public class ImageGridAdapter extends BaseAdapter {

    private static final int TYPE_CAMERA = 0;
    private static final int TYPE_NORMAL = 1;

    private DisplayImageOptions mOptions;

    private LayoutInflater mInflater;
    private boolean showCamera = true;
    private boolean singleChoice;

    private List<String> mImages;
    private HashSet<Integer> mSelected = new HashSet<>();
    private boolean mIsVideo;

    public ImageGridAdapter(Context context, boolean isVideo) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        builder.cacheInMemory(true).showStubImage(R.drawable.small_default);
        mOptions = builder.build();
        mIsVideo = isVideo;
    }

    public void setShowCamera(boolean b) {
        if (showCamera == b) {
            return;
        }

        showCamera = b;
        notifyDataSetChanged();
    }

    public boolean isShowCamera() {
        return showCamera;
    }

    public void setSingleChoice(boolean singleChoice) {
        this.singleChoice = singleChoice;
    }

    /**
     * 选择某个图片，改变选择状态
     *
     * @param image 选择的图片位置
     */
    public void select(int image) {
        if (mSelected.contains(image)) {
            mSelected.remove(image);
        } else {
            mSelected.add(image);
        }
        notifyDataSetChanged();
    }


    /**
     * 设置数据集
     *
     * @param images 图片路径
     */
    public void setData(List<String> images) {
        mSelected.clear();
        mImages = images;

        notifyDataSetChanged();
    }

    public HashSet<Integer> getSelected() {
        return mSelected;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera) {
            return position == 0 ? TYPE_CAMERA : TYPE_NORMAL;
        }
        return TYPE_NORMAL;
    }

    @Override
    public int getCount() {
        int count = mImages == null ? 0 : mImages.size();
        return showCamera ? count + 1 : count;
    }

    @Override
    public Object getItem(int i) {
        if (showCamera) {
            if (i == 0) {
                return null;
            }
            return mImages.get(i - 1);
        } else {
            return mImages.get(i);
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (position == 0 && showCamera) {
            if (view == null) {
                view = mInflater.inflate(R.layout.list_item_camera, viewGroup, false);
                int width;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    width = ((GridView) viewGroup).getColumnWidth();
                } else {
                    GridView gv = (GridView) viewGroup;
                    int cc = gv.getNumColumns();
                    int gap = (int) gv.getResources().getDisplayMetrics().density * 2;
                    width = (gv.getWidth() - (cc - 1) * gap) / cc;
                }
                view.getLayoutParams().width = width;
                view.getLayoutParams().height = width;
                if (mIsVideo) {
                    TextView tvBtn = view.findViewById(R.id.tv_btn);
                    tvBtn.setText("拍摄视频");
                }
            }
            return view;
        }

        ViewHolder holder = null;
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_image, viewGroup, false);
            holder = new ViewHolder(view);
            int width;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                width = ((GridView) viewGroup).getColumnWidth();
            } else {
                GridView gv = (GridView) viewGroup;
                int cc = gv.getNumColumns();
                int gap = (int) gv.getResources().getDisplayMetrics().density * 2;
                width = (gv.getWidth() - (cc - 1) * gap) / cc;
            }
            view.getLayoutParams().width = width;
            view.getLayoutParams().height = width;
            holder.changeViewParams(width);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (null != holder) {
            holder.changeViewVisibility(position);
            int p = showCamera ? position - 1 : position;
            ImageLoader.getInstance().displayImage("file://" + mImages.get(p), holder.image, mOptions);
        }

        return view;
    }

    private class ViewHolder {
        private ImageView image;
        private ImageView btnPlay;
        private ImageView checkmark;

        public ViewHolder(View view) {
            image = view.findViewById(R.id.image);
            btnPlay = view.findViewById(R.id.play_btn);
            checkmark = view.findViewById(R.id.checkmark);
        }

        void changeViewParams(int width) {
            image.getLayoutParams().width = width;
            image.getLayoutParams().height = width;
            btnPlay.getLayoutParams().height = width;
            btnPlay.getLayoutParams().width = width;
        }

        void changeViewVisibility(int position) {
            if (mIsVideo) {
                btnPlay.setVisibility(View.VISIBLE);
            } else {
                btnPlay.setVisibility(View.GONE);
            }
            if (singleChoice) {
                checkmark.setVisibility(View.GONE);
            } else {
                if (mSelected.contains(position)) {
                    checkmark.setImageResource(R.drawable.photo_sel_photo_mark);
                    image.setAlpha(1f);
                } else {
                    checkmark.setImageResource(R.drawable.photo_no);
                    image.setAlpha(1f);
                }
            }
        }
    }

}
