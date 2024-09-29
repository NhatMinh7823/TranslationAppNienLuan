package com.example.translationapp;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
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

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private EditText editTextTranslationResult;
    private EditText editText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Spinner sourceLanguageSpinner, targetLanguageSpinner;
    private Map<String, String> languageCodeMap;
    private String selectedSourceLanguage, selectedTargetLanguage;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer speechSynthesizer;

    @Override
    public void onCreate() {
        super.onCreate();

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null);

        // Initialize UI elements
        editTextTranslationResult = floatingView.findViewById(R.id.textView_result);
        editText = floatingView.findViewById(R.id.editText_input);
        Button closeButton = floatingView.findViewById(R.id.close_button);
        Button translateButton = floatingView.findViewById(R.id.button_translate);
        Button voiceButton = floatingView.findViewById(R.id.button_voice_translation);
        Button uploadButton = floatingView.findViewById(R.id.button_upload_image);
        Button takePictureButton = floatingView.findViewById(R.id.button_take_picture);
        Button copyButton = floatingView.findViewById(R.id.button_copy_result);
        Button speakButton = floatingView.findViewById(R.id.button_speak_result);
        Button buttonSwapLanguages = floatingView.findViewById(R.id.button_swap_languages);
        sourceLanguageSpinner = floatingView.findViewById(R.id.source_language_spinner);
        targetLanguageSpinner = floatingView.findViewById(R.id.target_language_spinner);

        // Setup language spinners and map
        initializeLanguageCodeMap();
        setupLanguageSpinners();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                700,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

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
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        closeButton.setOnClickListener(view -> stopSelf());
        buttonSwapLanguages.setOnClickListener(v-> swapLanguages());
        translateButton.setOnClickListener(view -> {
            String textToTranslate = editText.getText().toString().trim();
            if (!textToTranslate.isEmpty()) {
                translateText(textToTranslate);
            } else {
                Toast.makeText(this, "Vui lòng nhập văn bản để dịch", Toast.LENGTH_SHORT).show();
            }
        });

        voiceButton.setOnClickListener(view -> {
            Toast.makeText(this, "Voice translation feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        uploadButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        takePictureButton.setOnClickListener(view -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // Handle copy button functionality
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

        // Check if sourcePosition is 0 (Phát hiện ngôn ngữ) and handle it separately
        if (sourcePosition == 0) {
            // If "Phát hiện ngôn ngữ" is selected in the source spinner, we cannot swap with target language
            Toast.makeText(this, "Không thể đổi với 'Phát hiện ngôn ngữ'", Toast.LENGTH_SHORT).show();
        } else {
            // Swap the positions between source and target, adjusting the positions accordingly
            sourceLanguageSpinner.setSelection(targetPosition + 1); // Add 1 to account for the extra option in the source spinner
            targetLanguageSpinner.setSelection(sourcePosition - 1); // Subtract 1 to match the target spinner without "Phát hiện ngôn ngữ"
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

        // Set listener to update selected language
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
        // Initialize SpeechConfig with subscription and region
        speechConfig = SpeechConfig.fromSubscription(Constants.AZURE_TRANSLATOR_SUBSCRIPTION_KEY, Constants.AZURE_TRANSLATOR_REGION);

        // Get the correct Azure voice name based on languageCode
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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
