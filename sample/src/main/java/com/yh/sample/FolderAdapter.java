package com.yh.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;


/**
 * 文件夹Adapter
 */
public class FolderAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private DisplayImageOptions mOptions;

    private List<Folder> mFolders;

    public FolderAdapter(Context context){
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        builder.cacheInMemory(true)
                .showStubImage(R.drawable.default_img);
        mOptions = builder.build();
    }

    public void setFolders(List<Folder> folders) {
        mFolders = folders;
    }

    @Override
    public int getCount() {
        if(mFolders == null)
            return 0;
        return mFolders.size();
    }

    @Override
    public Folder getItem(int i) {
        if(mFolders == null)
            return null;
        return mFolders.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = mInflater.inflate(R.layout.list_item_folder, viewGroup, false);
        }

        Folder folder = mFolders.get(i);
        TextView tv = (TextView) view.findViewById(R.id.name);
        tv.setText(folder.name);
        tv = (TextView) view.findViewById(R.id.path);
        tv.setText(folder.path);
        tv = (TextView) view.findViewById(R.id.size);
        tv.setText(String.format("%d张", folder.files.size()));

        ImageView iv = (ImageView) view.findViewById(R.id.cover);
        String coverPath = folder.files.get(0);
        ImageLoader.getInstance().displayImage(
                "file://" + coverPath, iv, mOptions);

        ListView lv = (ListView)viewGroup;
        view.findViewById(R.id.indicator).setVisibility(lv.isItemChecked(i) ?
                                                        View.VISIBLE : View.INVISIBLE);

        return view;
    }
}
