package pl.kflorczyk.timelapsemaker.timelapse

/**
 * Created by Kamil on 2017-12-11.
 */
interface OnTimelapseStateChangeListener {
    fun onInit()
    fun onCapture(bytes: ByteArray?)
    fun onFail(msg: String)
}