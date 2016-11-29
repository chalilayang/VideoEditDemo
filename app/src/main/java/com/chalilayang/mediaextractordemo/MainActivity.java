package com.chalilayang.mediaextractordemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import com.chalilayang.mediaextractordemo.Adapters.VideosAdapter;
import com.chalilayang.mediaextractordemo.Utils.StorageEngine;
import com.chalilayang.mediaextractordemo.Utils.VideoThumbnailLoader;
import com.chalilayang.mediaextractordemo.entities.VideoData;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements VideosAdapter.onItemClickListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "MainActivity";

    public static final String KEY_FILE_NAME = "FILE_NAME";
    public static final String KEY_FILE_PATH = "FILE_PATH";
    private static final int MSG_IMGREAD = 32;
    public RecyclerView mRecyclerView;
    public DisplayMetrics mDisplayMetrics;
    private VideosAdapter videosAdapter;
    private List<VideoData> videoDatas = new ArrayList<>();
    private VideoThumbnailLoader loader;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_IMGREAD:
                    Log.i(TAG, "handleMessage: MSG_IMGREAD");
                    if (videosAdapter != null) {
                        int countOld = videosAdapter.getItemCount();
                        int newer = countOld - videoDatas.size();
                        if (newer > 0) {
                            Toast.makeText(getApplicationContext(), "新增" + newer + "条视频", Toast
                                    .LENGTH_SHORT).show();
                        }
                        videosAdapter.clearVideosList();
                        videosAdapter.addVideoList(videoDatas);
                        videosAdapter.notifyDataSetChanged();
                    } else {
                        Log.i(TAG, "handleMessage: videosAdapter == null");
                    }
                    break;
            }
        }
    };
    Runnable getVideosTask = new Runnable() {
        @Override
        public void run() {
            if (videoDatas != null) {
                Log.i(TAG, "run: ");
                List<VideoData> tmpList = getData();
                videoDatas.clear();
                videoDatas.addAll(tmpList);
                putBitmapToCache();
                handler.obtainMessage(MSG_IMGREAD).sendToTarget();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecordActivity();
            }
        });
        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab_edit_open_dir);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(mRecyclerView, "open dir", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
        initMetrics();
        init();
    }

    private void initMetrics() {
        mDisplayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        Log.i("displayMetrics", mDisplayMetrics.toString());
    }

    private void putBitmapToCache() {
        if (videoDatas != null) {
            for (int index = 0, count = videoDatas.size(); index < count; index++) {
                Log.i(TAG, "putBitmapToCache: index " + index);
                loader.notifyBitmap(videoDatas.get(index).filePath);
            }
        }
    }

    public void freshVideos() {
        Log.i(TAG, "freshVideos: ");
        new Thread(getVideosTask).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loader == null) {
            loader = VideoThumbnailLoader.getInstance(getApplicationContext());
        }
        freshVideos();
    }

    private void init() {
        if (videosAdapter == null) {
            videosAdapter = new VideosAdapter(getApplicationContext());
        }
        if (mRecyclerView == null) {
            mRecyclerView = (RecyclerView) findViewById(R.id.activity_main_recyclerview);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutAnimation(getAnimationController());
        }

        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(1,
                StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(videosAdapter);
        videosAdapter.setOnItemClickListener(this);
        videosAdapter.setOnItemClickListener(this);
    }

    private void startRecordActivity() {
        Intent intent = new Intent(this, CaptureActivity.class);
        this.startActivity(intent);
    }

    public List<VideoData> getData() {
        List<VideoData> list = new ArrayList<>();
        File dir = StorageEngine.getDownloadFolder(getApplicationContext());
//        dir = new File(StorageEngine.getDownloadRootPath(getApplicationContext()));
        if (dir.exists()) {
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.toLowerCase().endsWith("mp4")) {
                        return true;
                    }
                    return false;
                }
            });
            if (files != null) {
                for (int index = 0, count = files.length; index < count; index++) {
                    if (files[index].length() <= 0) {
                        files[index].delete();
                        continue;
                    }
                    list.add(new VideoData(files[index].getAbsolutePath(), files[index].getName()));
                }
            }
        }
        return list;
    }

    /**
     * Layout动画
     *
     * @return
     */
    protected LayoutAnimationController getAnimationController() {
        int duration = 600;
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(duration);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(duration);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        return controller;
    }

    @Override
    public void onItemClick(View view, int position) {
        if (videoDatas != null
                && videoDatas.size() > position) {
            VideoData video = videoDatas.get(position);
            startPlayActivity(video);
        }
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        menu(view, position);
        return true;
    }

    int selecttion = -1;
    void menu(View view, int position) {
        selecttion = position;
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.activity_flow_popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_video:
                if (selecttion >= 0
                        && videoDatas != null
                        && videoDatas.size() > selecttion) {
                    VideoData video = videoDatas.get(selecttion);
                    startVideoEditActivity(video);
                    return true;
                }
            case R.id.action_delete_video:
                if (selecttion >= 0
                        && videoDatas != null
                        && videoDatas.size() > selecttion) {
                    VideoData video = videoDatas.get(selecttion);
                    File file = new File(video.filePath);
                    file.delete();
                    videoDatas.remove(selecttion);
                    videosAdapter.removeVideo(selecttion);
                    return true;
                }

        }
        return false;
    }

    private void startVideoEditActivity(VideoData video) {
        Intent intent = new Intent(this, VideoEditActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_FILE_NAME, video.fileName);
        bundle.putString(KEY_FILE_PATH, video.filePath);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void startPlayActivity(VideoData video) {
        Intent intent = new Intent(this, FullscreenActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_FILE_NAME, video.fileName);
        bundle.putString(KEY_FILE_PATH, video.filePath);
        intent.putExtras(bundle);
        startActivity(intent);
//        Uri uri = Uri.parse(video.filePath);
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, "video/mp4");
//        startActivity(intent);
    }
}
