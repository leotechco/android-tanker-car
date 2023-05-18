package com.fcs.carrotaller.models;

import android.os.CountDownTimer;


public class MiContador extends CountDownTimer {
    private boolean ok_timer;
    public static int a;

    public MiContador(long starTime, long interval) {
        super(starTime, interval);
        this.ok_timer = false;
    }

    @Override
    public void onFinish() {
        this.ok_timer = true;
    }

    @Override
    public void onTick(long millisUntilFinished) {

    }

    public boolean isOk_timer() {
        return ok_timer;
    }

    public void setOk_timer(boolean ok_timer) {
        this.ok_timer = ok_timer;
    }
}
