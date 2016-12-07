package com.chalilayang.mediaextractordemo;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.chalilayang.mediaextractordemo.entities.VideoData;
import com.chalilayang.mediaextractordemo.ui.VideoEditPreView;

public class MediaCodecPlayer extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MediaCodecPlayer";

    public DisplayMetrics mDisplayMetrics;
    private VideoEditPreView videoEditPreView;
    private SeekBar seekBar;
    private int screenWidthPx;
    private int screenHeightPx;
    private VideoData videoToPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_player);
        initMetrics();
        initView();
        initData();
    }

    private void initData() {
        Log.i(TAG, "initData: ");
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String name = b.getString(MainActivity.KEY_FILE_NAME);
            String path = b.getString(MainActivity.KEY_FILE_PATH);
            videoToPlay = new VideoData(path, name);
            videoEditPreView.setVideoPath(videoToPlay.filePath);
        }
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
        videoEditPreView = (VideoEditPreView) findViewById(R.id.preview);
        videoEditPreView.getLayoutParams().height = screenHeightPx / 3;
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(VideoEditPreView.MAX);
        ((RelativeLayout.LayoutParams) seekBar.getLayoutParams()).topMargin = screenHeightPx / 8;
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
}
