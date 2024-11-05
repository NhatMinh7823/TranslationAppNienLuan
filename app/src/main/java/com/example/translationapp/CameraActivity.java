package com.example.translationapp;

import static com.example.translationapp.LanguageUtils.swapLanguages;
import static com.example.translationapp.TranslatorService.translateText;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

//public class CameraActivity extends AppCompatActivity {
//
//    private PreviewView previewView;
//    private ImageCapture imageCapture;
//    private TextView translationResult;
//    private final int REQUEST_CODE_CAMERA_PERMISSION = 200;
//    private Spinner sourceLanguageSpinner, targetLanguageSpinner;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera);
//
//        previewView = findViewById(R.id.camera_preview);
//        Button captureButton = findViewById(R.id.button_capture);
//        translationResult = findViewById(R.id.translationResult);
//
//        sourceLanguageSpinner = findViewById(R.id.source_language_spinner);
//        targetLanguageSpinner = findViewById(R.id.target_language_spinner);
//
//        LanguageUtils.setupLanguageSpinners(this, sourceLanguageSpinner, targetLanguageSpinner);
//
//        // Kiểm tra quyền truy cập camera
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
//        } else {
//            startCamera();
//        }
//
//        captureButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                capturePhoto();
//            }
//        });
//    }
//
//    private void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                bindPreview(cameraProvider);
//            } catch (ExecutionException | InterruptedException e) {
//                e.printStackTrace();
//                Toast.makeText(CameraActivity.this, "Không thể khởi động camera", Toast.LENGTH_SHORT).show();
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//
//    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
//        Preview preview = new Preview.Builder().build();
//
//        imageCapture = new ImageCapture.Builder()
//                .setTargetResolution(new Size(1280, 720))
//                .build();
//
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build();
//
//        preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
//    }
//
//    private void capturePhoto() {
//        if (imageCapture == null) {
//            Toast.makeText(CameraActivity.this, "ImageCapture chưa được khởi tạo", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
//            @Override
//            public void onCaptureSuccess(@NonNull ImageProxy image) {
//                processImage(image);
//                image.close();
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                exception.printStackTrace();
//                Toast.makeText(CameraActivity.this, "Lỗi chụp ảnh", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    @OptIn(markerClass = ExperimentalGetImage.class)
//    private void processImage(ImageProxy image) {
//        if (image.getImage() != null) {
//            InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());
//
//            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
//            recognizer.process(inputImage)
//                    .addOnSuccessListener(this::processTextRecognitionResult)
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(CameraActivity.this, "Lỗi nhận diện văn bản", Toast.LENGTH_SHORT).show();
//                        e.printStackTrace();
//                    });
//        } else {
//            Toast.makeText(CameraActivity.this, "Không thể xử lý hình ảnh", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void processTextRecognitionResult(Text text) {
//        String recognizedText = text.getText();
//        if (!recognizedText.isEmpty()) {
//            String sourceLanguage = LanguageUtils.getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
//            String targetLanguage = LanguageUtils.getLanguageCode(targetLanguageSpinner.getSelectedItem().toString());
//            TranslatorService.translateText(recognizedText, sourceLanguage,targetLanguage, new TranslatorService.TranslationCallback() {
//                @Override
//                public void onSuccess(String translatedText) {
//                    runOnUiThread(() -> translationResult.setText(translatedText));
//                }
//
//                @Override
//                public void onFailure(String error) {
//                    runOnUiThread(() -> translationResult.setText("Lỗi dịch thuật: " + error));
//                }
//            });
//        } else {
//            translationResult.setText("Không tìm thấy văn bản trong ảnh.");
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startCamera();
//            } else {
//                showPermissionDeniedDialog();
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//
//    private void showPermissionDeniedDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Yêu cầu quyền truy cập camera")
//                .setMessage("Ứng dụng cần quyền truy cập camera để chụp ảnh và nhận diện văn bản. Bạn có muốn bật quyền này trong cài đặt không?")
//                .setPositiveButton("Cài đặt", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                        Uri uri = Uri.fromParts("package", getPackageName(), null);
//                        intent.setData(uri);
//                        startActivity(intent);
//                    }
//                })
//                .setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .create()
//                .show();
//    }
//}


