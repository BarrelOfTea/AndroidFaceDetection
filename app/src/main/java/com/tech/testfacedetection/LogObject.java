package com.tech.testfacedetection;

import java.time.LocalTime;

public class LogObject {

    //this is the object of all current parameters of each taken image to be logged and analyzed over particular interval

    float rotY;
    float rotZ;
    float eop;
    float mor;
    float nl;

    LocalTime time;
    boolean eyeClosed;
    boolean mouthOpen;
    boolean headInclined;

    public LogObject(){

    }

    public LogObject(LocalTime time, boolean eyeClosed, boolean mouthOpen, boolean headInclined, float roty, float rotz, float eop, float mor, float nl){

        this.eyeClosed = eyeClosed;
        this.time = time;
        this.mouthOpen = mouthOpen;
        this.headInclined = headInclined;

        this.rotY = roty;
        this.rotZ = rotz;
        this.eop = eop;
        this.mor = mor;
        this.nl = nl;
    }

}
