package com.example.translationapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;

public class GalleryActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView imagePreview;
    private TextView translationResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Button selectImageButton = findViewById(R.id.button_select_image);
        imagePreview = findViewById(R.id.image_preview);
        translationResult = findViewById(R.id.translationResult);

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
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
                    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
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
}
