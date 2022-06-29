package com.ai2s_lab.gnss_dr.ui.log;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import androidx.fragment.app.Fragment;

import com.ai2s_lab.gnss_dr.R;
import com.ai2s_lab.gnss_dr.databinding.FragmentLogBinding;
import com.ai2s_lab.gnss_dr.gnss.FusedRetriever;
import com.ai2s_lab.gnss_dr.gnss.GnssRetriever;
import com.ai2s_lab.gnss_dr.io.Logger;
import com.ai2s_lab.gnss_dr.model.Satellite;
import com.ai2s_lab.gnss_dr.util.LogListAdapter;
import com.ai2s_lab.gnss_dr.util.Settings;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class LogFragment extends Fragment   {

    // constants
    private FragmentLogBinding binding;
    private final String TAG = "LOG";
    public boolean isLogging;

    // GPS providers
    private GnssRetriever gnssRetriever;
    private FusedRetriever fusedRetriever;

    // UI elements
    private TextView tvLogTitle;
    private TextView tvSubtitle;

    private TextView tvLat;
    private TextView tvLong;
    private TextView tvSpeed;
    private TextView tvHeight;
    private TextView tvNumSat;
    private TextView tvBearing;
    private TextView tvHorizontalAccuracy;
    private TextView tvVerticalAccuracy;
    private TextView tvSpeedAccuracy;
    private TextView tvFixStatus;

    private Button btnStart;
    private Button btnReset;
    private Button btnSave;
    private Button btnMap;
    private Switch switchGnss;

    private CardView logInfo;
    private CardView logSats;
    private LinearLayout logBtns;

    private TextView tvUpdateFreq;

    // UI elements + data for listview
    private ListView listView;
    private ArrayList<Satellite> satellites;
    private LogListAdapter logListAdapter;

    // Logger
    private Logger logger;

    // alert dialog
    private AlertDialog.Builder builder;

    // constants for map
    private static final float DEFAULT_ZOOM = 17;

    private GoogleMap map;
    private MapView mapView;
    private BottomSheetDialog bottomSheetDialog;
    private boolean dialogShown;

    // Service constant
    private boolean serviceOn;

    private boolean isUsingGNSS;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // init view and inflate
        binding = FragmentLogBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //initialise retrievers
        gnssRetriever = new GnssRetriever(getActivity().getApplicationContext(), this);
        fusedRetriever = new FusedRetriever(this);

        // initialize UI components
        tvLogTitle = binding.textLogTitle;
        tvSubtitle = binding.textLogFile;

        btnReset = binding.btnLogReset;
        btnStart = binding.btnLogStart;
        btnSave = binding.btnLogSave;
        btnMap = binding.btnMap;
        switchGnss = binding.switchLogTrack;

        tvLat = binding.textLogLatValue;
        tvLong = binding.textLogLongValue;
        tvSpeed = binding.textLogSpeedValue;
        tvHeight = binding.textLogHeightValue;
        tvNumSat = binding.textLogNumValue;
        tvBearing = binding.textLogBearingValue;
        tvHorizontalAccuracy = binding.textLogXValue;
        tvVerticalAccuracy = binding.textLogYValue;
        tvSpeedAccuracy = binding.textLogSpeedAccuracyValue;
        tvFixStatus = binding.tvFixStatus;

        logInfo =  binding.logInfo;
        logSats = binding.logSats;
        logBtns = binding.logButtonLayout;

        tvUpdateFreq = binding.tvUpdateFrequency;

        // initial states for logging buttons
        btnSave.setEnabled(false);
        btnReset.setEnabled(false);
        switchGnss.setChecked(false);
        tvSubtitle.setText("Not Logging");
        tvLogTitle.setText("GPS is turned off!");

        // apply current settings
        applyCurrentSettings();

        // adapter for listview
        this.satellites = new ArrayList<>();
        logListAdapter = new LogListAdapter(this.getContext(), satellites);
        listView = binding.listLog;
        listView.setAdapter(logListAdapter);

        // service constants
        serviceOn = false;
        isUsingGNSS = true;

        // create an Alert dialog
        builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Save Data");
        builder.setMessage("Are you sure you want to write the log date to a CSV file?");
        builder.setCancelable(false);

        builder.setPositiveButton("Save File", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logger.saveDataToFile();
            }
        });

        builder.setNegativeButton("Stop Logging", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logger.resetData();
                dialogInterface.cancel();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout);

        mapView = bottomSheetDialog.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        try {
            MapsInitializer.initialize(getContext());
        } catch (Exception e){
            e.printStackTrace();
        }
        mapView.onResume();
        dialogShown = false;


        //Action handlers for logging buttons
        //save button
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnStart.setEnabled(true);
                btnSave.setEnabled(false);
                btnReset.setEnabled(false);
                isLogging = false;
                tvSubtitle.setText("Not Logging");

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        //Start button
        btnStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), "You have started logging!", Snackbar.LENGTH_SHORT).show();

                    if(switchGnss.isChecked()){
                        logger = new Logger(getActivity());

                        tvSubtitle.setText("Started Logging GPS Data");
                        btnStart.setEnabled(false);
                        btnSave.setEnabled(true);
                        btnReset.setEnabled(true);
                        isLogging = true;
                    } else {
                        Snackbar.make(getActivity().findViewById(android.R.id.content), "GNSS Is Off", Snackbar.LENGTH_SHORT).show();
                    }

                }
            });

        //Reset button
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(getActivity().findViewById(android.R.id.content), "You have reset the logged data", Snackbar.LENGTH_SHORT).show();
                logger.resetData();
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogShown = true;
                showMap(savedInstanceState);
            }
        });

        //Log switch listener
        switchGnss.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Settings.toggleGPS();
                applyCurrentSettings();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void updateChart(double lat, double lon, double alt, double bearing, double speed, double horizontal_accuracy, double vertical_accuracy, double speed_accuracy){
        DecimalFormat five_points = new DecimalFormat("#.#####");
        DecimalFormat one_point = new DecimalFormat("#.#");

        tvLat.setText(five_points.format(lat));
        tvLong.setText(five_points.format(lon));

        if(alt != -1)
            tvHeight.setText(five_points.format(alt) + " m");
        else
            tvHeight.setText("N/A");

        if(bearing != -1)
            tvBearing.setText(five_points.format(bearing));
        else
            tvBearing.setText("N/A");

        if(speed != -1 && speed_accuracy != -1)
            tvSpeed.setText(five_points.format(speed) + " m/s");
        else
            tvSpeed.setText("N/A");

        if(horizontal_accuracy != -1)
            tvHorizontalAccuracy.setText(one_point.format(horizontal_accuracy) + "%");
        else
            tvHorizontalAccuracy.setText("N/A");

        if(vertical_accuracy != -1)
            tvVerticalAccuracy.setText(one_point.format(vertical_accuracy) + " m");
        else
            tvVerticalAccuracy.setText("N/A");

        if(speed_accuracy != -1)
            tvSpeedAccuracy.setText(one_point.format(speed_accuracy) + "%");
        else
            tvSpeedAccuracy.setText("N/A");
    }

    public void updateFixStatus(String status){ tvFixStatus.setText(status); }
    public void updateSatNum(int satNum){
        tvNumSat.setText(Integer.toString(satNum));
    }

    //Gets satellite count.
    public int getSatCount() {
        return Integer.parseInt((String) tvNumSat.getText());
    }
    public void updateList(ArrayList<Satellite> satellites){

        this.satellites = satellites;
        logListAdapter = new LogListAdapter(getActivity(), satellites);
        listView = binding.listLog;
        listView.setAdapter(logListAdapter);
    }

    public Logger getLogger() { return this.logger; }

    private void resetList(){
        this.satellites = new ArrayList<>();
        logListAdapter = new LogListAdapter(getActivity(), satellites);
        listView = binding.listLog;
        listView.setAdapter(logListAdapter);
    }

    public void resetUI(){
        tvLat.setText("N/A");
        tvLong.setText("N/A");
        tvSpeed.setText("N/A");
        tvHeight.setText("N/A");
        tvNumSat.setText("N/A");
        tvBearing.setText("N/A");
        tvHorizontalAccuracy.setText("N/A");
        tvVerticalAccuracy.setText("N/A");
        tvSpeedAccuracy.setText("N/A");
    }

    public void applyGNSS(){
        tvLogTitle.setText("Using GNSS");
        logSats.setVisibility(View.VISIBLE);
        gnssRetriever.setCanUpdateUI(true);
        fusedRetriever.setCanUpdateUI(false);
        gnssRetriever.requestData();
        fusedRetriever.stopGettingData();
        tvFixStatus.setVisibility(View.VISIBLE);
        isUsingGNSS = true;
    }

    public void applyFused(){
        tvLogTitle.setText("Using FusedLocationProvider");
        logSats.setVisibility(View.INVISIBLE);
        tvNumSat.setText("N/A");
        tvFixStatus.setVisibility(View.INVISIBLE);
        gnssRetriever.setCanUpdateUI(false);
        fusedRetriever.setCanUpdateUI(true);
        fusedRetriever.requestData();
        isUsingGNSS = false;
    }


    private void applyCurrentSettings(){
        // GPS On
        if(Settings.getGPS()){
            tvUpdateFreq.setText("Update Every " + Settings.getUpdateFrequency() + "ms");
            switchGnss.setText("GPS On");
            switchGnss.setChecked(true);
            btnMap.setEnabled(true);
            gnssRetriever.requestData();

            tvUpdateFreq.setVisibility(View.VISIBLE);
        }
        // GPS Off
        else{
            tvUpdateFreq.setVisibility(View.INVISIBLE);
            tvLogTitle.setText("GPS is turned off!");
            switchGnss.setText("GPS Off");
            tvFixStatus.setVisibility(View.INVISIBLE);
            switchGnss.setChecked(false);
            btnMap.setEnabled(false);
            resetList();
            resetUI();
            fusedRetriever.stopGettingData();
            gnssRetriever.stopGettingData();
            isUsingGNSS = true;
        }
    }

    private void showMap(Bundle savedInstanceState){
        bottomSheetDialog.show();

        mapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                map = googleMap;
                map.setMyLocationEnabled(true);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public boolean getMapShown() { return this.dialogShown; }

    public float getZoom() { return DEFAULT_ZOOM; }
    public GoogleMap getMap() { return this.map; }

    public void updateSubtitle(int count){
        String temp = "Logged " + count + " lines";
        tvSubtitle.setText(temp);
    }

    public boolean getIsUsingGNSS(){ return this.isUsingGNSS; }
}