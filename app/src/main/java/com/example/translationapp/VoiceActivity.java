package com.example.translationapp;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VOICE = 100;
    private TextView transcriptionTextView;
    private TextView translationResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        transcriptionTextView = findViewById(R.id.text_transcription);
        translationResult = findViewById(R.id.translationResult);
        Button startVoiceButton = findViewById(R.id.button_start_voice);

        startVoiceButton.setOnClickListener(v -> startVoiceRecognition());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_VOICE);
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
                    transcriptionTextView.setText(recognizedText);

                    // Dịch văn bản nhận diện từ giọng nói
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
                }
            }
        }
    }
}
