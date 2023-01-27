package com.tech.testfacedetection;

import java.util.ArrayList;
import java.util.TimerTask;

public class DrowsinessAnalyzer extends TimerTask {

    int eyeFlag = 0;
    int mouthFlag = 0;

    private ArrayList<LogObject> log;
    private FaceDetectorActivity activity;

    public DrowsinessAnalyzer(FaceDetectorActivity activ){
        this.activity = activ;
        this.log = activ.log;
    }

    @Override
    public void run() {
        for (int i = 0; i<log.size(); i++){
            if (log.get(i).eyeClosed) eyeFlag++;
            if (log.get(i).mouthOpen) mouthFlag++;
        }

        //if ()
    }

    //TODO is Cleaner even necessary? you can set log to null every time you have processed information
    // use for implementing frequency measurement and yawning (sort of additional yellow warning parameters to offer a driver some sleep
    //
}
