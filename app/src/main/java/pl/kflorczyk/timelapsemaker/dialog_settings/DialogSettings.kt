package pl.kflorczyk.timelapsemaker.dialog_settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.Window
import android.widget.*
import pl.kflorczyk.timelapsemaker.MyApplication
import pl.kflorczyk.timelapsemaker.R
import pl.kflorczyk.timelapsemaker.StorageManager
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.app_settings.SharedPreferencesManager
import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.camera.Resolution
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseSettings
import java.util.*

class DialogSettings(context: Context, fab: FloatingActionButton, onDialogSettingChangeListener: OnDialogSettingChangeListener) {
    val context: Context
    val fab: FloatingActionButton
    val timelapseSettings: TimelapseSettings
    var onDialogSettingChangeListener: OnDialogSettingChangeListener? = null

    init {
        this.context = context
        this.fab = fab
        this.timelapseSettings = ((context as Activity).application as MyApplication).timelapseSettings!!
        this.onDialogSettingChangeListener = onDialogSettingChangeListener
    }



    fun show() {
        val dialogView = View.inflate(context, R.layout.dialog_settings, null)
        val dialog = Dialog(context, R.style.MyAlertDialogStyle)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.show()

        val listView = dialogView.findViewById(R.id.optionsList) as ListView
        setListViewClickListenerForBasicCategory(listView)


        dialog.setOnShowListener {
            revealShow(dialogView, true, null)
        }

        dialog.setOnKeyListener(DialogInterface.OnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_BACK){
                revealShow(dialogView, false, dialog)
                return@OnKeyListener true
            }
            false
        });

        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun getAspectRatioString(width: Int, height: Int): String {
        val aspectRatio = width.div(height.toFloat())
        if(aspectRatio > 1.7f && aspectRatio < 1.8f) {
            return "16:9"
        }
        return "4:3"
    }

    fun getPhotoResolutionDescription(): String {
        if (timelapseSettings.resolution != null) {
            val ratio = getAspectRatioString(timelapseSettings.resolution!!.width, timelapseSettings.resolution!!.height)
            return "${timelapseSettings.resolution!!.width}x${timelapseSettings.resolution!!.height} ($ratio)"
        }
        return ""
    }

    fun getIntervalDescription(): String = "%.1fs".format(timelapseSettings.frequencyCapturing.div(1000f))
    fun getPhotosLimitDescription(): String = "${timelapseSettings.photosMax}"
    fun getCameraApiDescription(): String = if(timelapseSettings.cameraVersion == CameraVersionAPI.V_1) "v1" else "v2"
    fun getStorageDescription(): String = if(timelapseSettings.storageType == StorageManager.StorageType.EXTERNAL_EMULATED) "External Emulated" else "Physic SD Card"

    private fun setListViewClickListenerForBasicCategory(listView: ListView) {
        val options = ArrayList<DialogOption>()
        options.add(DialogOption(R.drawable.ic_photo_size_select, "Photo resolution", getPhotoResolutionDescription()));
        options.add(DialogOption(R.drawable.ic_interval, "Interval", getIntervalDescription()))
        options.add(DialogOption(R.drawable.ic_amount, "Limit", getPhotosLimitDescription()))
        options.add(DialogOption(R.drawable.ic_camera, "Camera API", getCameraApiDescription()))
        options.add(DialogOption(R.drawable.ic_remote, "WebAccess", "Access your timelapse progress through a website",
                if (timelapseSettings.webEnabled) DialogOption.Switch.ENABLED else DialogOption.Switch.DISABLED,
                CompoundButton.OnCheckedChangeListener { _, b ->
                    Toast.makeText(this@DialogSettings.context, if (b) R.string.text_dialog_webserver_toggleOn else R.string.text_dialog_webserver_toggleOff, Toast.LENGTH_LONG).show()

                    timelapseSettings.webEnabled = b

                    val sharedPreferencesManager = SharedPreferencesManager(this@DialogSettings.context)
                    sharedPreferencesManager.setWebEnabled(b)
                    onDialogSettingChangeListener?.onToggleWebServer(b)
                }))
        options.add(DialogOption(R.drawable.ic_sd_storage, "Storage", getStorageDescription()))
        options.add(DialogOption(R.drawable.ic_help, "Info", "Informations about application"))


        val adapter = DialogSettingsAdapter(this@DialogSettings.context, options)
        listView.adapter = adapter

        var prefs = SharedPreferencesManager(this@DialogSettings.context)

        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
            when (position) {
                0 -> {
                    var resOptions = Array<String>(timelapseSettings.availableResolutions.size, { i -> "$i"})

                    var i = 0
                    for(r in timelapseSettings.availableResolutions) {
                        val ratio = getAspectRatioString(r.width, r.height)
                        resOptions[i++] = "${r.width}x${r.height} ($ratio)"
                    }

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle(R.string.dialog_choose_resolution)
                            .setItems(resOptions, DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()

                                var selectedResolution = timelapseSettings.availableResolutions[which]

                                prefs.setResolution(selectedResolution)
                                timelapseSettings.resolution = selectedResolution

                                options[0].description = getPhotoResolutionDescription()
                                adapter.notifyDataSetChanged()
                                this@DialogSettings.onDialogSettingChangeListener?.onChangePhotoResolution(selectedResolution)
                            }).create().show()
                }
                1 -> {
                    val numberPicker = NumberPicker(this@DialogSettings.context)
                    numberPicker.minValue = 500
                    numberPicker.maxValue = 10 * 1000
                    numberPicker.value = timelapseSettings.frequencyCapturing.toInt()

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle(R.string.dialog_choose_interval)
                            .setView(numberPicker)
                            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                                timelapseSettings.frequencyCapturing = numberPicker.value.toLong()

                                prefs.setFrequencyCapturing(numberPicker.value.toLong())

                                options[1].description = getIntervalDescription()
                                adapter.notifyDataSetChanged()
                                this@DialogSettings.onDialogSettingChangeListener?.onChangeInterval(numberPicker.value)
                            })
                            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                            }).create().show()
                }
                2 -> {
                    val numberPicker = NumberPicker(this@DialogSettings.context)
                    numberPicker.minValue = 3
                    numberPicker.maxValue = 10000
                    numberPicker.value = timelapseSettings.photosMax

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle(R.string.dialog_choose_amount_photos)
                            .setView(numberPicker)
                            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                                timelapseSettings.photosMax = numberPicker.value

                                prefs.setPhotosMax(numberPicker.value)

                                options[2].description = getPhotosLimitDescription()
                                adapter.notifyDataSetChanged()
                                this@DialogSettings.onDialogSettingChangeListener?.onChangePhotosLimit(numberPicker.value)
                            })
                            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                            }).create().show()
                }
                3 -> {
                    val items = if(Build.VERSION.SDK_INT >= 21) arrayOf("API 1", "API 2") else arrayOf("API 1")

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle(R.string.dialog_choose_camera_api)
                            .setSingleChoiceItems(items,
                                    if(timelapseSettings.cameraVersion == CameraVersionAPI.V_1) 0 else 1,
                                    DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                val currentCameraSelectedIndex = if(timelapseSettings.cameraVersion == CameraVersionAPI.V_1) 0 else 1
                                if(which != currentCameraSelectedIndex) {
                                    var selected = if(which == 0) CameraVersionAPI.V_1 else CameraVersionAPI.V_2

                                    timelapseSettings.cameraVersion = selected
                                    prefs.setCameraVersionAPI(selected)

                                    options[3].description = getCameraApiDescription()

                                    this@DialogSettings.onDialogSettingChangeListener?.onCameraApiChange(selected)
                                    adapter.notifyDataSetChanged()
                                }
                            }).create().show()
                }
                4 -> {
                    val input = EditText(this@DialogSettings.context)
                    input.inputType = InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD

                    var strPass: String = if(timelapseSettings.adminPassword.first) timelapseSettings.adminPassword.second!! else ""
                    input.text = Editable.Factory.getInstance().newEditable(strPass)

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle(R.string.dialog_set_password)
                            .setView(input)
                            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                val newPassword = input.text.toString()

                                if(newPassword.length == 0) {
                                    prefs.removeWebAdminPassword()
                                } else {
                                    // todo: make hash, basic auth or JWT
                                    prefs.setWebAdminPassword(newPassword)
                                }
                                Toast.makeText(this@DialogSettings.context, R.string.text_dialog_change_password_success, Toast.LENGTH_LONG).show()
                            })
                            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                            }).create().show()
                }
                5 -> {
                    val storages = StorageManager.getStorages(context)
                    val currentStorageType = prefs.getStorageType()
                    var resOptions = Array<String>(storages.size, { i-> "$i" })

                    var i = 0
                    for(r in storages) {
                        resOptions[i++] = if(r.second == StorageManager.StorageType.EXTERNAL_EMULATED)
                            "External Emulated (%s)".format(r.first.absolutePath)
                        else
                            "Real SDCard (%s)".format(r.first.absolutePath)
                    }

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle(R.string.dialog_set_storage)
                            .setItems(resOptions, DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()

                                var selectedStorage = storages[which]

                                if(currentStorageType == null || (selectedStorage.second != currentStorageType)) {
                                    prefs.setStorageType(selectedStorage.second)
                                    timelapseSettings.storageType = selectedStorage.second

                                    options[5].description = getStorageDescription()
                                    adapter.notifyDataSetChanged()
                                    this@DialogSettings.onDialogSettingChangeListener?.onStorageTypeChange(selectedStorage.second)
                                }
                            }).create().show()
                }
                6 -> {
                    var msg = StringBuilder()
                    msg.append("Simple app to shoot timelapses")
                    msg.append("\n\nVersion: " + Util.getApplicationVersion(this@DialogSettings.context))
                    msg.append("\nAuthor: florczykkamil@gmail.com")
                    msg.append("\nGithub: https://github.com/kflorczyk/TimelapseMaker")

                    AlertDialog.Builder(this@DialogSettings.context)
                            .setTitle("Info")
                            .setMessage(msg.toString())
                            .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
                                dialogInterface.dismiss()
                            }).create().show()
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun revealShow(dialogView: View, b: Boolean, dialog: Dialog?) {
        val view = dialogView.findViewById(R.id.dialog)

        val w = view.width
        val h = view.height

        val endRadius = Math.hypot(w.toDouble(), h.toDouble()).toInt()

        val cx = (fab.x + (fab.width/2)).toInt()
        val cy = ((fab.y)+ fab.height + 56).toInt()

        if(b){
            var revealAnimator = ViewAnimationUtils.createCircularReveal(view, cx,cy, 0f, endRadius.toFloat())

            view.visibility = View.VISIBLE
            revealAnimator.duration = 700
            revealAnimator.start()

        } else {
            var anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, endRadius.toFloat(), 0f)

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dialog?.dismiss()
                    view.visibility = View.INVISIBLE
                    this@DialogSettings.onDialogSettingChangeListener?.onDialogExit()
                }
            })
            anim.duration = 700
            anim.start()
        }

    }

    interface OnDialogSettingChangeListener {
        fun onChangePhotoResolution(resolution: Resolution)
        fun onChangeInterval(intervalMiliseconds: Int)
        fun onChangePhotosLimit(amount: Int)
        fun onToggleWebServer(toggle: Boolean)
        fun onCameraApiChange(cameraVersion: CameraVersionAPI)
        fun onStorageTypeChange(storageType: StorageManager.StorageType)

        fun onDialogExit()
    }

}