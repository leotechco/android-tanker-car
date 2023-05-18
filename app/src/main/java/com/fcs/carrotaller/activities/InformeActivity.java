package com.fcs.carrotaller.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.fcs.carrotaller.Adapters.MyAdapter;
import com.fcs.carrotaller.R;

import java.util.ArrayList;
import java.util.List;

public class InformeActivity extends AppCompatActivity {

    private ListView listViewVentas;
    private List<String> names;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informe);


        listViewVentas = (ListView)findViewById(R.id.listViewVentas);

        //Datos a mostrar
        names = new ArrayList<String>();
        names.add("Venta No 004  Fecha: 26-09-2018 09:50  Estado:Sin sincronizar");
        names.add("Venta No 003  Fecha: 26-09-2018 09:45  Estado:Sincronizada");
        names.add("Venta No 002  Fecha: 26-09-2018 09:30  Estado:Sin sincronizar");
        names.add("Venta No 001  Fecha: 26-09-2018 09:15  Estado:Sin sincronizar");


        listViewVentas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(InformeActivity.this, "clicked "+names.get(position), Toast.LENGTH_LONG).show();
            }
        });

        MyAdapter myAdapter = new MyAdapter(this, R.layout.list_ventas, names);
        listViewVentas.setAdapter(myAdapter);
    }
}

