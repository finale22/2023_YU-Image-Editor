package com.yufinal.editor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.yufinal.editor.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {

    ActivityResultBinding binding;

    private void displayProcessedImage(Uri processedImageUri) {
        Glide.with(this)
                .load(processedImageUri)
                .into(binding.resultImage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Uri processedImageUri = getIntent().getData();
        if (processedImageUri != null) {
            displayProcessedImage(processedImageUri);
        }

        getSupportActionBar().hide();
        binding.resultImage.setImageURI(getIntent().getData());

        binding.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareImage(processedImageUri);
            }
        });
    }

    private void shareImage(Uri imageUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }
}
