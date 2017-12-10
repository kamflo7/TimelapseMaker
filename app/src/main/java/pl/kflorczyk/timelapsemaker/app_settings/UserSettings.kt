//package pl.kflorczyk.timelapsemaker.app_settings
//
//import android.content.Context
//import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
//import pl.kflorczyk.timelapsemaker.camera.Resolution
//
///**
// * Created by Kamil on 2017-12-08.
// */
//class UserSettings(context: Context) {
//    private val context: Context = context
//    private val prefsManager: SharedPreferencesManager = SharedPreferencesManager(context)
//
//    fun getActiveCamera(): String? = prefsManager.getActiveCamera()
//
//    fun setActiveCamera(id: String) = prefsManager.setActiveCamera(id)
//
//    fun getFrequencyCapturing():Long = prefsManager.getFrequencyCapturing() ?: 3000
//
//    fun setFrequencyCapturing(frequency: Long) = prefsManager.setFrequencyCapturing(frequency)
//
//    fun getResolution():Resolution? = prefsManager.getResolution()
//
//    fun setResolution(resolution: Resolution) = prefsManager.setResolution(resolution)
//
//    fun getPhotosMax():Int {
//        return prefsManager.getPhotosMax() ?: 500
//    }
//
//    fun setPhotosMax(amount:Int) = prefsManager.setPhotosMax(amount)
//
//    fun getCameraVersionAPI(): CameraVersionAPI = prefsManager.getCameraVersionAPI() ?: CameraVersionAPI.V_1
//
//    fun setCameraVersionAPI(version: CameraVersionAPI) = prefsManager.setCameraVersionAPI(version)
//
//    fun getWebAdminPassword(): Pair<Boolean, String?> = prefsManager.getWebAdminPassword()
//
//    fun setWebAdminPassword(password: String): Boolean = prefsManager.setWebAdminPassword(password)
//
//    fun removeWebAdminPassword() = prefsManager.removeWebAdminPassword()
//
//    fun getWebGuestPassword(): Pair<Boolean, String?> = prefsManager.getWebGuestPassword()
//
//    fun setWebGuestPassword(password: String): Boolean = prefsManager.setWebGuestPassword(password)
//
//    fun removeWebGuestPassword() = prefsManager.removeWebGuestPassword()
//
//    fun getWebEnabled(): Boolean = getWebEnabled()
//
//    fun setWebEnabled(enabled: Boolean) = prefsManager.setWebEnabled(enabled)
//}