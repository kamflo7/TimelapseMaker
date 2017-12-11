package pl.kflorczyk.timelapsemaker.timelapse

import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
object TimelapseController {

    private var strategy: TimelapseControllerStrategy? = null
    private var settings: TimelapseSettings? = null

    private var state:State = State.NOTHING

    fun build(strategy: TimelapseControllerStrategy, settings: TimelapseSettings) {
        this.strategy = strategy
        this.settings = settings
    }

    fun startTimelapse() {
        if(strategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

//        strategy?.startTimelapse()
    }

    fun startPreviewing(settings: TimelapseSettings, surfaceHolder: SurfaceHolder) {
        if(strategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

        try {
            strategy?.startPreview(settings, surfaceHolder)
        } catch(e: CameraNotAvailableException) {
            throw e
        }

        state = State.PREVIEW
    }

    fun getState():State = state

    fun stopPreview() {
        strategy?.stopPreview()
    }

    enum class State {
        NOTHING,
        PREVIEW,
        TIMELAPSE
    }
}