package com.example.translationapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TriangleView extends View {
    private OnActivationChangeListener onActivationChangeListener;
    private Paint paint;
    private boolean isActivated = false; // Trạng thái kích hoạt

    public TriangleView(Context context) {
        super(context);
        init();
    }

    public TriangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED); // Màu mặc định của tam giác
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Path path = new Path();
        int width = getWidth();
        int height = getHeight();
        path.moveTo((float) width / 2, 0);
        path.lineTo(0, height);
        path.lineTo(width, height);
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isActivated) {
                    // Nếu tam giác đã được kích hoạt thì cho phép vẽ hình chữ nhật
                    if (onActivationChangeListener != null) {
                        onActivationChangeListener.onActivated(true);
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    void toggleActivation() {
        isActivated = !isActivated;
        if (isActivated) {
            paint.setColor(Color.BLUE); // Khi kích hoạt thì đổi sang màu xanh
        } else {
            paint.setColor(Color.RED); // Khi không kích hoạt thì đổi về màu đỏ
        }
        invalidate();
    }

    public boolean isActivated() {
        return isActivated;
    }

    public interface OnActivationChangeListener {
        void onActivated(boolean activated);
    }
}
