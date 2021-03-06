package com.carbon.restaurantinspection.model;

/**
 * Restaurant class models a restaurant's details.
 * It contains a restaurant's tracking number, name, physical address, city, factype, latitude,
 * and longitude
 */

public class Restaurant {

    private String trackingNumber;
    private String name;
    private String physicalAddress;
    private String city;
    private String factype;
    private double latitude;
    private double longitude;

    public Restaurant(String trackingNumber, String name, String physicalAddress, String city, String factype,
                      double latitude, double longitude) {
        this.trackingNumber = trackingNumber;
        this.name = name;
        this.physicalAddress = physicalAddress;
        this.city = city;
        this.factype = factype;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getName() {
        return name;
    }

    public String getPhysicalAddress() {
        return physicalAddress + ", " + city;
    }

    public String getCity() {
        return city;
    }

    public String getFactype() {
        return factype;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

}
