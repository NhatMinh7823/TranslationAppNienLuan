package com.example.translationapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class ScreenshotRequestActivity extends Activity {
    private static final int REQUEST_SCREENSHOT = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo MediaProjectionManager
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // Yêu cầu quyền chụp màn hình
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_SCREENSHOT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_SCREENSHOT && resultCode == RESULT_OK && data != null) {
            // Gửi kết quả trực tiếp đến Service
            Intent serviceIntent = new Intent(this, FloatingWindowService.class);
            serviceIntent.setAction("ACTION_SCREENSHOT");
            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("data", data);
            startService(serviceIntent);
        }

        // Đóng Activity sau khi đã xử lý xong yêu cầu
        finish();
    }
}
