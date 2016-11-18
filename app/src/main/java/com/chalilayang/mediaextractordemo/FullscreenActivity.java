package com.chalilayang.mediaextractordemo;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chalilayang.mediaextractordemo.Utils.StringUtil;
import com.chalilayang.mediaextractordemo.Utils.TimeUtil;
import com.chalilayang.mediaextractordemo.constants.Constants;
import com.chalilayang.mediaextractordemo.entities.VideoData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @Description 
 * @author chalilayang
 * @time 2016/11/15 16:49
 * 
 */
public class FullscreenActivity extends AppCompatActivity
        implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "FullscreenActivity";

    private static final int MSG_SURFACE_READY = 32;
    private static final int MSG_PREPARED = 33;
    private static final int MSG_UPDATE = 39;
    private static final int MSG_SEEK = 40;

    private VideoData videoToPlay;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SURFACE_READY:
                    if (player != null) {
                        Log.i(TAG, "handleMessage: MSG_SURFACE_READY ");
                        player.reset();
                        player.setDisplay(surfaceView.getHolder());
                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    }
                    playBtn.setEnabled(true);
                    break;
                case MSG_PREPARED:
                    if (player != null) {
                        Log.i(TAG, "handleMessage: MSG_PREPARED " + player.getDuration());
                        duration = player.getDuration();
                        durationTime.setText(TimeUtil.getFormatTime(duration));
                        player.start();
                        this.sendMessageDelayed(this.obtainMessage(MSG_UPDATE), 300);
                    }
                    break;
                case MSG_UPDATE:
                    updatePosition();
                    this.sendMessageDelayed(this.obtainMessage(MSG_UPDATE), 300);
                    break;
                case MSG_SEEK:
                    if (player != null) {
                        int pos = Math.round(msg.arg1 * duration / 100.0f);
                        player.seekTo(pos);
                    }
                    break;
            }
        }
    };
    private SurfaceView surfaceView;
    MediaPlayer player;

    ImageButton playBtn;
    SeekBar seekBar;
    TextView seekTime;
    TextView durationTime;

    private static final int STATE_IDLE = 1;
    private static final int STATE_PLAYING = STATE_IDLE + 1;
    private static final int STATE_PAUSING = STATE_PLAYING + 1;



    @IntDef({STATE_IDLE, STATE_PLAYING, STATE_PAUSING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayBackState {}

    private @PlayBackState int playBackState = STATE_IDLE;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            surfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_fullscreen);
        initData();
        Log.i(TAG, "onCreate: ");
        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(this);
        seekTime = (TextView) findViewById(R.id.seek_time);
        seekTime.setText(TimeUtil.getFormatTime(0));
        durationTime = (TextView) findViewById(R.id.duration_time);
        durationTime.setText(TimeUtil.getFormatTime(0));
        playBtn = (ImageButton) findViewById(R.id.play_button);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        hide();
        initMediaPlayer();
        playBtn.setEnabled(false);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playBackState) {
                    case STATE_IDLE:
                        playBackState = STATE_PLAYING;
                        playBtn.setImageResource(android.R.drawable.ic_media_pause);
                        play();
                        break;
                    case STATE_PAUSING:
                        playBackState = STATE_PLAYING;
                        playBtn.setImageResource(android.R.drawable.ic_media_pause);
                        resumePlay();
                        break;
                    case STATE_PLAYING:
                        playBackState = STATE_PAUSING;
                        playBtn.setImageResource(android.R.drawable.ic_media_play);
                        pausePlay();
                        break;
                }
            }
        });
    }

    private void initMediaPlayer() {
        this.player = new MediaPlayer();
        this.player.setOnPreparedListener(this);
        this.player.setOnErrorListener(this);
        this.player.setOnBufferingUpdateListener(this);
    }
    private void initData() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String name = b.getString(MainActivity.KEY_FILE_NAME);
            String path = b.getString(MainActivity.KEY_FILE_PATH);
            videoToPlay = new VideoData(path, name);
        }
    }
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        surfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void play() {
        try {
            if (player != null) {
                Log.i(TAG, "play: ");
                if (videoToPlay != null) {
                    player.setDataSource(videoToPlay.filePath);
                } else {
                    player.setDataSource(Constants.playurl_2);
                }
                player.prepareAsync();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private void resumePlay() {
        try {
            if (player != null) {
                player.start();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private void pausePlay() {
        try {
            if (player != null) {
                player.pause();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    private int duration = 0;
    public void updatePosition() {
        if (player != null) {
            int cur = player.getCurrentPosition();
            int ration = Math.round(cur * 100.0f / duration);
            seekTime.setText(TimeUtil.getFormatTime(cur));
            seekBar.setProgress(ration);
        }
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated: ");
        mHideHandler.obtainMessage(MSG_SURFACE_READY).sendToTarget();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "onPrepared: ");
        mHideHandler.obtainMessage(MSG_PREPARED).sendToTarget();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.i(TAG, "onBufferingUpdate: ");
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i(TAG, "onError: ");
        Toast.makeText(this, "播放器错误 ：" + what + "  " + extra, Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (player != null && fromUser) {
            Log.i(TAG, "onProgressChanged: " + progress);
            mHideHandler.removeMessages(MSG_SEEK);
            Message msg = mHideHandler.obtainMessage(MSG_SEEK);
            msg.arg1 = progress;
            mHideHandler.sendMessage(msg);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
