package pl.kflorczyk.timelapsemaker

import android.Manifest
import android.content.Intent
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
import android.widget.Toast
import com.tbruyelle.rxpermissions2.RxPermissions


class MainActivity : AppCompatActivity() {

    private lateinit var surfaceContainer: RelativeLayout
    private lateinit var btnStartTimelapse: ImageButton
    private lateinit var fabSettings: FloatingActionButton

    private var surfaceCamera: SurfaceView? = null

    private var timelapseControllerPreview: TimelapseController? = null

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

    fun btnSettingClick(view: View) {

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()

        if(Util.isMyServiceRunning(TimelapseService::class.java, this)) {
            stopService(Intent(this, TimelapseService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        if(Util.isMyServiceRunning(TimelapseService::class.java, this)) {

        } else {
            var intent = Intent(this, TimelapseService::class.java)
            startService(intent)
            Util.log("Start service")
//            startPreview()
        }
    }

    override fun onPause() {
        super.onPause()

        if(timelapseControllerPreview?.isPreviewing() == true) {
            timelapseControllerPreview!!.stopPreview()
            timelapseControllerPreview = null
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

                RxPermissions(this@MainActivity)
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if(granted) {
                            val settings: TimelapseSettings = Util.getTimelapseSettingsFromFile(this@MainActivity)
                            val strategy = if (settings.cameraVersion == CameraVersionAPI.V_1) TimelapseControllerV1Strategy() else TimelapseControllerV2Strategy()
                            timelapseControllerPreview = TimelapseController(strategy)

                            try {
                                timelapseControllerPreview!!.startPreviewing(settings, surfaceHolder!!)
                            } catch(e: CameraNotAvailableException) {
                                Toast.makeText(this@MainActivity, "Camera is currently not available. Ensure that camera is free and open the app again", Toast.LENGTH_LONG).show()
                                Util.log("camera not available exception")
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "You have to grant permissions to use app", Toast.LENGTH_LONG).show()
                        }
                     })

            }

        })
    }
}
