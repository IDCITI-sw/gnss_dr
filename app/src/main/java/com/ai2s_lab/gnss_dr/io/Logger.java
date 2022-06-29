package com.ai2s_lab.gnss_dr.io;

import android.app.Activity;
import com.google.android.material.snackbar.Snackbar;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
public class Logger {

    private String TAG = "LOG";
    private String baseDir;
    private String fileName;
    private String filePath;

    private File file;
    private FileWriter fileWriter;
    private CSVWriter csvWriter;
    private String [] firstLine;

    private ArrayList<String []> data;
    private Activity activity;

    public Logger(Activity activity){

        this.activity = activity;
        data = new ArrayList<>();
    }

    private String getCurrentTime(){
        String currentDate = new SimpleDateFormat("yy_MM_dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH_mm_ss", Locale.getDefault()).format(new Date());
        return currentDate + "_" + currentTime ;
    }

    // Save received GNSS data to local directory
    public void saveDataToFile(){
        fileName = "gnss_log_" + getCurrentTime() + ".csv";
        baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath = baseDir + File.separator +  fileName;

        file = new File(filePath);

        firstLine = new String[]{"Lat", "Long", "Speed", "Height", "NumSats", "Bearing", "Sat_ID", "Sat_Type", "Sat_Is_Used", "Sat_Elev", "Sat_Azim", "Sat_CNO"};

        try {
            if (file.exists() && !file.isDirectory()) {
                fileWriter = new FileWriter(filePath, true);
                csvWriter = new CSVWriter(fileWriter);
            } else {
                file.createNewFile();
                csvWriter = new CSVWriter(new FileWriter(filePath));
            }

            csvWriter.writeNext(firstLine);

            for(String [] line : data){
                csvWriter.writeNext(line);
            }

            csvWriter.close();

        } catch (IOException e) {
            Snackbar.make(activity.findViewById(android.R.id.content), "Could not create a log file!", Snackbar.LENGTH_SHORT).show();

        }
        Snackbar.make(activity.findViewById(android.R.id.content), fileName + " created", Snackbar.LENGTH_SHORT).show();

    }

    public void resetData(){
        data.clear();
    }

    public void addData(String [] line){
        data.add(line);
    }

    public int getDataCount() { return this.data.size(); }

    public String getFileName() { return this.fileName; }

    public String getFilePath() { return this.filePath; }


}
