package com.tech.testfacedetection;
import android.Manifest;
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
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

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
import java.util.concurrent.ExecutionException;


public class FaceDetectorActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 654654;

    private Button scan_button;
    private ImageView preview;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facedetector_activity);

        preview = findViewById(R.id.imageView);
        scan_button = findViewById(R.id.button);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            initCamera();
        }
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
                            new ImageAnalysis.Analyzer() {
                                @Override
                                public void analyze(@NonNull ImageProxy image) {
                                    long startTime = System.nanoTime();
                                    Image img = image.getImage();
                                    bitmap = translator.translateYUV(img, FaceDetectorActivity.this);
                                    bitmap = rotateImage(bitmap, image.getImageInfo().getRotationDegrees());


                                    if (img != null){
                                        InputImage inputImage = InputImage.fromMediaImage(img, image.getImageInfo().getRotationDegrees());
                                        Task<List<Face>> result =
                                                detector.process(inputImage)
                                                        .addOnSuccessListener(
                                                                new OnSuccessListener<List<Face>>() {
                                                                    @Override
                                                                    public void onSuccess(List<Face> faces) {
                                                                        // Task completed successfully
                                                                        //facesList = faces;
                                                                        Log.v(null, "faces were obtained");

                                                                        for (Face face : faces){
                                                                            Rect bounds = face.getBoundingBox();
                                                                            bitmap = drawer.drawRect(bitmap, bounds);

                                                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                                                            float rotZ = face.getHeadEulerAngleZ();

                                                                            List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
                                                                            bitmap = drawer.drawContours(bitmap, leftEyeContour);
                                                                            List<PointF> rightEyeContour = face.getContour(FaceContour.RIGHT_EYE).getPoints();
                                                                            bitmap = drawer.drawContours(bitmap, rightEyeContour);
                                                                            List<PointF> upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();
                                                                            bitmap = drawer.drawContours(bitmap, upperLipBottomContour);
                                                                            List<PointF> lowerLipTopContour = face.getContour(FaceContour.LOWER_LIP_TOP).getPoints();
                                                                            bitmap = drawer.drawContours(bitmap, lowerLipTopContour);

                                                                            log(leftEyeContour, rightEyeContour, upperLipBottomContour, lowerLipTopContour, rotY, rotZ);

                                                                            Log.v(null, Float.toString(rotY) + " roty");
                                                                            Log.v(null, Float.toString(rotZ) + " rotz");

                                                                        }

                                                                    }
                                                                })
                                                        .addOnFailureListener(
                                                                new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        // Task failed with an exception
                                                                        System.out.println("there was an error processing an image");
                                                                    }
                                                                })
                                                        .addOnCompleteListener(
                                                                new OnCompleteListener<List<Face>>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<List<Face>> task) {

                                                                        //preview.setRotation(image.getImageInfo().getRotationDegrees());

                                                                        preview.setImageBitmap(bitmap);

                                                                        image.close();

                                                                        long endTime = System.nanoTime();
                                                                        long timePassed = endTime - startTime;
                                                                        Log.v(null, "Execution time in milliseconds: " + timePassed / 1000000);

                                                                    }
                                                                }
                                                        );

                                    }

                                }
                            });

                    cameraProvider.bindToLifecycle(FaceDetectorActivity.this, cameraSelector, imageAnalysis);

                } catch (ExecutionException e){
                    e.printStackTrace();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void log(List<PointF> le, List<PointF> re, List<PointF> ul, List<PointF> ll, float rY, float rZ){

    }




    public static Bitmap rotateImage(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }




}
