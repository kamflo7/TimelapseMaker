package pl.kflorczyk.timelapsemaker.camera

import android.hardware.Camera
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
class CameraV1 {

    private var camera: Camera? = null

    fun prepare() {

    }

    fun capturePhoto() {

    }

    fun openForPreview(surfaceHolder: SurfaceHolder) {
        camera = getCameraInstance() ?: throw CameraNotAvailableException("")

        camera!!.setPreviewDisplay(surfaceHolder)
        camera!!.startPreview()
    }

    fun stop() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    fun getResolutions(): List<Resolution> {
        val camera: Camera = getCameraInstance() ?: throw CameraNotAvailableException("")
        val camerasIds = camera.parameters.supportedPictureSizes.map { it -> Resolution(it.width, it.height) }.toList()
        camera.release()
        return camerasIds
    }

    /**
     * @return array of elements consisting of camera id and facing property
     */
    fun getAvailableCameras(): List<Pair<String, LensFacing>> {
        val numberOfCameras = Camera.getNumberOfCameras()
        val cameras = ArrayList<Pair<String, LensFacing>>()

        for(i in 0..numberOfCameras-1) {
            var info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            cameras.add(Pair(i.toString(), if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) LensFacing.BACK else LensFacing.FRONT))
        }
        return cameras
    }

    private fun getCameraInstance(): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open()
        } catch (e: Exception) {
        }

        return c
    }
}