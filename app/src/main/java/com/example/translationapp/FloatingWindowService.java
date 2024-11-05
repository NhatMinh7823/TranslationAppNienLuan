package com.example.translationapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private TriangleView triangleView;
    private RectangleSelectionView rectangleSelectionView;
    private EditText editTextTranslationResult;
    private EditText editText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Spinner sourceLanguageSpinner, targetLanguageSpinner;
    private Map<String, String> languageCodeMap;
    private String selectedSourceLanguage, selectedTargetLanguage;
    private SpeechConfig speechConfig;
    private SpeechSynthesizer speechSynthesizer;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private boolean isCaptureInProgress = false;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (mediaProjection == null) {
            Intent screenshotIntent = new Intent(this, ScreenshotRequestActivity.class);
            screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivity(screenshotIntent);
        }

        createTriangleView();
        createFloatingWindow();
    }
//    -------------- End of onCreate() -------------------
//    -------------- End of onCreate() -------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            Log.e("FloatingWindowService", "Intent hoặc Action là null");
            return START_STICKY;
        }

        startForegroundServiceWithNotification();
        if ("ACTION_SCREENSHOT".equals(intent.getAction())) {
            if (mediaProjection == null) {
                int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
                Intent data = intent.getParcelableExtra("data");
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
//                    startVirtualDisplay();
                }
            }
        }

        return START_STICKY;
    }
    private void startForegroundServiceWithNotification() {
        String channelId = "screenshot_channel_id";
        String channelName = "Screenshot Service";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Floating Window Service")
                .setContentText("Service đang chạy để chụp màn hình.")
                .setSmallIcon(R.drawable.ic_translate)
                .build();

        startForeground(1, notification);
    }

    private void startVirtualDisplay() {
        if (mediaProjection == null || isCaptureInProgress) {
            return;
        }
        isCaptureInProgress = true;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int screenDensity = displayMetrics.densityDpi;

        // Khởi tạo ImageReader
        ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
        );

        imageReader.setOnImageAvailableListener(reader -> {
            if (isCaptureInProgress) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    // Cắt trực tiếp phần ảnh mong muốn từ Image
                    Bitmap croppedBitmap = cropImageDirectly(image);
                    if (croppedBitmap != null) {
                        processOCR(croppedBitmap);
                    } else {
                        showTooSmallMessage();
                        exitRectangleDrawingMode();
                    }
                    image.close();
                }
            }
        }, handler);
    }

    private void showTooSmallMessage() {
        handler.post(() -> {
            expandFloatingWindow();
            if (editText != null) {
                editText.setText("Xin hãy vẽ hình với kích thước lớn hơn");
            }
        });
    }

    @SuppressLint("InflateParams")
    private void expandFloatingWindow() {
        if (floatingView == null) {
            createFloatingWindow();
        } else if (floatingView.getParent() == null) {
            windowManager.addView(floatingView, floatingView.getLayoutParams());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void minimizeFloatingWindow() {
        if (floatingView != null && floatingView.getParent() != null) {
            windowManager.removeView(floatingView);
        }
        if(triangleView.isActivated()){
            triangleView.toggleActivation();
        }
    }

    private void setupFloatingWindowControls() {
        editText = floatingView.findViewById(R.id.editText_input);
        editTextTranslationResult = floatingView.findViewById(R.id.textView_result);
        TextInputLayout textInputLayout = floatingView.findViewById(R.id.text_input_layout);
        TextInputLayout textOutputLayout = floatingView.findViewById(R.id.text_output_layout);
        ImageButton closeButton = floatingView.findViewById(R.id.close_button);
        ImageButton stopButton = floatingView.findViewById(R.id.stop_button);
        ImageButton buttonSwapLanguages = floatingView.findViewById(R.id.button_swap_languages);
        sourceLanguageSpinner = floatingView.findViewById(R.id.source_language_spinner);
        targetLanguageSpinner = floatingView.findViewById(R.id.target_language_spinner);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed here
            }

            @Override
            public void afterTextChanged(Editable s) {
                String textToTranslate = s.toString().trim();
                if (!textToTranslate.isEmpty()) {
                    translateText(textToTranslate);
                } else {
                    // Clear the translation result if input is empty
                    editTextTranslationResult.setText("");
                }
            }
        });

        initializeLanguageCodeMap();

        setupLanguageSpinners();

        closeButton.setOnClickListener(view -> minimizeFloatingWindow());

        stopButton.setOnClickListener(view -> stopService());

        buttonSwapLanguages.setOnClickListener(v-> swapLanguages());

        textInputLayout.setStartIconOnClickListener(v -> {
            String text = editText.getText().toString();
            String srcLanguage = languageCodeMap.get(sourceLanguageSpinner.getSelectedItem().toString());
            if (!text.isEmpty()) {
                speakTextWithAzure(text, srcLanguage);
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            }
        });

        textInputLayout.setEndIconOnClickListener(v -> {
            String textToCopy = editText.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(FloatingWindowService.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        textOutputLayout.setStartIconOnClickListener(v -> {
            String text = editTextTranslationResult.getText().toString();
            String srcLanguage = languageCodeMap.get(targetLanguageSpinner.getSelectedItem().toString());
            if (!text.isEmpty()) {
                speakTextWithAzure(text, srcLanguage);
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            }
        });

        textOutputLayout.setEndIconOnClickListener(v -> {
            String textToCopy = editTextTranslationResult.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(FloatingWindowService.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });
    }

    public void stopService() {
        // Xóa floatingView nếu nó đang được hiển thị
        if (floatingView != null && floatingView.getParent() != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        if(triangleView != null && triangleView.getParent() != null){
            windowManager.removeView(triangleView);
            triangleView = null;
        }
        // Xóa rectangleSelectionView nếu nó đang được hiển thị
        if (rectangleSelectionView != null && rectangleSelectionView.getParent() != null) {
            windowManager.removeView(rectangleSelectionView);
            rectangleSelectionView = null;
        }
        // Dừng MediaProjection và hủy VirtualDisplay nếu đang hoạt động
        if (mediaProjection != null) {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            mediaProjection.stop();
            mediaProjection = null;
        }
        // Dừng dịch vụ và giải phóng các tài nguyên khác nếu cần
        isCaptureInProgress = false;
        stopForeground(true); // Dừng chế độ foreground của service nếu có
        stopSelf(); // Dừng chính dịch vụ này
    }

private void createFloatingWindow() {
    WindowManager.LayoutParams expandedParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
    );

    if (floatingView == null) {
        // Inflate the view only if it's null
        ContextThemeWrapper context = new ContextThemeWrapper(this, R.style.Theme_TranslationApp);
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_window, null);

        setupFloatingWindowControls(); // Initialize controls if it's a fresh inflate

        // Set the touch listener once, since it's applied to the view itself
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = expandedParams.x;
                        initialY = expandedParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        expandedParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        expandedParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, expandedParams);
                        return true;
                }
                return false;
            }
        });
    }

    expandedParams.gravity = Gravity.CENTER;

    windowManager.addView(floatingView, expandedParams); // Add the view back to the window manager
}


    @SuppressLint("ClickableViewAccessibility")
    private void createTriangleView() {
        if(triangleView != null){
            windowManager.removeView(triangleView);
        }
        try {

            triangleView = new TriangleView(this);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    100, 100,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.CENTER;

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.addView(triangleView, params);

            triangleView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (!triangleView.isActivated()) {
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(triangleView, params);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if(Math.abs(event.getRawX() - initialTouchX) < 10 && Math.abs(event.getRawY() - initialTouchY) < 10){
                                triangleView.toggleActivation();
                            }
                            // Kích hoạt chế độ vẽ hình chữ nhật khi nhấn vào tam giác sau khi đã đặt nó ở vị trí mong muốn
                            if (triangleView.isActivated()) {
                                startRectangleSelection();
                            }
                            return true;
                    }
                    return false;
                }
            });

        } catch (Exception e) {
            Log.e("FloatingWindowService", "Error adding TriangleView: " + e.getMessage());
        }
    }

    private void startRectangleSelection() {
        if (rectangleSelectionView != null && rectangleSelectionView.getParent() != null) {
            windowManager.removeView(rectangleSelectionView);
        }

        rectangleSelectionView = new RectangleSelectionView(this);
        rectangleSelectionView.setOnRectangleDrawnListener((startX, startY, endX, endY) -> {
//            Intent screenshotIntent = new Intent(this, ScreenshotRequestActivity.class);
//            screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//            screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
//            startActivity(screenshotIntent);

            if (mediaProjection != null) {
                startVirtualDisplay();
            } else {
                Intent screenshotIntent = new Intent(this, ScreenshotRequestActivity.class);
                screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                startActivity(screenshotIntent);
            }
            rectangleSelectionView.setStartEndCoordinates(startX, startY, endX, endY);
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(rectangleSelectionView, params);
    }

    private Bitmap cropImageDirectly(Image image) {
        float startX = rectangleSelectionView.getStartX();
        float startY = rectangleSelectionView.getStartY();
        float endX = rectangleSelectionView.getEndX();
        float endY = rectangleSelectionView.getEndY();

        int width = Math.round(endX - startX);
        int height = Math.round(rectangleSelectionView.getEndY() - rectangleSelectionView.getStartY());

        if (height <= 0) {
            float tempY = startY;
            startY = endY;
            endY = tempY;
            height = Math.round(endY - startY);
        }

        if(width <= 0){
            float tempX = startX;
            startX = endX;
            endX = tempX;
            width = Math.round(endX - startX);
        }

        final int MIN_SIZE = 35;

        if (width < MIN_SIZE || height < MIN_SIZE) {
            return null;
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();

        Bitmap fullBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        fullBitmap.copyPixelsFromBuffer(buffer);

        return Bitmap.createBitmap(fullBitmap, Math.round(startX), Math.round(startY+35), width, height);
    }


    private void processOCR(Bitmap croppedBitmap) {
        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        String sourceLanguage = languageCodeMap.get(sourceLanguageSpinner.getSelectedItem().toString());
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
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    showOCRResult(text.getText());
                    exitRectangleDrawingMode();
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Failed: " + e.getMessage());
                    resetCaptureState();
                });
    }

    private void showOCRResult(String recognizedText) {
        handler.post(() -> {
            expandFloatingWindow();
            if (editText != null) {
                editText.setText(recognizedText);
                String textToTranslate = editText.getText().toString().trim();
                if (!textToTranslate.isEmpty()) {
                    translateText(textToTranslate);
                }
            }
        });
    }

    private void resetCaptureState() {
        isCaptureInProgress = false;
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private void exitRectangleDrawingMode() {
        if (rectangleSelectionView != null && rectangleSelectionView.getParent() != null) {
            windowManager.removeView(rectangleSelectionView);
        }
        isCaptureInProgress = false;
    }

//------------ Translating part -------------------
// ------------ Translating part -------------------

    private void translateText(String text) {
        TranslatorService.translateText(text, selectedSourceLanguage, selectedTargetLanguage, new TranslatorService.TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                handler.post(() -> editTextTranslationResult.setText(translatedText));
            }

            @Override
            public void onFailure(String error) {
                handler.post(() -> editTextTranslationResult.setText("Lỗi dịch thuật: " + error));
            }
        });
    }

    private void swapLanguages() {
        int sourcePosition = sourceLanguageSpinner.getSelectedItemPosition();
        int targetPosition = targetLanguageSpinner.getSelectedItemPosition();

        if (sourcePosition == 0) {
            Toast.makeText(this, "Không thể đổi với 'Phát hiện ngôn ngữ'", Toast.LENGTH_SHORT).show();
        } else {
            sourceLanguageSpinner.setSelection(targetPosition + 1);
            targetLanguageSpinner.setSelection(sourcePosition - 1);
        }
    }
    private void setupLanguageSpinners() {
        ArrayAdapter<CharSequence> sourceAdapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, android.R.layout.simple_spinner_item);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(sourceAdapter);

        String[] targetLanguages = getResources().getStringArray(R.array.languages_array);
        String[] filteredTargetLanguages = new String[targetLanguages.length - 1];
        System.arraycopy(targetLanguages, 1, filteredTargetLanguages, 0, targetLanguages.length - 1);

        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filteredTargetLanguages);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetLanguageSpinner.setAdapter(targetAdapter);

        sourceLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSourceLanguage = languageCodeMap.get(sourceLanguageSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        targetLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTargetLanguage = languageCodeMap.get(targetLanguageSpinner.getSelectedItem().toString());

                String textToTranslate = editText.getText().toString().trim();
                if (!textToTranslate.isEmpty()) {
                    translateText(textToTranslate);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void speakTextWithAzure(String text, String languageCode) {
        speechConfig = SpeechConfig.fromSubscription(Constants.AZURE_TRANSLATOR_SUBSCRIPTION_KEY, Constants.AZURE_TRANSLATOR_REGION);

        String azureVoiceName = getAzureVoiceFromLanguageCode(languageCode);

        if (azureVoiceName != null) {
            speechConfig.setSpeechSynthesisVoiceName(azureVoiceName);

            speechSynthesizer = new SpeechSynthesizer(speechConfig);

            SpeechSynthesisResult result = speechSynthesizer.SpeakText(text);

            if (result.getReason() == ResultReason.Canceled) {
                SpeechSynthesisCancellationDetails cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result);
                String errorDetails = cancellationDetails.getErrorDetails();
                Toast.makeText(this, "Speech synthesis failed: " + errorDetails, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ngôn ngữ không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private String getAzureVoiceFromLanguageCode(String languageCode) {
        switch (languageCode) {
            case "en":
                return "en-US-JennyNeural";
            case "es":
                return "es-ES-ElviraNeural";
            case "fr":
                return "fr-FR-DeniseNeural";
            case "de":
                return "de-DE-KatjaNeural";
            case "hi":
                return "hi-IN-SwaraNeural";
            case "zh":
                return "zh-CN-XiaoxiaoNeural";
            case "ja":
                return "ja-JP-NanamiNeural";
            case "ru":
                return "ru-RU-DariyaNeural";
            case "vi":
                return "vi-VN-HoaiMyNeural";
            case "ko":
                return "ko-KR-SunHiNeural";
            default:
                return null;
        }
    }

    private void initializeLanguageCodeMap() {
        languageCodeMap = new HashMap<>();
        languageCodeMap.put("Phát hiện ngôn ngữ", "auto");
        languageCodeMap.put("English", "en");
        languageCodeMap.put("Spanish", "es");
        languageCodeMap.put("French", "fr");
        languageCodeMap.put("German", "de");
        languageCodeMap.put("Hindi", "hi");
        languageCodeMap.put("Chinese", "zh");
        languageCodeMap.put("Japanese", "ja");
        languageCodeMap.put("Russian", "ru");
        languageCodeMap.put("Vietnamese", "vi");
        languageCodeMap.put("Korean", "ko");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        if (speechSynthesizer != null) {
            speechSynthesizer.close();
        }
        if (triangleView != null) windowManager.removeView(triangleView);
        if (rectangleSelectionView != null) windowManager.removeView(rectangleSelectionView);
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        Log.d("FloatingWindowService", "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}