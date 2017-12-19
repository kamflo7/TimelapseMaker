package pl.kflorczyk.timelapsemaker.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.Camera
import android.util.Base64
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.MyApplication
import pl.kflorczyk.timelapsemaker.StorageManager
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Created by Kamil on 2017-12-09.
 */
class CameraV1 {

    private var camera: Camera? = null
    private lateinit var dummySurface: SurfaceTexture

    fun openForPreview(surfaceHolder: SurfaceHolder, context: Context) {
        camera = getCameraInstance() ?: throw CameraNotAvailableException("")

        camera!!.setPreviewDisplay(surfaceHolder)
        camera!!.startPreview()
    }

    fun openForTimelapse(context: Context) {
        camera = getCameraInstance() ?: throw CameraNotAvailableException("")

        try {
            dummySurface = SurfaceTexture(10)
            camera!!.setPreviewTexture(dummySurface)

            val timelapseSettings = (context.applicationContext as MyApplication).timelapseSettings
            val params = camera!!.parameters
            params.setPictureSize(timelapseSettings!!.resolution!!.width, timelapseSettings!!.resolution!!.height)

            camera!!.parameters = params
        } catch(e: IOException) {
            throw CameraNotAvailableException(e.message ?: "IOException")
        }
    }

    fun capturePhoto(listener: CameraStateChangeListener) {
        camera!!.startPreview()
        camera!!.takePicture(null, null, object : Camera.PictureCallback {
            override fun onPictureTaken(bytes: ByteArray?, camera: Camera?) {
                listener.onCapture(bytes)
            }
        })
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

    interface CameraStateChangeListener {
        fun onCapture(bytes: ByteArray?)
    }
}