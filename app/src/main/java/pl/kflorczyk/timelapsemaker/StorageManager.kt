package pl.kflorczyk.timelapsemaker

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Created by Kamil on 2017-12-11.
 */
object StorageManager {

    fun getStorages(context: Context): List<Pair<File, StorageType>> {
        val externalFilesDir = context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES)

        val validate = fun(f: File): Boolean {
            val accessible = f.canWrite() && f.canRead()
            return accessible
        }

        val list = ArrayList<Pair<File, StorageType>>()

        for (f in externalFilesDir) {
            if (validate(f)) {
                val type = if (f.absolutePath.contains("emulated")) StorageType.EXTERNAL_EMULATED else StorageType.REAL_SDCARD
                list.add(Pair(f, type))
            }
        }

        return list
    }

    enum class StorageType {
        EXTERNAL_EMULATED,
        REAL_SDCARD
    }
}