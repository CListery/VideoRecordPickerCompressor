package com.yh.sample;

import android.app.Application;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Created by Clistery on 18-5-24.
 */
public class App extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
    
        initImageLoader();
        
    }
    
    private void initImageLoader() {
        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        builder.cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)// 保留图片文件头信息
                .showImageForEmptyUri(R.drawable.default_img)
                .showImageOnFail(R.drawable.default_img);
        ImageLoaderConfiguration conf = new ImageLoaderConfiguration
                .Builder(this)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheSize(100 * 1024 * 1024)
                .memoryCacheSize(50 * 1024 * 1024)
                .defaultDisplayImageOptions(builder.build())
                //                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(conf);
    }
}
