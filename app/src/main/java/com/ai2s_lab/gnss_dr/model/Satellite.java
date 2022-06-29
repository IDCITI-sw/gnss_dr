package com.ai2s_lab.gnss_dr.model;

public class Satellite {
    private int id;
    private String type;
    private boolean isUsed;
    private double elev;
    private double azim;
    private double cno;

    public Satellite(int id, String type, boolean isUsed, double elev, double azim, double cno) {
        this.id = id;
        this.type = type;
        this.isUsed = isUsed;
        this.elev = elev;
        this.azim = azim;
        this.cno = cno;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getElev() {
        return elev;
    }

    public double getAzim() {
        return azim;
    }

    public double getCno() {
        return cno;
    }
}
