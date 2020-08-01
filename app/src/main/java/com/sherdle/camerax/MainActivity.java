package com.sherdle.camerax;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static androidx.camera.core.CameraX.bindToLifecycle;
import static androidx.camera.core.CameraX.unbindAll;

public class MainActivity extends AppCompatActivity {
private int REQUEST_CODE_PERMISSIONS = 101;
private final String [] REQUIRED_PERMISSIONS =new String[] {"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"};
TextureView textureView;
ImageView cameraFlip;
    private boolean backlensfacing=true;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.view_finder);
        cameraFlip = findViewById(R.id.imageflip);
        cameraFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(backlensfacing){
                        startCamera();
                        backlensfacing=false;
                    }
                    else {
                        startCamera();
                        backlensfacing =true;
                    }
                                }

        });
        findViewById(R.id.flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(galleryIntent);
            }
        });
        if(allPermissionsGranted()){
            startCamera();
        }
        else
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);

    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startCamera() {
        unbindAll();
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(),textureView.getHeight());
        PreviewConfig pConfig;
        Preview preview;
        if(backlensfacing) {
            pConfig = new PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.BACK).setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
            preview = new Preview(pConfig);
        }
        else
        {
            pConfig = new PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.FRONT).setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
             preview = new Preview(pConfig);
        }
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup)textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView,0);
                textureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });
        final ImageCaptureConfig imageCaptureConfig ;
        if(backlensfacing)
        imageCaptureConfig= new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.BACK).build();
       else
            imageCaptureConfig= new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.FRONT).build();

        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);
        findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File image = null;
                String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_"+ timeStamp + "_";
                File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                try {
                    image = File.createTempFile(
                            imageFileName,
                            ".jpeg",
                            storageDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File file = new File(image.getAbsolutePath());
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = "Pic saved at "+ file.getAbsolutePath();
                        galleryAddPic(file.getAbsolutePath());
                        //Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic saved at "+ message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if (cause !=null){
                            cause.printStackTrace();

                            Toast.makeText(getBaseContext(), cause.toString(),Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });


        bindToLifecycle(this,preview, imgCap);
    }


        private void galleryAddPic(String  currentFilePath){
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File file = new File (currentFilePath);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            //Toast.makeText(getBaseContext(), "saved to gallery",Toast.LENGTH_LONG).show();
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();
         float cX = w / 2f;
         float cY = h / 2f;
         int rotationDgr;
         int rotation = (int)textureView.getRotation();
         switch (rotation){
             case Surface.ROTATION_0:
                 rotationDgr = 0;
                 break;
             case Surface.ROTATION_90:
                 rotationDgr = 90;
                 break;
             case Surface.ROTATION_180:
                 rotationDgr = 180;
                 break;
             case Surface.ROTATION_270:
                 rotationDgr = 270;
                 break;
             default: return;
         }
         mx.postRotate((float)rotationDgr, cX,cY);
         textureView.setTransform(mx);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()) {

                startCamera();
            }
            else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return  true;
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        // this device has a camera
        // no camera on this device
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    private void toggleFrontBackCamera() {


    }
}