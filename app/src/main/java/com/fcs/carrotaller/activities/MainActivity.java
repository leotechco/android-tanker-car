package com.fcs.carrotaller.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fcs.carrotaller.R;
import com.fcs.carrotaller.models.Ventas;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;


import cz.msebera.android.httpclient.Header;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {

    private Realm realm;
    private String BASE_URL = "http://fcservices.distracom.com.co/TestRestPos/TramaRestService.svc/";
    //------------------Protocolo-------------------//
    boolean ok_conection;
    String funcion;
    String envioVenta;
    String fechaInicial;
    String fechaFinal;
    String km;
    String chip_leido;
    int ppu;
    int dinero;
    int volumen;
    int numVentasSincronizar;
    boolean ok_envioventa;
    boolean botonSincronizarOcupado;
    //private Button boton_informe;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button boton_configurar = findViewById(R.id.buttonConfigurar);
        //------------------Widget-------------------//
        Button boton_ventas = findViewById(R.id.buttonVender);
        Button boton_sincronizar = findViewById(R.id.buttonSincronizarVentas);

        realm = Realm.getDefaultInstance();

        boton_sincronizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( (!botonSincronizarOcupado) && (numVentasSincronizar == 0)) {
                    botonSincronizarOcupado = true;
                    RealmQuery<Ventas> query;
                    query = realm.where(Ventas.class);
                    query.equalTo("sync", false);
                    RealmResults<Ventas> result1 = query.findAll();
                    int size = result1.size();
                    for (int i = 0; i < size; i++) {
                        numVentasSincronizar++;
                        Ventas ventas1 = result1.get(i);
                        assert ventas1 != null;
                        fechaInicial = ventas1.getFecha_i();
                        fechaFinal = ventas1.getFecha_f();
                        volumen = ventas1.getVolumen();
                        dinero = ventas1.getDinero();
                        chip_leido = ventas1.getChip();
                        km = ventas1.getKm();
                        km = "0" + km;
                        ppu = ventas1.getPpu();

                        envioVenta = "GetProccesSaleRest/F2;" + fechaInicial + ";" + fechaFinal + ";" + volumen + ";" + dinero + ";I-" + chip_leido +
                                ";0;0;2;1;0;0;0;" + km + ";0;0;" + ppu + ";1;1;9999000;TRUE;0;MFC105";
                        requestData(BASE_URL + envioVenta);
                    }
                    botonSincronizarOcupado = false;
                    if(size >= 1) {
                        String msgv = "Sincronizando " + size + " ventas";
                        Toast.makeText(getBaseContext(), msgv, Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getBaseContext(), "No hay ventas para sincronizar", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        boton_ventas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                funcion = "vender";
                Intent intent = new Intent(MainActivity.this, VentasActivity.class);
                intent.putExtra("funcion", funcion);
                startActivity(intent);
            }
        });

        boton_configurar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ConfiguracionActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        ok_conection = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        botonSincronizarOcupado = false;
        numVentasSincronizar = 0;
    }

    private void requestData(String url) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String resp_venta = response.getString("GetProccesSaleRestResult");
                    int pos_ack = resp_venta.indexOf("01ACK");
                    if(pos_ack == 0){
                        String fechaConsulta = resp_venta.substring(6, 20);
                        RealmQuery<Ventas> query;
                        query = realm.where(Ventas.class);
                        query.equalTo("fecha_f", fechaConsulta);
                        RealmResults<Ventas> result1 = query.findAll();
                        if(result1.size() >= 1) {
                            realm.beginTransaction();
                            for (Ventas sync : result1) {
                                sync.setSync(true);
                            }
                            realm.commitTransaction();
                        }
                        String msg = "Venta enviada con exito " + numVentasSincronizar;
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getBaseContext(), resp_venta, Toast.LENGTH_LONG).show();
                    }
                    numVentasSincronizar--;
                    ok_envioventa = true;
                } catch (JSONException e2) {
                    Toast.makeText(getBaseContext(), "No se pudo Procesar Respuesta", Toast.LENGTH_LONG).show();
                    numVentasSincronizar--;
                    ok_envioventa = true;
                }

            }

            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                Toast.makeText(getBaseContext(), "Fallo Conexion al Servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
}