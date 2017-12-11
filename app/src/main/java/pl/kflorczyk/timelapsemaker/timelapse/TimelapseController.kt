package pl.kflorczyk.timelapsemaker.timelapse

import android.content.Context
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException

/**
 * Created by Kamil on 2017-12-09.
 */
object TimelapseController {

    private var strategy: TimelapseControllerStrategy? = null
    private var settings: TimelapseSettings? = null

    private var state:State = State.NOTHING
    private var listenerOutside: OnTimelapseStateChangeListener? = null

    fun build(strategy: TimelapseControllerStrategy, settings: TimelapseSettings) {
        this.strategy = strategy
        this.settings = settings
    }

    fun startTimelapse(onTimelapseStateChangeListener: OnTimelapseStateChangeListener, context: Context) {
        if(strategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

        this.listenerOutside = onTimelapseStateChangeListener
        strategy?.startTimelapse(object : OnTimelapseStateChangeListener {
            override fun onInit() {
                this@TimelapseController.state = State.TIMELAPSE
                this@TimelapseController.listenerOutside!!.onInit()
            }

            override fun onCapture(bytes: ByteArray?) {
                this@TimelapseController.listenerOutside!!.onCapture(bytes)
            }

            override fun onFail(msg: String) {
                this@TimelapseController.listenerOutside!!.onFail(msg)
            }
        }, context)
    }

    fun capturePhoto() {
        if(state != State.TIMELAPSE)
            return

        strategy?.capturePhoto()
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