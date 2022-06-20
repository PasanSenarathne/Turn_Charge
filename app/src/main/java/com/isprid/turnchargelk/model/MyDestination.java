package com.isprid.turnchargelk.model;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

public class MyDestination implements Serializable {
    String name;
    double latitude;
    double longitude;

    public MyDestination(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public MyDestination(String name, LatLng latLng) {
        this.name = name;
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LatLng getLocation() {
        return new LatLng(latitude, longitude);
    }
}
