package com.tech.testfacedetection;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

import com.tech.testfacedetection.FaceAnalyzer;


public class FaceDetectorActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 654654;

    private Button scan_button;
    private ImageView preview;
    private TextView textView;

    private FaceDetectorOptions realTimeOpts = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();
    private FaceDetector detector = FaceDetection.getClient(realTimeOpts);
    private List<Face> facesList;

    private YUVtoRGB translator = new YUVtoRGB();
    ListenableFuture<ProcessCameraProvider> cameraProvideFuture;

    private DrawContours drawer = new DrawContours();
    private Bitmap bitmap;

    DriverParameters params;
    private MediaPlayer playerRed;
    private MediaPlayer playerYellow;
    ArrayList<LogObject> log = new ArrayList<>();

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    //private DrowsinessAnalyzer timelyAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facedetector_activity);

        preview = findViewById(R.id.imageView);
        scan_button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);

        params = new DriverParameters();

        /*playerRed = MediaPlayer.create(this, R.raw.bleep);
        playerRed.setVolume(1.0f, 1.0f);
        playerYellow = MediaPlayer.create(this, R.raw.notification);
        playerYellow.setVolume(0.5f, 0.5f);*/

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            initCamera();
        }

        sharedPrefs = getSharedPreferences("driverParams", MODE_PRIVATE);

        /*if(sharedPrefs.contains("paramsSet")){
            boolean areParamsSet = sharedPrefs.getBoolean("paramsSet", false);
            while (!areParamsSet){
                setDriverParameters();
            }
        } else {
            sharedPrefs = getSharedPreferences("driverParams", MODE_PRIVATE);
            boolean areParamsSet = sharedPrefs.getBoolean("paramsSet", false);
            while (!areParamsSet){
                setDriverParameters();
            }
        }*/

        //new Timer().schedule(new DrowsinessAnalyzer(FaceDetectorActivity.this), 0, 3000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            initCamera();
        }
    }

    private void initCamera(){
        cameraProvideFuture = ProcessCameraProvider.getInstance(this);
        cameraProvideFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProvideFuture.get();
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setTargetResolution(new Size(480, 360))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(FaceDetectorActivity.this),
                            new FaceAnalyzer(FaceDetectorActivity.this));

                    cameraProvider.bindToLifecycle(FaceDetectorActivity.this, cameraSelector, imageAnalysis);

                } catch (ExecutionException e){
                    e.printStackTrace();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }



    public void setPreview(Bitmap bitmap){
        preview.setImageBitmap(bitmap);
    }

    public void playerStart(){
        if (!playerRed.isPlaying()) playerRed.start();
    }

    public void playerStop(){
        if (playerRed.isPlaying()){
            playerRed.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerRed.release();
        playerRed = null;
    }

    
    
    private void setDriverParameters(){
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e){
            Log.e(null, "error with Thread.sleep in setting driver params");
        }

        float avMor = 0;
        float avEop = 0;
        float avRotY = 0;
        float avRotZ = 0;
        int blinkFreq = 0;

        for (int i = 0; i<log.size(); i++){
            avMor += log.get(i).mor;
            avEop += log.get(i).eop;
            avRotY += log.get(i).rotY;
            avRotZ += log.get(i).rotZ;

            if (log.get(i).eyeClosed) blinkFreq++;
        }

        avMor = avMor/log.size();
        avEop = avEop/log.size();
        avRotY = avRotY/log.size();
        avRotZ = avRotZ/log.size();
        blinkFreq = blinkFreq/10;

        if (avMor > 0.4 || avEop < 0.1 || avRotY > 30.0 || avRotZ > 30.0){
            return;
        } else {
            params.setMOR(avMor);
            params.setEOP(avEop);
            params.setRotY(avRotY);
            params.setRotZ(avRotZ);
            params.setEyeCloseFreq(blinkFreq);
            params.setAreParamsSet(true);

            editor = sharedPrefs.edit();

            editor.putFloat("MOR", avMor);
            editor.putFloat("EAR", avEop);
            editor.putFloat("rotY", avRotY);
            editor.putFloat("rotZ", avRotZ);
            editor.putFloat("blinkFreq", blinkFreq);
            editor.putBoolean("paramsSet", true);

            editor.apply();
        }
    }


    public void enableWarning(String msg){
        //textView.setText("YOU SEEM TO BE SLEEPY, PLEASE CONSIDER STOPPING SOMEWHERE");
        textView.setText(msg);
        //if (!playerYellow.isPlaying()) playerYellow.start();
    }

    public void enableAlert(String msg){
        //textView.setText("WAKE UP! FIND SOME PLACE TO REST");
        textView.setText(msg);
        //if (!playerRed.isPlaying()) playerRed.start();
    }

    public void resetText(){
        textView.setText("You are good");
    }
    //TODO do not forget to turn on the player again
}


//TODO implement runnable to create thread to clean array and compare blinking frequency every 10 seconds
//TODO and compare MOR and EOP every 2 seconds
