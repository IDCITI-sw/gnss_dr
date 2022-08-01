package com.ai2s_lab.gnss_dr.ui.inference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ai2s_lab.gnss_dr.R;
import com.ai2s_lab.gnss_dr.databinding.FragmentInferBinding;
import com.ai2s_lab.gnss_dr.tflite.Inference;
import com.ai2s_lab.gnss_dr.util.Settings;

import java.io.File;
import java.util.concurrent.locks.Lock;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InferFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InferFragment extends Fragment{
    private static final String TAG = "InferFragment";
    // DataBinding
    private FragmentInferBinding binding;

    // constants
    private int FILE_OPEN_INTENT_REQUEST_CODE = 777;

    // tflite inference
    private Inference inference;

    // UI elements
    private TextView tvCurrPos;
    private TextView tvModel;
    private TextView tvStatus;
    private Button btnOpen;
    private Button btnStart;
    private Button btnStop;

    // variables
    private String Model_Path;
    private Uri Model_Uri;
    private Context context;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public InferFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InferFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InferFragment newInstance(String param1, String param2) {
        InferFragment fragment = new InferFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // init view and inflate
        binding = FragmentInferBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // initialize UI components
        tvCurrPos = binding.textInferPos;
        tvModel = binding.textInferModel;
        tvStatus = binding.textInferStatus;
        btnOpen = binding.btnInferOpen;
        btnStart = binding.btnInferStart;
        btnStop = binding.btnInferStop;

        // initialize tflite inference
        inference = new Inference(getActivity().getApplicationContext(), this);

        //Action handlers for File Open Button
        btnOpen.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Log.e(TAG, "Open Button");
               file_open_dialog();
           }
        });

        //Action handlers for Start Button
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"Start Button");
                Settings.setGps();
                inference.startInference();
                tvStatus.setText("Start");
            }
        });

        //Action handlers for Stop Button
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"Stop Button");
                inference.stopInference();
                tvStatus.setText("Stop");
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        inference.stopInference();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        inference.stopInference();
    }

    public void setTvCurrPos(int position){
        tvCurrPos.setText(Integer.toString(position));
    }

    private void file_open_dialog(){
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_OPEN_INTENT_REQUEST_CODE);
        Log.d(TAG, "File Open Dialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_OPEN_INTENT_REQUEST_CODE) {
            Model_Uri = data.getData();
            Model_Path = getRealPathFromURI(Model_Uri);
            tvModel.setText(Model_Path);
            Log.e(TAG, "data " + data.getData());
            Log.e(TAG, "Path " + Model_Path);
            inference.loadTFLiteModel(Model_Path);      // Load Model for inference
        }
    }

    // Get Absolute Path from URI object
    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }

        String id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + id;
        Cursor cursor = getContext().getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null);
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