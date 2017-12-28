package pl.kflorczyk.timelapsemaker

import android.content.Context
import android.os.Environment
import android.util.Log
import pl.kflorczyk.timelapsemaker.Util.log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.io.FileOutputStream
import java.io.IOException

class StorageManager(type: StorageType, context: Context) {
    private val TAG = "StorageManager"

    private val type: StorageType = type
    private val context: Context = context
    private val storageBase: File
    private var savingPath: File? = null

    init {
        val storages = getStorages(context)
        val storage = storages.find { p -> p.second == this.type } ?: throw RuntimeException("Storage not found for given type")
        storageBase = storage.first
    }

    fun createTimelapseDirectory(): Boolean {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val dirName = sdf.format(Date())

        savingPath = File(getPath(dirName))
        return savingPath!!.mkdirs()
    }

    fun getPath(directoryName: String): String {
        return "${storageBase.absolutePath}/$directoryName/"
    }

    fun savePhoto(bytes: ByteArray, number: Int) {
        if(savingPath == null) {
            throw RuntimeException("Saving path is not specified (Did you invoke createTimelapseDirectory?)")
        }

        val photo = File(String.format("%s/photo%d.jpg", savingPath!!.absolutePath, number))

        try {
            val fos = FileOutputStream(photo.path)
            fos.write(bytes)
            fos.close()
            log(TAG, "savePhoto(ByteArray, Int), location = " + photo.absolutePath)
        } catch (e: IOException) {
            log(TAG, "savePhoto(ByteArray, Int) -> IOException:  " + e.message)
            throw e
        }
    }

    companion object {
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
    }

    enum class StorageType {
        EXTERNAL_EMULATED,
        REAL_SDCARD
    }
}