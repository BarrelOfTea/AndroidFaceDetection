package com.tech.testfacedetection;

public class DriverParameters {
    //TODO save data to storage
    private boolean areParamsSet = false;

    private float rotY = 0;
    private float rotZ = 0;

    private float EAR = 1;
    private float MOR = 0;

    private float eyeCloseFreq = 0.25f;

    public float getEAR() {
        return EAR;
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

    public void setEAR(float EAR) {
        this.EAR = EAR;
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
