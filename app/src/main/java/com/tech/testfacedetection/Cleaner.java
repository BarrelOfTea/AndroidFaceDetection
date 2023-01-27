package com.tech.testfacedetection;

import java.util.ArrayList;

public class Cleaner implements Runnable {

    private ArrayList<LogObject> log;

    public Cleaner(ArrayList<LogObject> log){
        this.log = log;
    }

    @Override
    public void run() {

    }
}
