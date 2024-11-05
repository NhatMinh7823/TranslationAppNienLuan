package com.example.translationapp;

import static com.example.translationapp.TranslatorService.translateText;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText editTextInput, editTextTranslationResult;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    private Spinner sourceLanguageSpinner, targetLanguageSpinner;

    private SpeechSynthesizer speechSynthesizer;

    // Language map to store language codes corresponding to spinner values
    private Map<String, String> languageCodeMap;
    private static final int REQUEST_CODE_VOICE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check if overlay permission is granted after returning from settings
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        // Start the floating window service if permission is granted
                        startFloatingWindowService();
                    } else {
                        // Permission denied, show a message
                        Toast.makeText(this, "Quyền vẽ trên ứng dụng khác không được cấp!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        setUpUI();
    }
    private void setUpUI(){
        // Initialize UI elements
        editTextInput = findViewById(R.id.editText_input);
        editTextTranslationResult = findViewById(R.id.editText_translationResult);

        TextInputLayout textInputLayout = findViewById(R.id.text_input_layout);
        TextInputLayout textOutputLayout = findViewById(R.id.text_output_layout);
        ImageButton buttonVoiceTranslation = findViewById(R.id.button_voice_translation);
        ImageButton buttonUploadImage = findViewById(R.id.button_upload_image);
        ImageButton buttonTakePicture = findViewById(R.id.button_take_picture);
        ImageButton buttonSwapLanguages = findViewById(R.id.button_swap_languages);
        Button startFloatingWindowButton = findViewById(R.id.button_start_floating_window);
        sourceLanguageSpinner = findViewById(R.id.source_language_spinner);
        targetLanguageSpinner = findViewById(R.id.target_language_spinner);

        // Xử lý sự kiện cho các nút chức năng
        buttonVoiceTranslation.setOnClickListener(v -> startVoiceRecognition());

        buttonUploadImage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        });

        buttonTakePicture.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        textInputLayout.setStartIconOnClickListener(v -> {
            String text = editTextInput.getText().toString();
            String srcLanguage = languageCodeMap.get(sourceLanguageSpinner.getSelectedItem().toString());
            if (!text.isEmpty()) {
                speakTextWithAzure(text, srcLanguage);
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            }
        });

        textInputLayout.setEndIconOnClickListener(v -> {
            String textToCopy = editTextInput.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        initializeLanguageCodeMap();
        setupLanguageSpinners();
        buttonSwapLanguages.setOnClickListener(v -> swapLanguages());

        // Handle start floating window button
        startFloatingWindowButton.setOnClickListener(v -> {
            // Check if overlay permission is granted
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                // If permission is not granted, request it
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                overlayPermissionLauncher.launch(intent);  // Use the launcher to start the permission intent
            } else {
                // If permission is already granted, start the floating window service
                startFloatingWindowService();
            }
        });

        editTextInput.addTextChangedListener(new TextWatcher() {
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
                    String sourceLanguage = sourceLanguageSpinner.getSelectedItem().toString();
                    String targetLanguage = targetLanguageSpinner.getSelectedItem().toString();
                    String sourceLangCode = languageCodeMap.get(sourceLanguage);
                    String targetLangCode = languageCodeMap.get(targetLanguage);
                    translateText(textToTranslate, sourceLangCode, targetLangCode);
                }  else {
                    // Clear the translation result if input is empty
                    editTextTranslationResult.setText("");
                }
            }
        });
    }
    // Method to speak the given text
    private void speakTextWithAzure(String text, String languageCode) {
        // Initialize SpeechConfig with subscription and region
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(Constants.AZURE_TRANSLATOR_SUBSCRIPTION_KEY, Constants.AZURE_TRANSLATOR_REGION);

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
                Toast.makeText(MainActivity.this, "Speech synthesis failed: " + errorDetails, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Ngôn ngữ không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    // Map language codes to Azure voice names
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

    // Method to set up the language spinners
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

        targetLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTargetLanguage = languageCodeMap.get(targetLanguageSpinner.getSelectedItem().toString());
                String selectedSourceLanguage = languageCodeMap.get(sourceLanguageSpinner.getSelectedItem().toString());

                String textToTranslate = editTextInput.getText().toString().trim();
                if (!textToTranslate.isEmpty()) {
                    translateText(textToTranslate, selectedSourceLanguage, selectedTargetLanguage);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // Method to swap the source and target languages
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

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Set language from the source spinner selection using BCP-47 language tag
        String sourceLanguageCode = LanguageUtils.getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
        Locale sourceLocale = Locale.forLanguageTag(sourceLanguageCode);  // Use BCP-47 tag

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLocale.toLanguageTag()); // Set the language tag directly

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_VOICE);
        } else {
            Toast.makeText(this, "Voice recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VOICE && resultCode == RESULT_OK) {
            if (data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String recognizedText = result.get(0);
                    editTextInput.setText(recognizedText);

                    // Translate recognized text using LanguageUtils
                    String sourceLanguage = sourceLanguageSpinner.getSelectedItem().toString();
                    String targetLanguage = targetLanguageSpinner.getSelectedItem().toString();
                    String sourceLangCode = languageCodeMap.get(sourceLanguage);
                    String targetLangCode = languageCodeMap.get(targetLanguage);
                    translateText(recognizedText, sourceLangCode, targetLangCode);
                }
            }
        }
    }

    // Method to start the floating window service
    private void startFloatingWindowService() {
        Intent intent = new Intent(this, FloatingWindowService.class);
        startService(intent);
    }

    // Method to translate text
    private void translateText(String text, String sourceLanguage, String targetLanguage) {
        TranslatorService.translateText(text, sourceLanguage, targetLanguage, new TranslatorService.TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                runOnUiThread(() -> editTextTranslationResult.setText(translatedText));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> editTextTranslationResult.setText("Lỗi dịch thuật: " + error));
            }
        });
    }

    // Initialize language code map
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

    protected void onDestroy() {
        if (speechSynthesizer != null) {
            speechSynthesizer.close();
        }
        super.onDestroy();
    }
}