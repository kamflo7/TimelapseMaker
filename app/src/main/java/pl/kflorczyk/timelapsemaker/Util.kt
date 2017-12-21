package pl.kflorczyk.timelapsemaker

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.widget.Toast
import pl.kflorczyk.timelapsemaker.app_settings.SharedPreferencesManager
import pl.kflorczyk.timelapsemaker.camera.*
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseControllerStrategy
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseControllerV1Strategy
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseSettings

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

/**
 * Created by Kamil on 11/13/2016.
 */

object Util {
    val NECESSARY_PERMISSIONS_START_APP = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN)

    fun generateDeviceUUID(): String = "%s_%s".format(android.os.Build.PRODUCT, UUID.randomUUID())

    fun getAvailableCameraAPI():Array<CameraVersionAPI> = if(Build.VERSION.SDK_INT >= 21) arrayOf(CameraVersionAPI.V_1, CameraVersionAPI.V_2) else arrayOf(CameraVersionAPI.V_1)

    fun getTimelapseSettingsFromFile(context:Context):TimelapseSettings {
        var prefsManager = SharedPreferencesManager(context)

        var choosenStorage = prefsManager.getStorageType()
        if(choosenStorage == null) {
            val availableStorages = StorageManager.getStorages(context)
            var lookForSDCard = availableStorages.find { p -> p.second == StorageManager.StorageType.REAL_SDCARD }
            if(lookForSDCard == null) choosenStorage = availableStorages[0].second
            else choosenStorage = lookForSDCard.second
        }

        var settings = TimelapseSettings(
            prefsManager.getPhotosMax() ?: 500,
            prefsManager.getFrequencyCapturing() ?: 3000,
            prefsManager.getResolution(),
            ArrayList<Resolution>(),
            PictureFormat.JPEG,
            prefsManager.getWebEnabled(),
prefsManager.getCameraVersionAPI() ?: CameraVersionAPI.V_1,
            prefsManager.getActiveCamera(),
            prefsManager.getWebAdminPassword(),
            choosenStorage!!
        )

        if(settings.cameraId == null) {
            settings.cameraId = CameraHelper(settings.cameraVersion).getAvailableCameras().find { it -> it.second == LensFacing.BACK }?.first
        }


        settings.availableResolutions = CameraHelper(settings.cameraVersion).getAvailableResolutions(settings.cameraId!!, settings.pictureFormat)
        if(settings.resolution == null && settings.cameraId != null) {
            settings.resolution = settings.availableResolutions.maxBy { resolution -> resolution.height }
        }

        return settings
    }

    fun getTimelapseControllerStrategy(context: Context): TimelapseControllerStrategy {
        return if ((context.applicationContext as MyApplication).timelapseSettings!!.cameraVersion == CameraVersionAPI.V_1) TimelapseControllerV1Strategy() else TimelapseControllerV1Strategy() // todo: temporary
    }

    fun getApplicationVersion(context: Context): String {
        var version = ""

        var pInfo: PackageInfo? = null
        try {
            pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            version = pInfo!!.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return version
    }

    fun log(str: String, tag: String = "test", vararg params: Any) {
        val formated = if (params.isEmpty()) str else String.format(str, *params)

        Log.d(tag, formated)
    }

    fun isMyServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun getLocalIpAddress(useIPv4: Boolean): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(0, delim).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
        }
        // for now eat exceptions
        return null
    }

    fun getBatteryLevel(context: Context): Float {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        // Error checking that probably isn't needed but I added just in case.
        return if (level == -1 || scale == -1) {
            50.0f
        } else level.toFloat() / scale.toFloat() * 100.0f

    }

    fun secondsToTime(seconds: Int): String {
        val minutes = seconds / 60
        val newSeconds = seconds % 60

        return String.format("%02dm:%02ds", minutes, newSeconds)
    }

    fun checkPermissions(permissions: Array<String>, context: Context): Boolean {
        for (permission in permissions) {
            val permissionCheckResult = ContextCompat.checkSelfPermission(context, permission)
            if (permissionCheckResult == PermissionChecker.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

}
