package com.ai2s_lab.gnss_dr.gnss;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ai2s_lab.gnss_dr.ui.log.LogFragment;
import com.ai2s_lab.gnss_dr.util.Settings;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;

public class FusedRetriever {

    // constants
    private LocationCallback locationCallback;
    private Location lastKnownLocation;
    private boolean canUpdateUI;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final int DEFAULT_INTERVAL = Settings.getUpdateFrequency();
    private static final int POWER_INTERVAL = 0;

    private LogFragment logFragment;

    public FusedRetriever(LogFragment logFragment){
        this.logFragment = logFragment;
        canUpdateUI = false;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(logFragment.getContext());

        // init location request
        locationRequest = LocationRequest.create()
                .setInterval(DEFAULT_INTERVAL)
                .setFastestInterval(POWER_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // event triggered for location update
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateUI(locationResult.getLastLocation());
            }
        };
    }

    public void stopGettingData(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @SuppressLint("MissingPermission")
    public void requestData(){
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateUI(Location location){
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        double altitude = -1;
        double bearing = -1;
        double speed = -1;
        double horizontal_accuracy = -1;
        double vertical_accuracy = -1;
        double speed_accuracy = -1;

        if(location.hasAltitude())
            altitude = location.getAltitude();

        if(location.hasBearing())
            bearing = location.getBearing();

        if(location.hasSpeed())
            speed = location.getSpeed();

        if(location.hasAccuracy())
            horizontal_accuracy = location.getAccuracy();

        if(location.hasVerticalAccuracy())
            vertical_accuracy = location.getVerticalAccuracyMeters();

        if(location.hasSpeedAccuracy())
            speed_accuracy = location.getSpeedAccuracyMetersPerSecond();

        if(logFragment.isVisible() && canUpdateUI){
            logFragment.updateChart(latitude, longitude, altitude, bearing, speed, horizontal_accuracy, vertical_accuracy, speed_accuracy);
        }

        if(logFragment.getMapShown()){
            logFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), logFragment.getZoom()));
        }

        long time_milli_long = location.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String time_as_string = formatter.format(time_milli_long);

        Log.d("FUSED", time_as_string);

        if(logFragment.isLogging){
            String [] temp = {Double.toString(latitude), Double.toString(longitude), Double.toString(speed), Double.toString(altitude), "" , Double.toString(bearing), time_as_string};
            logFragment.getLogger().addData(temp);
            logFragment.updateSubtitle(logFragment.getLogger().getDataCount());
        }
    }

    public boolean getCanUpdateUI() { return this.canUpdateUI; }

    public void setCanUpdateUI(boolean value) { this.canUpdateUI = value; }
}
