package pl.kflorczyk.timelapsemaker.timelapse

import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseController(strategy: TimelapseControllerStrategy) {

    private val timelapseControllerStrategy: TimelapseControllerStrategy = strategy

    fun startTimelapse() {
        timelapseControllerStrategy.startTimelapse()
    }

    fun startPreviewing(settings: TimelapseSettings, surfaceHolder: SurfaceHolder) {
        try {
            timelapseControllerStrategy.startPreview(settings, surfaceHolder)
        } catch(e: CameraNotAvailableException) {
            throw e
        }
    }
}