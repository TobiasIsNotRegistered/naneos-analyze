package com.stephanleuch.partector_v6;


/**
 * Class for GPS record which is used/necessary for GoogleEarthFile class (-> kml)
 *
 */
public class GPSrecord {
    public long time_ms;
    public double longitude;
    public double latitude;
    public float measurement;

    public GPSrecord(long _time_ms, double _longitude, double _latitude, float _measurement){
        time_ms = _time_ms;
        longitude = _longitude;
        latitude = _latitude;
        measurement = _measurement;
    }
}
