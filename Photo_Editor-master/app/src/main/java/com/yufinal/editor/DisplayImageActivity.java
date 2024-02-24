package com.yufinal.editor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DisplayImageActivity extends AppCompatActivity {

    private ImageView displayedImageView;
    private Bitmap imageBitmap;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);

        displayedImageView = findViewById(R.id.downloadedImageView);

        // Retrieve the Bitmap from the intent
        imageBitmap = getIntent().getParcelableExtra("imageBitmap");

        if (imageBitmap != null) {
            // Set the Bitmap to the ImageView
            displayedImageView.setImageBitmap(imageBitmap);
        } else {
            // Handle the case where the Bitmap is null
            Log.e("DisplayImageActivity", "Bitmap is null");
        }

        Button saveToGalleryButton = findViewById(R.id.saveToGalleryButton);
        saveToGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageToGallery();
            }
        });
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to MainActivity
                Intent intent = new Intent(DisplayImageActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional, depending on whether you want to finish the DisplayImageActivity
            }
        });
    }

    private void saveImageToGallery() {
        if (imageBitmap != null) {
            // Save the image to the gallery
            String savedImagePath = saveImage(imageBitmap);

            // Notify the user about the successful save
            if (savedImagePath != null) {
                Toast.makeText(this, "Image saved to Gallery", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveImage(Bitmap bitmap) {
        String savedImagePath = null;

        // Create a file name for the saved image
        String imageFileName = "saved_image_" + System.currentTimeMillis() + ".jpg";

        // Get the directory where the image will be saved
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/YourAppName");

        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Create the file where the image will be saved
        File imageFile = new File(storageDir, imageFileName);
        savedImagePath = imageFile.getAbsolutePath();

        try {
            // Create an output stream to save the image
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            // Notify the system about the new image
            MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);

        } catch (IOException e) {
            e.printStackTrace();
            savedImagePath = null;
        }

        return savedImagePath;
    }

}
