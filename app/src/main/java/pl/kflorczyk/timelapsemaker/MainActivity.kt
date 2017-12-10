package pl.kflorczyk.timelapsemaker

import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import pl.kflorczyk.timelapsemaker.timelapse.*

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceContainer: RelativeLayout
    private lateinit var btnStartTimelapse: ImageButton
    private lateinit var fabSettings: FloatingActionButton

    private var surfaceCamera: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceContainer = findViewById(R.id.surfaceContainer) as RelativeLayout
        btnStartTimelapse = findViewById(R.id.btnStartTimelapse) as ImageButton
        fabSettings = findViewById(R.id.fab) as FloatingActionButton

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun btnStartTimelapseClick(view: View) {

    }

    override fun onStart() {
        super.onStart()

        if(Util.isMyServiceRunning(TimelapseService::class.java, this)) {

        } else {
//            val numberOfCameras = Camera.getNumberOfCameras()
//
//            Util.log("Number of cameras $numberOfCameras")
//
//            for(i in 0..numberOfCameras) {
//                var index = i
//                Util.log("current loop $i")
//
//                try {
//                    var cam: Camera.CameraInfo = Camera.CameraInfo()
//                    Camera.getCameraInfo(i, cam)
//                    Util.log(if (cam.facing == Camera.CameraInfo.CAMERA_FACING_BACK) "kamera tylnia" else "kamera przednia")
//                } catch (e: RuntimeException) {
//                    Util.log("Camera ex: ${e.message}")
//                }
//            }

            startPreview()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun startPreview() {
        if(surfaceContainer.childCount > 0) {
            surfaceContainer.removeView(surfaceCamera)
        }

        surfaceCamera = SurfaceView(this)
        surfaceCamera!!.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        surfaceContainer.addView(surfaceCamera)

        surfaceCamera!!.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceChanged(surfaceHolder: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}
            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder?) { }

            override fun surfaceCreated(surfaceHolder: SurfaceHolder?) {
                if(surfaceHolder == null) {
                    Util.log("MainActivity>startPreview>SurfaceHolder.Callback>surfaceCreated NULL")
                    return
                }

                val settings: TimelapseSettings = Util.getTimelapseSettingsFromFile(this@MainActivity)

                val strategy = if (settings.cameraVersion == CameraVersionAPI.V_1) TimelapseControllerV1Strategy() else TimelapseControllerV2Strategy()
                val timelapseController = TimelapseController(strategy)

                try {
                    timelapseController.startPreviewing(settings, surfaceHolder!!)
                } catch(e: CameraNotAvailableException) {
                    Util.log("camera not available exception")
                }
            }

        })
    }
}
