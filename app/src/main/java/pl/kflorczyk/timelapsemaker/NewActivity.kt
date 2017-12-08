package pl.kflorczyk.timelapsemaker

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager

class NewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}
