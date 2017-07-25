package com.loony.timelapsemaker.dialog_settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;

import java.util.ArrayList;

/**
 * Created by Kamil on 7/25/2017.
 */

public class DialogSettingsAdapter extends ArrayAdapter<DialogOption> {
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<DialogOption> options;

    public DialogSettingsAdapter(Context context, ArrayList<DialogOption> options) {
        super(context, 0, options);
        this.context = context;
        this.options = options;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Util.log("Is this visible that calling?");
        Holder holder;
        View view;

        if (convertView == null) {
            view = inflater.inflate(R.layout.item_settings_element, parent, false);
            holder = new Holder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.label = (TextView) view.findViewById(R.id.label);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.switchCompat = (SwitchCompat) view.findViewById(R.id.switchBtn);

            view.setTag(holder);
        } else {
            view = convertView;
            holder = (Holder) view.getTag();
        }

        DialogOption item = getItem(position);
        holder.icon.setImageResource(item.icon);
        holder.label.setText(item.label);
        holder.description.setText(item.description);

        if(item.switchState != DialogOption.Switch.NOT_EXIST) {
            holder.switchCompat.setVisibility(View.VISIBLE);
            holder.switchCompat.setChecked(item.switchState == DialogOption.Switch.ENABLED);
        } else {
            holder.switchCompat.setVisibility(View.GONE);
        }

        return view;
    }

    private class Holder {
        public ImageView icon;
        public TextView label, description;
        public SwitchCompat switchCompat;
    }
}
