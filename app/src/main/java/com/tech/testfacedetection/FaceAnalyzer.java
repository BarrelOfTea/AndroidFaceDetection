package com.tech.testfacedetection;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

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
    int noseFlag;
    int notBlinkFlag;
    static final int EYE_THRESH = 16;
    static final int MOUTH_THRESH = 18;
    static final int NO_BLINK_TH = 80;
    static final float ROUND = 0.6f;

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
        noseFlag = 0;
        notBlinkFlag = 0;
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
                                            Log.v(null, faces.size() + " FACES WERE DETECTED");

                                            for (Face face : faces){
                                                Rect bounds = face.getBoundingBox();
                                                bitmap = drawer.drawRect(bitmap, bounds);


                                                float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                                float rotZ = face.getHeadEulerAngleZ(); //TODO rotY and rotZ are somehow always 0.0 and -0.0
                                                float rotX = face.getHeadEulerAngleX();

                                                List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
                                                bitmap = drawer.drawContours(bitmap, leftEyeContour);
                                                List<PointF> rightEyeContour = face.getContour(FaceContour.RIGHT_EYE).getPoints();
                                                bitmap = drawer.drawContours(bitmap, rightEyeContour);
                                                List<PointF> upperLipCon = face.getContour(FaceContour.UPPER_LIP_TOP).getPoints();
                                                bitmap = drawer.drawContours(bitmap, upperLipCon);
                                                List<PointF> lowerLipCon = face.getContour(FaceContour.LOWER_LIP_BOTTOM).getPoints();
                                                bitmap = drawer.drawContours(bitmap, lowerLipCon);
                                                List<PointF> noseCon = face.getContour(FaceContour.NOSE_BRIDGE).getPoints();
                                                bitmap = drawer.drawContours(bitmap, noseCon);


                                                /*long endTime = System.nanoTime();
                                                long timePassed = endTime - startTime;
                                                Log.v(null, "Execution time before eop " + timePassed / 1000000);*/

                                                float REOP = getOneEOP(rightEyeContour);
                                                float LEOP = getOneEOP(leftEyeContour);

                                                /*long endTime1 = System.nanoTime();
                                                long timePassed1 = endTime1 - startTime;
                                                Log.v(null, "Execution time after eop: " + timePassed1 / 1000000);*/

                                                notBlinkFlag++;

                                                if ((LEOP+REOP)/2 < activity.params.getEOP()) {
                                                    eyeFlag++;
                                                    notBlinkFlag = 0;
                                                    Log.v(null, "you blinked");
                                                }
                                                else {
                                                    eyeFlag = 0;
                                                }
                                                
                                                if (eyeFlag>=EYE_THRESH){
                                                    activity.enableAlert("WAKE UP! FIND A SPOT TO HAVE REST");
                                                    Log.v(null, "REASON closed eyes");
                                                }
                                                if (notBlinkFlag > NO_BLINK_TH){
                                                    activity.enableAlert("WAKE UP! YOU ARE SLEEPING WITH OPEN EYES");
                                                    Log.v(null, "REASON always open eyes");
                                                }

                                                /*long endTime2 = System.nanoTime();
                                                long timePassed2 = endTime2 - startTime;
                                                Log.v(null, "Execution time before mor: " + timePassed2 / 1000000);*/
                                                float MOR = getMOR(upperLipCon, lowerLipCon);
                                                /*long endTime3 = System.nanoTime();
                                                long timePassed3 = endTime3 - startTime;
                                                Log.v(null, "Execution time after mor: " + timePassed3 / 1000000);*/
                                                if (MOR > activity.params.getMOR()) mouthFlag++;
                                                else {
                                                    mouthFlag = 0;
                                                }
                                                Log.v(null, "mouthflag is "+mouthFlag+" with mor "+MOR);
                                                if (mouthFlag>=MOUTH_THRESH){
                                                    activity.enableWarning("YOU ARE SLEEPY! DRIVE TO THE CLOSEST PARKING TO HAVE SOME REST");
                                                    Log.v(null, "REASON yawn");
                                                }

                                                if(eyeFlag<EYE_THRESH && mouthFlag<MOUTH_THRESH && noseFlag<EYE_THRESH) activity.resetText();

                                                float nl = getNL(noseCon);
                                                if (nl < activity.params.getNL()) noseFlag++;
                                                else {
                                                    noseFlag = 0;
                                                }
                                                Log.v(null, "nose flag is "+noseFlag+" with nose length "+nl);
                                                if (noseFlag >= EYE_THRESH){
                                                    activity.enableAlert("YOU ARE DOZING OFF! DRIVE TO THE CLOSEST PARKING LOT");
                                                    Log.v(null, "REASON dosed off");
                                                }

                                                log(LEOP, REOP, MOR, rotY, rotZ, nl);

                                                Log.v(null, rotY + " roty");
                                                Log.v(null, rotZ + " rotz");
                                                Log.v(null, rotX + " rotx");
                                            }

                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            System.out.println("there was an error processing an image");
                                        }
                                    })
                            .addOnCompleteListener(
                                    new OnCompleteListener<List<Face>>() {
                                        @Override
                                        public void onComplete(@NonNull Task<List<Face>> task) {
                                            //activity.preview.setRotation(image.getImageInfo().getRotationDegrees());
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
    private void log(float leop, float reop, float mor, float rY, float rZ, float nl){


        boolean eyeClosed = false;
        boolean mouthOpen = false;
        boolean headInclined = false;

        if (rY >= activity.params.getRotY() || rZ >= activity.params.getRotZ()){
            headInclined = true;
        }

        if (mor>activity.params.getMOR()) mouthOpen = true;

        float averOpenProb = (leop+reop)/2;
        if (averOpenProb<activity.params.getEOP()) eyeClosed = true;

        activity.log.add(new LogObject(LocalTime.now(), eyeClosed, mouthOpen, headInclined, rY, rZ, averOpenProb, mor, nl));
        Log.v(null, "size of log is " + activity.log.size());
    }
    //you can do it, move on!

    float getMOR(List<PointF> ul, List<PointF> ll){
        PointF [] upoints = new PointF[ul.size()];
        ul.toArray(upoints);

        PointF [] lpoints = new PointF[ll.size()];
        ll.toArray(lpoints);

        float ver = (float) Math.sqrt(Math.pow(upoints[5].x - lpoints[4].x, 2) + Math.pow(upoints[5].y - lpoints[4].y, 2));
        float hor = (float) Math.sqrt(Math.pow(upoints[0].x - upoints[10].x, 2) + Math.pow(upoints[0].y - upoints[10].y, 2));


        return ver/hor;

        /*
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
           */
    }

    //re = right eye, ru = right upper
    float getOneEOP(List<PointF> contour){

        PointF [] points = new PointF[contour.size()];
        contour.toArray(points);

        float rVer = (float) Math.sqrt(Math.pow(points[4].x - points[12].x, 2) + Math.pow(points[4].y - points[12].y, 2));
        float rHor = (float) Math.sqrt(Math.pow(points[0].x - points[8].x, 2) + Math.pow(points[0].y - points[8].y, 2));

        
        return rVer/rHor;

    }

    float getNL(List<PointF> contour){
        PointF [] points = new PointF[contour.size()];
        contour.toArray(points);

        float nl = (float) Math.sqrt(Math.pow(points[0].x - points[1].x, 2) + Math.pow(points[0].y - points[1].y, 2));

        return nl;
    }
    
}


//TODO it is better done with opencv, but if yu go on using ml kit, use ImageAnalysis.STRATEGY_BLOCK_PRODUCER
