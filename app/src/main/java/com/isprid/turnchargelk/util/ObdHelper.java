package com.isprid.turnchargelk.util;

public class ObdHelper {

    private static float drivingTime = 180;
    private static float drivingDistance = -1;
    private static int batteryLevel = -1;

    public static float getDrivingDistance(int batteryLevel) {
        return (float) (180 / 100.0 * batteryLevel);
    }

    public static float getDrivingTime() {
        return drivingTime;
    }

    public static void setDrivingTime(float drivingTime) {
        ObdHelper.drivingTime = drivingTime;
    }

    public static float getDrivingDistance() {
        return drivingDistance;
    }

    public static void setDrivingDistance(float drivingDistance) {
        ObdHelper.drivingDistance = drivingDistance;
    }

    public static int getBatteryLevel() {
        return batteryLevel;
    }

    public static void setBatteryLevel(int batteryLevel) {
        ObdHelper.batteryLevel = batteryLevel;
    }
}
