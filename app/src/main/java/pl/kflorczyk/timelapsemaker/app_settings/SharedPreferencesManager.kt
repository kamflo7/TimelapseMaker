package pl.kflorczyk.timelapsemaker.app_settings

import android.content.Context
import android.content.SharedPreferences
import pl.kflorczyk.timelapsemaker.StorageManager
import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.camera.Resolution
import pl.kflorczyk.timelapsemaker.validators.PasswordValidator
import java.util.regex.Pattern

class SharedPreferencesManager(context:Context) {

    private val PACKAGE = "pl.kflorczyk.timelapsemaker"
    private val FILE_KEY = "$PACKAGE.settingsFile"
    private val KEY_WEB_PASSWORD_ADMIN = "$PACKAGE.settingsFile.webpasswordadmin"
    private val KEY_WEB_ENABLED = "$PACKAGE.settingsFile.webenabled"
    private val KEY_CAMERA_API = "$PACKAGE.settingsFile.cameraapi"
    private val KEY_CAMERA_PHOTOSMAX = "$PACKAGE.settingsFile.cameraapi.photosmax"
    private val KEY_CAMERA_RESOLUTION = "$PACKAGE.settingsFile.cameraapi.resolution"
    private val KEY_CAMERA_FREQUENCY = "$PACKAGE.settingsFile.cameraapi.frequency"
    private val KEY_CAMERA_ACTIVEID = "$PACKAGE.settingsFile.cameraapi.activeid"
    private val KEY_STORAGE_TYPE = "$PACKAGE.settingsFile.storagetype"

    private val context:Context = context
    private val sharedPref:SharedPreferences = context.getSharedPreferences(FILE_KEY, Context.MODE_PRIVATE)

    fun setStorageType(type:StorageManager.StorageType) {
        sharedPref.edit().putInt(KEY_STORAGE_TYPE, type.ordinal).apply()
    }

    fun getStorageType():StorageManager.StorageType? {
        val value = sharedPref.getInt(KEY_STORAGE_TYPE, -1)
        val finalValue: StorageManager.StorageType?
        when(value) {
            0 -> finalValue = StorageManager.StorageType.EXTERNAL_EMULATED
            1 -> finalValue = StorageManager.StorageType.REAL_SDCARD
            else -> finalValue = null
        }
        return finalValue
    }

    fun getActiveCamera(): String? {
        val cameraID = sharedPref.getString(KEY_CAMERA_ACTIVEID, "null")
        return if (cameraID.equals("null")) null else cameraID
    }

    fun setActiveCamera(id: String) = sharedPref.edit().putString(KEY_CAMERA_ACTIVEID, id).apply()

    fun getFrequencyCapturing():Long? {
        val frequency: Long = sharedPref.getLong(KEY_CAMERA_FREQUENCY, -1)
        return if (frequency == -1L) null else frequency
    }

    fun setFrequencyCapturing(frequency: Long) = sharedPref.edit().putLong(KEY_CAMERA_FREQUENCY, frequency).apply()

    fun getResolution(): Resolution? {
        val resolutionStr: String = sharedPref.getString(KEY_CAMERA_RESOLUTION, "null")
        if(resolutionStr.equals("null")) {
            return null
        }

        val parts: List<String> = resolutionStr.split("x")
        if(parts.size == 2) {
            return Resolution(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]))
        }

        return null
    }

    fun setResolution(resolution: Resolution) {
        sharedPref.edit().putString(KEY_CAMERA_RESOLUTION, "${resolution.width}x${resolution.height}").apply()
    }

    fun getPhotosMax():Int? {
        val amount:Int = sharedPref.getInt(KEY_CAMERA_PHOTOSMAX, -1)
        return if (amount != -1) amount else null
    }

    fun setPhotosMax(amount:Int) {
        sharedPref.edit().putInt(KEY_CAMERA_PHOTOSMAX, amount).apply()
    }

    fun getCameraVersionAPI(): CameraVersionAPI? {
        val value:Int = sharedPref.getInt(KEY_CAMERA_API, -1)

        var cameraVersion: CameraVersionAPI? = null
        when (value) {
            -1 -> cameraVersion = null
            1 -> cameraVersion = CameraVersionAPI.V_1
            2 -> cameraVersion = CameraVersionAPI.V_2
        }
        return cameraVersion
    }

    fun setCameraVersionAPI(version: CameraVersionAPI) {
        throw NotImplementedError("Not implemented method yet")
    }

    /**
     * @return passwordExists:Boolean, the password if yes
     */
    fun getWebAdminPassword(): Pair<Boolean, String?> {
        val password:String? = sharedPref.getString(KEY_WEB_PASSWORD_ADMIN, null)
        return Pair(password != null, password)
    }

    fun setWebAdminPassword(password: String): Boolean {
        if(!PasswordValidator().validate(password)) {
            return false
        }

        sharedPref.edit().putString(KEY_WEB_PASSWORD_ADMIN, password).apply()
        return true
    }

    fun removeWebAdminPassword() {
        sharedPref.edit().remove(KEY_WEB_PASSWORD_ADMIN)
    }

    fun getWebEnabled(): Boolean = sharedPref.getBoolean(KEY_WEB_ENABLED, false)

    fun setWebEnabled(enabled: Boolean) = sharedPref.edit().putBoolean(KEY_WEB_ENABLED, enabled).apply()

}