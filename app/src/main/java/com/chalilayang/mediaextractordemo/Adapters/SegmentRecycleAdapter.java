package com.chalilayang.mediaextractordemo.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ptteng.bf8.R;
import com.ptteng.bf8.videoedit.entities.VideoSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chalilayang on 2016/12/13.
 */

public class SegmentRecycleAdapter extends RecyclerView.Adapter {

    private Context context;
    private List<VideoSegment> segmentList;
    private int itemWidth;
    private int imageHeight;
    private onItemClickListener listener;
    public SegmentRecycleAdapter(Context cont, int item_width) {
        this.context = cont;
        segmentList = new ArrayList<>();
        this.itemWidth = item_width;
    }
    public void loadVideoSegments(List<VideoSegment> list) {
        this.segmentList.clear();
        this.segmentList.addAll(list);
        this.notifyDataSetChanged();
    }
    public void addVideoSegments(List<VideoSegment> list) {
        this.segmentList.addAll(list);
    }
    public void setOnItemClickListener(onItemClickListener lis) {
        if (lis != null) {
            this.listener = lis;
        }
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_segment, parent,
                false);
        ViewGroup.LayoutParams vlp = view.getLayoutParams();
        vlp.width = this.itemWidth;
        vlp.height = this.itemWidth;

        float rate = 9.0f / 16;
        int imageHeight = (int)(itemWidth * rate);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams)
                view.findViewById(R.id.segment_item_image).getLayoutParams();
        rlp.height = rlp.width = (int)(this.itemWidth * 0.9);
        rlp = (RelativeLayout.LayoutParams)
                view.findViewById(R.id.segment_delete_icon).getLayoutParams();
        rlp.height = rlp.width = (int)(this.itemWidth * 0.2);
        return new SegmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (segmentList != null) {
            VideoSegment mData = segmentList.get(position);
            SegmentViewHolder tempHolder = (SegmentViewHolder) holder;
            tempHolder.mPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return segmentList.size();
    }

    class SegmentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public ImageView mImageView;
        public View mRoot;
        public int mPosition;

        public SegmentViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.segment_item_image);
            mRoot = itemView.findViewById(R.id.segment_item_root);
            mRoot.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (SegmentRecycleAdapter.this.listener != null) {
                SegmentRecycleAdapter.this.listener.onItemClick(v, mPosition);
            }
        }
    }

    public interface onItemClickListener {
        void onItemClick(View view, int position);
    }
}
