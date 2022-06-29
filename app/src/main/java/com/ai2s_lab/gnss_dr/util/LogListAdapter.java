package com.ai2s_lab.gnss_dr.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ai2s_lab.gnss_dr.R;
import com.ai2s_lab.gnss_dr.model.Satellite;

import java.util.ArrayList;

public class LogListAdapter extends ArrayAdapter<Satellite> {

    private Context context;

    private ArrayList<Satellite> satellites;

    public LogListAdapter(Context context, ArrayList<Satellite> satellites){
        super(context, R.layout.log_list_item, satellites);

        this.context = context;
        this.satellites = satellites;
    }

    public View getView(int position, View view, ViewGroup parent){
        if(view == null){
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(R.layout.log_list_item, null);

        }
        TextView textId = view.findViewById(R.id.log_item_id);
        TextView textType = view.findViewById(R.id.log_item_type);
        TextView textCno = view.findViewById(R.id.log_item_cno);
        TextView textElev = view.findViewById(R.id.log_item_elev);
        TextView textAzim = view.findViewById(R.id.log_item_azim);

        Satellite satellite = satellites.get(position);
        textId.setText(Integer.toString(satellite.getId()));
        textType.setText(satellite.getType());
        textCno.setText(Double.toString(satellite.getCno()));
        textElev.setText(Double.toString(satellite.getElev()));
        textAzim.setText(Double.toString(satellite.getAzim()));

        return view;
    }
}
