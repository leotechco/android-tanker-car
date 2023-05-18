package com.fcs.carrotaller.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fcs.carrotaller.R;
import com.fcs.carrotaller.models.ConfigInicial;
import com.fcs.carrotaller.models.MiContador;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class ConfiguracionActivity extends AppCompatActivity {

    private Realm realm;
    private String BASE_URL = "http://fcservices.distracom.com.co/TestRestPos/TramaRestService.svc/";
    private EditText editTextgalonesDespachados;
    private TextView textPppu;
    private TextView textpulsos_por_galon;
    private TextView textpulsos_calibracion;
    private TextView textgalones_calibracion;
    private TextView text_titulo_cal;
    private SharedPreferences prefcalibracion;
    //------------------Bluetooth-------------------//
    RecivirBt bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //------------------Protocolo-------------------//
    static final int estado = 1;
    static final int calibrar = 2;
    static final int pulsos_calibracion = 3;
    static final int reporte_pulsos = 4;
    boolean ok_polling;
    boolean ok_conection;
    boolean ok_respuesta;
    int funcion_protocolo;
    char mfc_estado;
    int galones_calibrados;
    int pulsos_galon;
    int pulsos_actuales;
    String dataInPrint;
    String mequede;
    private MiContador ContadorPolling;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        ContadorPolling = new MiContador(5000,1000);

        bluetoothIn = new RecivirBt();

        realm = Realm.getDefaultInstance();
        //------------------Widget-------------------//
        Button boton_solicitarppu = findViewById(R.id.buttonConfigInicial);
        Button boton_calibrar = findViewById(R.id.buttonCalibrar);
        Button boton_calcular = findViewById(R.id.buttonCalcular);
        editTextgalonesDespachados = findViewById(R.id.editTextGalonesDes);
        textPppu = findViewById(R.id.textPppu);
        textpulsos_por_galon = findViewById(R.id.textViewPulsosporGalon);
        textpulsos_calibracion = findViewById(R.id.textViewpulsoCalibracion);
        textgalones_calibracion = findViewById(R.id.textViewGalonesCalibracion);
        text_titulo_cal = findViewById(R.id.textTituloCal);
        prefcalibracion = getSharedPreferences("MisPreferenciasCalibracion",Context.MODE_PRIVATE);

        actualizar_ppu();
        actualizar_calibracion();

        boton_solicitarppu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestData(BASE_URL + "GetInitialSettings/344;1");
                requestData(BASE_URL + "GetInitialVehiculeByEDS/344");
            }
        });

        boton_calibrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ok_conection) {
                    if(mfc_estado == 'a') {
                        funcion_protocolo = calibrar;
                    }else{
                        Toast.makeText(getBaseContext(), "APP Ocupada", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getBaseContext(), "No hay conexion con MFC", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

        boton_calcular.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if(mfc_estado == 'i'){
                    String galones_des = editTextgalonesDespachados.getText().toString();
                    int size = galones_des.length();
                    if(size > 0) {
                        galones_calibrados = Integer.parseInt(galones_des);
                        pulsos_galon = pulsos_actuales / galones_calibrados;
                        textpulsos_por_galon.setText("Pulsos por Galon: " + pulsos_galon);
                        textgalones_calibracion.setText("Galones Calibracion:" + galones_calibrados);
                        textpulsos_calibracion.setText("Pulsos Calibracion:" + pulsos_actuales);
                        SharedPreferences.Editor editor = prefcalibracion.edit();
                        editor.putInt("pulsos_por_galon", pulsos_galon);
                        editor.putInt("pulsos_calibracion", pulsos_actuales);
                        editor.putInt("galones_calibracion", galones_calibrados);
                        editor.apply();
                        text_titulo_cal.setText("Calibracion Exitosa");
                    }else{
                        Toast.makeText(getBaseContext(), "Ingrese un valor Valido", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getBaseContext(), "No hay ningun proceso de Calibracion Terminado", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        ok_polling = false;
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String address = "20:16:10:24:75:59";
        conectarBt(address);
        funcion_protocolo = estado;
        ok_polling = true;
        PollingThread polling = new PollingThread();
        String estado_polling = String.valueOf(polling.getState());
        if(estado_polling.equals("NEW")) {
            polling.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ok_polling = false;
    }

    private void conectarBt(String address){
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            ok_conection = false;
        }
        try
        {
            btSocket.connect();
            ok_conection = true;
        } catch (IOException e) {
            try {
                btSocket.close();
                ok_conection = false;
            } catch (IOException e2) {
                ok_conection = false;
            }
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    private void requestData(String url) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new JsonHttpResponseHandler() {

            @SuppressLint("SetTextI18n")
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String config_inicial = response.getString("GetInitialSettingsResult");
                    int pos = config_inicial.indexOf("MFC");
                    String ppu = config_inicial.substring((pos+11), (pos+15));
                    int ppu_entero = Integer.parseInt(ppu);
                    RealmQuery<ConfigInicial> query;
                    query = realm.where(ConfigInicial.class);
                    query.equalTo("id", 8);
                    RealmResults<ConfigInicial> result1 = query.findAll();
                    if(result1.size() >= 1) {
                        realm.beginTransaction();
                        for (ConfigInicial ppu_nuevo : result1) {
                            ppu_nuevo.setPpu(ppu_entero);
                        }
                        realm.commitTransaction();
                        textPppu.setText("PPU: $" + ppu_entero);
                    }else{
                        realm.beginTransaction();
                        ConfigInicial configInicial = new ConfigInicial(8, ppu_entero);
                        realm.copyToRealm(configInicial);
                        realm.commitTransaction();
                        textPppu.setText("PPU: $" + ppu_entero);
                    }
                    Toast.makeText(getBaseContext(), "PPU Actualizado", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    try {
                        String clientes_autorizados = response.getString("GetInitialVehiculeByEDSResult");
                        try
                        {
                            OutputStreamWriter actualizar_vehiculos=
                                    new OutputStreamWriter(
                                            openFileOutput("vehiculos.txt", Context.MODE_PRIVATE));
                            actualizar_vehiculos.write(clientes_autorizados);
                            actualizar_vehiculos.close();
                            Toast.makeText(getBaseContext(), "Vehiculos Actualizados", Toast.LENGTH_LONG).show();
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(getBaseContext(), "Error al escribir Fichero", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e1) {
                        Toast.makeText(getBaseContext(), "Error en la respuesta", Toast.LENGTH_LONG).show();
                    }
                }
            }

            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                Toast.makeText(getBaseContext(), "Fallo Conexion al Servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void actualizar_ppu(){
        RealmQuery<ConfigInicial> query;
        query = realm.where(ConfigInicial.class);
        query.equalTo("id", 8);
        RealmResults<ConfigInicial> result1 = query.findAll();
        if(result1.size() >= 1) {
            ConfigInicial configInicial = result1.get(0);
            assert configInicial != null;
            int ppu = configInicial.getPpu();
            textPppu.setText("PPU: $" + ppu);
        }else{
            textPppu.setText("PPU: $" + 0);
        }
    }

    @SuppressLint("SetTextI18n")
    private void actualizar_calibracion(){
        int pulsos_por_galon = prefcalibracion.getInt("pulsos_por_galon", 0);
        int pulsos_cali = prefcalibracion.getInt("pulsos_calibracion", 0);
        int galones_calibracion = prefcalibracion.getInt("galones_calibracion", 0);
        textpulsos_por_galon.setText("Pulsos por Galon: " + pulsos_por_galon);
        textpulsos_calibracion.setText("Pulsos Calibracion:" + pulsos_cali);
        textgalones_calibracion.setText("Galones Calibracion:" + galones_calibracion);
        text_titulo_cal.setText("Calibracion");
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private void VerificarEstadoBT() {
        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e){
                ok_conection = false;
                VerificarConexionBt verificarConexionBt = new VerificarConexionBt();
                verificarConexionBt.start();
            }
        }
    }

    private class PollingThread extends Thread{
        PollingThread(){

        }

        public void run(){
            while (ok_polling){
                if(ok_conection) {
                    switch (funcion_protocolo) {
                        case estado:
                            MyConexionBT.write("MFCA0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            ContadorPolling.start();
                            while ((!ContadorPolling.isOk_timer()) && (!ok_respuesta)) {
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if (!ok_respuesta) {
                                text_titulo_cal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        text_titulo_cal.setText("No responde MFC");
                                    }
                                });
                            } else {
                                if (mfc_estado == 'g') {
                                    text_titulo_cal.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            text_titulo_cal.setText("Levante la Manija Por Favor");
                                        }
                                    });
                                } else if (mfc_estado == 'h') {
                                    funcion_protocolo = pulsos_calibracion;
                                    text_titulo_cal.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            text_titulo_cal.setText("Tanqueando");
                                        }
                                    });
                                } else if (mfc_estado == 'i') {
                                    funcion_protocolo = reporte_pulsos;
                                } else if (mfc_estado == 'a') {
                                    text_titulo_cal.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            text_titulo_cal.setText("MFC Libre");
                                        }
                                    });
                                }
                            }
                            break;

                        case calibrar:
                            MyConexionBT.write("MFCG0#");
                            ContadorPolling.setOk_timer(false);
                            ok_respuesta = false;
                            ContadorPolling.start();
                            while ((!ContadorPolling.isOk_timer()) && (!ok_respuesta)) {
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if (ok_respuesta) {
                                funcion_protocolo = estado;
                                text_titulo_cal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        text_titulo_cal.setText("Inicie la Calibracion");
                                    }
                                });
                            } else {
                                funcion_protocolo = estado;
                                text_titulo_cal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        text_titulo_cal.setText("No responde MFC");
                                    }
                                });
                            }
                            break;

                        case pulsos_calibracion:
                            MyConexionBT.write("MFCH0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            ContadorPolling.start();
                            while ((!ContadorPolling.isOk_timer()) && (!ok_respuesta)) {
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if (ok_respuesta) {
                                funcion_protocolo = estado;
                                textpulsos_calibracion.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textpulsos_calibracion.setText("Pulsos Calibracion:" + pulsos_actuales);
                                    }
                                });
                            } else {
                                funcion_protocolo = estado;
                                text_titulo_cal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        text_titulo_cal.setText("No responde MFC");
                                    }
                                });
                            }
                            break;

                        case reporte_pulsos:
                            MyConexionBT.write("MFCI0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            ContadorPolling.start();
                            while ((!ContadorPolling.isOk_timer()) && (!ok_respuesta)) {
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if (ok_respuesta) {
                                funcion_protocolo = 0;
                                textpulsos_calibracion.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textpulsos_calibracion.setText("Pulsos Calibracion:" + pulsos_actuales);
                                    }
                                });
                                text_titulo_cal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        text_titulo_cal.setText("Ingrese los Galones Tanqueados");
                                    }
                                });
                            } else {
                                funcion_protocolo = estado;
                                text_titulo_cal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        text_titulo_cal.setText("No responde MFC");
                                    }
                                });
                            }
                            break;
                    }
                }
            }
        }
    }

    private class VerificarConexionBt extends Thread{
        VerificarConexionBt(){

        }
        public void run(){
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String address = "20:16:10:24:75:59";
            conectarBt(address);
            ok_conection = true;
        }
    }

    @SuppressLint("HandlerLeak")
    private class RecivirBt extends Handler{
        RecivirBt(){

        }

        public void handleMessage(android.os.Message msg) {
            if (msg.what == handlerState) {
                String readMessage = (String) msg.obj;
                DataStringIN.append(readMessage);

                int endOfLineIndex = DataStringIN.indexOf("#");

                if (endOfLineIndex > 0) {
                    dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                    if(dataInPrint.indexOf("APP") == 0){
                        char comando = dataInPrint.charAt(3);
                        char estado = dataInPrint.charAt(4);
                        switch (comando){
                            case 'A':
                                mfc_estado = estado;
                                ok_respuesta = true;
                                break;

                            case 'G':
                                ok_respuesta = true;
                                break;

                            case 'H':
                                String pulsos = dataInPrint.substring(4, 10);
                                pulsos_actuales = Integer.parseInt(pulsos);
                                ok_respuesta = true;
                                break;

                            case 'I':
                                pulsos = dataInPrint.substring(4, 10);
                                pulsos_actuales = Integer.parseInt(pulsos);
                                ok_respuesta = true;
                                break;

                        }
                    }
                    DataStringIN.delete(0, DataStringIN.length());
                }
            }
        }

    }

}
