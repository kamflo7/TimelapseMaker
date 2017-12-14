package pl.kflorczyk.timelapsemaker.camera

/**
 * Created by Kamil on 2017-12-10.
 */
data class Resolution(val width:Int, val height:Int) {
    override fun toString(): String {
        return "${width}x$height"
    }
}