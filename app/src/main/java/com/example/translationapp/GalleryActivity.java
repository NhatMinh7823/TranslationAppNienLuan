package com.example.translationapp;

import static com.example.translationapp.LanguageUtils.swapLanguages;
import static com.example.translationapp.TranslatorService.translateText;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;

public class GalleryActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView imagePreview;
    private TextView translationResult;
    private Spinner sourceLanguageSpinner, targetLanguageSpinner;
    private String recognizedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Button selectImageButton = findViewById(R.id.button_select_image);
        imagePreview = findViewById(R.id.image_preview);
        translationResult = findViewById(R.id.translationResult);
        sourceLanguageSpinner = findViewById(R.id.source_language_spinner);
        targetLanguageSpinner = findViewById(R.id.target_language_spinner);
        ImageButton buttonSwapLanguages = findViewById(R.id.button_swap_languages);
        TextInputLayout textLayout = findViewById(R.id.textInputLayout_translationResult);
        ImageButton buttonHome = findViewById(R.id.button_home);
        buttonHome.setOnClickListener(v -> {
            Intent intent = new Intent(GalleryActivity.this, MainActivity.class);
            startActivity(intent);
        });

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        LanguageUtils.setupLanguageSpinners(this, sourceLanguageSpinner, targetLanguageSpinner);
        buttonSwapLanguages.setOnClickListener(v -> swapLanguages(this, sourceLanguageSpinner, targetLanguageSpinner));
        LanguageUtils.setupCopyButton(this, textLayout, translationResult);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    imagePreview.setImageBitmap(bitmap);

                    // Nhận diện văn bản từ hình ảnh đã chọn
                    InputImage image = InputImage.fromBitmap(bitmap, 0);
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
                    recognizer.process(image)
                            .addOnSuccessListener(this::processTextRecognitionResult)
                            .addOnFailureListener(e -> translationResult.setText("Lỗi nhận diện văn bản: " + e.getMessage()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processTextRecognitionResult(Text text) {
        String sourceLanguage = LanguageUtils.getLanguageCode(sourceLanguageSpinner.getSelectedItem().toString());
        String targetLanguage = LanguageUtils.getLanguageCode(targetLanguageSpinner.getSelectedItem().toString());
        recognizedText = text.getText();
        if (!recognizedText.isEmpty()) {
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
    }
}
