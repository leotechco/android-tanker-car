package com.fcs.carrotaller.models;

import io.realm.RealmObject;

public class ConfigInicial extends RealmObject{
    //@PrimaryKey
    private int id;
    private int ppu;

    public ConfigInicial(){

    }

    public ConfigInicial(int id, int ppu){
        this.id = id;
        this.ppu = ppu;
    }


    public int getPpu() {
        return ppu;
    }

    public void setPpu(int ppu) {
        this.ppu = ppu;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
