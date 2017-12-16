package pl.kflorczyk.timelapsemaker.timelapse

import android.content.Context
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import android.os.PowerManager



/**
 * Created by Kamil on 2017-12-09.
 */
object TimelapseController {

    private var strategy: TimelapseControllerStrategy? = null
    private var settings: TimelapseSettings? = null

    private var state:State = State.NOTHING
    private var listenerOutside: OnTimelapseStateChangeListener? = null

    private var capturedPhotos: Int = 0
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock


    fun getCapturedPhotos(): Int = capturedPhotos
    fun getTimeToNextCapture(): Long {
        return 500 // todo: implement this
    }

    fun build(strategy: TimelapseControllerStrategy, settings: TimelapseSettings) {
        this.strategy = strategy
        this.settings = settings
    }

    fun startTimelapse(onTimelapseStateChangeListener: OnTimelapseStateChangeListener, context: Context) {
        if(strategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimelapseController_WakeLock")
        wakeLock.acquire()

        capturedPhotos = 0

        this.listenerOutside = onTimelapseStateChangeListener
        strategy?.startTimelapse(object : OnTimelapseStateChangeListener {
            override fun onInit() {
                this@TimelapseController.state = State.TIMELAPSE
                this@TimelapseController.listenerOutside!!.onInit()
            }

            override fun onCapture(bytes: ByteArray?) {
                capturedPhotos++
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

    fun stopTimelapse() {

    }

    enum class State {
        NOTHING,
        PREVIEW,
        TIMELAPSE
    }
}