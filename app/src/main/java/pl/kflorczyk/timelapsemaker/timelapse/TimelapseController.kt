package pl.kflorczyk.timelapsemaker.timelapse

import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
object TimelapseController {

    private var timelapseControllerStrategy: TimelapseControllerStrategy? = null

    private var isPreviewing: Boolean = false

    fun getStrategy(): TimelapseControllerStrategy? = timelapseControllerStrategy

    fun setStrategy(strategy: TimelapseControllerStrategy) {
        timelapseControllerStrategy = strategy
    }


    fun startTimelapse() {
        if(timelapseControllerStrategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

        timelapseControllerStrategy?.startTimelapse()
    }

    fun startPreviewing(settings: TimelapseSettings, surfaceHolder: SurfaceHolder) {
        if(timelapseControllerStrategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

        try {
            timelapseControllerStrategy?.startPreview(settings, surfaceHolder)
        } catch(e: CameraNotAvailableException) {
            throw e
        }

        isPreviewing = true
    }

    fun isPreviewing():Boolean = isPreviewing

    fun stopPreview() {
        timelapseControllerStrategy?.stopPreview()
    }
}