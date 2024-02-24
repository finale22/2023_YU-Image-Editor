package com.yufinal.editor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import okhttp3.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.yufinal.editor.databinding.ActivityMainBinding;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    int IMAGE_REQUEST_CODE= 45;
    int CAMERA_REQUEST_CODE= 14;
    int EDITED_IMAGE_RESULT_CODE= 200;
    private static final int RESTORE_IMAGE_REQUEST_CODE = 101;
    private static final String API_BASE_URL = "http://192.168.137.107:5000";

    private OkHttpClient client = new OkHttpClient();


    private void sendImageToApi(Uri imageUri, String endpoint) {
        // Convert Uri to File
        File imageFile = saveImageToFile(imageUri);

        if (imageFile != null && imageFile.exists()) {
            // File exists, proceed with API call
            MediaType mediaType = MediaType.parse("image/*");
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", imageFile.getName(), RequestBody.create(mediaType, imageFile))
                    .build();

            // Ensure correct URL format by appending endpoint with a forward slash
            String apiUrl = API_BASE_URL + "/upload";

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    Log.e("APIRequest", "API request failed: " + e.getMessage());
                    // Handle API request failure, e.g., show an error message to the user
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String responseData = response.body().string();
                        Log.d("APIResponse", "API response: " + responseData);
                        // Assuming responseData contains the URL of the downloaded image
                        String imageUrl = responseData;

                        // Create an instance of DownloadImageTask and execute it
                        runOnUiThread(() -> {
                            DownloadImageTask downloadImageTask = new DownloadImageTask(MainActivity.this, binding.imageView);
                            downloadImageTask.execute(imageUrl);
                        });
                    } else {
                        Log.e("APIResponse", "API request failed with code: " + response.code());
                        // Handle non-successful API response, e.g., show an error message to the user
                    }
                }
            });
        } else {
            Log.e("Error", "File does not exist or could not be created");
            // Handle the case where the file does not exist or could not be created, e.g., show an error message to the user
        }
    }

    private File saveImageToFile(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            File filesDir = getFilesDir(); // Internal storage directory
            File imageFile = new File(filesDir, "image.jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper function to get real path from URI
    private String getRealPathFromURI(Uri contentUri) {
        String filePath = null;
        if (contentUri.getScheme().equals("content")) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    filePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        } else if (contentUri.getScheme().equals("file")) {
            filePath = contentUri.getPath();
        }
        return filePath;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // admob initialization
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        View downloadedImageViewLayout = getLayoutInflater().inflate(R.layout.activity_display_image, null);
        ImageView downloadedImageView = downloadedImageViewLayout.findViewById(R.id.downloadedImageView);
        LinearLayout imageContainer = findViewById(R.id.imageContainer);
        imageContainer.addView(downloadedImageViewLayout);
        getSupportActionBar().hide();

        binding.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
            }
        });

        binding.cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, 32);
                }
                else{
                    Intent cameraIntent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                }
            }
        });

        binding.restoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, RESTORE_IMAGE_REQUEST_CODE);
                } else {
                    Toast.makeText(MainActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MyApp", "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        if(requestCode == IMAGE_REQUEST_CODE){
            if(data.getData() != null){
                Log.d("MyApp", "Received image from gallery. Data: " + data.getData().toString());
                Intent dsPhotoEditorIntent = new Intent(this, DsPhotoEditorActivity.class);
                dsPhotoEditorIntent.setData(data.getData());

                // directory for edited images
                dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "picaso");

                int[] toolsToHide = {DsPhotoEditorActivity.TOOL_ORIENTATION, DsPhotoEditorActivity.TOOL_CROP};
                dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, toolsToHide);
                startActivityForResult(dsPhotoEditorIntent, EDITED_IMAGE_RESULT_CODE);
            }
        }

        if(requestCode == EDITED_IMAGE_RESULT_CODE){
            if(data.getData()!=null) {
                Log.d("MyApp", "Received edited image. Data: " + data.getData().toString());
                Toast.makeText(this, "Image saved to gallery.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.setData(data.getData());
                startActivity(intent);
            }
        }

        if (requestCode == CAMERA_REQUEST_CODE) {
            Log.d("MyApp", "Received image from camera.");
            Bitmap cameraPhoto = (Bitmap) data.getExtras().get("data");
            Uri uri = getImageUriFromBitmap(this, cameraPhoto);

            // navigating to edit activity after capturing image from camera
            Intent dsPhotoEditorIntent = new Intent(this, DsPhotoEditorActivity.class);
            dsPhotoEditorIntent.setData(uri);

            // directory for edited images
            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "picaso");

            int[] toolsToHide = {DsPhotoEditorActivity.TOOL_ORIENTATION, DsPhotoEditorActivity.TOOL_CROP};
            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, toolsToHide);
            startActivityForResult(dsPhotoEditorIntent, EDITED_IMAGE_RESULT_CODE);
        }

        if (requestCode == RESTORE_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                Log.d("ActivityResult", "Restored Image Uri: " + imageUri.toString());
                // Send the loaded Uri to the API
                sendImageToApi(getImageUriFromUri(imageUri), "restore");
            }
        }
    }

    public Uri getImageUriFromBitmap(Context context, Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", "Desc");
        return Uri.parse(path);
    }

    public Uri getImageUriFromUri(Uri uri) {
        return uri;
    }
}