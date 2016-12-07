package com.chalilayang.mediaextractordemo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.chalilayang.mediaextractordemo.Utils.FileUtils;
import com.chalilayang.mediaextractordemo.Utils.StorageEngine;
import com.chalilayang.mediaextractordemo.Utils.SystemClock;
import com.chalilayang.mediaextractordemo.Utils.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CaptureActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    public static final int UPDATE_TIME = 0; // 更新录音时间的消息编号
    public static final int ROTATION_HINT = 90;
    private static final String TAG = "CaptureActivity";
    private static final int MSG_SURFACE_CREATED = 35;
    SurfaceView preSurfaceView; // 摄像预览用的SurfaceView
    Handler handler; // 消息处理器
    private SurfaceHolder my_SurfaceHolder;
    private FloatingActionButton bt_Start; // 开始录制按钮
//    private FloatingActionButton bt_Stop; // 停止录制按钮
    private MediaRecorder mMediaRecorder; // 多媒体录制器
    private Camera camera;
    private TextView tv_Time; // 显示录制时间的文本View

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_capture);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        preSurfaceView = (SurfaceView) this.findViewById(R.id.mySurfaceView); // 初始化
        // SurfaceHolder
        my_SurfaceHolder = preSurfaceView.getHolder();
        my_SurfaceHolder.addCallback(this);
        bt_Start = (FloatingActionButton) this.findViewById(R.id.ImageButton01);
        bt_Start.setEnabled(false);
//        bt_Stop = (FloatingActionButton) this.findViewById(R.id.ImageButton02);
//        bt_Stop.setEnabled(false);
        bt_Start.setOnClickListener(this);
//        bt_Stop.setOnClickListener(this);
        tv_Time = (TextView) this.findViewById(R.id.tvTime); // 拿到显示录制时长的文本框的引用
        handler = new Handler() { // 线程中创建一个Handler
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg); // 调用父类处理
                switch (msg.what) { // 根据不同的消息进行不同的处理
                    case UPDATE_TIME: // 将消息中的内容提取出来
                        setTime();
                        Message newmsg = this.obtainMessage(UPDATE_TIME);
                        this.sendMessageDelayed(newmsg, 900);
                        break;
                    case MSG_SURFACE_CREATED:
                        prepareVideoRecorder();
                        bt_Start.setEnabled(true);
//                        bt_Stop.setEnabled(true);
                        break;
                }
            }
        };
    }

    private volatile boolean isRecording = false;
    private volatile long startPointTimeMs = 0L;

    @Override
    public void onClick(View v) {
        if (v == bt_Start) { // 按下了开始录制按钮
            if (isRecording) { // 若录制器不为空则报错并返回
                bt_Start.setImageResource(R.drawable.ic_action_record_start);
                isRecording = false;
                handler.removeCallbacksAndMessages(null);
                releaseMediaRecorder();
                releaseCamera();
                startPointTimeMs = 0L; // 计时器清0
                finish();
                return;
            }
            if (!checkCameraHardware(getApplicationContext())) {
                Toast.makeText(this, "设备没有摄像头！", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            try {
                isRecording = true;
                if (mMediaRecorder == null) {
                    prepareVideoRecorder();
                }
                mMediaRecorder.setPreviewDisplay(preSurfaceView.getHolder().getSurface());
                mMediaRecorder.start(); // 开始录制
                bt_Start.setImageResource(R.drawable.ic_action_record_stop);
                startPointTimeMs = SystemClock.get().now();
                handler.obtainMessage(UPDATE_TIME).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "录制故障：" + e.toString(), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public void setTime() { // 设置显示时间的方法
        tv_Time.setText(TimeUtil.getFormatTime(SystemClock.get().now() - startPointTimeMs));
    }

    private boolean prepareVideoRecorder() {

        camera = getCameraInstance();
        camera.setDisplayOrientation(ROTATION_HINT);//摄像图旋转90度
        try {
            camera.setPreviewDisplay(preSurfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Camera.Parameters parameters = camera.getParameters();
        parameters.setRecordingHint(true);
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        camera.setParameters(parameters);
        camera.startPreview();

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mMediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 设置输出格式为mp4
        mMediaRecorder.setVideoSize(1184, 720); // 设置视频大小
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);// 设置视频编码
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);// 设置音频编码
        mMediaRecorder.setAudioSamplingRate(8000);
        mMediaRecorder.setMaxDuration(100000); // 设置最大时长
//        mMediaRecorder.setOrientationHint(ROTATION_HINT);


        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile().getAbsolutePath());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private File getOutputMediaFile() {
        File outputFile = StorageEngine.getDownloadFile(getApplicationContext()
                , FileUtils.generateNameByDate() + ".mp4");
        Log.i(TAG, "createTempFile: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        handler.obtainMessage(MSG_SURFACE_CREATED).sendToTarget();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
