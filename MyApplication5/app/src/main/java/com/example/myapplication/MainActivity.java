package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    private static final int REQUEST_IMAGE_PICK = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectImageBtn = (Button) findViewById(R.id.selectImageBtn);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 이미지 선택 액티비티 호출 (갤러리에서 이미지 선택)
                Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickImageIntent.setType("image/*");
                if (pickImageIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            // 선택한 이미지를 처리하고 서버로 업로드하는 AsyncTask를 실행
            Bitmap imageBitmap = getSelectedImage(data);
            new UploadImageTask().execute(imageBitmap);
        }
    }

    private Bitmap getSelectedImage(Intent data) {
        try {
            // 선택한 이미지의 URI를 통해 비트맵 이미지를 가져옵니다.
            Uri imageUri = data.getData();
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class UploadImageTask extends AsyncTask<Bitmap, Void, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap imageBitmap = bitmaps[0];

            // 이미지를 파일로 저장
            File imageFile = saveImageToFile(imageBitmap);

            if (imageFile != null) {
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", imageFile.getName(), RequestBody.create(MultipartBody.FORM, imageFile))
                        .build();
                Request request = new Request.Builder()
                        .url("http://192.168.137.107:5000/upload")
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient();

                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        return response.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d("TEST : ", result);

                // 이미지 다운로드를 시작
                new DownloadImageTask().execute();
            } else {
                Log.d("TEST", "Request failed.");
            }
        }

        private File saveImageToFile(Bitmap imageBitmap) {
            // 이미지를 저장할 내부 저장소의 파일 디렉토리를 가져옵니다.
            File filesDir = getFilesDir();
            File imageFile = new File(filesDir, "image.jpg"); // 이미지 파일을 생성하고 파일 이름을 지정합니다.

            try {
                FileOutputStream fos = new FileOutputStream(imageFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos); // 이미지를 JPEG 형식으로 저장합니다.
                fos.close();
                return imageFile; // 저장한 이미지 파일을 반환합니다.
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                // 이미지 URL을 서버로부터 받아옴
                String imageUrl = "http://192.168.137.107:5000/download/processed_image.jpg";

                // 이미지 다운로드
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                // 다운로드한 이미지를 이미지 뷰에 표시
                ImageView imageView = findViewById(R.id.imageView); // 이미지를 표시할 이미지 뷰
                imageView.setImageBitmap(result);
            } else {
                Log.d("TEST", "Image download failed.");
            }
        }
    }
}
