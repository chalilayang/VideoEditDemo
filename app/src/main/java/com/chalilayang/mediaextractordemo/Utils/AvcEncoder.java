package com.chalilayang.mediaextractordemo.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.chalilayang.mediaextractordemo.entities.VideoCodecModel;

@TargetApi(18)
public class AvcEncoder {
	private static final String TAG = "AvcEncoder";
	private int srcWidth, srcHeight, dstWidth, dstHeight;
	private int videoMaxInputSize = 0, videoRotation = 0;
	private long videoDuration;
	private MediaExtractor mediaExtractor;
	private MediaMuxer muxer;
	private int videoTrackIndex = -1;
	private MediaFormat srcVideoFormat;
	private MediaCodec mediaDecode;
	private MediaCodec mediaEncoder;

	private ByteBuffer[] decodeInputBuffers;
	private ByteBuffer[] decodeOutputBuffers;
	private MediaCodec.BufferInfo decodeBufferInfo;

	private VideoCodecModel mVideo;
	private boolean decodeOver = false, encoding = false;
	private DecodeRunnable decodeRunnable;
	private EncodeRunnable encodeRunnable;
	private ArrayList<Frame> timeDataContainer;
	private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private SimpleDateFormat videoTimeFormat;
	public void start(VideoCodecModel video) {
		if (video == null) {
			return;
		}
		if (!new File(video.srcPath).exists()) {
			return;
		}
		mVideo = video;
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				init(mVideo.srcPath, mVideo.dstPath);
				decodeRunnable = new DecodeRunnable();
				decodeRunnable.start();
				encodeRunnable = new EncodeRunnable();
				encodeRunnable.start();
			}
		};
		AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
	}

	public void init(String srcPath, String dstpath) {
		videoTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		timeDataContainer = new ArrayList<>();
		textPaint = new Paint();
		textPaint.setTextSize(100);
		textPaint.setAntiAlias(true);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setColor(Color.RED);

		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(srcPath);
		try {
			srcWidth = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever
					.METADATA_KEY_VIDEO_WIDTH));
			srcHeight = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever
					.METADATA_KEY_VIDEO_HEIGHT));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		try {
			mediaExtractor = new MediaExtractor();
			mediaExtractor.setDataSource(srcPath);

			String mime = null;
			for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
				MediaFormat format = mediaExtractor.getTrackFormat(i);
				mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("video/")) {
					videoTrackIndex = i;
					this.srcVideoFormat = format;
					break;
				}
			}

			mediaExtractor.selectTrack(videoTrackIndex);

			srcWidth = srcVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
			srcHeight = srcVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
			videoMaxInputSize = srcVideoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
			videoDuration = srcVideoFormat.getLong(MediaFormat.KEY_DURATION);
			if (srcVideoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
				videoRotation = srcVideoFormat.getInteger(MediaFormat.KEY_ROTATION);
			}

			if (videoRotation == 90 || videoRotation == 270) {
				dstWidth = srcHeight;
				dstHeight = srcWidth;
			} else if (videoRotation == 0 || videoRotation == 180) {
				dstWidth = srcWidth;
				dstHeight = srcHeight;
			}

			Log.i(TAG, "videoWidth=" + srcWidth + ",videoHeight=" + srcHeight + "," +
					"videoMaxInputSize=" + videoMaxInputSize + ",videoDuration=" + videoDuration
					+ ",videoRotation=" + videoRotation);
			MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
			videoInfo.presentationTimeUs = 0;

			initMediaDecode();
			initMediaEncode();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initMediaDecode() {
		try {
			mediaDecode = MediaCodec.createDecoderByType(
					this.srcVideoFormat.getString(MediaFormat.KEY_MIME));
			mediaDecode.configure(this.srcVideoFormat, null, null, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (mediaDecode == null) {
			Log.e(TAG, "create mediaDecode failed");
			return;
		}
		mediaDecode.start();
		decodeInputBuffers = mediaDecode.getInputBuffers();
		decodeOutputBuffers = mediaDecode.getOutputBuffers();
		decodeBufferInfo = new MediaCodec.BufferInfo();
	}

	private void initMediaEncode() {
		try {
			MediaFormat targetFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
					dstWidth, dstHeight);
			mediaEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
			targetFormat.setInteger(MediaFormat.KEY_BIT_RATE, dstWidth * dstHeight);
			targetFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 27);

			targetFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
					.COLOR_FormatYUV420SemiPlanar);
			targetFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
			mediaEncoder.configure(targetFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (mediaEncoder == null) {
			Log.e(TAG, "create mediaEncode failed");
			return;
		}
		mediaEncoder.start();
	}

	private Frame getFrameData() {
		synchronized (MediaCodec.class) {
			if (timeDataContainer.isEmpty()) {
				return null;
			}
			Frame frame = timeDataContainer.remove(0);
			frame.data = getNV21(dstWidth, dstHeight, frame.bitmap);
			return frame;
		}
	}

	private void extract() {
		Log.d(TAG, "extract() called");
		int inputIndex = mediaDecode.dequeueInputBuffer(-1);

		if (inputIndex < 0) {
			Log.i(TAG, "=========== code over =======");
			return;
		}
		ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];
		inputBuffer.clear();
		int length = mediaExtractor.readSampleData(inputBuffer, 0);
		if (length < 0) {
			Log.i(TAG, "extract Over");
			decodeOver = true;
			return;
		} else {
			MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
			videoInfo.offset = 0;
			videoInfo.size = length;
			int sampleFlag = mediaExtractor.getSampleFlags();
			if ((sampleFlag & MediaExtractor.SAMPLE_FLAG_SYNC)
					== MediaExtractor.SAMPLE_FLAG_SYNC) {
				videoInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
			} else {
				videoInfo.flags = 0;
			}
			videoInfo.presentationTimeUs = mediaExtractor.getSampleTime();
			Log.i(TAG, "extract: presentationTimeUs " + videoInfo.presentationTimeUs);
			decode(videoInfo, inputIndex);
			mediaExtractor.advance();
		}
	}

	private MediaFormat decodeFormat;
	private int decodeColorFormat;
	private int frameWidth;
	private int frameHeight;
	private void decode(MediaCodec.BufferInfo videoInfo, int inputIndex) {
		Log.i(TAG, "decode method : start");
		mediaDecode.queueInputBuffer(inputIndex, 0, videoInfo.size, videoInfo.presentationTimeUs,
				videoInfo.flags);
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputIndex = mediaDecode.dequeueOutputBuffer(bufferInfo, 50000);

		switch (outputIndex) {
			case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
				Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
				decodeOutputBuffers = mediaDecode.getOutputBuffers();
				break;
			case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
				decodeFormat = mediaDecode.getOutputFormat();
				decodeColorFormat = decodeFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
				Log.i(TAG, "New format " + mediaDecode.getOutputFormat());
				frameWidth = decodeFormat.getInteger(MediaFormat.KEY_WIDTH);
				if (decodeFormat.containsKey("crop-left") && decodeFormat.containsKey("crop-right")) {
					frameWidth = decodeFormat.getInteger("crop-right") + 1 - decodeFormat.getInteger("crop-left");
				}
				frameHeight = decodeFormat.getInteger(MediaFormat.KEY_HEIGHT);
				if (decodeFormat.containsKey("crop-top") && decodeFormat.containsKey("crop-bottom")) {
					frameHeight = decodeFormat.getInteger("crop-bottom") + 1 - decodeFormat.getInteger("crop-top");
				}
				break;
			case MediaCodec.INFO_TRY_AGAIN_LATER:
				Log.i(TAG, "dequeueOutputBuffer timed out!");
				break;
			default:
				Log.i(TAG, "decode: default " + outputIndex);
				ByteBuffer outputBuffer;
				byte[] frame;
				while (outputIndex >= 0) {
					outputBuffer = decodeOutputBuffers[outputIndex];
					frame = new byte[bufferInfo.size];
					outputBuffer.get(frame);
					outputBuffer.clear();
					handleFrameData(frame, videoInfo);
					mediaDecode.releaseOutputBuffer(outputIndex, false);
					outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 50000);
				}
				break;
		}
		Log.i(TAG, "decode method : end");
	}

	private void handleFrameData(byte[] data, MediaCodec.BufferInfo info) {
		Log.i(TAG, "handleFrameData()");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, frameWidth, frameHeight, null);
		yuvImage.compressToJpeg(new Rect(0, 0, srcWidth, srcHeight), 100, out);
		byte[] imageBytes = out.toByteArray();

		Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length)
				.copy(Bitmap.Config.ARGB_8888, true);

		Canvas canvas = new Canvas(image);
		canvas.drawText(videoTimeFormat.format(info.presentationTimeUs /
				1000), srcWidth/2, srcHeight/2, textPaint);
		saveBitmap(image);
		Log.i(TAG, "handleFrameData: Bitmap " + image.getWidth() + "*" + image.getHeight());
		synchronized (MediaCodec.class) {
			timeDataContainer.add(new Frame(info, image));
		}
	}

	String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera";
	public void saveBitmap(Bitmap bm) {
		Log.e(TAG, "保存图片");
		File f = new File(path, "ddd.png");
		if (f.exists()) {
			f.delete();
		}
		try {
			FileOutputStream out = new FileOutputStream(f);
			bm.compress(Bitmap.CompressFormat.PNG, 90, out);
			out.flush();
			out.close();
			Log.i(TAG, "已经保存");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void encode() {
		Log.i(TAG, "encode: ");
		byte[] chunkTime;
		Frame frame = getFrameData();
		if (frame == null) {
			return;
		}
		chunkTime = frame.data;
		int inputIndex = mediaEncoder.dequeueInputBuffer(-1);

		if (inputIndex < 0) {
			Log.d(TAG, "dequeueInputBuffer return inputIndex " + inputIndex + ",then break");
			mediaEncoder.signalEndOfInputStream();
		}

		ByteBuffer inputBuffer = mediaEncoder.getInputBuffers()[inputIndex];
		inputBuffer.clear();
		inputBuffer.put(chunkTime);
		inputBuffer.limit(frame.videoInfo.size);
		mediaEncoder.queueInputBuffer(inputIndex, 0, chunkTime.length, frame.videoInfo
				.presentationTimeUs, frame.videoInfo.flags);

		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputIndex = mediaEncoder.dequeueOutputBuffer(bufferInfo, 50000);

		switch (outputIndex) {
			case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
				Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
				break;
			case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
				MediaFormat outputFormat = mediaEncoder.getOutputFormat();
				outputFormat.setInteger(MediaFormat.KEY_ROTATION, videoRotation);
				Log.d(TAG, "mediaEncode find New format " + outputFormat);
				videoTrackIndex = muxer.addTrack(outputFormat);
				muxer.start();
				break;
			case MediaCodec.INFO_TRY_AGAIN_LATER:
				Log.d(TAG, "dequeueOutputBuffer timed out!");
				break;
			default:
				ByteBuffer outputBuffer;
				while (outputIndex >= 0) {
					outputBuffer = mediaEncoder.getOutputBuffers()[outputIndex];
					muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
					mediaEncoder.releaseOutputBuffer(outputIndex, false);
					outputIndex = mediaEncoder.dequeueOutputBuffer(bufferInfo, 50000);
				}
				break;
		}
	}

	public static byte[] getNV21(int width, int height, Bitmap scaled) {
		int[] argb = new int[width * height];
		scaled.getPixels(argb, 0, width, 0, 0, width, height);
		byte[] yuv = new byte[width * height * 3 / 2];
		encodeYUV420SP(yuv, argb, width, height);
		scaled.recycle();
		return yuv;
	}

	public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
		final int frameSize = width * height;

		int yIndex = 0;
		int uvIndex = frameSize;

		int R, G, B, Y, U, V;
		int index = 0;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				R = (argb[index] & 0xff0000) >> 16;
				G = (argb[index] & 0xff00) >> 8;
				B = (argb[index] & 0xff) >> 0;

				Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
				U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
				V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

				yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
				if (j % 2 == 0 && index % 2 == 0) {
					yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
					yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
				}
				index++;
			}
		}
	}
	private class DecodeRunnable extends Thread {
		@Override
		public void run() {
			decodeOver = false;
			while (!decodeOver) {
				try {
					extract();
				} catch (Exception e) {
					decodeOver = true;
					Log.i(TAG, "decode" + e.toString());
				}
				synchronized (encodeRunnable) {
					encodeRunnable.notify();
				}
			}
		}
	}

	private class EncodeRunnable extends Thread {
		@Override
		public void run() {
			encoding = true;
			while (encoding) {
				if (timeDataContainer.isEmpty()) {
					if (decodeOver) {
						break;
					}
					try {
						synchronized (encodeRunnable) {
							wait();
						}
					} catch (InterruptedException e) {
						encoding = false;
						e.printStackTrace();
					}
				} else {
					encode();
				}
			}
			Log.i(TAG, "encode: end");
			release();
			encoding = false;
		}
	}

	class Frame {
		MediaCodec.BufferInfo videoInfo;
		byte[] data;
		Bitmap bitmap;

		public Frame(MediaCodec.BufferInfo videoInfo, Bitmap bitmap) {
			this.videoInfo = videoInfo;
			this.bitmap = bitmap;
		}
	}

	public void release() {
		if (mediaDecode != null) {
			mediaDecode.stop();
			mediaDecode.release();
			mediaDecode = null;
		}
		if (mediaEncoder != null) {
			mediaEncoder.stop();
			mediaEncoder.release();
			mediaEncoder = null;
		}
		if (mediaExtractor != null) {
			mediaExtractor.release();
			mediaExtractor = null;
		}
	}
}
