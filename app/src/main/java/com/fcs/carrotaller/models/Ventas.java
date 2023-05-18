package com.fcs.carrotaller.models;

import com.fcs.carrotaller.app.MyApplication;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Ventas extends RealmObject {

    @PrimaryKey
    private int id;

    private String fecha_i;
    private String fecha_f;
    private String chip;
    private int dinero;
    private int volumen;
    private int ppu;
    private String km;
    private String placa;
    private Boolean sync;

    public Ventas(){

    }

    public Ventas (String fecha_i, String fecha_f, String chip, int dinero, int volumen, int ppu, String km, String placa, Boolean sync) {
        this.id = MyApplication.VentasId.incrementAndGet();
        this.fecha_f = fecha_f;
        this.fecha_i = fecha_i;
        this.chip = chip;
        this.dinero = dinero;
        this.volumen = volumen;
        this.ppu = ppu;
        this.km = km;
        this.placa = placa;
        this.sync = sync;
    }

    public int getId() {
        return id;
    }

    public String getFecha_i() {
        return fecha_i;
    }

    public void setFecha_i(String fecha_i) {
        this.fecha_i = fecha_i;
    }

    public String getFecha_f() {
        return fecha_f;
    }

    public void setFecha_f(String fecha_f) {
        this.fecha_f = fecha_f;
    }

    public String getChip() {
        return chip;
    }

    public void setChip(String chip) {
        this.chip = chip;
    }

    public int getDinero() {
        return dinero;
    }

    public void setDinero(int dinero) {
        this.dinero = dinero;
    }

    public int getVolumen() {
        return volumen;
    }

    public void setVolumen(int volumen) {
        this.volumen = volumen;
    }

    public int getPpu() {
        return ppu;
    }

    public void setPpu(int ppu) {
        this.ppu = ppu;
    }

    public String getKm() {
        return km;
    }

    public void setKm(String km) {
        this.km = km;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public Boolean getSync() {
        return sync;
    }

    public void setSync(Boolean sync) {
        this.sync = sync;
    }
}
