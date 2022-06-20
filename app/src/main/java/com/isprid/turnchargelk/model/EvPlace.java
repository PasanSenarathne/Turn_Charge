package com.isprid.turnchargelk.model;

import com.google.android.gms.maps.model.LatLng;

public class EvPlace {

    String name;
    String type;
    String contact;
    String address;
    double latitude;
    double longitude;
    double distance;

    public EvPlace() {
    }

    public EvPlace(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public EvPlace(String name, String type, String contact, String address, double latitude, double longitude) {
        this.name = name;
        this.type = type;
        this.contact = contact;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }


    public String getContact() {
        return contact;
    }


    public String getAddress() {
        return address;
    }


    public double getLatitude() {
        return latitude;
    }


    public double getLongitude() {
        return longitude;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public LatLng getLocation() {
        return new LatLng(latitude, longitude);
    }
}
