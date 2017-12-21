package pl.kflorczyk.timelapsemaker.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import pl.kflorczyk.timelapsemaker.MainActivity
import pl.kflorczyk.timelapsemaker.Util
import android.bluetooth.BluetoothDevice

/**
 * Created by Kamil on 2017-12-20.
 */
class BluetoothManager(activity: Activity) {
    private var activity: Activity = activity
    private lateinit var listener: OnBluetoothStateChangeListener

    private var devices: List<BluetoothDevice> = ArrayList<BluetoothDevice>()

    fun passActivityResult(requestCode: Int, resultCode: Int) {
        when(requestCode) {
            MainActivity.ACTIVITY_RESULT_BT_ENABLE -> listener.onBluetoothEnable(resultCode == Activity.RESULT_OK)
        }
    }

    fun passDiscoverDevice(intent: Intent) {
        val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
        val deviceName = device.name
        val deviceHardwareAddress = device.address

        devices += device
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
        if(Util.isMyServiceRunning(BluetoothService::class.java, activity)) {
            activity.stopService(Intent(activity, BluetoothService::class.java))
        }

        activity.startService(Intent(activity, BluetoothService::class.java))
    }

    fun startDiscovering() {
        devices = ArrayList<BluetoothDevice>()

        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()

        if(defaultAdapter.isDiscovering) {
            defaultAdapter.cancelDiscovery()
        }

        val startDiscovery = defaultAdapter.startDiscovery()
    }

    fun getBondedDevices() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = btAdapter.bondedDevices

        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address
                Util.log("[PairedDevice] $deviceName; $deviceHardwareAddress")
            }
        }
    }

    interface OnBluetoothStateChangeListener {
        fun onBluetoothEnable(enabled: Boolean)
    }
}