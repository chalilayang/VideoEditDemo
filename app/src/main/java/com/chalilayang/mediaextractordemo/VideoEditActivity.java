package com.chalilayang.mediaextractordemo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chalilayang.mediaextractordemo.Utils.AvcEncoder;
import com.chalilayang.mediaextractordemo.Utils.CameraToMpegTest;
import com.chalilayang.mediaextractordemo.Utils.ExtractDecodeEditEncodeMuxTest;
import com.chalilayang.mediaextractordemo.Utils.FileUtils;
import com.chalilayang.mediaextractordemo.Utils.SDCardUtil;
import com.chalilayang.mediaextractordemo.Utils.StorageEngine;
import com.chalilayang.mediaextractordemo.Utils.TimeUtil;
import com.chalilayang.mediaextractordemo.Utils.VideoDecoder;
import com.chalilayang.mediaextractordemo.Utils.VideoUtils;
import com.chalilayang.mediaextractordemo.aidl.BinderPool;
import com.chalilayang.mediaextractordemo.aidl.BinderPoolImpl;
import com.chalilayang.mediaextractordemo.aidl.VideoEditManagerImpl;
import com.chalilayang.mediaextractordemo.entities.SrtEntity;
import com.chalilayang.mediaextractordemo.entities.VideoCodecModel;
import com.chalilayang.mediaextractordemo.entities.VideoData;
import com.chalilayang.mediaextractordemo.services.CodecService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VideoEditActivity extends AppCompatActivity {

    private static final String TAG = "VideoEditActivity";
    private static final int MSG_PARSED = 26;
    private static final int MSG_CUT_FINISHED = 30;
    private static final int MSG_ADD_TEXT_TRACK_FINISHED = 31;
    private static final int MSG_BINDER_READY = 32;
    private static final int MSG_PROGRESS = 33;
    private static final int MSG_MEMINFO = 35;
    private static final int MSG_MERGE_FINISHED = 36;
    private VideoData videoToEdit;

    private TextView videoNameTv;
    private TextView videoDurationTv;
    private TextView videoSizeTv;
    private TextView videoSampleRateTv;
    private TextView videoTrackTv;
    private TextView videoMemInfoTv;

    private EditText headCutEdtv;
    private EditText tailCutEdtv;

    private FloatingActionButton cutBtn;
    private FloatingActionButton addBtn;
    private FloatingActionButton mergeBtn;

    private MediaExtractor mediaExtractor = new MediaExtractor();
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;
    private String mime = null;

    private long videoDuration; // in microseconds
    private long audioDuration; // in microseconds
    private int videoHint;
    private int video_width;
    private int video_height;
    private int sampleRate;
    private int audioChannelCount;
    private int frameRate;
    private int trackNumber;
    private boolean videoHasParsed = false;

    private IVideoEditManager iVideoEditManager;
    private BinderPool mBinderPool;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PARSED:
                    Log.i(TAG, "handleMessage: 解析完成");
                    Log.i(TAG, "handleMessage:videoDuration " + videoDuration);
                    videoDurationTv.setText(getString(R.string.video_edit_duration) + TimeUtil
                            .toHumanReadableTime(videoDuration / 1000) + "  音频时长:" + TimeUtil
                            .toHumanReadableTime(audioDuration / 1000));
                    videoSampleRateTv.setText(getString(R.string.video_edit_samplerate) +
                            sampleRate);
                    videoTrackTv.setText(getString(R.string.video_edit_track_number) + trackNumber);
                    break;
                case MSG_CUT_FINISHED:
                    Boolean result = (Boolean) msg.obj;
                    if (result != null) {
                        if (result.booleanValue()) {
                            Snackbar.make(cutBtn, "剪切完成", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        } else {
                            Snackbar.make(cutBtn, "剪切出错", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                    this.removeMessages(MSG_MEMINFO);
                    break;
                case MSG_ADD_TEXT_TRACK_FINISHED:
                    Snackbar.make(cutBtn, "字幕添加完成", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                case MSG_MERGE_FINISHED:
                    Snackbar.make(cutBtn, "合并完成", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                case MSG_BINDER_READY:
                    try {
                        iVideoEditManager.registerListener(mIOnNewBookArrivedListener);
                        iVideoEditManager.editVideo(videoToEdit);
                        int remote_pid = iVideoEditManager.getPid();
                        int locale_pid = Process.myPid();
                        Toast.makeText(getApplicationContext(), "remotePid: " + remote_pid + " " +
                                "localePid:" + locale_pid, Toast.LENGTH_SHORT).show();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_PROGRESS:
                    Toast.makeText(getApplicationContext(), "" + msg.arg1, Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MSG_MEMINFO:
                    String str = getMemInfo();
                    videoMemInfoTv.setText(str);
                    this.sendEmptyMessageDelayed(MSG_MEMINFO, 1000);
                    break;
            }
        }
    };
    private IVideoEditListener mIOnNewBookArrivedListener = new IVideoEditListener.Stub() {
        @Override
        public void onProgress(int progress, int max) throws RemoteException {
            Log.i(TAG, "onProgress: " + progress);
            Message msg = handler.obtainMessage(MSG_PROGRESS);
            msg.arg1 = progress;
            handler.sendMessage(msg);
        }

        @Override
        public void onCodecStart(VideoData video) throws RemoteException {

        }

        @Override
        public void onCodecFinish(VideoData video) throws RemoteException {

        }
    };
    AsyncTask<VideoData, Void, Void> parseTask = new AsyncTask<VideoData, Void, Void>() {
        @Override
        protected Void doInBackground(VideoData... params) {
            for (VideoData video : params
                    ) {
                parseVideo(video);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "onPreExecute: 开始解析");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            handler.obtainMessage(MSG_PARSED).sendToTarget();
        }
    };
    AsyncTask<Long, Void, Boolean> videoCutTask = new AsyncTask<Long, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(Long... params) {
            if (params.length != 3) {
                return false;
            }
            long head = params[0];
            long tail = params[1];
            long duration = params[2];
            String path = videoToEdit.filePath;
            String dest = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                    + File.separatorChar
                    + FileUtils.parseFileName(path);
            if (dest.endsWith(".mp4")) {
                dest = dest.substring(0, dest.lastIndexOf(".")) + "_output.mp4";
            }
            handler.sendEmptyMessageDelayed(MSG_MEMINFO, 1000);
            VideoUtils.cropVideo(path, dest, head, duration - tail, VideoUtils.METHOD_BY_MEDIA);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Message msg = handler.obtainMessage(MSG_CUT_FINISHED);
            msg.obj = aBoolean ? Boolean.TRUE : Boolean.FALSE;
            handler.sendMessage(msg);
            super.onPostExecute(aBoolean);
        }
    };
    AsyncTask<Void, Void, Void> addTextgTrackTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            String path = videoToEdit.filePath;
            String dest = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                    + File.separatorChar
                    + FileUtils.parseFileName(path);
            List<SrtEntity> list = new ArrayList<SrtEntity>();
            long start = 0;
            long sec = videoDuration / 1000000l;
            for (int index = 0; index < sec; index++) {
                start += 1000;
                list.add(new SrtEntity(start, start + 1000, "第" + start / 1000 + "秒 " +
                        "字幕：----------"));
            }
//            list.add(new SrtEntity(start, 1000, "0-----1"));
//            list.add(new SrtEntity(2000, 10000, "2------10"));
//            list.add(new SrtEntity(17000, 20000, "17-----20"));
            VideoUtils.addTextTrack(path, list);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            handler.obtainMessage(MSG_ADD_TEXT_TRACK_FINISHED).sendToTarget();
        }
    };
    AsyncTask<Void, Void, Void> mergeVideosTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            String path = videoToEdit.filePath;
            String appendVideo = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                    + File.separatorChar
                    + "appendVideo.mp4";
            List<String> videos = new ArrayList<>(2);
            videos.add(path);
            videos.add(appendVideo);
            String dest = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                    + File.separatorChar
                    + "mergedVideo.mp4";

            String tmp1 = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                    + File.separatorChar
                    + "tmp1.mp4";

            String tmp2 = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                    + File.separatorChar
                    + "tmp2.mp4";
            VideoDecoder.Segment ss = new VideoDecoder.Segment(0, 8000000);
            VideoDecoder.Segment ss1 = new VideoDecoder.Segment(4000000, 12000000);
//            List<VideoDecoder.Segment> list = new ArrayList<>();
//            list.add(ss);
//            list.add(ss1);
//            VideoDecoder decoder = new VideoDecoder();
//            decoder.mergeSegments(path, dest, list);
            VideoUtils.cropVideo(path, tmp1, 0, 8000000, VideoUtils.METHOD_BY_MEDIA);
            VideoUtils.cropVideo(path, tmp2, 4000000, 12000000, VideoUtils.METHOD_BY_MEDIA);
            List<String> paths = new ArrayList<>();
            paths.add(tmp1);
            paths.add(tmp2);
            VideoUtils.mergeVideos(paths, dest);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            handler.obtainMessage(MSG_MERGE_FINISHED).sendToTarget();
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_edit);
        initData();
        initView();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                mBinderPool = BinderPool.getInstance(getApplicationContext());
//                IBinder binder = mBinderPool.queryBinder(BinderPoolImpl.BINDER_ID_VIDEO_EDIT);
//                iVideoEditManager = VideoEditManagerImpl.asInterface(binder);
//                handler.obtainMessage(MSG_BINDER_READY).sendToTarget();
//            }
//        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        parseTask.cancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!videoHasParsed) {
            parseTask.execute(videoToEdit);
        }
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent.getAction() != null &&
                Intent.ACTION_VIEW.equals(intent.getAction())) {
            String path = getVideoPath(getApplicationContext(), intent.getData());
            String name = path.substring(path.lastIndexOf(File.separator));
            videoToEdit = new VideoData(path, name);
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String name = b.getString(MainActivity.KEY_FILE_NAME);
            String path = b.getString(MainActivity.KEY_FILE_PATH);
            videoToEdit = new VideoData(path, name);
        }
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        videoNameTv = (TextView) findViewById(R.id.video_edit_name);
        videoDurationTv = (TextView) findViewById(R.id.video_edit_duration);
        videoSizeTv = (TextView) findViewById(R.id.video_edit_filesize);
        videoSampleRateTv = (TextView) findViewById(R.id.video_edit_samplerate);
        videoTrackTv = (TextView) findViewById(R.id.video_edit_track_num);
        videoMemInfoTv = (TextView) findViewById(R.id.video_edit_meminfo);

        headCutEdtv = (EditText) findViewById(R.id.video_edit_input_header_edtv);
        tailCutEdtv = (EditText) findViewById(R.id.video_edit_input_tail_edtv);

        if (videoToEdit != null) {
            videoNameTv.setText(videoToEdit.fileName);
            videoSizeTv.setText(getString(R.string.video_edit_size) + SDCardUtil
                    .formatSize(new File(videoToEdit.filePath).length()));
        }

        cutBtn = (FloatingActionButton) findViewById(R.id.cut);
        cutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCut();
            }
        });
        addBtn = (FloatingActionButton) findViewById(R.id.add_text_track);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doAdd();
            }
        });
        mergeBtn = (FloatingActionButton) findViewById(R.id.merge_videos);
        mergeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                doMerge();
                doEdit();
            }
        });
    }

    private void parseVideo(VideoData videoData) {
        try {
            String path = videoData.filePath;
            mediaExtractor.setDataSource(path);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }

        final int trackcount = this.trackNumber = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackcount; i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    video_width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    video_height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    videoHint = mediaFormat.getInteger(MediaFormat.KEY_ROTATION);
                    frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                } else if (mime.startsWith("audio/")) {
                    this.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    this.audioChannelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                }
                Log.d(TAG, "file mime is " + mime);
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        videoHasParsed = true;
    }

    private void doCut() {
        String tmpStr1 = this.headCutEdtv.getText().toString().trim().toLowerCase();
        String tmpStr2 = this.tailCutEdtv.getText().toString().trim().toLowerCase();
        long head = -1L;
        long tail = -1L;
        try {
            head = Long.parseLong(tmpStr1) * 1000;
            tail = Long.parseLong(tmpStr2) * 1000;
            if (checkInput(head, tail, this.videoDuration)) {
                videoCutTask.execute(head, tail, this.videoDuration);
            }
        } catch (NumberFormatException e) {
            Snackbar.make(cutBtn, "输入错误", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void doAdd() {
        addTextgTrackTask.execute();
    }
    private void doMerge() {
        mergeVideosTask.execute();
    }
    private void doEdit() {
        String path = videoToEdit.filePath;
        String appendVideo = StorageEngine.getDownloadFolder(getApplicationContext()).getAbsolutePath()
                + File.separatorChar
                + "editVideo.mp4";
        File ff = new File(appendVideo);
        if (ff.exists()) {
            ff.delete();
        }
        ExtractDecodeEditEncodeMuxTest test = new ExtractDecodeEditEncodeMuxTest();
        try {
            test.testExtractDecodeEditEncodeMuxAudioVideo(path);
        } catch (Throwable e) {
            e.printStackTrace();
        }
//        AvcEncoder avcEncoder = new AvcEncoder();
//        avcEncoder.start(new VideoCodecModel(path, appendVideo));
//        VideoCodecModel video = new VideoCodecModel(path, appendVideo);
//        Intent intent = new Intent(this, CodecService.class);
//        intent.putExtra("action", CodecService.REQUEST_CODEC);
//        intent.putExtra("video", video);
//        startService(intent);
    }

    private boolean checkInput(long head, long tail, long duration) {
        if (head < 0 || head >= duration) {
            return false;
        }
        if (tail < 0 || tail >= duration) {
            return false;
        }
        if (head >= duration - tail) {
            return false;
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
//        BinderPool.unbindService();
    }

    public String getMemInfo() {
        StringBuilder sb = new StringBuilder();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        sb.append(" memoryInfo.availMem " + memoryInfo.availMem + "\n");
        sb.append(" memoryInfo.lowMemory " + memoryInfo.lowMemory + "\n");
        sb.append(" memoryInfo.threshold " + memoryInfo.threshold + "\n");

        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager
                .getRunningAppProcesses();

        Map<Integer, String> pidMap = new TreeMap<Integer, String>();
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (Process.myPid() == runningAppProcessInfo.pid) {
                pidMap.put(runningAppProcessInfo.pid, runningAppProcessInfo.processName);
            }
        }

        Collection<Integer> keys = pidMap.keySet();

        for (int key : keys) {
            int pids[] = new int[1];
            pids[0] = key;
            android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo
                    (pids);
            for (android.os.Debug.MemoryInfo pidMemoryInfo : memoryInfoArray) {
                sb.append(String.format("** MEMINFO in pid %d [%s] **\n", pids[0], pidMap.get
                        (pids[0])));
                sb.append(" pidMemoryInfo.getTotalPss(): " + pidMemoryInfo.getTotalPss() + "\n");
                sb.append(" pidMemoryInfo.getTotalSharedDirty(): " + pidMemoryInfo
                        .getTotalSharedDirty() + "\n");
            }
        }
        return sb.toString();
    }
}
