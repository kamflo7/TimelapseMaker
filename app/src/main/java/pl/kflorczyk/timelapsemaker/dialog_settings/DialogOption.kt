package pl.kflorczyk.timelapsemaker.dialog_settings

import android.support.annotation.DrawableRes
import android.widget.CompoundButton

data class DialogOption(@DrawableRes var icon: Int,
                        var label: String,
                        var description: String,
                        var switchState: Switch,
                        var onSwitchCheckedChange: CompoundButton.OnCheckedChangeListener?) {

    constructor(@DrawableRes icon: Int, label: String, description: String): this(icon, label, description, Switch.NOT_EXIST, null)

    enum class Switch {
        NOT_EXIST,
        DISABLED,
        ENABLED
    }
}