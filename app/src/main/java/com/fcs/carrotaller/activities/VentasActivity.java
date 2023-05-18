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
import com.fcs.carrotaller.models.Ventas;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class VentasActivity extends AppCompatActivity {

    private Realm realm;
    private String BASE_URL = "http://fcservices.distracom.com.co/TestRestPos/TramaRestService.svc/";
    Bundle bundle;
    String funcion_actividad;
    private SharedPreferences prefcalibracion;
    private SimpleDateFormat formateador;
    private SimpleDateFormat formateadorHora;
    private SimpleDateFormat formateadorFechaTexto;
    private DecimalFormat formateadorDecimal;
    //------------------Bluetooth-------------------//
    RecivirBt bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    //------------------Protocolo-------------------//
    static final int estado = 1;
    static final int identificar = 2;
    static final int obtener_chip = 3;
    static final int autorizar = 4;
    static final int reportar_pulsos = 5;
    static final int reportar_venta = 6;
    boolean ok_polling;
    boolean busqueda_chip;
    boolean ok_conection;
    boolean ok_respuesta;
    boolean estado_autorizado;
    boolean intentar_sincronizar;
    boolean intentar_imprimir;
    String dataInPrint;
    String placa;
    Date fechaActual;
    String fechaInicial;
    String fechaFinal;
    String fechaTexto;
    String horaInicial;
    String horaFinal;
    String km;
    private MiContador ContadorPolling;
    int funcion_protocolo;
    double pulsos_actuales;
    int ppu;
    int dinero;
    int volumen;
    double volumen_decimal;
    double pulsos_por_galon;
    int id_recibo;
    char mfc_estado;
    String chip_leido;
    //--------------Widgets---------------//
    private TextView textChip;
    private TextView textEstadoVenta;
    private TextView textVolumen;
    private TextView textDinero;
    private TextView textPpu;
    private TextView textViewPlaca;
    private TextView textViewHoraInicial;
    private TextView textViewHoraFinal;
    private TextView textViewFecha;
    private EditText editTextKm;

    String mequede;

    @SuppressLint({"HandlerLeak", "SimpleDateFormat"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ventas);

        bundle = getIntent().getExtras();
        assert bundle != null;
        funcion_actividad = bundle.getString("funcion");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        bluetoothIn = new RecivirBt();

        ContadorPolling = new MiContador(5000,1000);

        realm = Realm.getDefaultInstance();
        prefcalibracion = getSharedPreferences("MisPreferenciasCalibracion",Context.MODE_PRIVATE);
        formateador = new SimpleDateFormat("yyyyMMddHHmmss");
        formateadorFechaTexto = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        formateadorHora = new SimpleDateFormat("hh:mm:ss");
        formateadorDecimal = new DecimalFormat("###,###.##");

        textChip = findViewById(R.id.textChip);
        textEstadoVenta = findViewById(R.id.textEstadoVenta);
        textVolumen = findViewById(R.id.textVolumen);
        textDinero = findViewById(R.id.textDinero);
        textPpu = findViewById(R.id.textPpu);
        textViewPlaca = findViewById(R.id.textViewPlaca);
        textViewHoraInicial = findViewById(R.id.textViewHoraInicial);
        textViewHoraFinal = findViewById(R.id.textViewHoraFinal);
        textViewFecha = findViewById(R.id.textViewFecha);
        editTextKm = findViewById(R.id.editTextKm);

        //------------------Widget-------------------//
        Button boton_identificar = findViewById(R.id.buttonIdentificar);
        Button boton_imprimir = findViewById(R.id.buttonImprimir);
        Button boton_sincronizar = findViewById(R.id.buttonSincronizar);

        actualizar_datos_venta();
        actualizar_ppu();

        boton_sincronizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String envioVenta = "GetProccesSaleRest/F2;"+fechaInicial+";"+fechaFinal+";"+volumen+";"+dinero+";I-"+chip_leido+
                        ";0;0;2;1;0;0;0;"+km+";0;0;"+ppu+";1;1;9999000;TRUE;0;MFC105";
                if(intentar_sincronizar){
                    requestData(BASE_URL + envioVenta);
                    intentar_sincronizar = false;
                }else{
                    Toast.makeText(getBaseContext(), "No hay ventas para sincronizar", Toast.LENGTH_LONG).show();
                }
            }
        });

        boton_imprimir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String volumenFloat = formateadorDecimal.format(volumen_decimal);
                String recibo =  "    ESTACION DE SERVICIO     \n    MINCIVIL CARROTALLER      \n" +
                        "     NIT: 811.009.788-8       \n        " +
                        "TEL:2894490           \n " +
                        "DIR: AUTOPISTA NORTE KM 26   \n          " +
                        "Medellin            \n---------------------------\n" +
                        "Cliente:   \n" +
                        "Contrato:     No Encontrado   \n" +
                        "---------------------------\n" +
                        "FECHA     "+fechaTexto+"\nNUMERO DE RECIBO:   "+id_recibo+"\n   " +
                        "\n EQUIPO - CARA - MANGUERA    \n   MFC105 -   1  -     1      \n                           \n " +
                        "CANTIDAD -  PRODUCTO  - PPU  \n "+volumenFloat+"    -   DIESEL - $"+ppu+" \n---------------------------\n" +
                        "Placa:   "+placa+"\nHorometro:   " +km+
                        "\n---------------------------\n      " +
                        "Total: $         "+dinero+"\n                          \n" +
                        "\n ";

                if(intentar_imprimir) {
                    if (ok_conection) {
                        ok_conection = false;
                        try {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            btSocket.close();
                        } catch (IOException e2) {
                            Toast.makeText(getBaseContext(), "La Conexión fallo cerrando MFC", Toast.LENGTH_LONG).show();
                        }
                    }

                    address = "00:1B:35:12:34:66";
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    boolean ok_print = false;
                    try {
                        btSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        Toast.makeText(getBaseContext(), "La creacción del Socket Print fallo", Toast.LENGTH_LONG).show();
                    }
                    // Establece la conexión con el socket Bluetooth.
                    try {
                        btSocket.connect();
                        Toast.makeText(getBaseContext(), "Conexion con Print Exitosa", Toast.LENGTH_LONG).show();
                        ok_print = true;
                    } catch (IOException e) {
                        try {
                            btSocket.close();
                            Toast.makeText(getBaseContext(), "La creacción del Socket Print fallo", Toast.LENGTH_LONG).show();
                        } catch (IOException e2) {
                            Toast.makeText(getBaseContext(), "La creacción del Socket Print fallo", Toast.LENGTH_LONG).show();
                        }
                    }
                    if(ok_print) {
                        MyConexionBT = new ConnectedThread(btSocket);
                        MyConexionBT.start();
                        MyConexionBT.write(recibo);
                    }
                }else{
                    Toast.makeText(getBaseContext(), "No puede imprimir en este momento", Toast.LENGTH_LONG).show();
                }
            }
        });

        boton_identificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(funcion_actividad.equals("vender")){
                    if(ok_conection) {
                        if(mfc_estado == 'a') {
                            funcion_protocolo = identificar;
                        }else{
                            Toast.makeText(getBaseContext(), "APP Ocupada", Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getBaseContext(), "No hay conexion con MFC", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }else{
                    Toast.makeText(getBaseContext(), "Ingrese desde la opcion vender para usar esta Funcion", Toast.LENGTH_LONG).show();
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
    protected void onDestroy() {
        super.onDestroy();
        ok_polling = false;
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

    @SuppressLint("SetTextI18n")
    private void actualizar_datos_venta(){
        if(funcion_actividad.equals("vender")){
            estado_autorizado = false;
            intentar_sincronizar = false;
            intentar_imprimir = false;
            textChip.setText("0");
            textEstadoVenta.setText("Estado: 0");
            textVolumen.setText("Volumen: G 0");
            textDinero.setText("Dinero: $0");
            textViewPlaca.setText("Placa: 0");
            textViewHoraFinal.setText("Hora Final: 0");
            textViewHoraInicial.setText("Hora Inicial: 0");
            textViewFecha.setText("Fecha: 0");
            chip_leido = prefcalibracion.getString("chip_leido", "0");
            fechaInicial = prefcalibracion.getString("fechaInicial", "0");
            km = prefcalibracion.getString("km", "0");
            //textViewFecha.setText("Fecha Inicial: "+ formateadorFechaTexto.format(fechaInicial));
            textChip.setText(chip_leido);
        }
    }

    @SuppressLint("SetTextI18n")
    private void actualizar_ppu(){
        if(funcion_actividad.equals("vender")) {
            RealmQuery<ConfigInicial> query;
            query = realm.where(ConfigInicial.class);
            query.equalTo("id", 8);
            RealmResults<ConfigInicial> result1 = query.findAll();
            if (result1.size() >= 1) {
                ConfigInicial configInicial = result1.get(0);
                assert configInicial != null;
                ppu = configInicial.getPpu();
                textPpu.setText(("Dinero: $") + ppu);
            } else {
                Toast.makeText(getBaseContext(), "PPU 0 no se pueden hacer ventas", Toast.LENGTH_LONG).show();
                finish();
            }
            pulsos_por_galon = prefcalibracion.getInt("pulsos_por_galon", 0);
            if (pulsos_por_galon == 0) {
                Toast.makeText(getBaseContext(), "Calibrar antes de hacer ventas", Toast.LENGTH_LONG).show();
                finish();
            }
        }
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
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;
            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                ok_conection = false;
                VerificarConexionBt verificarConexionBt = new VerificarConexionBt();
                verificarConexionBt.start();
            }
        }
    }

    private void requestData(String url) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new JsonHttpResponseHandler() {

            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String resp_venta = response.getString("GetProccesSaleRestResult");
                    int pos_ack = resp_venta.indexOf("01ACK");
                    if(pos_ack == 0){
                        RealmQuery<Ventas> query;
                        query = realm.where(Ventas.class);
                        query.equalTo("id", id_recibo);
                        RealmResults<Ventas> result1 = query.findAll();
                        if(result1.size() >= 1) {
                            realm.beginTransaction();
                            for (Ventas sync : result1) {
                                sync.setSync(true);
                            }
                            realm.commitTransaction();
                        }
                        Toast.makeText(getBaseContext(), "Venta enviada con exito", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getBaseContext(), resp_venta, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e2) {
                    Toast.makeText(getBaseContext(), "No se pudo Procesar Respuesta", Toast.LENGTH_LONG).show();
                }
            }

            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                Toast.makeText(getBaseContext(), "Fallo Conexion al Servidor", Toast.LENGTH_SHORT).show();
            }
        });
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

                            case 'B':
                                ok_respuesta = true;
                                break;

                            case 'C':
                                chip_leido = dataInPrint.substring(4, 20);
                                textChip.setText(chip_leido);
                                try
                                {
                                    BufferedReader fin =
                                            new BufferedReader(
                                                    new InputStreamReader(
                                                            openFileInput("vehiculos.txt")));
                                    String texto = fin.readLine();
                                    fin.close();
                                    int index = texto.indexOf(chip_leido);
                                    String chip_leido_min = chip_leido.toLowerCase();
                                    int index2 = texto.indexOf(chip_leido_min);
                                    if((index >= 0) || (index2 >= 0)){
                                        if(index < 0){
                                            index = index2;
                                        }
                                        busqueda_chip = true;
                                        int index_placa1 = index;
                                        while(texto.charAt(index_placa1) != ','){
                                            index_placa1--;
                                        }
                                        int index_placa2 = index_placa1;
                                        while(texto.charAt(index_placa2) != ':'){
                                            index_placa2--;
                                        }
                                        fechaActual = new Date();
                                        fechaInicial = formateador.format(fechaActual);
                                        fechaTexto = formateadorFechaTexto.format(fechaActual);
                                        horaInicial = formateadorHora.format(fechaActual);
                                        index_placa2++;
                                        texto = texto.substring(index_placa2, index_placa1);
                                        placa = texto.replace("\"", "");
                                    }
                                }
                                catch (Exception ex)
                                {
                                    Toast.makeText(getBaseContext(), "Error al abrir Fichero", Toast.LENGTH_LONG).show();
                                }
                                ok_respuesta = true;
                                break;

                            case 'D':
                                SharedPreferences.Editor editor = prefcalibracion.edit();
                                editor.putString("km", km);
                                editor.putString("chip_leido", chip_leido);
                                editor.putString("fechaInicial", fechaInicial);
                                editor.apply();
                                ok_respuesta = true;
                                break;

                            case 'E':
                                String pulsos = dataInPrint.substring(4, 10);
                                pulsos_actuales = Integer.parseInt(pulsos);
                                ok_respuesta = true;
                                break;

                            case 'F':
                                pulsos = dataInPrint.substring(4, 10);
                                pulsos_actuales = Integer.parseInt(pulsos);
                                fechaActual = new Date();
                                fechaFinal = formateador.format(fechaActual);
                                fechaTexto = formateadorFechaTexto.format(fechaActual);
                                horaFinal = formateadorHora.format(fechaActual);
                                km = editTextKm.getText().toString();
                                km = "0" + km;
                                volumen_decimal = pulsos_actuales / pulsos_por_galon;
                                volumen = (int)(volumen_decimal*1000);
                                dinero = (volumen * ppu)/1000;

                                realm.beginTransaction();
                                Ventas ventas = new Ventas(fechaInicial, fechaFinal, chip_leido, dinero, volumen, ppu, km, placa, false);
                                realm.copyToRealm(ventas);
                                realm.commitTransaction();

                                RealmQuery<Ventas> query;
                                query = realm.where(Ventas.class);
                                query.equalTo("fecha_i", fechaInicial);
                                RealmResults<Ventas> result1 = query.findAll();
                                if(result1.size() >= 1) {
                                    Ventas ventas1 = result1.get(0);
                                    assert ventas1 != null;
                                    id_recibo = ventas1.getId();
                                }

                                editor = prefcalibracion.edit();
                                editor.putString("km", "0");
                                editor.putString("fechaInicial", "0");
                                editor.putString("chip_leido", "0");
                                editor.apply();

                                ok_respuesta = true;
                                break;

                        }
                    }
                    DataStringIN.delete(0, DataStringIN.length());
                }
            }
        }

    }

    private class PollingThread extends Thread{
        PollingThread(){

        }

        public void run(){
            while (ok_polling){
                if (ok_conection){
                    switch (funcion_protocolo){
                        case estado:
                            MyConexionBT.write("MFCA0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            ContadorPolling.start();
                            while((!ContadorPolling.isOk_timer()) && (!ok_respuesta)){
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if(!ok_respuesta ){
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: No responde MFC");
                                    }
                                });
                            }else{
                                if(mfc_estado == 'b') {
                                    textEstadoVenta.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textEstadoVenta.setText("Estado: Leyendo Ibuttton");
                                        }
                                    });
                                }else if(mfc_estado == 'c') {
                                    funcion_protocolo = obtener_chip;
                                    textEstadoVenta.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textEstadoVenta.setText("Estado: Obteniendo Ibutton");
                                        }
                                    });
                                }else if(mfc_estado == 'e') {
                                    funcion_protocolo = reportar_pulsos;
                                }else if(mfc_estado == 'f') {
                                    funcion_protocolo = reportar_venta;
                                }else if(mfc_estado == 'a') {
                                    textEstadoVenta.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textEstadoVenta.setText("Estado: MFC Libre");
                                        }
                                    });
                                }
                            }
                            break;

                        case identificar:
                            MyConexionBT.write("MFCB0#");
                            ContadorPolling.setOk_timer(false);
                            ok_respuesta = false;
                            ContadorPolling.start();
                            while((!ContadorPolling.isOk_timer()) && (!ok_respuesta)){
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if(ok_respuesta ){
                                funcion_protocolo = estado;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: Identificando...");
                                    }
                                });
                            }else{
                                funcion_protocolo = 0;
                                finish();
                            }
                            break;

                        case obtener_chip:
                            MyConexionBT.write("MFCC0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            busqueda_chip = false;
                            ContadorPolling.start();
                            while((!ContadorPolling.isOk_timer()) && (!ok_respuesta)){
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if(ok_respuesta ){
                                if(busqueda_chip) {
                                    funcion_protocolo = autorizar;
                                    estado_autorizado = true;
                                    textEstadoVenta.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textEstadoVenta.setText("Estado: Chip encontrado Inicie Venta");
                                        }
                                    });
                                    textViewFecha.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textViewFecha.setText("Fecha: " + fechaTexto);
                                        }
                                    });
                                    textViewHoraInicial.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textViewHoraInicial.setText("Hora: " + horaInicial);
                                        }
                                    });
                                    textViewPlaca.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textViewPlaca.setText("Placa: " + placa);
                                        }
                                    });
                                }else{
                                    funcion_protocolo = estado;
                                    estado_autorizado = false;
                                    textEstadoVenta.post(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            textEstadoVenta.setText("Estado: Chip no encontrado");
                                        }
                                    });
                                }
                            }else{
                                funcion_protocolo = estado;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: No responde MFC");
                                    }
                                });
                            }
                            break;

                        case autorizar:
                            MyConexionBT.write("MFCD0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            while((!ContadorPolling.isOk_timer()) && (!ok_respuesta)){
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if(ok_respuesta ){
                                funcion_protocolo = estado;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: Autorizado");
                                    }
                                });
                            }else{
                                funcion_protocolo = estado;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: No responde MFC");
                                    }
                                });
                            }
                            break;

                        case reportar_pulsos:
                            MyConexionBT.write("MFCE0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            ContadorPolling.start();
                            while((!ContadorPolling.isOk_timer()) && (!ok_respuesta)){
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if(ok_respuesta ){
                                funcion_protocolo = estado;
                                volumen_decimal = pulsos_actuales / pulsos_por_galon;
                                volumen = (int)(volumen_decimal*1000);
                                dinero = (volumen * ppu)/1000;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: Tanqueando");
                                    }
                                });
                                textVolumen.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textVolumen.setText(("Volumen: G") + formateadorDecimal.format(volumen_decimal));
                                    }
                                });
                                textDinero.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textDinero.setText(("Dinero: $") + dinero);
                                    }
                                });
                            }else{
                                funcion_protocolo = estado;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("No responde MFC");
                                    }
                                });
                            }
                            break;

                        case reportar_venta:
                            MyConexionBT.write("MFCF0#");
                            ok_respuesta = false;
                            ContadorPolling.setOk_timer(false);
                            ContadorPolling.start();
                            while((!ContadorPolling.isOk_timer()) && (!ok_respuesta)){
                                mequede = "Siiiiii";
                            }
                            mequede = "Noooooo";
                            if(ok_respuesta ){
                                funcion_protocolo = estado;
                                intentar_sincronizar = true;
                                intentar_imprimir = true;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("Estado: Venta Finalizada");
                                    }
                                });
                                textVolumen.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textVolumen.setText(("Volumen: G") + formateadorDecimal.format(volumen_decimal));
                                    }
                                });
                                textDinero.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textDinero.setText(("Dinero: $") + dinero);
                                    }
                                });
                                textViewHoraFinal.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textViewHoraFinal.setText("Hora Final:" + fechaTexto);
                                    }
                                });
                            }else{
                                funcion_protocolo = estado;
                                textEstadoVenta.post(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        textEstadoVenta.setText("No responde MFC");
                                    }
                                });
                            }
                            break;

                    }
                }
            }
        }
    }

}
