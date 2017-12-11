package pl.kflorczyk.timelapsemaker.timelapse

import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.camera.CameraV1
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseControllerV1Strategy : TimelapseControllerStrategy {
    private var camera: CameraV1? = null
    private var onTimelapseStateChangeListener: OnTimelapseStateChangeListener? = null

    override fun startPreview(timelapseSettings: TimelapseSettings, surfaceHolder: SurfaceHolder) {
        camera = CameraV1()
        surfaceHolder.setFixedSize(timelapseSettings.resolution!!.width, timelapseSettings.resolution!!.height)

        try {
            camera!!.openForPreview(surfaceHolder)
        } catch(e: CameraNotAvailableException) {
            throw e
        }
    }

    override fun stopPreview() {
        camera?.stop()
    }

    override fun startTimelapse(onTimelapseStateChangeListener: OnTimelapseStateChangeListener) {
        this.onTimelapseStateChangeListener = onTimelapseStateChangeListener
    }
}