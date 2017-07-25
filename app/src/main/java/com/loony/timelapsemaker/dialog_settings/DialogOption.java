package com.loony.timelapsemaker.dialog_settings;

import android.support.annotation.DrawableRes;

/**
 * Created by Kamil on 7/25/2017.
 */

public class DialogOption {
    public int icon;
    public String label, description;

    public Switch switchState;

    public DialogOption(@DrawableRes int icon, String label, String description, Switch switchState) {
        this.icon = icon;
        this.label = label;
        this.description = description;
        this.switchState = switchState;
    }

    public DialogOption(@DrawableRes int icon, String label, String description) {
        this(icon, label, description, Switch.NOT_EXIST);
    }

    public enum Switch {
        NOT_EXIST,
        DISABLED,
        ENABLED
    }
}
