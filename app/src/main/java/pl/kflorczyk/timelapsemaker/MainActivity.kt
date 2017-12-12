package pl.kflorczyk.timelapsemaker

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
import pl.kflorczyk.timelapsemaker.exceptions.CameraNotAvailableException
import pl.kflorczyk.timelapsemaker.timelapse.*
import android.widget.Toast
import com.tbruyelle.rxpermissions2.RxPermissions

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceContainer: RelativeLayout
    private lateinit var btnStartTimelapse: ImageButton
    private lateinit var fabSettings: FloatingActionButton

    private var surfaceCamera: SurfaceView? = null

    private lateinit var app:MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceContainer = findViewById(R.id.surfaceContainer) as RelativeLayout
        btnStartTimelapse = findViewById(R.id.btnStartTimelapse) as ImageButton
        fabSettings = findViewById(R.id.fab) as FloatingActionButton

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val functionAfterPermissionsDone = fun() {
            app = application as MyApplication
            if(app.timelapseSettings == null) {
                app.timelapseSettings = Util.getTimelapseSettingsFromFile(this)
            }
        }

        if(!Util.checkPermissions(Util.NECESSARY_PERMISSIONS_START_APP, this)) {
            RxPermissions(this@MainActivity)
                    .request(*Util.NECESSARY_PERMISSIONS_START_APP)
                    .subscribe({ granted ->
                        if(granted) {
                            functionAfterPermissionsDone()
                        } else {
                            Toast.makeText(this@MainActivity, "You have to grant permissions to use app", Toast.LENGTH_LONG).show()
                        }
                    })
        } else {
            functionAfterPermissionsDone()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun btnStartTimelapseClick(view: View) {
        if(!Util.isMyServiceRunning(TimelapseService::class.java, this)) {
            if(TimelapseController.getState() == TimelapseController.State.PREVIEW) {
                TimelapseController.stopPreview()
            }

            startService(Intent(this, TimelapseService::class.java))
        }
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
            startPreview()
        }
    }

    override fun onPause() {
        super.onPause()

        if(TimelapseController.getState() == TimelapseController.State.PREVIEW) {
            TimelapseController.stopPreview()
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
                    Toast.makeText(this@MainActivity, "Problem with creating the surface for camera preview", Toast.LENGTH_LONG).show()
                    Util.log("MainActivity>startPreview>SurfaceHolder.Callback>surfaceCreated NULL")
                    return
                }

                if(Util.checkPermissions(Util.NECESSARY_PERMISSIONS_START_APP, this@MainActivity)) {
                    val strategy = Util.getTimelapseControllerStrategy(this@MainActivity)
                    TimelapseController.build(strategy, app.timelapseSettings!!)

                    try {
                        TimelapseController.startPreviewing(app.timelapseSettings!!, surfaceHolder!!)
                    } catch(e: CameraNotAvailableException) {
                        Toast.makeText(this@MainActivity, "Camera is currently not available. Ensure that camera is free and open the app again", Toast.LENGTH_LONG).show()
                        Util.log("camera not available exception")
                    }
                }
            }

        })
    }
}
