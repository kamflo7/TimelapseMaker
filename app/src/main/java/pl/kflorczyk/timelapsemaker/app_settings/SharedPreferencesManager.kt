package pl.kflorczyk.timelapsemaker.app_settings

import android.content.Context
import android.content.SharedPreferences
import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.validators.PasswordValidator

/**
 * Created by Kamil on 2017-12-08.
 */
class SharedPreferencesManager(context:Context) {

    private val PACKAGE = "pl.kflorczyk.timelapsemaker"
    private val FILE_KEY = "$PACKAGE.settingsFile"
    private val KEY_WEB_PASSWORD_GUEST = "$PACKAGE.settingsFile.webpasswordguest"
    private val KEY_WEB_PASSWORD_ADMIN = "$PACKAGE.settingsFile.webpasswordadmin"
    private val KEY_WEB_ENABLED = "$PACKAGE.settingsFile.webenabled"
    private val KEY_CAMERA_API = "$PACKAGE.settingsFile.cameraapi"

    private val context:Context = context
    private val sharedPref:SharedPreferences = context.getSharedPreferences(FILE_KEY, Context.MODE_PRIVATE)

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
            return false;
        }

        sharedPref.edit().putString(KEY_WEB_PASSWORD_ADMIN, password).apply()
        return true
    }

    fun removeWebAdminPassword() {
        sharedPref.edit().remove(KEY_WEB_PASSWORD_ADMIN)
    }

    /**
     * @return passwordExists:Boolean, the password if yes
     */
    fun getWebGuestPassword(): Pair<Boolean, String?> {
        val password:String? = sharedPref.getString(KEY_WEB_PASSWORD_GUEST, null)
        return Pair(password != null, password)
    }

    fun setWebGuestPassword(password: String): Boolean {
        if(!PasswordValidator().validate(password)) {
            return false;
        }

        sharedPref.edit().putString(KEY_WEB_PASSWORD_GUEST, password).apply()
        return true
    }

    fun removeWebGuestPassword() {
        sharedPref.edit().remove(KEY_WEB_PASSWORD_GUEST)
    }

    fun getWebEnabled(): Boolean = sharedPref.getBoolean(KEY_WEB_ENABLED, false)

    fun setWebEnabled(enabled: Boolean) = sharedPref.edit().putBoolean(KEY_WEB_ENABLED, enabled).apply()

}