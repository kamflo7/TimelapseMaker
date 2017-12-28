package pl.kflorczyk.timelapsemaker.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import pl.kflorczyk.timelapsemaker.MainActivity
import pl.kflorczyk.timelapsemaker.Util
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * Created by Kamil on 2017-12-20.
 */
class BluetoothManager(activity: Activity) {
    private val TAG = "BluetoothManager"
    
    companion object {
        val uuid:UUID = UUID.fromString("b30eec59-135e-4cd7-b6f1-bf126261ed3f")
    }

    private var activity: Activity = activity
    private lateinit var listener: OnBluetoothStateChangeListener
    private var discoverListener: OnDiscoveringStateChangeListener? = null

    private var devices: List<BluetoothDevice> = ArrayList<BluetoothDevice>()

    fun passActivityResult(requestCode: Int, resultCode: Int) {
        when(requestCode) {
            MainActivity.ACTIVITY_RESULT_BT_ENABLE -> listener.onBluetoothEnable(resultCode == Activity.RESULT_OK)
        }
    }

    fun getDiscoveredDevices(): List<BluetoothDevice> = devices

    fun passDiscoverDevice(intent: Intent) {
        val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice

        discoverListener?.onDiscoverDevice(device)
        devices += device
    }

    fun connectToServer(indexPosition: Int) {
        if(indexPosition >= devices.size) throw RuntimeException("Invalid index")

        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
        var btDevice = devices[indexPosition]

        if(!Util.isMyServiceRunning(BluetoothClientService::class.java, activity))
            activity.stopService(Intent(activity, BluetoothClientService::class.java))

        val intent = Intent(activity, BluetoothClientService::class.java)
        intent.putExtra("btDevice", btDevice)
        activity.startService(intent)
    }

    /**
     * @return true if device has Bluetooth, otherwise false
     * @param listener a listener to know the result of a method
     */
    fun enableBluetooth(listener: OnBluetoothStateChangeListener): Boolean {
        this.listener = listener

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            if (!btAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(enableBtIntent, MainActivity.ACTIVITY_RESULT_BT_ENABLE)
            } else {
                listener.onBluetoothEnable(true)
            }
            return true
        }
        return false
    }

    fun enableDiscoverability() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        activity.startActivity(discoverableIntent)
    }

    fun startBluetoothServiceServer() {
        if(Util.isMyServiceRunning(BluetoothServerService::class.java, activity)) {
            activity.stopService(Intent(activity, BluetoothServerService::class.java))
        }

        activity.applicationContext.startService(Intent(activity.applicationContext, BluetoothServerService::class.java))
    }

    fun startDiscovering(listener: OnDiscoveringStateChangeListener) {
        this.discoverListener = listener
        devices = ArrayList<BluetoothDevice>()

        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()

        if(defaultAdapter.isDiscovering) {
            defaultAdapter.cancelDiscovery()
        }

        defaultAdapter.startDiscovery()
    }

    fun getBondedDevices() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = btAdapter.bondedDevices

        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address
                Log.d(TAG, "getBondedDevices() -> $deviceName; $deviceHardwareAddress")
            }
        }
    }

    interface OnDiscoveringStateChangeListener {
        fun onDiscoverDevice(device: BluetoothDevice)
    }

    interface OnBluetoothStateChangeListener {
        fun onBluetoothEnable(enabled: Boolean)
    }
}