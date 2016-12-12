package com.chalilayang.mediaextractordemo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.chalilayang.mediaextractordemo.R;

/**
 * Created by chalilayang on 2016/12/12.
 */

public class VideoEditSeekView extends LinearLayout {
    private static final String TAG = "VideoEditSeekView";
    public static int MAX = 100000;
    private static final int DEFAULT_SHADOW_COLOR = Color.parseColor("#000000");

    private int frontValue = MAX / 3;
    private int backValue = MAX / 2;
    private float mDownX;
    private float mDownY;
    private float mTouchSlop;

    private boolean isDragging = false;

    private Drawable seekBarSelect;
    private Drawable seekBarNormal;
    private int seekBarWidth;
    private int seekBarHeight;
    private int currentDrawbleSelected = 0;

    private Rect frontShadowRect = new Rect();
    private Rect backShadowRect = new Rect();
    private Rect frontSeekBarRect = new Rect();
    private Rect backSeekBarRect = new Rect();

    private int measuredWidth;
    private int measuredHeight;

    private int seekBarExtra = 0;

    // Paint to draw shadow.
    private Paint shadowPaint;
    private int shadowColor  = DEFAULT_SHADOW_COLOR;

    // Paint to draw frontBar.
    private Paint frontBarPaint;
    private int frontBarColor;

    // Paint to draw BackBar.
    private Paint backBarPaint;
    private int backBarColor;

    private OnSeekUpdateListener mCallback;
    public void setOnSeekUpdateListener(OnSeekUpdateListener lis) {
        if (lis != null) {
            this.mCallback = lis;
        }
    }
    public VideoEditSeekView(Context context) {
        this(context, null);
    }

    public VideoEditSeekView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoEditSeekView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(false);
        setWillNotDraw(false);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOrientation(LinearLayout.HORIZONTAL);

        seekBarSelect = getResources().getDrawable(R.drawable.ic_action_seekbar_select);
        seekBarNormal = getResources().getDrawable(R.drawable.ic_action_seekbar_normal);
        seekBarWidth = seekBarSelect.getIntrinsicWidth();
        seekBarHeight = seekBarSelect.getIntrinsicHeight();
        seekBarExtra = (int) (seekBarHeight * 0.1);

        shadowPaint = new Paint();
        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(shadowColor);

        frontBarPaint = new Paint();
        frontBarPaint.setAntiAlias(true);
        frontBarPaint.setStyle(Paint.Style.FILL);

        backBarPaint = new Paint();
        backBarPaint.setAntiAlias(true);
        backBarPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawShadow(canvas);
        drawSeekBar(canvas);
    }

    private void drawShadow(Canvas canvas) {
        canvas.save();
        canvas.drawRect(frontShadowRect, shadowPaint);
        canvas.drawRect(backShadowRect, shadowPaint);
        canvas.restore();
    }

    private void drawSeekBar(Canvas canvas) {
        canvas.save();
        if (currentDrawbleSelected == -1) {
            seekBarNormal.setBounds(frontSeekBarRect);
            seekBarNormal.draw(canvas);
            seekBarNormal.setBounds(backSeekBarRect);
            seekBarNormal.draw(canvas);
        } else {
            if (currentDrawbleSelected == 0) {
                seekBarNormal.setBounds(backSeekBarRect);
                seekBarNormal.draw(canvas);
                seekBarSelect.setBounds(frontSeekBarRect);
                seekBarSelect.draw(canvas);
            } else if (currentDrawbleSelected == 1) {
                seekBarNormal.setBounds(frontSeekBarRect);
                seekBarNormal.draw(canvas);
                seekBarSelect.setBounds(backSeekBarRect);
                seekBarSelect.draw(canvas);
            }
        }
        canvas.restore();
    }

    public void setFrontValue(int tmpValue) {
        if (this.frontValue != tmpValue && tmpValue < this.backValue) {
            Log.i(TAG, "onTouchEvent: value " + tmpValue);
            this.frontValue = tmpValue;
            updateShadowRect();
            if (this.mCallback != null) {
                this.mCallback.onFrontValueUpdate(tmpValue);
            }
            invalidate();
        }
    }

    public void setBackValue(int tmpValue) {
        if (this.backValue != tmpValue && this.frontValue < tmpValue) {
            Log.i(TAG, "onTouchEvent: value " + tmpValue);
            this.backValue = tmpValue;
            updateShadowRect();
            if (this.mCallback != null) {
                this.mCallback.onBackValueUpdate(tmpValue);
            }
            invalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        int actionMasked = MotionEventCompat.getActionMasked(event);
        final float x = event.getX();
        final float y = event.getY();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                if (frontSeekBarRect.contains((int)x, (int)y)) {
                    Log.i(TAG, "onTouchEvent: frontSeekBarRect");
                    currentDrawbleSelected = 0;
                    isDragging = true;
                    invalidate();
                } else if (backSeekBarRect.contains((int)x, (int)y)){
                    Log.i(TAG, "onTouchEvent: backSeekBarRect");
                    currentDrawbleSelected = 1;
                    isDragging = true;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    final float distance2 = Math.abs(mDownX - x);
                    Log.i(TAG, "onTouchEvent:  x " + x);
                    if (distance2 > mTouchSlop) {
                        int value = getProgressByPos((int)x);
                        if (currentDrawbleSelected == 0) {
                            setFrontValue(value);
                        } else if (currentDrawbleSelected == 1) {
                            setBackValue(value);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                }
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measuredWidth = measureWidth(widthMeasureSpec);
        measuredHeight = measureHeight(heightMeasureSpec);
        updateShadowRect();
        Log.i(TAG, "onMeasure: measuredWidth " + measuredWidth + " measuredHeight " + measuredHeight);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private void updateShadowRect() {
        if (frontValue >= 0 && measuredWidth > 0) {
            int front = measuredWidth * frontValue / MAX;
            frontShadowRect.set(0, seekBarExtra, front, measuredHeight - seekBarExtra);
            frontSeekBarRect.set(frontShadowRect.right-seekBarWidth, 0,
                    frontShadowRect.right, frontShadowRect.bottom + seekBarExtra);
        }
        if (backValue >= 0 && measuredWidth > 0) {
            int back = measuredWidth * backValue / MAX;
            backShadowRect.set(back, seekBarExtra, measuredWidth, measuredHeight - seekBarExtra);
            backSeekBarRect.set(backShadowRect.left, 0,
                    backShadowRect.left+seekBarWidth, backShadowRect.bottom + seekBarExtra);
        }
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // The parent has determined an exact size for the child.
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // The parent has not imposed any constraint on the child.
            result = specSize;
        }
        return result;
    }

    private int measureHeight(int measureSpecHeight) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpecHeight);
        int specSize = MeasureSpec.getSize(measureSpecHeight);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be.
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number).
            result = specSize;
        }
        return result;
    }

    /**
     * Paint.setTextSize(float textSize) default unit is px.
     *
     * @param spValue The real size of text
     * @return int - A transplanted sp
     */
    private int sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    private int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private int getProgressByPos(int posX) {
        if (measuredWidth <= 0) {
            return 0;
        }
        final float tmpX = (float)(posX * 1.0 / measuredWidth);
        return (int)(tmpX * MAX);
    }

    public interface OnSeekUpdateListener {
        void onFrontValueUpdate(int frontvalue);
        void onBackValueUpdate(int frontvalue);
    }
}
