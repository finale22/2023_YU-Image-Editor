package com.yufinal.editor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private MainActivity mActivity; // Reference to MainActivity
    private ImageView imageView; // ImageView to update with the downloaded image

    public DownloadImageTask(MainActivity activity, ImageView imageView) {
        this.mActivity = activity;
        this.imageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        String responseString = urls[0];
        try {
            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(responseString);

            // Check if the API upload was successful
            String isUploadSuccess = jsonResponse.optString("isUploadSuccess");

            if (isUploadSuccess.equals("success")) {
                // Retrieve the image URL from the response
                String imageUrl = "http://192.168.137.107:5000/download/processed_image.jpg";

                // Ensure that the image URL is not empty
                if (!imageUrl.isEmpty()) {
                    // Download image from the corrected URL
                    InputStream input = new URL(imageUrl).openStream();

                    // Decode the input stream into a Bitmap
                    return BitmapFactory.decodeStream(input);
                } else {
                    Log.e("DownloadImageTask", "Image URL is empty in the JSON response");
                }
            } else {
                Log.e("DownloadImageTask", "API upload was not successful");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null && mActivity != null) {
            mActivity.runOnUiThread(() -> {
                // Start DisplayImageActivity with the image Bitmap
                Intent displayImageIntent = new Intent(mActivity, DisplayImageActivity.class);
                displayImageIntent.putExtra("imageBitmap", result);
                mActivity.startActivity(displayImageIntent);
            });
        }
    }
}
