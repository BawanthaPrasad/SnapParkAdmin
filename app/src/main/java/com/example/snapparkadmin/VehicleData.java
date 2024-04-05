package com.example.snapparkadmin;

import java.util.Date;

public class VehicleData {


    private String vehicleNo;
    private Double latitude;
    private Double longitude;
    private Date timestamp;


    public VehicleData() {
        // Required for Firestore deserialization
    }

    public VehicleData(Double latitude, Double longitude,  Date timeStamp, String VehicleNo) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timeStamp;
        this.vehicleNo = vehicleNo;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
