package com.example.translationapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RectangleSelectionView extends View {
    private Paint paint;
    private float startX, startY, endX, endY;
    private boolean isDrawing = false;
    private long lastDrawTime = 0; // Timestamp for last redraw
    private static final long DRAW_DELAY = 16; // Delay in ms for approximately 60 FPS

    public RectangleSelectionView(Context context) {
        super(context);
        init();
    }

//    public RectangleSelectionView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init();
//    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLUE);  // Color of the rectangle
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isDrawing) {
            canvas.drawRect(startX, startY, endX, endY, paint);  // Draw the rectangle
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                isDrawing = true;
                break;

            case MotionEvent.ACTION_MOVE:
                endX = event.getX();
                endY = event.getY();

                // Limit redraw frequency to approximately 60 FPS
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDrawTime >= DRAW_DELAY) {
                    invalidate(); // Redraw rectangle
                    lastDrawTime = currentTime;
                }
                break;

            case MotionEvent.ACTION_UP:
                endX = event.getX();
                endY = event.getY();
                isDrawing = false;
                invalidate(); // Final redraw
                if (listener != null) {
                    listener.onRectangleDrawn(startX, startY, endX, endY);
                }
                break;
        }
        return true;
    }

    private OnRectangleDrawnListener listener;

    public void setOnRectangleDrawnListener(OnRectangleDrawnListener listener) {
        this.listener = listener;
    }

    public interface OnRectangleDrawnListener {
        void onRectangleDrawn(float startX, float startY, float endX, float endY);
    }

    public void setStartEndCoordinates(float startX, float startY, float endX, float endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        invalidate();  // Redraw with saved coordinates
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getEndX() {
        return endX;
    }

    public float getEndY() {
        return endY;
    }
}
