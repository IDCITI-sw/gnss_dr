package com.ai2s_lab.gnss_dr.gnss;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ai2s_lab.gnss_dr.util.Settings;
import com.ai2s_lab.gnss_dr.model.Satellite;
import com.ai2s_lab.gnss_dr.ui.log.LogFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class GnssRetriever {
    private static final String TAG = "GNSSRetriever";

    private int satCount;
    private boolean canUpdateUI;

    //init location manager
    private final LocationManager my_location_manager;
    private LogFragment logFragment;

    //Initial frequency for logging GNSS signals
    private int log_frequency = Settings.getUpdateFrequency();

    public GnssRetriever(Context context, LogFragment logFragment) {
        this.my_location_manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.logFragment = logFragment;
        satCount = 0;
        canUpdateUI = false;
    }

    //Listener for Location data
    private final LocationListener my_location_listener = new LocationListener() {
        private static final String TAG = "LocationListener";

        @Override
        public void onLocationChanged(@NonNull Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            double altitude = -1;
            double bearing = -1;
            double speed = -1;
            double horizontal_accuracy = -1;
            double vertical_accuracy = -1;
            double speed_accuracy = -1;
            long time_milli_long = location.getTime();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String time_as_string = formatter.format(time_milli_long);

            location.getExtras();
            if(location.hasAltitude())
                altitude = location.getAltitude();

            if(location.hasBearing())
                bearing = location.getBearing();

            if(location.hasSpeed())
                speed = location.getSpeed();

            if(location.hasAccuracy()) {
                horizontal_accuracy = location.getAccuracy();
            }

            if(location.hasVerticalAccuracy())
                vertical_accuracy = location.getVerticalAccuracyMeters();

            if(location.hasSpeedAccuracy())
                speed_accuracy = location.getSpeedAccuracyMetersPerSecond();

//            Bundle bundle = location.getExtras();
//
//            for (String key : bundle.keySet()){
//                Log.d(TAG, key + " = \"" + bundle.get(key) + "\"");
//
//            }
//            Log.d(TAG, String.valueOf(location.getProvider()));


            if(logFragment.isVisible()){
                logFragment.updateChart(latitude, longitude, altitude, bearing, speed, horizontal_accuracy, vertical_accuracy, speed_accuracy);

                if(location.hasAltitude() && satCount >= 4)
                    logFragment.updateFixStatus("3D Fix");
                else if(satCount >= 3){
                    logFragment.updateFixStatus("2D Fix");
                } else {
                    logFragment.updateFixStatus("No Fix");
                    logFragment.resetUI();
                }
            }

            String provider = location.getProvider();

            if(logFragment.isLogging){
                Log.d(TAG, "time logging: " + time_as_string);
                String [] temp = {Double.toString(latitude), Double.toString(longitude), Double.toString(speed), Double.toString(altitude), "" , Double.toString(bearing), time_as_string};
                logFragment.getLogger().addData(temp);
                logFragment.updateSubtitle(logFragment.getLogger().getDataCount());
            }

            Log.d(TAG, "lat: " + latitude
                    + " long: " + longitude
                    + " alt: " + altitude
                    + " acc: " + horizontal_accuracy
                    + " bearing: " + bearing
                    + " speed: " + speed);
            Log.d(TAG, "provider: " + provider);

            if(logFragment.getMapShown()){
                logFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), logFragment.getZoom()));
            }
        }
    };

    private final GnssMeasurementsEvent.Callback gnss_measurement_event = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);
            eventArgs.getMeasurements();
            eventArgs.getClock();
        }
    };

    //Listener for GNSS data (satellite info)
    private final GnssStatus.Callback gnss_status_listener = new GnssStatus.Callback() {
        private static final String TAG = "GnssStatusCallback";

        @Override
        public void onFirstFix(int ttffMillis) {
            super.onFirstFix(ttffMillis);
        }

        @Override
        public void onStopped(){
            Log.d(TAG, "GNSS has stopped");
        }
        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            int tempSatCount = status.getSatelliteCount();

            ArrayList<Satellite> satellites = new ArrayList<>();

            for (int i = 0; i < tempSatCount; i++) {
                int sat_type = status.getConstellationType(i);
                String sat_constellation_name = getConstellationName(sat_type);
                int sat_vid = status.getSvid(i);
                boolean sat_is_used = status.usedInFix(i);
                double sat_elevation = round(status.getElevationDegrees(i), 5);
                double sat_azim_degree = round(status.getAzimuthDegrees(i), 5);
                double sat_car_t_noise_r = round(status.getCn0DbHz(i), 5);
                if(sat_is_used){
                    Log.d(TAG, " satellite ID: " + sat_vid
                            + "  constellation type: " + sat_constellation_name
                            + " satellite used: " + sat_is_used
                            + " elevation: " + sat_elevation
                            + " azimuth: " + sat_azim_degree
                            + " carrier2noiseR: " + sat_car_t_noise_r);

                    Satellite satellite = new Satellite(sat_vid, sat_constellation_name, sat_is_used, sat_elevation, sat_azim_degree, sat_car_t_noise_r);
                    satellites.add(satellite);

                    if(logFragment.isLogging){
                        String [] temp = {"", "", "", "", "", "", Integer.toString(sat_vid), sat_constellation_name, Boolean.toString(sat_is_used), Double.toString(sat_elevation), Double.toString(sat_azim_degree), Double.toString(sat_car_t_noise_r)};
                        logFragment.getLogger().addData(temp);
                    }
                }
            }

            satCount = satellites.size();

            if(satCount > 3){
                logFragment.applyGNSS();
            } else {
                if(logFragment.getIsUsingGNSS()){
                    logFragment.applyFused();
                }
            }

            if(logFragment.isVisible() && canUpdateUI){
                logFragment.updateList(satellites);
                logFragment.updateSatNum(satellites.size());
                Log.i(TAG, "satellite count: " + satCount);

                if(logFragment.isLogging){
                    logFragment.updateSubtitle(logFragment.getLogger().getDataCount());

                }
            }

        }
    };


    @SuppressLint("MissingPermission")
    public void requestData() {
        boolean isEnabled = my_location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isEnabled) {
            my_location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, log_frequency, 0.0f, my_location_listener);
            my_location_manager.registerGnssStatusCallback(gnss_status_listener, null);
        }
    }

    public void stopGettingData() {
        my_location_manager.removeUpdates(my_location_listener);
        my_location_manager.unregisterGnssStatusCallback(gnss_status_listener);
    }

    private String getConstellationName(int type_no) {
        switch(type_no) {
            case 0:
                return "Unknown";
            case 1:
                return "GPS";
            case 2:
                return "SBAS";
            case 3:
                return "GLONASS";
            case 4:
                return "QZSS";
            case 5:
                return "Beidou";
            case 6:
                return "Galileo";
            case 7:
                return "IRNSS";
        }
        return "ERROR";
    }

    //get
    //log_frequency
    public int getLogFrequency() {
        return log_frequency;
    }

    //set
    //log_frequency
    public void setLogFrequency(int log_freq) {
        log_frequency = log_freq;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public int getSatCount() { return this.satCount; }

    public boolean getCanUpdateUI() { return this.canUpdateUI; }

    public void setCanUpdateUI(boolean value) { this.canUpdateUI = value; }

}
