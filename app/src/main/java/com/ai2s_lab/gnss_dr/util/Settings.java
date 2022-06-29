package com.ai2s_lab.gnss_dr.util;

public class Settings {

    private static boolean gps = false;
    private static int updateFrequency = 100;   //Unit: milliseconds

    public Settings(){}

    public static boolean getGPS(){
        return gps;
    }

    public static void toggleGPS(){
        gps = !gps;
    }

    public static int getUpdateFrequency() {
        return updateFrequency;
    }

    public static void setUpdateFrequency(int choice) {
        updateFrequency = choice;
    }
}
