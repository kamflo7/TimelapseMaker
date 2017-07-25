package com.loony.timelapsemaker.dialog_settings;

import android.support.annotation.DrawableRes;

/**
 * Created by Kamil on 7/25/2017.
 */

public class DialogOption {
    public int icon;
    public String label, description;

    public DialogOption(@DrawableRes int icon, String label, String description) {
        this.icon = icon;
        this.label = label;
        this.description = description;
    }
}
