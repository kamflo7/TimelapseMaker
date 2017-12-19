package pl.kflorczyk.timelapsemaker.timelapse

import android.content.Context
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.camera.CameraV1
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseControllerV1Strategy : TimelapseControllerStrategy {
    private var camera: CameraV1? = null
    private lateinit var listener: OnTimelapseStateChangeListener

    override fun startPreview(timelapseSettings: TimelapseSettings, surfaceHolder: SurfaceHolder, context:Context) {
        camera = CameraV1()
        surfaceHolder.setFixedSize(timelapseSettings.resolution!!.width, timelapseSettings.resolution!!.height)

        try {
            camera!!.openForPreview(surfaceHolder,context)
        } catch(e: CameraNotAvailableException) {
            throw e
        }
    }

    override fun stopPreview() {
        camera?.stop()
    }

    override fun startTimelapse(onTimelapseStateChangeListener: OnTimelapseStateChangeListener, context: Context) {
        this.listener = onTimelapseStateChangeListener

        try {
            camera = CameraV1()
            camera!!.openForTimelapse(context)

            this.listener.onInit()
        } catch(e: CameraNotAvailableException) {
            this.listener.onFail("Camera not available")
        }
    }

    override fun stopTimelapse() {
        camera?.stop()
        camera = null
    }

    override fun capturePhoto() {
        camera!!.capturePhoto(object : CameraV1.CameraStateChangeListener {
            override fun onCapture(bytes: ByteArray?) {
                this@TimelapseControllerV1Strategy.listener.onCapture(bytes)
            }

        })
    }

}