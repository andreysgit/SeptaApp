package edu.temple.septaapp;

import com.google.android.gms.maps.model.LatLng;

public class Bus {

    String busId;
    LatLng busPos;

    public Bus(String busId, LatLng busPos){
        this.busId = busId;
        this.busPos = busPos;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public LatLng getBusPos() {
        return busPos;
    }

    public void setBusPos(LatLng busPos) {
        this.busPos = busPos;
    }


}
