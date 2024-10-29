package com.example.translationapp;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
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
        createTriangleView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(screenshotReceiver, new IntentFilter("com.example.ACTION_SCREENSHOT"), Context.RECEIVER_NOT_EXPORTED);
        }
    }
//    -------------- End of onCreate() -------------------
//    -------------- End of onCreate() -------------------
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
                    // Nhận được quyền từ Activity
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                    startVirtualDisplay(); // Bắt đầu tạo Virtual Display để chụp màn hình
                }
            } else {
                // Nếu đã có mediaProjection, bắt đầu ngay việc chụp màn hình
                startVirtualDisplay();
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
                .setSmallIcon(R.drawable.ic_translate)  // Bạn cần thêm icon phù hợp vào project của mình
                .build();

        startForeground(1, notification);
    }

    private void startVirtualDisplay() {
        if (mediaProjection == null || isCaptureInProgress) {
            return;
        }

        // Mark the capture as in progress
        isCaptureInProgress = true;

        // Setup Virtual Display
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
        );

        // Listener for ImageReader
        imageReader.setOnImageAvailableListener(reader -> {
            if (isCaptureInProgress) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    if (planes.length > 0) {
                        ByteBuffer buffer = planes[0].getBuffer();
                        int width = image.getWidth();
                        int height = image.getHeight();
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        image.close();

                        // Get coordinates from RectangleSelectionView
                        float startX = rectangleSelectionView.getStartX();
                        float startY = rectangleSelectionView.getStartY();
                        float endX = rectangleSelectionView.getEndX();
                        float endY = rectangleSelectionView.getEndY();

                        // Crop the captured bitmap based on the drawn rectangle
                        Bitmap croppedBitmap = cropBitmap(bitmap, startX, startY, endX, endY);
                        processOCR(croppedBitmap);  // OCR processing and reset state after completion
                    }
                }
            }
        }, handler);
    }

    @SuppressLint("InflateParams")
    private void expandFloatingWindow() {
        if (floatingView == null) {
            createFloatingWindow();
        } else if (floatingView.getParent() == null) {
            // Nếu cửa sổ nổi đã tồn tại nhưng chưa được thêm vào WindowManager, thêm nó vào
            windowManager.addView(floatingView, floatingView.getLayoutParams());
            setupFloatingWindowControls(); // Cài đặt lại các điều khiển nếu cần thiết
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void minimizeFloatingWindow() {
        if (floatingView != null && floatingView.getParent() != null) {
            windowManager.removeView(floatingView);
        }
//        createTriangleView();
//        triangleView.toggleActivation();
    }

    private void setupFloatingWindowControls() {
        editText = floatingView.findViewById(R.id.editText_input);
        editTextTranslationResult = floatingView.findViewById(R.id.textView_result);
        Button closeButton = floatingView.findViewById(R.id.close_button);
        Button translateButton = floatingView.findViewById(R.id.button_translate);
        Button copyButton = floatingView.findViewById(R.id.button_copy_result);
        Button speakButton = floatingView.findViewById(R.id.button_speak_result);
        Button buttonSwapLanguages = floatingView.findViewById(R.id.button_swap_languages);
        sourceLanguageSpinner = floatingView.findViewById(R.id.source_language_spinner);
        targetLanguageSpinner = floatingView.findViewById(R.id.target_language_spinner);

        initializeLanguageCodeMap();

        setupLanguageSpinners();

        closeButton.setOnClickListener(view -> minimizeFloatingWindow());

        buttonSwapLanguages.setOnClickListener(v-> swapLanguages());

        translateButton.setOnClickListener(view -> {
            String textToTranslate = editText.getText().toString().trim();
            if (!textToTranslate.isEmpty()) {
                translateText(textToTranslate);
            } else {
                Toast.makeText(this, "Vui lòng nhập văn bản để dịch", Toast.LENGTH_SHORT).show();
            }
        });

        copyButton.setOnClickListener(view -> {
            String textToCopy = editTextTranslationResult.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(FloatingWindowService.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        speakButton.setOnClickListener(v -> {
            String text = editTextTranslationResult.getText().toString();
            String targetLanguage = languageCodeMap.get(targetLanguageSpinner.getSelectedItem().toString());

            if (!text.isEmpty()) {
                speakTextWithAzure(text, targetLanguage);
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createFloatingWindow() {
        if (floatingView != null) {
            Toast.makeText(this, "FloatingWindow have been created", Toast.LENGTH_SHORT).show();
            return;
        }
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null);

        WindowManager.LayoutParams expandedParams = new WindowManager.LayoutParams(
                700,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        expandedParams.gravity = Gravity.TOP | Gravity.START;
        expandedParams.x = 0;
        expandedParams.y = 100;

        windowManager.addView(floatingView, expandedParams);

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
        setupFloatingWindowControls();
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
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 300;
            params.y = 800;

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
            Intent screenshotIntent = new Intent(this, ScreenshotRequestActivity.class);
            screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivity(screenshotIntent);
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



    private final BroadcastReceiver screenshotReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.ACTION_SCREENSHOT".equals(intent.getAction())) {
                    Bitmap screenshot = takeScreenshot();
                    if (screenshot != null) {
                        // Sử dụng tọa độ đã vẽ từ RectangleSelectionView
                        float startX = rectangleSelectionView.getStartX();
                        float startY = rectangleSelectionView.getStartY();
                        float endX = rectangleSelectionView.getEndX();
                        float endY = rectangleSelectionView.getEndY();
                        Bitmap cropped = cropBitmap(screenshot, startX, startY, endX, endY);
                        processOCR(cropped);
                    }
            }
        }
    };

    private Bitmap takeScreenshot() {
        if (mediaProjection == null) {
            Log.e("FloatingWindowService", "MediaProjection is null, cannot take screenshot.");
            return null;
        }
        // Thiết lập các thuộc tính cho VirtualDisplay
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int density = getResources().getDisplayMetrics().densityDpi;

        // Tạo ImageReader để lấy ảnh màn hình
        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        virtualDisplay = mediaProjection.createVirtualDisplay("Screenshot",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        Image image = null;
        Bitmap bitmap = null;

        try {
            // Đợi cho đến khi ImageReader có thể nhận hình ảnh
            image = imageReader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                // Tạo bitmap từ dữ liệu của Image
                bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
            }
        } catch (Exception e) {
            Log.e("FloatingWindowService", "Error taking screenshot: " + e.getMessage());
        } finally {
            if (image != null) {
                image.close();
            }
            imageReader.close();
            //            if (virtualDisplay != null) {
//                virtualDisplay.release();
//            }
        }

        return bitmap;
    }

    private Bitmap cropBitmap(Bitmap screenshot, float startX, float startY, float endX, float endY) {
        int width = (int) (endX - startX);
        int height = (int) (endY - startY);

        if (width <= 0 || height <= 0) {
            Log.e("FloatingWindowService", "Invalid crop dimensions: width=" + width + ", height=" + height);
            return null;
        }

        return Bitmap.createBitmap(screenshot, (int) startX, (int) startY, width, height);
    }

//    private void processOCR(Bitmap croppedBitmap) {
//        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
//        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
//        recognizer.process(image)
//                .addOnSuccessListener(text -> showOCRResult(text.getText()))
//                .addOnFailureListener(e -> Log.e("OCR", "Failed: " + e.getMessage()));
//    }

    private void processOCR(Bitmap croppedBitmap) {
        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    // Show OCR result and reset state
                    showOCRResult(text.getText());
                    exitRectangleDrawingMode();
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Failed: " + e.getMessage());
                    resetCaptureState(); // Reset flag on failure too
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
            triangleView.toggleActivation();
        });
    }
    private void resetCaptureState() {
        isCaptureInProgress = false; // Allow new captures
        // Release resources related to virtual display if needed
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private void exitRectangleDrawingMode() {
        // Ensure the RectangleSelectionView is removed from the screen
        if (rectangleSelectionView != null && rectangleSelectionView.getParent() != null) {
            windowManager.removeView(rectangleSelectionView);
        }
        // Reset the drawing state or disable any listeners if needed
        isCaptureInProgress = false; // Reset capture flag to allow future captures if needed
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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void speakTextWithAzure(String text, String languageCode) {
        speechConfig = SpeechConfig.fromSubscription(Constants.AZURE_TRANSLATOR_SUBSCRIPTION_KEY, Constants.AZURE_TRANSLATOR_REGION);

        String azureVoiceName = getAzureVoiceFromLanguageCode(languageCode);

        if (azureVoiceName != null) {
            // Set the voice name for the speech synthesizer
            speechConfig.setSpeechSynthesisVoiceName(azureVoiceName);

            // Now create the speech synthesizer after setting voice name
            speechSynthesizer = new SpeechSynthesizer(speechConfig);

            // Speak the text
            SpeechSynthesisResult result = speechSynthesizer.SpeakText(text);

            // Check if synthesis was canceled
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
                return "en-US-JennyNeural"; // English
            case "es":
                return "es-ES-ElviraNeural"; // Spanish
            case "fr":
                return "fr-FR-DeniseNeural"; // French
            case "de":
                return "de-DE-KatjaNeural"; // German
            case "hi":
                return "hi-IN-SwaraNeural"; // Hindi
            case "zh":
                return "zh-CN-XiaoxiaoNeural"; // Chinese
            case "ja":
                return "ja-JP-NanamiNeural"; // Japanese
            case "ru":
                return "ru-RU-DariyaNeural"; // Russian
            case "vi":
                return "vi-VN-HoaiMyNeural"; // Vietnamese
            case "ko":
                return "ko-KR-SunHiNeural"; // Korean
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
        unregisterReceiver(screenshotReceiver);
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null; // Hủy mediaProjection khi dịch vụ bị hủy
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
