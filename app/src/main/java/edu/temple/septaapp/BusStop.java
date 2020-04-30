package edu.temple.septaapp;

import com.google.android.gms.maps.model.LatLng;

public class BusStop {

    String busStopId;
    LatLng busStopPos;

    public BusStop(String busStopId, LatLng busStopPos) {
        this.busStopId = busStopId;
        this.busStopPos = busStopPos;
    }

    public void setBusStopId(String busStopId) {
        this.busStopId = busStopId;
    }

    public LatLng getBusStopPos() {
        return busStopPos;
    }

    public void setBusStopPos(LatLng busStopPos) {
        this.busStopPos = busStopPos;
    }

    public String getBusStopId() {
        return busStopId;
    }
}
