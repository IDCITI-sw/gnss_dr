package com.ai2s_lab.gnss_dr.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionSupport {

    private Context context;
    private Activity activity;

    //Permissions related
    private final int MULTIPLE_PERMISSIONS = 1023;
    private List permissionList;

    // List of required permissions
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

    public PermissionSupport(Activity activity, Context context){
        this.activity = activity;
        this.context = context;
    }

    // Check if required permissions are enabled.
    public boolean arePermissionsEnabled(){
        permissionList = new ArrayList<>();

        for(String pm : permissions){
            Log.i("PERMISSION", pm);
            if(ActivityCompat.checkSelfPermission(context, pm) != PackageManager.PERMISSION_GRANTED)
                permissionList.add(pm);
        }

        if(permissionList.size() > 0 )
            return false;

        return true;
    }

    //Request for permissions in the list above.
    public void requestMultiplePermissions(){
        ActivityCompat.requestPermissions(activity, (String[]) permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == MULTIPLE_PERMISSIONS && (grantResults.length > 0)){
            for(int i = 0; i < grantResults.length; i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                   if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])){
                       requestMultiplePermissions();
                   }
                return false;
            }
        }
        return true;
    }

}
