package com.example.translationapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private TextView translationResult;
    private final int REQUEST_CODE_CAMERA_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.camera_preview);
        Button captureButton = findViewById(R.id.button_capture);
        translationResult = findViewById(R.id.translationResult);

        // Kiểm tra quyền truy cập camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(CameraActivity.this, "Không thể khởi động camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1280, 720))
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(CameraActivity.this, "ImageCapture chưa được khởi tạo", Toast.LENGTH_SHORT).show();
            return;
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                processImage(image);
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
                Toast.makeText(CameraActivity.this, "Lỗi chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy image) {
        if (image.getImage() != null) {
            InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(inputImage)
                    .addOnSuccessListener(this::processTextRecognitionResult)
                    .addOnFailureListener(e -> {
                        Toast.makeText(CameraActivity.this, "Lỗi nhận diện văn bản", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        } else {
            Toast.makeText(CameraActivity.this, "Không thể xử lý hình ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void processTextRecognitionResult(Text text) {
        String recognizedText = text.getText();
        if (!recognizedText.isEmpty()) {
            TranslatorService.translateText(recognizedText, "en","vi", new TranslatorService.TranslationCallback() {
                @Override
                public void onSuccess(String translatedText) {
                    runOnUiThread(() -> translationResult.setText(translatedText));
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> translationResult.setText("Lỗi dịch thuật: " + error));
                }
            });
        } else {
            translationResult.setText("Không tìm thấy văn bản trong ảnh.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showPermissionDeniedDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền truy cập camera")
                .setMessage("Ứng dụng cần quyền truy cập camera để chụp ảnh và nhận diện văn bản. Bạn có muốn bật quyền này trong cài đặt không?")
                .setPositiveButton("Cài đặt", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }
}
