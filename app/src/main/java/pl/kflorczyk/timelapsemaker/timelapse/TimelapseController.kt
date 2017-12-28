package pl.kflorczyk.timelapsemaker.timelapse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.SurfaceHolder
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import android.os.PowerManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import pl.kflorczyk.timelapsemaker.MainActivity
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.Util.log
import pl.kflorczyk.timelapsemaker.bluetooth.BluetoothClientService
import pl.kflorczyk.timelapsemaker.bluetooth.BluetoothServerService

/**
 * Created by Kamil on 2017-12-09.
 */
object TimelapseController {
    private val TAG = "TimelapseController"

    private var strategy: TimelapseControllerStrategy? = null
    private var settings: TimelapseSettings? = null

    private var context: Pair<Context, Any?>? = null

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

    enum class BluetoothMode {
        DISABLED,
        SERVER,
        CLIENT
    }

    var currentBtMode: BluetoothMode = BluetoothMode.DISABLED

    // special flags for server use
    private var isServerCapturingPhoto: Boolean = false
    private var clientsDoneCapturing: Boolean = true

    private var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra(MainActivity.BROADCAST_MSG)

            log(TAG, "[MessageReceiver][btMode: $currentBtMode] MSG: $msg")

            if(currentBtMode == BluetoothMode.SERVER) {
                when(msg) {
                    BluetoothServerService.BT_SERVER_CLIENTS_READY -> {
                        log(TAG, "[MessageReceiver][btMode: $currentBtMode] BT_SERVER_CLIENTS_READY")
                        timeAtStartCapturingPhoto = System.currentTimeMillis()
                        isServerCapturingPhoto = true
                        clientsDoneCapturing = false
                        strategy!!.capturePhoto()
                        Util.broadcastMessage(this@TimelapseController.context!!.first, BluetoothServerService.BT_SERVER_DO_CAPTURE)
                    }
                    BluetoothServerService.BT_SERVER_CAPTURE_PHOTO_COMPLETE -> {
                        log(TAG, "[MessageReceiver][btMode: $currentBtMode] BT_SERVER_CAPTURE_PHOTO_COMPLETE; All clients captured single photo -> Can capture next..")
                        clientsDoneCapturing = true
                        if(!isServerCapturingPhoto) {
                            scheduleServerToCaptureNextPhoto()
                        }
                    }
                }
            } else if(currentBtMode == BluetoothMode.CLIENT) {
                when(msg) {
                    BluetoothClientService.BT_CLIENT_DO_CAPTURE -> {
                        log(TAG, "[MessageReceiver][btMode: $currentBtMode] BT_CLIENT_DO_CAPTURE -> Capture photo right now")
                        timeAtStartCapturingPhoto = System.currentTimeMillis()
                        strategy!!.capturePhoto()
                    }
                }
            }
        }
    }

    fun scheduleServerToCaptureNextPhoto() {
        if(capturedPhotos == settings!!.photosMax) {
            this@TimelapseController.stopTimelapse()
            this@TimelapseController.listenerOutside!!.onComplete()
            //todo: send BT signal to end
            return
        }

        var delayToNextCapture = settings!!.frequencyCapturing - (System.currentTimeMillis() - timeAtStartCapturingPhoto)
        if(delayToNextCapture < 0)
            delayToNextCapture = 0

        timeAtWillBeNextCapture = System.currentTimeMillis() + delayToNextCapture
        log(TAG, "scheduleServerToCaptureNextPhoto() -> onCapture, now we are waiting precisely ${delayToNextCapture}ms to next capture")
        try {
            Thread.sleep(delayToNextCapture)
        } catch(e: InterruptedException) {
            log(TAG, "scheduleServerToCaptureNextPhoto() -> Thread.sleep() InterruptedException")
            e.printStackTrace()
            this@TimelapseController.stopTimelapse()
            this@TimelapseController.listenerOutside!!.onFail(e.message)
            //todo: HANDLE BLUETOOTH
        }
        timeAtStartCapturingPhoto = System.currentTimeMillis()
        isServerCapturingPhoto = true
        clientsDoneCapturing = false
        strategy!!.capturePhoto()
        Util.broadcastMessage(this@TimelapseController.context!!.first, BluetoothServerService.BT_SERVER_DO_CAPTURE)
    }

    fun startTimelapse(onTimelapseProgressListener: OnTimelapseProgressListener, context: Context, mode: BluetoothMode = BluetoothMode.DISABLED) {
        if(strategy == null || settings == null) throw RuntimeException("TimelapseControllerStrategy is not prepared correctly (Did you invoke build() method?)")
        if(mode != BluetoothMode.DISABLED) {
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, IntentFilter(MainActivity.BROADCAST_FILTER))
        }
        currentBtMode = mode
        log(TAG, "startTimelapse() with btMode: $mode")

        this.context = Pair(context, null)

        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimelapseController_WakeLock")
        wakeLock!!.acquire()

        capturedPhotos = 0

        this.listenerOutside = onTimelapseProgressListener
        stopPreview()

        when(mode) {
            BluetoothMode.DISABLED -> strategy!!.startTimelapse(btDisabled_onTimelapseStateChangeListener, context)
            BluetoothMode.SERVER -> strategy!!.startTimelapse(btServer_onTimelapseStateChangeListener, context)
            BluetoothMode.CLIENT -> strategy!!.startTimelapse(btClient_onTimelapseStateChangeListener, context)
        }
    }

    private var btClient_onTimelapseStateChangeListener = object : OnTimelapseStateChangeListener {
        override fun onInit() {
            this@TimelapseController.state = State.TIMELAPSE
            Util.broadcastMessage(context!!.first, BluetoothClientService.BT_CLIENT_TIMELAPSE_INITIALIZED)
//            timeAtStartCapturingPhoto = System.currentTimeMillis()
//            strategy!!.capturePhoto()
        }

        override fun onCapture(bytes: ByteArray?) {
            capturedPhotos++

            log(TAG, "btClient_onTimelapseStateChangeListener::onCapture capturedPhotos=$capturedPhotos")
//            if(capturedPhotos == settings!!.photosMax) {
//                this@TimelapseController.stopTimelapse()
//                this@TimelapseController.listenerOutside!!.onComplete()
//                return
//            }
//
            this@TimelapseController.listenerOutside!!.onCapture(bytes)
            Util.broadcastMessage(this@TimelapseController.context!!.first, BluetoothClientService.BT_CLIENT_CAPTURED)
//
//            var delayToNextCapture = settings!!.frequencyCapturing - (System.currentTimeMillis() - timeAtStartCapturingPhoto)
//            if(delayToNextCapture < 0)
//                delayToNextCapture = 0
//
//            timeAtWillBeNextCapture = System.currentTimeMillis() + delayToNextCapture
//            log(TAG, "onCapture, now we are waiting precisely ${delayToNextCapture}ms to next capture")
//            Thread.sleep(delayToNextCapture) // todo: Handle interrupted exception and invoke onFail()
//
//            timeAtStartCapturingPhoto = System.currentTimeMillis()
//            strategy!!.capturePhoto()
        }

        override fun onFail(msg: String) {
            this@TimelapseController.stopTimelapse()
            this@TimelapseController.listenerOutside!!.onFail(msg)
        }
    }

    private var btServer_onTimelapseStateChangeListener = object : OnTimelapseStateChangeListener {
        override fun onInit() {
            this@TimelapseController.state = State.TIMELAPSE

            Util.broadcastMessage(context!!.first, BluetoothServerService.BT_SERVER_TIMELAPSE_START)

//            timeAtStartCapturingPhoto = System.currentTimeMillis()
//            strategy!!.capturePhoto()
        }

        override fun onCapture(bytes: ByteArray?) {
            capturedPhotos++

            log(TAG, "btServer_onTimelapseStateChangeListener::onCapture capturedPhotos=$capturedPhotos")

//            if(capturedPhotos == settings!!.photosMax) {
//                this@TimelapseController.stopTimelapse()
//                this@TimelapseController.listenerOutside!!.onComplete()
//                return
//            }
//
            this@TimelapseController.listenerOutside!!.onCapture(bytes)
            isServerCapturingPhoto = false

            if(clientsDoneCapturing) {
                scheduleServerToCaptureNextPhoto()
            }
//
//            var delayToNextCapture = settings!!.frequencyCapturing - (System.currentTimeMillis() - timeAtStartCapturingPhoto)
//            if(delayToNextCapture < 0)
//                delayToNextCapture = 0
//
//            timeAtWillBeNextCapture = System.currentTimeMillis() + delayToNextCapture
//            log(TAG, "onCapture, now we are waiting precisely ${delayToNextCapture}ms to next capture")
//            Thread.sleep(delayToNextCapture) // todo: Handle interrupted exception and invoke onFail()
//
//            timeAtStartCapturingPhoto = System.currentTimeMillis()
//            strategy!!.capturePhoto()
        }

        override fun onFail(msg: String) {
            this@TimelapseController.stopTimelapse()
            this@TimelapseController.listenerOutside!!.onFail(msg)
        }
    }

    private var btDisabled_onTimelapseStateChangeListener = object : OnTimelapseStateChangeListener {
        override fun onInit() {
            this@TimelapseController.state = State.TIMELAPSE
            timeAtStartCapturingPhoto = System.currentTimeMillis()
            strategy!!.capturePhoto()
        }

        override fun onCapture(bytes: ByteArray?) {
            capturedPhotos++

            log(TAG, "btDisabled_onTimelapseStateChangeListener::onCapture capturedPhotos=$capturedPhotos")

            if(capturedPhotos == settings!!.photosMax) {
                this@TimelapseController.stopTimelapse()
                this@TimelapseController.listenerOutside!!.onComplete()
                return
            }

            this@TimelapseController.listenerOutside!!.onCapture(bytes)

            var delayToNextCapture = settings!!.frequencyCapturing - (System.currentTimeMillis() - timeAtStartCapturingPhoto)
            if(delayToNextCapture < 0)
                delayToNextCapture = 0

            timeAtWillBeNextCapture = System.currentTimeMillis() + delayToNextCapture
            log(TAG, "btDisabled_onTimelapseStateChangeListener -> onCapture, now we are waiting precisely ${delayToNextCapture}ms to next capture")

            try {
                Thread.sleep(delayToNextCapture) // todo: Handle interrupted exception and invoke onFail()
            } catch(e: InterruptedException) {
                log(TAG, "scheduleServerToCaptureNextPhoto() -> Thread.sleep() InterruptedException")
                e.printStackTrace()
                this@TimelapseController.stopTimelapse()
                this@TimelapseController.listenerOutside!!.onFail(e.message)
            }

            timeAtStartCapturingPhoto = System.currentTimeMillis()
            strategy!!.capturePhoto()
        }

        override fun onFail(msg: String) {
            this@TimelapseController.stopTimelapse()
            this@TimelapseController.listenerOutside!!.onFail(msg)
        }
    }

    fun startPreviewing(settings: TimelapseSettings, surfaceHolder: SurfaceHolder, context:Context) {
        if(strategy == null) throw RuntimeException("TimelapseControllerStrategy is null")

        try {
            strategy?.startPreview(settings, surfaceHolder, context)
        } catch(e: CameraNotAvailableException) {
            throw e
        }

        state = State.PREVIEW
    }

    fun getState():State = state

    fun stopPreview() {
        if(state == State.PREVIEW) {
            strategy?.stopPreview()

            state = State.NOTHING
        }
    }

    fun stopTimelapse() {
        if(state == State.TIMELAPSE) {
            strategy?.stopTimelapse()
            wakeLock?.release()

            LocalBroadcastManager.getInstance(context!!.first).unregisterReceiver(mMessageReceiver)

            capturedPhotos = 0
            state = State.NOTHING
        }
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