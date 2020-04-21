package com.cellule.hunter;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;

@Entity(nameInDb = "user")
public class Note {

    @Id(autoincrement = true)
    private Long id;

    @Property(nameInDb = "mac_address")
    private String  macAddress;

    @Property(nameInDb = "location_lat")
    private double locationLat;

    @Property(nameInDb = "location_lon")
    private double locationLon;

    @Property(nameInDb = "date")
    private Date date;

    @Generated(hash = 1023967575)
    public Note(Long id, String macAddress, double locationLat, double locationLon,
            Date date) {
        this.id = id;
        this.macAddress = macAddress;
        this.locationLat = locationLat;
        this.locationLon = locationLon;
        this.date = date;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public double getLocationLat() {
        return this.locationLat;
    }

    public void setLocationLat(double locationLat) {
        this.locationLat = locationLat;
    }

    public double getLocationLon() {
        return this.locationLon;
    }

    public void setLocationLon(double locationLon) {
        this.locationLon = locationLon;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