public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private TextView translationResult;
    private final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private Spinner sourceLanguageSpinner, targetLanguageSpinner;
    private ProcessCameraProvider cameraProvider;
    private String recognizedText = "";// Hold reference to the camera provider to unbind

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.camera_preview);
        Button captureButton = findViewById(R.id.button_capture);
        translationResult = findViewById(R.id.translationResult);

        sourceLanguageSpinner = findViewById(R.id.source_language_spinner);
        targetLanguageSpinner = findViewById(R.id.target_language_spinner);
        ImageButton buttonSwapLanguages = findViewById(R.id.button_swap_languages);
        ImageButton buttonCopy = findViewById(R.id.button_copy);
        ImageButton buttonHome = findViewById(R.id.button_home);
        buttonHome.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.this, MainActivity.class);
            startActivity(intent);
        });
        LanguageUtils.setupCopyButton(this, buttonCopy, translationResult);

        LanguageUtils.setupLanguageSpinners(this, sourceLanguageSpinner, targetLanguageSpinner);

        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        buttonSwapLanguages.setOnClickListener(v -> swapLanguages(this, sourceLanguageSpinner, targetLanguageSpinner));
        captureButton.setOnClickListener(v -> capturePhoto());
        targetLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sourceLanguage = LanguageUtils.getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
                String targetLanguage = LanguageUtils.getLanguageCode(targetLanguageSpinner.getSelectedItem().toString());

                String textToTranslate = recognizedText;
                if (!textToTranslate.isEmpty()) {
                    translateText(textToTranslate,sourceLanguage ,targetLanguage , new TranslatorService.TranslationCallback() {
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
                    // Clear the translation result if input is empty
                    translationResult.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
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
                displayCapturedImage(image);
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

    private void displayCapturedImage(@NonNull ImageProxy image) {
        // Unbind the camera to stop the live preview
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        // Convert ImageProxy to Bitmap for displaying in PreviewView
        @SuppressLint("UnsafeOptInUsageError") Image capturedImage = image.getImage();
        if (capturedImage != null) {
            Bitmap bitmap = convertImageProxyToBitmap(capturedImage);

            // Set the Bitmap to PreviewView using an ImageView overlay
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setRotation(90);
            previewView.addView(imageView);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // After showing the image, optionally restart the camera preview after a delay
//            new Handler().postDelayed(this::startCamera, 10000); // Restart preview after 3 seconds
        }
    }

    private Bitmap convertImageProxyToBitmap(Image image) {
        // Convert the Image to Bitmap for displaying in PreviewView
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

@OptIn(markerClass = ExperimentalGetImage.class)
private void processImage(ImageProxy image) {
    if (image.getImage() != null) {
        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

        String sourceLanguage = LanguageUtils.getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
        TextRecognizer recognizer;

        // Choose the TextRecognizer based on the selected source language
        switch (sourceLanguage) {
            case "zh": // Chinese
                recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
                break;
            case "ja": // Japanese
                recognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
                break;
            case "ko": // Korean
                recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
                break;
            case "hi": // Hindi (Devanagari script)
                recognizer = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
                break;
            default: // Default to Latin script (covers English, Spanish, etc.)
                recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                break;
        }

        recognizer.process(inputImage)
                .addOnSuccessListener(this::processTextRecognitionResult)
                .addOnFailureListener(e -> {
                    Toast.makeText(CameraActivity.this, "Lỗi nhận diện văn bản", Toast.LENGTH_SHORT).show();
                    startCamera();
                    e.printStackTrace();
                });
    } else {
        Toast.makeText(CameraActivity.this, "Không thể xử lý hình ảnh", Toast.LENGTH_SHORT).show();
        startCamera();
    }

}


    private void processTextRecognitionResult(Text text) {
        recognizedText = text.getText();
        if (!recognizedText.isEmpty()) {
            String sourceLanguage = LanguageUtils.getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
            String targetLanguage = LanguageUtils.getLanguageCode(targetLanguageSpinner.getSelectedItem().toString());
            TranslatorService.translateText(recognizedText, sourceLanguage,targetLanguage, new TranslatorService.TranslationCallback() {
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
        startCamera();
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
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
