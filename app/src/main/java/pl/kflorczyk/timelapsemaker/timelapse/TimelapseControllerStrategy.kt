package pl.kflorczyk.timelapsemaker.timelapse

import android.content.Context
import android.view.SurfaceHolder

/**
 * Created by Kamil on 2017-12-09.
 */
interface TimelapseControllerStrategy {
    fun startPreview(timelapseSettings: TimelapseSettings, surfaceHolder: SurfaceHolder, context:Context)
    fun stopPreview()

    fun startTimelapse(onTimelapseStateChangeListener: OnTimelapseStateChangeListener, context: Context)
    fun capturePhoto()
    fun stopTimelapse()
}