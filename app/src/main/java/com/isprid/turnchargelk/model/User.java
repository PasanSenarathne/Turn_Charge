package com.isprid.turnchargelk.model;

import java.io.Serializable;

public class User implements Serializable {
    String uid;
    String fname;
    String email;
    String phone;
    String vehicle_no;
    String password;

    public User() {
    }


    public User(String uid, String fname, String email, String phone, String vehicle_no, String password) {
        this.uid = uid;
        this.fname = fname;
        this.email = email;
        this.phone = phone;
        this.vehicle_no = vehicle_no;
        this.password = password;
    }

    public User(String fname, String email, String phone, String vehicle_no, String password) {
        this.fname = fname;
        this.email = email;
        this.phone = phone;
        this.vehicle_no = vehicle_no;
        this.password = password;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFname() {
        return fname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getVehicleNo() {
        return vehicle_no;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
