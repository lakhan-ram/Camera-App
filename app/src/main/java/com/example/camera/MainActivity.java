package com.example.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ImageCapture imageCapture = null;
    File file;
    ExecutorService executorService;
    PreviewView previewView;
    ImageView ivClickedImage;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA};
    private static final int REQUEST_CODE = 10;
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        previewView = findViewById(R.id.previewView);
        ImageButton btnClickImage = findViewById(R.id.btnClickImage);
        ivClickedImage = findViewById(R.id.ivClickedImage);

        file = getOutputDirectory();
        executorService = Executors.newSingleThreadExecutor();

        if (allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE);
        }

        btnClickImage.setOnClickListener(v -> {
            takePhoto();
        });
    }

    private File getOutputDirectory() {
        File mediaDir = getExternalMediaDirs().length > 0 ? new File(getExternalMediaDirs()[0], getString(R.string.app_name)): null;
        if (mediaDir != null) {
           mediaDir.mkdirs();
        }
        return (mediaDir != null && mediaDir.exists()) ? mediaDir: getFilesDir();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(
                file,
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri uri = Uri.fromFile(photoFile);
                        ivClickedImage.setVisibility(View.VISIBLE);
                        ivClickedImage.setImageURI(uri);
                        new Handler().postDelayed(() -> {
                            ivClickedImage.setVisibility(View.GONE);
                        }, 2000);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(
                                MainActivity.this,
                                "Error: " + exception.getLocalizedMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.createSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Error: " + e.getLocalizedMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionGranted() {
        for (String permission: REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}