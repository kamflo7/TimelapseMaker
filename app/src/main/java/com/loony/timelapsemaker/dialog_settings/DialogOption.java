package com.loony.timelapsemaker.dialog_settings;

import android.support.annotation.DrawableRes;
import android.view.View;
import android.widget.CompoundButton;

/**
 * Created by Kamil on 7/25/2017.
 */

public class DialogOption {
    public int icon;
    public String label, description;

    public Switch switchState;
    public CompoundButton.OnCheckedChangeListener onSwitchCheckedChange;

    public DialogOption(@DrawableRes int icon, String label, String description, Switch switchState, CompoundButton.OnCheckedChangeListener onSwitchCheckedChange) {
        this.icon = icon;
        this.label = label;
        this.description = description;
        this.switchState = switchState;
        this.onSwitchCheckedChange = onSwitchCheckedChange;
    }

    public DialogOption(@DrawableRes int icon, String label, String description) {
        this(icon, label, description, Switch.NOT_EXIST, null);
    }

    public enum Switch {
        NOT_EXIST,
        DISABLED,
        ENABLED
    }


}
