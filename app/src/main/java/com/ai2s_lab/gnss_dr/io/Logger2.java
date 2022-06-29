package com.ai2s_lab.gnss_dr.io;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.opencsv.CSVWriter;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
    This version of the Logger utility class is not used (Only applicable for API 31+)
 */
public class Logger2 {

    private String TAG = "LOG";
    private String base_dir;
    private String file_name;
    private String file_path;

    private File file;
    private FileWriter fileWriter;
    private CSVWriter csvWriter;

    //    private Context context;
    private Activity activity;


    public Logger2(Activity activity){

        this.activity = activity;
        file_name = "gnss_log_" + getCurrentTime();

        // create csv file
        ContentResolver resolver = activity.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file_name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/gnss_log_files/");

        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        OutputStream outputStream = null;
        try {
            outputStream = resolver.openOutputStream(uri);
            outputStream.write("Lat Long Speed Height #Sats Bearing".getBytes());

            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, file_name + " created");
        Snackbar.make(activity.findViewById(android.R.id.content), file_name + " created", Snackbar.LENGTH_SHORT).show();

    }

    public String getFileName() { return file_name; }

    // TODO Once GNSS data is known
    public void logData(String input){
        Log.d(TAG, input);
    }

    private String getCurrentTime(){
        String currentDate = new SimpleDateFormat("yy_MM_dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH_mm_ss", Locale.getDefault()).format(new Date());
        return currentDate + "_" + currentTime ;
    }

    public void resetFile(){
        Uri contentUri = MediaStore.Files.getContentUri("external");
        ContentResolver resolver = activity.getContentResolver();

        String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{Environment.DIRECTORY_DOCUMENTS + "/gnss_log_files/"};

        Cursor cursor = resolver.query(contentUri, null, selection, selectionArgs, null);

        Uri uri = null;

        if(cursor.getCount() == 0){
            Snackbar.make(activity.findViewById(android.R.id.content), "No File found", Snackbar.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()){
                int index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                String fileName = cursor.getString(index);
                Log.d(TAG, fileName);
                if(fileName.toLowerCase().equals(file_name + ".csv")){
                    int temp = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                    long id = cursor.getLong(temp);

                    uri = ContentUris.withAppendedId(contentUri, id);

                    break;
                }

            }

            if(uri == null){
                Snackbar.make(activity.findViewById(android.R.id.content), "No File found", Snackbar.LENGTH_SHORT).show();
            } else {
                try {
                    OutputStream outputStream = resolver.openOutputStream(uri, "rwt");
                    outputStream.write("This is overwritten data".getBytes());
                    outputStream.close();

                    Snackbar.make(activity.findViewById(android.R.id.content), "File Written Successfully", Snackbar.LENGTH_SHORT).show();

                } catch (IOException e){
                    Snackbar.make(activity.findViewById(android.R.id.content), "Failed to write file", Snackbar.LENGTH_SHORT).show();

                }
            }
        }
    }

}
