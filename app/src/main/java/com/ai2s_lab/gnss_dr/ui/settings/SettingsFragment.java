package com.ai2s_lab.gnss_dr.ui.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ai2s_lab.gnss_dr.databinding.FragmentSettingsBinding;
import com.ai2s_lab.gnss_dr.util.Settings;
import com.google.android.material.slider.Slider;


public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    // UI Elements
    private TextView settingsTitle;
    private TextView sliderTitle;

    private Slider slider;

    // settings
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // initialize UI elements
        settingsTitle = binding.settingsTitle;
        slider = binding.settingsSlider;
        sliderTitle = binding.settingsSliderTitle;

        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                Settings.setUpdateFrequency((int) value);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void invisibleUI(){
        sliderTitle.setVisibility(View.INVISIBLE);
        slider.setVisibility(View.INVISIBLE);
    }

    public void visibleUI(){
        sliderTitle.setVisibility(View.VISIBLE);
        slider.setVisibility(View.VISIBLE);

    }

}