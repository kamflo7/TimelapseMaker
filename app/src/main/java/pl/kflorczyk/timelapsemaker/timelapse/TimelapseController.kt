package pl.kflorczyk.timelapsemaker.timelapse

import android.content.Context
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import android.os.PowerManager
import pl.kflorczyk.timelapsemaker.Util


/**
 * Created by Kamil on 2017-12-09.
 */
object TimelapseController {

    private var strategy: TimelapseControllerStrategy? = null
    private var settings: TimelapseSettings? = null

    private var state:State = State.NOTHING
    private var listenerOutside: OnTimelapseProgressListener? = null

    private var capturedPhotos: Int = 0
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var timeAtStartCapturingPhoto: Long = 0
    private var timeAtWillBeNextCapture: Long = 0

    fun getCapturedPhotos(): Int = capturedPhotos
    fun getTimeToNextCapture(): Long {
        var timeLeft = timeAtWillBeNextCapture - System.currentTimeMillis()
        if(timeLeft < 0) timeLeft = 0
        return timeLeft
    }

    fun build(strategy: TimelapseControllerStrategy, settings: TimelapseSettings) {
        this.strategy = strategy
        this.settings = settings
    }

    fun startTimelapse(onTimelapseProgressListener: OnTimelapseProgressListener, context: Context) {
        if(strategy == null || settings == null) throw RuntimeException("TimelapseControllerStrategy is not prepared correctly (Did you invoke build() method?)")

        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimelapseController_WakeLock")
        wakeLock!!.acquire()

        capturedPhotos = 0

        this.listenerOutside = onTimelapseProgressListener
        strategy!!.startTimelapse(object : OnTimelapseStateChangeListener {
            override fun onInit() {
                this@TimelapseController.state = State.TIMELAPSE
                timeAtStartCapturingPhoto = System.currentTimeMillis()
                strategy!!.capturePhoto()
            }

            override fun onCapture(bytes: ByteArray?) {
                capturedPhotos++
                this@TimelapseController.listenerOutside!!.onCapture(bytes)

                var delayToNextCapture = settings!!.frequencyCapturing - (System.currentTimeMillis() - timeAtStartCapturingPhoto)
                if(delayToNextCapture < 0)
                    delayToNextCapture = 0

                timeAtWillBeNextCapture = System.currentTimeMillis() + delayToNextCapture
                Util.log("onCapture, now we are waiting precisely ${delayToNextCapture}ms to next capture")
                Thread.sleep(delayToNextCapture) // todo: Handle interrupted exception and invoke onFail()

                timeAtStartCapturingPhoto = System.currentTimeMillis()
                strategy!!.capturePhoto()
            }

            override fun onFail(msg: String) {
                this@TimelapseController.listenerOutside!!.onFail(msg)
                this@TimelapseController.stopTimelapse()
            }

            override fun onComplete() {
                this@TimelapseController.listenerOutside!!.onComplete()
                this@TimelapseController.stopTimelapse()
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
        strategy?.stopTimelapse()
        wakeLock?.release()
    }

    enum class State {
        NOTHING,
        PREVIEW,
        TIMELAPSE
    }

    interface OnTimelapseProgressListener {
        fun onCapture(bytes: ByteArray?)
        fun onFail(msg: String?)
        fun onComplete()
    }
}