package com.example.translationapp;


import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TranslatorService {

    private static final OkHttpClient client = new OkHttpClient();
    // Modified method to accept sourceLanguage and targetLanguage
    public static void translateText(String textToTranslate, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        try {
                // Create JSON payload
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Text", textToTranslate);
                jsonArray.put(jsonObject);

                RequestBody body = RequestBody.create(
                        jsonArray.toString(),
                        MediaType.get("application/json; charset=utf-8"));

                String requestUrl = Constants.AZURE_TRANSLATOR_ENDPOINT + "&from=" + sourceLanguage + "&to=" + targetLanguage;
                if("auto".equals(sourceLanguage)){
                    requestUrl = Constants.AZURE_TRANSLATOR_ENDPOINT  + "&to=" + targetLanguage;
                }

                // Create HTTP request
                Request request = new Request.Builder()
                        .url(requestUrl)
                        .post(body)
                        .addHeader("Ocp-Apim-Subscription-Key", Constants.AZURE_TRANSLATOR_API_KEY)
                        .addHeader("Ocp-Apim-Subscription-Region", Constants.AZURE_TRANSLATOR_REGION)
                        .addHeader("Content-Type", "application/json")
                        .build();

                // Send the request and handle the response
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        callback.onFailure(e.getMessage() + "Xin hãy thử lại lần nữa ");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            try {
                                JSONArray jsonArray = new JSONArray(responseData);
                                JSONObject translation = jsonArray.getJSONObject(0)
                                        .getJSONArray("translations")
                                        .getJSONObject(0);
                                String translatedText = translation.getString("text");

                                callback.onSuccess(translatedText);

                            } catch (JSONException e) {
                                callback.onFailure("JSON parsing error: " + e.getMessage());
                            }
                        } else {
                            callback.onFailure(response.message() + "Xin hãy thử lại lần nữa ");
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                callback.onFailure(e.getMessage() + "Xin hãy thử lại lần nữa ");
            }
    }

    // Translation callback interface
    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(String error);
    }
}
