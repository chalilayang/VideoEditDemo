package com.chalilayang.mediaextractordemo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.chalilayang.mediaextractordemo.Adapters.SegmentRecycleAdapter;
import com.chalilayang.mediaextractordemo.entities.VideoData;
import com.chalilayang.mediaextractordemo.entities.VideoEditRepository;
import com.chalilayang.mediaextractordemo.entities.VideoSegment;
import com.chalilayang.mediaextractordemo.ui.VideoEditPreView;

import java.io.File;

public class MediaCodecPlayer extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener,
        VideoEditPreView.onPlayBacKListener {
    private static final String TAG = "MediaCodecPlayer";

    public DisplayMetrics mDisplayMetrics;
    private VideoEditPreView videoEditPreView;
    private SeekBar seekBar;
    private int screenWidthPx;
    private int screenHeightPx;
    private VideoData videoToPlay;
    private View preViewContainer;
    private RecyclerView recyclerView;
    private SegmentRecycleAdapter segmentRecycleAdapter;

    private VideoEditRepository repository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_player);
        initMetrics();
        initView();
        initData();
    }
    public static String getVideoPath(Context context, Uri uri) {
        Uri videopathURI = uri;
        if (uri.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                videopathURI = Uri.parse(cursor.getString(column_index));
                return videopathURI.getPath();
            }
        } else if (uri.getScheme().compareTo("file") == 0) {
            return videopathURI.getPath();
        }

        return videopathURI.toString();
    }

    private void initData() {
        Log.i(TAG, "initData: ");
        Intent intent = getIntent();
        if (intent.getAction() != null &&
                Intent.ACTION_VIEW.equals(intent.getAction())) {
            String path = getVideoPath(getApplicationContext(), intent.getData());
            String name = path.substring(path.lastIndexOf(File.separator));
            videoToPlay = new VideoData(path, name);
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String name = b.getString(MainActivity.KEY_FILE_NAME);
            String path = b.getString(MainActivity.KEY_FILE_PATH);
            videoToPlay = new VideoData(path, name);
        }
        videoEditPreView.setVideoPath(videoToPlay.filePath);
        repository = VideoEditRepository.getInstance();
        repository.addSegment(new VideoSegment(videoToPlay, 0, 2));
        repository.addSegment(new VideoSegment(videoToPlay, 0, 2));
        repository.addSegment(new VideoSegment(videoToPlay, 0, 2));

        segmentRecycleAdapter.loadVideoSegments(repository.getSegmentList());
    }

    private void initMetrics() {
        Log.i(TAG, "initMetrics: ");
        mDisplayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        screenWidthPx = mDisplayMetrics.widthPixels;
        screenHeightPx = mDisplayMetrics.heightPixels;
        Log.i("displayMetrics", mDisplayMetrics.toString());
    }

    private void initView() {
        Log.i(TAG, "initView: ");
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
        preViewContainer = findViewById(R.id.preview_container);
        videoEditPreView = (VideoEditPreView) findViewById(R.id.preview);
        videoEditPreView.setOnPlayBackPositionListener(this);
        preViewContainer.getLayoutParams().height = screenHeightPx / 3;
        preViewContainer.getLayoutParams().width = screenWidthPx;
        preViewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isPlaying = videoEditPreView.isPlaying();
                if (!isPlaying) {
                    videoEditPreView.play();
                } else {
                    videoEditPreView.pause();
                }
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(VideoEditPreView.MAX);
        ((RelativeLayout.LayoutParams) seekBar.getLayoutParams()).topMargin = screenHeightPx / 3;

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(1,
                StaggeredGridLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        int itemWidth = (int)(screenWidthPx * 0.2);
        segmentRecycleAdapter = new SegmentRecycleAdapter(getApplicationContext(), itemWidth);
        recyclerView.setAdapter(segmentRecycleAdapter);
        segmentRecycleAdapter.setOnItemClickListener(new SegmentRecycleAdapter.onItemClickListener() {


            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getApplicationContext(),
                        "onItemClick " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemDelete(View view, int position) {
                Toast.makeText(getApplicationContext(),
                        "onItemDelete " + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.i(TAG, "onProgressChanged: ");
        if (fromUser) {
            videoEditPreView.seek(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onUpdatePosition(long presentTime, long duration) {
        Log.i(TAG, "onUpdatePosition: presentTime " + presentTime + " duration  " + duration);
        if (seekBar != null) {
            int dd = (int)(presentTime * seekBar.getMax() / duration);
            seekBar.setProgress(dd);
        }
    }

    @Override
    public void onError(int error, String message) {
        Log.i(TAG, "onError: error " + error + " message  " + message);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        finish();
    }
}
