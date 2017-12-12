package pl.kflorczyk.timelapsemaker.dialog_settings

import android.content.Context
import android.support.v7.widget.SwitchCompat
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import pl.kflorczyk.timelapsemaker.R

class DialogSettingsAdapter(context: Context, options: ArrayList<DialogOption>) : ArrayAdapter<DialogOption>(context, 0, options) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: Holder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(R.layout.item_settings_element, parent, false)
            holder = Holder()
            holder.icon = view.findViewById(R.id.icon) as ImageView
            holder.label = view.findViewById(R.id.label) as TextView
            holder.description = view.findViewById(R.id.description) as TextView
            holder.switchCompat = view.findViewById(R.id.switchBtn) as SwitchCompat

            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as Holder
        }

        val item = getItem(position)
        holder.icon!!.setImageResource(item!!.icon)
        holder.label!!.text = item.label
        holder.description!!.text = item.description

        if (item.switchState !== DialogOption.Switch.NOT_EXIST) {
            holder.switchCompat!!.visibility = View.VISIBLE
            holder.switchCompat!!.isChecked = item.switchState === DialogOption.Switch.ENABLED
            holder.switchCompat!!.setOnCheckedChangeListener(item.onSwitchCheckedChange)
        } else {
            holder.switchCompat!!.visibility = View.GONE
        }

        return view
    }

    private inner class Holder {
        var icon: ImageView? = null
        var label: TextView? = null
        var description: TextView? = null
        var switchCompat: SwitchCompat? = null
    }
}