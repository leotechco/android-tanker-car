package com.fcs.carrotaller.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fcs.carrotaller.R;

import java.util.List;

public class MyAdapter extends BaseAdapter {
    private Context context;
    private int layout;
    private List<String> ventas;

    public MyAdapter(Context context, int layout, List<String> names){
        this.context = context;
        this.layout = layout;
        this.ventas = names;
    }

    @Override
    public int getCount() {

        return this.ventas.size();
    }

    @Override
    public Object getItem(int position) {

        return this.ventas.get(position);
    }

    @Override
    public long getItemId(int id) {

        return id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Copiamos la vista
        View v = convertView;

        //Inflamos la vista
        LayoutInflater layoutInflater = LayoutInflater.from(this.context);
        v = layoutInflater.inflate(R.layout.list_ventas, null);

        //Traemos el valor actual dependiendo la pos
        String currentName = ventas.get(position);
        //currentName = (String) getItem(position);

        //Referenciamos el elemento a modificar y lo rellenamos
        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(currentName);

        return v;
    }
}
