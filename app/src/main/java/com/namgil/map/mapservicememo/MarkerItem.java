package com.namgil.map.mapservicememo;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by jisun on 2017-06-07.
 */

public class MarkerItem implements ClusterItem {
    private double lat;
    private double lon;
    private String memo;
    private LatLng location;


    public MarkerItem(double lat, double lon, String memo) {
        this.lat = lat;
        this.lon = lon;
        this.memo = memo;
    }
    public MarkerItem(LatLng location){
        this.location = location;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Override
    public LatLng getPosition() {
        return location;
    }
}
