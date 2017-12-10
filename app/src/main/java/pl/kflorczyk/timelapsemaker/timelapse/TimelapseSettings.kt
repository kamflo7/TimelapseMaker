package pl.kflorczyk.timelapsemaker.timelapse

import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.camera.PictureFormat
import pl.kflorczyk.timelapsemaker.camera.Resolution

/**
 * Created by Kamil on 2017-12-10.
 */
data class TimelapseSettings(var photosMax:Int?,
                             var frequencyCapturing: Long,
                             var resolution: Resolution?,
                             var pictureFormat: PictureFormat,
                             var webEnabled: Boolean,
                             var cameraVersion: CameraVersionAPI,
                             var cameraId: String?)