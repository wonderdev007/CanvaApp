package com.pencil.prescription;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ColorArrayAdapter extends ArrayAdapter<String> {
    private Context context;
    private String[] colorNames;
    private int[] colorValues;

    public ColorArrayAdapter(Context context, String[] colorNames, int[] colorValues) {
        super(context, android.R.layout.simple_spinner_item, colorNames);
        this.context = context;
        this.colorNames = colorNames;
        this.colorValues = colorValues;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(colorValues[position]);
        return label;
    }
}
