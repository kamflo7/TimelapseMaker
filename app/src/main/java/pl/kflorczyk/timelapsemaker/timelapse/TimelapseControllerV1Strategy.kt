package pl.kflorczyk.timelapsemaker.timelapse

import android.hardware.Camera
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.camera.CameraV1
import pl.kflorczyk.timelapsemaker.camera.LensFacing
import pl.kflorczyk.timelapsemaker.camera.Resolution
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseControllerV1Strategy : TimelapseControllerStrategy {
    override fun startPreview(timelapseSettings: TimelapseSettings, surfaceHolder: SurfaceHolder) {
        val camera = CameraV1()

        surfaceHolder.setFixedSize(timelapseSettings.resolution!!.width, timelapseSettings.resolution!!.height)

        try {
            camera.openForPreview(surfaceHolder)
        } catch(e: CameraNotAvailableException) {
            Util.log("TimelapseControllerV1Strategy>startPreview>CameraNotAvailableException")
            throw e
        }

    }


    override fun startTimelapse() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}