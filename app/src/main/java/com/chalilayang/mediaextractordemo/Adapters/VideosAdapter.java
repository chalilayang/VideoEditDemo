package com.chalilayang.mediaextractordemo.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chalilayang.mediaextractordemo.R;
import com.chalilayang.mediaextractordemo.Utils.VideoThumbnailLoader;
import com.chalilayang.mediaextractordemo.Utils.VideoUtils;
import com.chalilayang.mediaextractordemo.entities.VideoData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chalilayang on 2016/5/13.
 */
public class VideosAdapter extends RecyclerView.Adapter implements VideoThumbnailLoader.ThumbnailListener {

    private List<VideoData> videoList = new ArrayList<>();
    private Context mContext;
    private onItemClickListener onItemClickListener;
    private VideoThumbnailLoader loader;

    public VideosAdapter(Context mContext) {
        this.mContext = mContext;
        loader = VideoThumbnailLoader.getInstance(mContext);
    }

    public void addVideoList(List<VideoData> videoList) {
        this.videoList.addAll(videoList);
    }

    public void clearVideosList() {
        this.videoList.clear();
    }

    public void removeVideo(int index) {
        this.videoList.remove(index);
        this.notifyItemRemoved(index);
    }

    public void setOnItemClickListener(VideosAdapter.onItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.cardview_item_layout, parent,
                false);
        return new ActivityViewHolder(view);
    }

    private static final String TAG = "VideosAdapter";
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (videoList != null) {
            VideoData mData = videoList.get(position);
            ActivityViewHolder tempHolder = (ActivityViewHolder) holder;
            tempHolder.mPosition = position;
            Bitmap bp = loader.get(mData.filePath);
            if (bp == null) {
                Log.i(TAG, "onBindViewHolder: pos " + position + "  bitmap null");
            } else {
                Log.i(TAG, "onBindViewHolder: pos " + position + "  bitmap " + bp.getByteCount());
            }
            tempHolder.mImageView.setImageBitmap(bp);
            tempHolder.mTextView.setText(mData.fileName);
        }
    }

    @Override
    public int getItemCount() {
        if (videoList != null) {
            return videoList.size();
        }
        return 0;
    }

    @Override
    public void onThumbnailLoadCompleted(String url, ImageView iv, Bitmap bitmap) {
        if (bitmap == null) {
            Log.i(TAG, "onBindViewHolder: url " + url + "  bitmap null");
        } else {
            Log.i(TAG, "onBindViewHolder: url " + url + "  bitmap " + bitmap.getByteCount());
        }
        iv.setImageBitmap(bitmap);
    }

    public interface onItemClickListener {
        void onItemClick(View view, int position);
        boolean onItemLongClick(View view, int position);
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        public ImageView mImageView;
        public TextView mTextView;
        public View mRoot;
        public int mPosition;

        public ActivityViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.activity_icon);
            mTextView = (TextView) itemView.findViewById(R.id.activity_title);
            mRoot = itemView.findViewById(R.id.cardview);
            mRoot.setOnClickListener(this);
            mRoot.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (VideosAdapter.this.onItemClickListener != null) {
                VideosAdapter.this.onItemClickListener.onItemClick(v, mPosition);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (VideosAdapter.this.onItemClickListener != null) {
                return VideosAdapter.this.onItemClickListener.onItemLongClick(v, mPosition);
            }
            return false;
        }
    }
}
