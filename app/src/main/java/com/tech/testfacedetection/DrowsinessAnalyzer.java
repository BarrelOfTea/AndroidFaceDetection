package com.tech.testfacedetection;

import android.os.Handler;

import java.util.ArrayList;
import java.util.TimerTask;



public class DrowsinessAnalyzer implements Runnable {

    private Handler handler;
    private Runnable handlerTask;

    int eyeFlag = 0;

    private ArrayList<LogObject> log;
    private FaceDetectorActivity activity;

    public DrowsinessAnalyzer(FaceDetectorActivity activ){
        this.activity = activ;
        this.log = activ.log;
    }

    @Override
    public void run() {


        for (int i = 0; i<activity.log.size(); i++){
            if (log.get(i).eyeClosed) eyeFlag++;
        }

        float freq = 0.25f;
        if (log.size()!=0) freq=eyeFlag/log.size();

        if (freq>activity.params.getEyeCloseFreq()){
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    activity.setText("Your eyes are closing more often, consider some rest");
                }
            });

            //activity.setText("Your eyes are closing more often, consider some rest");

            /*handler = new Handler();
            handlerTask = new Runnable()
            {
                @Override
                public void run() {
                    // do something
                    activity.setText("some text");
                    handler.postDelayed(handlerTask, 0);
                }
            };
            handlerTask.run();*/
        }

        log.clear();
    }

}
