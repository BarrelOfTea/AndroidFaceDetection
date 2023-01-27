package com.tech.testfacedetection;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.time.LocalTime;
import java.util.List;

public class FaceAnalyzer implements ImageAnalysis.Analyzer {
    
    int eyeFlag;
    int mouthFlag;
    static final int EYE_THRESH = 14;
    static final int MOUTH_THRESH = 21;

    private FaceDetectorActivity activity;

    private FaceDetectorOptions realTimeOpts = new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build();
    private FaceDetector detector = FaceDetection.getClient(realTimeOpts);
    //private List<Face> facesList;

    private YUVtoRGB translator = new YUVtoRGB();

    private DrawContours drawer = new DrawContours();

    private Bitmap bitmap;

    public FaceAnalyzer(FaceDetectorActivity activity){
        this.activity = activity;
        eyeFlag = 0;
        mouthFlag = 0;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        long startTime = System.nanoTime();
        Image img = image.getImage();
        bitmap = translator.translateYUV(img, activity);
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
                                            Log.v(null, faces.size() + "FACES WERE DETECTED");

                                            for (Face face : faces){
                                                Rect bounds = face.getBoundingBox();
                                                bitmap = drawer.drawRect(bitmap, bounds);

                                                float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                                float rotZ = face.getHeadEulerAngleZ(); //TODO rotY and rotZ are somehow always 0.0 and -0.0

                                                List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
                                                bitmap = drawer.drawContours(bitmap, leftEyeContour);
                                                List<PointF> rightEyeContour = face.getContour(FaceContour.RIGHT_EYE).getPoints();
                                                bitmap = drawer.drawContours(bitmap, rightEyeContour);
                                                List<PointF> upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();
                                                bitmap = drawer.drawContours(bitmap, upperLipBottomContour);
                                                List<PointF> lowerLipTopContour = face.getContour(FaceContour.LOWER_LIP_TOP).getPoints();
                                                bitmap = drawer.drawContours(bitmap, lowerLipTopContour);

                                                float REOP = 0;
                                                float LEOP = 0;
                                                if (face.getRightEyeOpenProbability() != null) {
                                                    REOP = face.getRightEyeOpenProbability();
                                                }
                                                if (face.getLeftEyeOpenProbability() != null) {
                                                    LEOP = face.getRightEyeOpenProbability();
                                                }
                                                
                                                if ((REOP+LEOP)/2<activity.params.getEOP()) eyeFlag++;
                                                else {
                                                    eyeFlag = 0;
                                                    activity.resetText();
                                                }
                                                if (eyeFlag>=EYE_THRESH){
                                                    activity.enableAlert("WAKE UP! FIND A SPOT TO HAVE REST");
                                                }

                                                if (getMOR(upperLipBottomContour, lowerLipTopContour)>activity.params.getMOR()) mouthFlag++;
                                                else {
                                                    mouthFlag = 0;
                                                    //activity.resetText();
                                                }
                                                if (mouthFlag>=MOUTH_THRESH){
                                                    activity.enableWarning("YOU ARE SLEEPY! DRIVE TO THE CLOSEST PARKING TO HAVE SOME REST");
                                                }



                                                //log(REOP, LEOP, upperLipBottomContour, lowerLipTopContour, rotY, rotZ);

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



                                            activity.setPreview(bitmap);

                                            image.close();

                                            long endTime = System.nanoTime();
                                            long timePassed = endTime - startTime;
                                            Log.v(null, "Execution time in milliseconds: " + timePassed / 1000000);

                                        }
                                    }
                            );
        }
    }



    public static Bitmap rotateImage(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    // leop = left eye open probability
    private void log(float leop, float reop, List<PointF> ul, List<PointF> ll, float rY, float rZ){

        PointF upper = new PointF(0,0);
        PointF lower= new PointF(0,0);
        //these are points for calculating width of a mouth
        PointF leftCorner = new PointF(0,0);
        PointF rightCorner = new PointF(0,0);

        boolean eyeClosed = false;
        boolean mouthOpen = false;
        boolean headInclined = false;

        if (rY >= activity.params.getRotY() || rZ >= activity.params.getRotZ()){
            headInclined = true;
        }

        for (int i = 0; i < 8; i++){
            if (i==0){
                leftCorner = ul.iterator().next();
            }
            if (i==4) {
                upper = ul.iterator().next();
                lower = ll.iterator().next();
            }
            if (i==8){
                rightCorner = ul.iterator().next();
            }
        }

        float mouthDistVert = (float) Math.sqrt((upper.x - lower.x) * (upper.x - lower.x) + (upper.y - lower.y) * (upper.y - lower.y));
        float mouthDistHor = (float) Math.sqrt(Math.pow(leftCorner.x - rightCorner.x, 2) + Math.pow(leftCorner.y - rightCorner.y, 2));
        float MOR = mouthDistVert/mouthDistHor;
        if (MOR>activity.params.getMOR()) mouthOpen = true;

        float averOpenProb = (leop+reop)/2;
        if (averOpenProb<activity.params.getEOP()) eyeClosed = true;

        activity.log.add(new LogObject(LocalTime.now(), eyeClosed, mouthOpen, headInclined, rY, rZ, averOpenProb, MOR));

    }
    //you can do it, move on!

    float getMOR(List<PointF> ul, List<PointF> ll){
        PointF upper = new PointF(0,0);
        PointF lower= new PointF(0,0);
        //these are points for calculating width of a mouth
        PointF leftCorner = new PointF(0,0);
        PointF rightCorner = new PointF(0,0);

        boolean mouthOpen = false;

        for (int i = 0; i < 8; i++){
            if (i==0){
                leftCorner = ul.iterator().next();
            }
            if (i==4) {
                upper = ul.iterator().next();
                lower = ll.iterator().next();
            }
            if (i==8){
                rightCorner = ul.iterator().next();
            }
        }

        float mouthDistVert = (float) Math.sqrt((upper.x - lower.x) * (upper.x - lower.x) + (upper.y - lower.y) * (upper.y - lower.y));
        float mouthDistHor = (float) Math.sqrt(Math.pow(leftCorner.x - rightCorner.x, 2) + Math.pow(leftCorner.y - rightCorner.y, 2));
        float MOR = mouthDistVert/mouthDistHor;

        Log.v(null, "calculated MOR is "+ MOR);
        return MOR;

    }
}

/*
* if driver params are set
*   log
*   compare every 10 seconds
*   repeat
* else
*   for 10 seconds measure and log
*   compare data in the log and check it meets min/max parameters
*   set new driver parameters
*   state that driver params are set
*
*
* */
