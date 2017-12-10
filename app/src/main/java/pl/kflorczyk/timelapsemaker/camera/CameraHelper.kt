package pl.kflorczyk.timelapsemaker.camera

/**
 * Created by Kamil on 2017-12-10.
 */
class CameraHelper(version: CameraVersionAPI) {

    private val version: CameraVersionAPI = version

    fun getAvailableCameras(): List<Pair<String, LensFacing>>
        = if (version == CameraVersionAPI.V_1) CameraV1().getAvailableCameras() else CameraV2().getAvailableCameras()

    fun getAvailableResolutions(cameraid: String, format: PictureFormat)
        = if (version == CameraVersionAPI.V_1) CameraV1().getResolutions() else CameraV2().getResolutions(format)
}