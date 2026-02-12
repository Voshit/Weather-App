package com.weatherapp.app.logic;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.weatherapp.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HuggingFaceService {

    private static final String API_URL = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2";
    private Context context;
    private String apiKey;

    public interface SummaryCallback {
        void onSuccess(String summary);
        void onError(String error);
    }

    public HuggingFaceService(Context context) {
        this.context = context;
        this.apiKey = com.weatherapp.app.BuildConfig.HUGGING_FACE_API_KEY;
    }

    public void generateWeatherSummary(String weatherDescription, double temp, double high, double low, double rainChance, int aqi, SummaryCallback callback) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            callback.onError("No API Key configured");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        String prompt = "<s>[INST] You are a friendly weather reporter. Write a very short (max 25 words), witty weather summary for today based on this data:\n" +
                "Condition: " + weatherDescription + "\n" +
                "Temperature: " + (int)temp + "°C\n" +
                "High: " + (int)high + "°C\n" +
                "Rain Chance: " + (int)rainChance + "%\n" +
                "AQI: " + aqi + "\n" +
                "Do not repeat the data exactly, just give the vibe. [/INST]";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("inputs", prompt);
            JSONObject parameters = new JSONObject();
            parameters.put("max_new_tokens", 50);
            parameters.put("temperature", 0.8); // Slightly creative
            parameters.put("return_full_text", false);
            jsonBody.put("parameters", parameters);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Using JsonArrayRequest as HF returns an array of results
        com.android.volley.toolbox.JsonArrayRequest request = new com.android.volley.toolbox.JsonArrayRequest(Request.Method.POST, API_URL, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            String text = response.getJSONObject(0).getString("generated_text");
                            callback.onSuccess(text.trim());
                        } else {
                            callback.onError("Empty Response");
                        }
                    } catch (JSONException e) {
                        Log.e("HF_API", "JSON Parse Error: " + e.getMessage());
                        callback.onError("Parse Error");
                    }
                },
                error -> {
                    Log.e("HF_API", "Volley Error: " + error.toString());
                    callback.onError("Network Error");
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + apiKey);
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public byte[] getBody() {
                return jsonBody.toString().getBytes();
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
    }
}
