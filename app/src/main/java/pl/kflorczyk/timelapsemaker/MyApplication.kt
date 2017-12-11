package pl.kflorczyk.timelapsemaker

import android.app.Application
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseSettings

/**
 * Created by Kamil on 2017-12-11.
 */
class MyApplication : Application() {
    var timelapseSettings: TimelapseSettings? = null

}