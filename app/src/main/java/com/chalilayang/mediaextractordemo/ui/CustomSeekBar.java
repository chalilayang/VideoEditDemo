package com.chalilayang.mediaextractordemo.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.chalilayang.mediaextractordemo.R;

/**
 * Created by chalilayang on 2016/12/13.
 */

public class CustomSeekBar extends View {

    private static final String TAG = "CustomSeekBar";
    public static final int MAX = 100000;
    private static final int DEFAULT_CIRCLE_WIDTH = 20;
    private static final int DEFAULT_BAR_WIDTH = 50;
    private static final int DEFAULT_CIRCLE_COLOR = Color.parseColor("#ffffff");
    private static final int DEFAULT_BAR_COLOR = Color.parseColor("#e02d3f");
    private static final int DEFAULT_BAR_SHADOW_COLOR = Color.parseColor("#333333");

    private float mDownX;
    private float mDownY;
    private float mTouchSlop;

    private float circleRadius;
    private int circleColor;
    private Paint circlePaint;

    private float barHeight;
    private int barColor;
    private Paint barPaint;
    private Paint barShadowPaint;

    private int measuredWidth;
    private int measuredHeight;

    private int progress = MAX / 3;

    private Point circleCenter = new Point();
    private Rect barRect = new Rect();
    private Rect barShadowRect = new Rect();

    private OnSeekUpdateListener mCallback;

    private boolean isDragging = false;

    public CustomSeekBar(Context context) {
        this(context, null);
    }

    public CustomSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(false);
        setWillNotDraw(false);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        // Load the styled attributes and set their properties
        TypedArray attributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.CustomSeekBar,
                defStyleAttr,
                0
        );
        circleRadius = attributes.getDimension(
                R.styleable.CustomSeekBar_csb_circleRadius,
                dp2px(DEFAULT_CIRCLE_WIDTH)
        );

        circleColor = attributes.getColor(
                R.styleable.CustomSeekBar_csb_circleColor,
                DEFAULT_CIRCLE_COLOR
        );
        barHeight = attributes.getDimension(
                R.styleable.CustomSeekBar_csb_barHeight,
                dp2px(DEFAULT_BAR_WIDTH)
        );

        barColor = attributes.getColor(
                R.styleable.CustomSeekBar_csb_barColor,
                DEFAULT_BAR_COLOR
        );
        barColor = attributes.getColor(
                R.styleable.CustomSeekBar_csb_barShadowColor,
                DEFAULT_BAR_COLOR
        );
        Log.i(TAG, "init: circleRadius " + circleRadius + " barHeight " + barHeight);
        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setStrokeWidth(circleRadius);
        circlePaint.setColor(circleColor);

        barPaint = new Paint();
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setStrokeWidth(barHeight);
        barPaint.setColor(barColor);

        barShadowPaint = new Paint();
        barShadowPaint.setAntiAlias(true);
        barShadowPaint.setStyle(Paint.Style.FILL);
        barShadowPaint.setStrokeWidth(barHeight);
        barShadowPaint.setColor(DEFAULT_BAR_SHADOW_COLOR);

        attributes.recycle();
    }

    private void setProgressValue(int tmpValue, boolean fromUser) {
        if (tmpValue >= 0 && this.progress != tmpValue && tmpValue <= MAX) {
            this.progress = tmpValue;
            updateProgressBarRect();
            if (this.mCallback != null) {
                this.mCallback.onProgressUpdate(tmpValue, fromUser);
            }
            invalidate();
        }
    }

    public void setProgress(int tmpValue) {
        setProgressValue(tmpValue, true);
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

                final float distance = (float) Math.sqrt(
                        (mDownX - circleCenter.x) * (mDownX - circleCenter.x) +
                                (mDownY-circleCenter.y)*(mDownY-circleCenter.y)
                );
                if (distance <= 2*circleRadius) {
                    Log.i(TAG, "onTouchEvent: isDragging " + isDragging);
                    isDragging = true;
                    invalidate();
                } else {
                    int value = getProgressByPos(x, y);
                    Log.i(TAG, "onTouchEvent: value " + value);
                    setProgressValue(value, true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    final float distance2 = Math.abs(mDownX - x);
                    Log.i(TAG, "onTouchEvent: isDragging " + isDragging + "  x " + x);
                    if (distance2 > mTouchSlop) {
                        int value = getProgressByPos(x, y);
                        Log.i(TAG, "onTouchEvent: value " + value);
                        setProgressValue(value, true);
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

    private int getProgressByPos(float posX, float posY) {
        final float tmpX = posX - barShadowRect.left;
        float value = tmpX * MAX * 1.0f / barShadowRect.width();
        return (int)value;
    }

    public void setOnSeekUpdateListener(OnSeekUpdateListener lis) {
        if (lis != null) {
            this.mCallback = lis;
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBar(canvas);
        drawCircle(canvas);
    }

    private void drawBar(Canvas canvas) {
        canvas.drawRect(barShadowRect, barShadowPaint);
        canvas.drawRect(barRect, barPaint);
    }
    private void drawCircle(Canvas canvas) {
        canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, circlePaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        if (height < barHeight) {
            measuredHeight = (int)(barHeight + 0.5);
        }
        measuredWidth = width;
        measuredHeight = height;
        updateBarRect();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private void updateBarRect() {
        if (measuredWidth > 0 && measuredHeight > 0) {
            int top = (int)((measuredHeight - barHeight) * 0.5);
            int left = (int)circleRadius;
            int right = (int) (measuredWidth - circleRadius);
            int bottom = measuredHeight - top;
            barShadowRect.set(left, top, right, bottom);
            updateProgressBarRect();
        }
    }

    private void updateProgressBarRect() {
        if (measuredWidth > 0 && measuredHeight > 0) {
            int right = (int) (
                            (barShadowRect.right - barShadowRect.left) * progress * 1.0 / MAX
                                    + barShadowRect.left
                    );
            barRect.set(barShadowRect.left, barShadowRect.top, right, barShadowRect.bottom);
            updateCircleRect();
        }
    }

    private void updateCircleRect() {
        if (measuredWidth > 0 && measuredHeight > 0) {
            int centerX = barRect.right;
            int centerY = (int) (measuredHeight * 0.5);
            circleCenter.set(centerX, centerY);
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
    public int sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    protected int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public interface OnSeekUpdateListener {
        void onProgressUpdate(int value, boolean fromUser);
    }
}
