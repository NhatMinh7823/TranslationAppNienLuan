package com.example.translationapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class TriangleView extends ImageView {
    private OnActivationChangeListener onActivationChangeListener;
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
        setImageResource(R.drawable.ic_screen_shot); // Đặt tên icon theo drawable của bạn
        setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
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
        setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN); // Khi kích hoạt thì đổi sang màu xanh
    } else {
        setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN); // Khi không kích hoạt thì đổi về màu đỏ
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
