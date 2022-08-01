package com.ai2s_lab.gnss_dr.tflite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.tensorflow.lite.Interpreter;

import com.ai2s_lab.gnss_dr.model.Satellite;
import com.ai2s_lab.gnss_dr.ui.inference.InferFragment;

import java.nio.FloatBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class Inference {
    private static final String TAG = "Inference";
    private Context context;

    private int satCount;

    // Constant
    private static int NUM_CATEGORY = 146; // number of output

    // variables
    private int currPos = -1;

    // parameters for inference
    private double latitude = 0;
    private double longitude = 0;
    private double altitude = -1;
    private double bearing = -1;
    private double speed = -1;
    private double horizontal_accuracy = -1;
    private double vertical_accuracy = -1;
    private double speed_accuracy = -1;

    int sat_type;
    String sat_constellation_name;
    int sat_vid;
    boolean sat_is_used;
    double sat_elevation;
    double sat_azim_degree;
    double sat_car_t_noise_r;

    // location manager
    private final LocationManager my_location_manager;

    private InferFragment inferFragment;

    private Interpreter interpreter;

    public Inference(Context context, InferFragment inferFragment) {
        this.context = context;
        this.my_location_manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.inferFragment = inferFragment;
        satCount = 0;
    }

    //Listener for Location data
    private final LocationListener my_location_listener = new LocationListener() {
        private static final String TAG = "LocationListener";

        @Override
        public void onLocationChanged(@NonNull Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            altitude = -1;
            bearing = -1;
            speed = -1;
            horizontal_accuracy = -1;
            vertical_accuracy = -1;
            speed_accuracy = -1;

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

            if(inferFragment.isVisible()){

            }

            String provider = location.getProvider();

            Log.d(TAG, "lat: " + latitude
                    + " long: " + longitude
                    + " alt: " + altitude
                    + " acc: " + horizontal_accuracy
                    + " bearing: " + bearing
                    + " speed: " + speed);
            Log.d(TAG, "provider: " + provider);
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
            ArrayList<Satellite> gps_satellites = new ArrayList<>();
            ArrayList<Satellite> gln_satellites = new ArrayList<>();
            ArrayList<Integer> gps_ids = new ArrayList<Integer>();
            ArrayList<Integer> gln_ids = new ArrayList<Integer>();

            for (int i = 0; i < tempSatCount; i++) {
                sat_type = status.getConstellationType(i);
                sat_constellation_name = getConstellationName(sat_type);
                sat_vid = status.getSvid(i);
                sat_is_used = status.usedInFix(i);
                sat_elevation = round(status.getElevationDegrees(i), 5);
                sat_azim_degree = round(status.getAzimuthDegrees(i), 5);
                sat_car_t_noise_r = round(status.getCn0DbHz(i), 5);
                if(sat_is_used){
                    Log.d(TAG, " satellite ID: " + sat_vid
                            + "  constellation type: " + sat_constellation_name
                            + " satellite used: " + sat_is_used
                            + " elevation: " + sat_elevation
                            + " azimuth: " + sat_azim_degree
                            + " carrier2noiseR: " + sat_car_t_noise_r);

                    Satellite satellite = new Satellite(sat_vid, sat_constellation_name, sat_is_used, sat_elevation, sat_azim_degree, sat_car_t_noise_r);
                    satellites.add(satellite);
                }
                if (sat_type == 1){ // GPS
                    gps_ids.add(sat_vid);
                    Satellite satellite = new Satellite(sat_vid, sat_constellation_name, sat_is_used, sat_elevation, sat_azim_degree, sat_car_t_noise_r);
                    gps_satellites.add(satellite);
                } else if (sat_type == 3){ // GLONASS
                    gln_ids.add(sat_vid);
                    Satellite satellite = new Satellite(sat_vid, sat_constellation_name, sat_is_used, sat_elevation, sat_azim_degree, sat_car_t_noise_r);
                    gln_satellites.add(satellite);}
            }

            satCount = satellites.size();

            // === Model Inference ===
            FloatBuffer input = prepareInput(gps_satellites, gln_satellites, gps_ids, gln_ids);
            FloatBuffer output = FloatBuffer.allocate(NUM_CATEGORY);
            try{
                interpreter.run(input, output);
                output.rewind();
                float[] result = output.array();
                Log.d(TAG,"interpreter result: " + Arrays.toString(result));

                int pos = 0;
                if (currPos < 0) {
                    for (int i = 0; i < NUM_CATEGORY; i++)
                        if (result[pos] < result[i]) pos = i;
                } else {
                    for (int i = currPos - 5; i < currPos + 5; i++)
                        if(result[pos] < result[i]) pos = i;
                }
                currPos = pos;
                Log.e(TAG,"infered current position: " + currPos);
                inferFragment.setTvCurrPos(currPos);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @SuppressLint("MissingPermission")
    public void startInference() {
        // initialize current position
        currPos = -1;
        // Start Location Listener
        boolean isEnabled = my_location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isEnabled) {
            my_location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.0f, my_location_listener);
            my_location_manager.registerGnssStatusCallback(gnss_status_listener, null);
        }
    }

    public void stopInference() {
        // Stop Location Listener
        my_location_manager.removeUpdates(my_location_listener);
        my_location_manager.unregisterGnssStatusCallback(gnss_status_listener);
    }

    // Load TFLite Model
    public void loadTFLiteModel(String modelPath){
        try {
            interpreter = new Interpreter(new File(modelPath));
            Toast.makeText(context, "Model Load Success", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Model Load Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private FloatBuffer prepareInput(ArrayList<Satellite> gps_satellites,
                                     ArrayList<Satellite> gln_satellites,
                                     ArrayList<Integer> gps_ids,
                                     ArrayList<Integer> gln_ids){
        // input shape
        // (:, 4 + 3*32 + 3*24)
        // LAT,LONG, SPEED, HEIGHT, GPS1(Elev, Azim, CNO):GPS32, GLONASS1(Elev, Azim, CNO):24
        FloatBuffer input = FloatBuffer.allocate(4 + 3*32 + 3*24);//.order(ByteOrder.nativeOrder());
        float[] arr = new float[4 + 3*32 + 3*24];
        arr[0] = (float) latitude;
        arr[1] = (float) longitude;
        arr[2] = (float) speed;
        arr[3] = (float) altitude;
        for (int i = 0; i < 32; i++){   // GPS
            // init values
            arr[i*3 + 4] = 0;   // Elev
            arr[i*3 + 5] = 0;   // Azim
            arr[i*3 + 6] = 0;   // CNO

            int index = gps_ids.indexOf(i+1);
            if (index > 0){
                Satellite satellite = gps_satellites.get(index);
                arr[i*3 + 4] = (float) satellite.getElev();   // Elev
                arr[i*3 + 5] = (float) satellite.getAzim();   // Azim
                arr[i*3 + 6] = (float) satellite.getCno();   // CNo
            }
        }
        for (int i = 0; i < 24; i++){   // GLONASS
            // init values
            arr[(i+32)*3 + 4] = 0;   // Elev
            arr[(i+32)*3 + 5] = 0;   // Azim
            arr[(i+32)*3 + 6] = 0;   // CNO

            int index = gln_ids.indexOf(i+1);
            if (index > 0){
                Satellite satellite = gln_satellites.get(index);
                arr[(i+32)*3 + 4] = (float) satellite.getElev();   // Elev
                arr[(i+32)*3 + 5] = (float) satellite.getAzim();   // Azim
                arr[(i+32)*3 + 6] = (float) satellite.getCno();   // CNo
            }
        }
        input.put(arr);
        input.rewind();
        return input;
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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public int getSatCount() { return this.satCount; }

    public int getCurrPos() { return this.currPos; }

    public void test(String model_path) {
        try (Interpreter interpreter = new Interpreter(loadModelFile(model_path))) {
            Log.e(TAG, "In the test func.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(Uri model_uri) {
        String model_path = getRealPathFromURI(model_uri);
        Log.e(TAG, "Real model path: " + model_path);
//        try (Interpreter interpreter = new Interpreter(loadModelFile(model_path))) {
//            Log.e(TAG, "In the test func.");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        try (Interpreter interpreter = new Interpreter(new File(model_path))) {
            Log.e(TAG, "In the test func.");
        }
    }

    // Convert Model File into ByteBuffer
    private MappedByteBuffer loadModelFile(String model_path) throws IOException {
        Log.e(TAG, "loadModelFile func 1 model path: " + model_path);
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(model_path);
        Log.e(TAG, "loadModelFile func 2");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Get Absolute Path from URI object
    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }

        String id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + id;
        Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null);
        try {
            int columnIndex = cursor.getColumnIndex(columns[0]);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
