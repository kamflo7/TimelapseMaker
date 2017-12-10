package pl.kflorczyk.timelapsemaker

import android.Manifest
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import io.reactivex.functions.Consumer
import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import pl.kflorczyk.timelapsemaker.timelapse.*
import android.widget.Toast
import android.content.pm.PackageManager



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

    private val REQUEST_PERMISSIONS_PREVIEW = 1
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode === REQUEST_PERMISSIONS_PREVIEW) {
            if (grantResults.isNotEmpty()) {
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "You have to provide permissions to use app.", Toast.LENGTH_LONG).show()
                        return
                    }
                }

                startPreviewReally()
            }
        }
    }

    private var surfaceHolder:SurfaceHolder? = null

    private fun startPreviewReally() {
        val settings: TimelapseSettings = Util.getTimelapseSettingsFromFile(this@MainActivity)
        val strategy = if (settings.cameraVersion == CameraVersionAPI.V_1) TimelapseControllerV1Strategy() else TimelapseControllerV2Strategy()
        val timelapseController = TimelapseController(strategy)

        try {
            timelapseController.startPreviewing(settings, surfaceHolder!!)
        } catch(e: CameraNotAvailableException) {
            Util.log("camera not available exception")
        }
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

                this@MainActivity.surfaceHolder = surfaceHolder

                if(!Util.checkPermissions(Util.NECESSARY_PERMISSIONS_START_APP, this@MainActivity)) {
                    ActivityCompat.requestPermissions(this@MainActivity, Util.NECESSARY_PERMISSIONS_START_APP, REQUEST_PERMISSIONS_PREVIEW)
                } else {
                    startPreviewReally()
                }
            }

        })
    }
}
