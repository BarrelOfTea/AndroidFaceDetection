package com.tech.testfacedetection;


import static android.content.Context.MODE_PRIVATE;

public class DriverParameters {

    public DriverParameters(float ry, float rz, float eop, float mor, float eyeclf){
        this.rotY = ry;
        this.rotZ = rz;
        this.EOP= eop;
        this.MOR = mor;
        this.eyeCloseFreq = eyeclf;
        this.areParamsSet = true;
    }

    public DriverParameters(){
        rotY = 0;
        rotZ = 0;
        EOP = 0.4f;
        MOR = 0.2f;
    }

    private boolean areParamsSet = false;

    private float rotY;
    private float rotZ;

    //instead of eye aspect ratio EAR use EOP which is eye open probability
    //set the appropriate number between 0.0 and 1.0
    private float EOP;
    private float MOR;

    private float eyeCloseFreq = 0.25f;

    public float getEOP() {
        return EOP;
    }

    public float getEyeCloseFreq() {
        return eyeCloseFreq;
    }

    public float getMOR() {
        return MOR;
    }

    public float getRotY() {
        return rotY;
    }

    public float getRotZ() {
        return rotZ;
    }

    public void setEOP(float EOP) {
        this.EOP = EOP;
    }

    public void setEyeCloseFreq(float eyeCloseFreq) {
        this.eyeCloseFreq = eyeCloseFreq;
    }

    public void setMOR(float MOR) {
        this.MOR = MOR;
    }

    public void setRotY(float rotY) {
        this.rotY = rotY;
    }

    public void setRotZ(float rotZ) {
        this.rotZ = rotZ;
    }

    public boolean isAreParamsSet() {
        return areParamsSet;
    }

    public void setAreParamsSet(boolean areParamsSet) {
        this.areParamsSet = areParamsSet;
    }
}
