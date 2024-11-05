package com.example.translationapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.textfield.TextInputLayout;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

import java.util.HashMap;
import java.util.Map;

public class LanguageUtils {
    private static final Map<String, String> languageCodeMap = new HashMap<>();
    private static SpeechSynthesizer speechSynthesizer;

    static {
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

    public static void setupLanguageSpinners(Context context, Spinner sourceSpinner, Spinner targetSpinner) {
        ArrayAdapter<CharSequence> sourceAdapter = ArrayAdapter.createFromResource(context,
                R.array.languages_array, android.R.layout.simple_spinner_item);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(sourceAdapter);

        String[] targetLanguages = context.getResources().getStringArray(R.array.languages_array);
        String[] filteredTargetLanguages = new String[targetLanguages.length - 1];
        System.arraycopy(targetLanguages, 1, filteredTargetLanguages, 0, targetLanguages.length - 1);

        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, filteredTargetLanguages);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetSpinner.setAdapter(targetAdapter);
    }

    public static String getLanguageCode(String language) {
        return languageCodeMap.getOrDefault(language, "auto");
    }

    public static void swapLanguages(Context context, Spinner sourceSpinner, Spinner targetSpinner) {
        int sourcePosition = sourceSpinner.getSelectedItemPosition();
        int targetPosition = targetSpinner.getSelectedItemPosition();

        if (sourcePosition == 0) {
            Toast.makeText(context, "Không thể đổi với 'Phát hiện ngôn ngữ'", Toast.LENGTH_SHORT).show();
        } else {
            sourceSpinner.setSelection(targetPosition + 1);
            targetSpinner.setSelection(sourcePosition - 1);
        }
    }

    public static void setupCopyButtons(Context context, TextInputLayout sourceCopyButton, TextInputLayout targetCopyButton, EditText sourceEditText, EditText targetEditText) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        sourceCopyButton.setEndIconOnClickListener(v -> copyTextToClipboard(context, clipboard, "Source Text", sourceEditText.getText().toString()));
        targetCopyButton.setEndIconOnClickListener(v -> copyTextToClipboard(context, clipboard, "Translated Text", targetEditText.getText().toString()));
    }

    public static void setupCopyButton(Context context, TextInputLayout editText, TextView sourceEditText) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        editText.setEndIconOnClickListener(v -> copyTextToClipboard(context, clipboard, "Source Text", sourceEditText.getText().toString()));
    }

    public static void setupCopyButton(Context context, ImageButton editText, TextView textView) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        editText.setOnClickListener(v -> copyTextToClipboard(context, clipboard, "Source Text", textView.getText().toString()));
    }

    private static void copyTextToClipboard(Context context, ClipboardManager clipboard, String label, String text) {
        if (!text.isEmpty()) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show();
        }
    }

private static void speakTextWithAzure(Context context, String text, String languageCode) {
    SpeechConfig speechConfig = SpeechConfig.fromSubscription(Constants.AZURE_TRANSLATOR_SUBSCRIPTION_KEY, Constants.AZURE_TRANSLATOR_REGION);

    String azureVoiceName = getAzureVoiceFromLanguageCode(languageCode);

    if (azureVoiceName != null) {
        speechConfig.setSpeechSynthesisVoiceName(azureVoiceName);
        speechSynthesizer = new SpeechSynthesizer(speechConfig);
        SpeechSynthesisResult result = speechSynthesizer.SpeakText(text);

        if (result.getReason() == ResultReason.Canceled) {
            SpeechSynthesisCancellationDetails cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result);
            String errorDetails = cancellationDetails.getErrorDetails();
            Toast.makeText(context, "Speech synthesis failed: " + errorDetails, Toast.LENGTH_SHORT).show();
        }
    } else {
        Toast.makeText(context, "Ngôn ngữ không hợp lệ", Toast.LENGTH_SHORT).show();
    }
}

    public static void setupSpeakButtons(Context context, TextInputLayout sourceSpeakButton, TextInputLayout targetSpeakButton,
                                         EditText sourceEditText, EditText targetEditText,
                                         Spinner sourceLanguageSpinner, Spinner targetLanguageSpinner) {

        sourceSpeakButton.setStartIconOnClickListener(v -> {
            String sourceText = sourceEditText.getText().toString();
            String sourceLang = getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
            speakTextWithAzure(context, sourceText, sourceLang);
        });

        targetSpeakButton.setStartIconOnClickListener(v -> {
            String targetText = targetEditText.getText().toString();
            String targetLang = getLanguageCode(targetLanguageSpinner.getSelectedItem().toString());
            speakTextWithAzure(context, targetText, targetLang);
        });
    }


    private static String getAzureVoiceFromLanguageCode(String languageCode) {
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
}
