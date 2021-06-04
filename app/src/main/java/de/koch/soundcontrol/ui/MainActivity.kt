package de.koch.soundcontrol.ui

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koch.soundcontrol.R
import de.koch.soundcontrol.api.Source
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Method


class MainActivity : AppCompatActivity(), SettingsDialog.SettingsDialogListener {

    private val viewModel: MainViewModel by viewModels()

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 99
    private val PERMISSION_REQUEST_LOCATION = 98

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Check locations permissions for BT
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_LOCATION)

        // Get Settings save state
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val ipSaveState = sharedPref.getString(getString(R.string.ip_key), " ") ?: " "
        val portSaveState = sharedPref.getString(getString(R.string.port_key), " ") ?: " "
        val vibrateSaveState = sharedPref.getBoolean(getString(R.string.vibrate_key), true)
        viewModel.setVibration(vibrateSaveState)
        if (ipSaveState == " " && portSaveState == " ") {
            Toast.makeText(
                applicationContext,
                "Set your IP Config",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.initNetwork(ipSaveState, portSaveState)
            viewModel.initialize()
        }

        viewModel.loading.observe(this, {
            if (it) progressBar.visibility = View.VISIBLE else progressBar.visibility =
                View.INVISIBLE
        })

        viewModel.error.observe(this, {
            if (it != null) {
                Snackbar.make(findViewById(R.id.rootView), it, Snackbar.LENGTH_LONG).show()
                findViewById<TextView>(R.id.errorText).text = it
            }

        })

        viewModel.power.observe(this, {
            if (it) {
                viewModel.getMuteStatus()
                viewModel.getStatusSpeakerA()
                viewModel.getStatusSpeakerB()
                viewModel.getSource()
            }
            togglePowerButtons(powerOn, powerOff, it)
        })

        viewModel.mute.observe(this, {
            if (it) {
                volumeMute.setImageResource(R.drawable.ic_baseline_volume_off_24)
            } else {
                volumeMute.setImageResource(R.drawable.ic_baseline_volume_up_24)
            }
        })

        viewModel.speakerA.observe(this, {
            speakerA.isChecked = it
        })

        viewModel.speakerB.observe(this, {
            speakerB.isChecked = it
        })

        viewModel.source.observe(this, {
            if (it != null) {
                Log.i("hallo source", it.name)
            }
            if (it == Source.CD) {
                if (bluetoothAdapter != null) {
                    if (!bluetoothAdapter!!.isEnabled) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    }
                }
                connectToBTDevice()
            }
            if (it == null) {
                for (i in 0 until checkboxesLayout.childCount) {
                    val checkbox = checkboxesLayout.getChildAt(i) as AppCompatCheckBox
                    checkbox.isChecked = false
                }
            } else {
                for (i in 0 until checkboxesLayout.childCount) {
                    val checkbox = checkboxesLayout.getChildAt(i) as AppCompatCheckBox
                    checkbox.isChecked = i == it.ordinal
                }
            }
        })

        setUpButtons(sharedPref)


        for (i in 0 until checkboxesLayout.childCount) {
            if (checkboxesLayout.getChildAt(i) is AppCompatCheckBox) {
                val checkbox: AppCompatCheckBox = checkboxesLayout.getChildAt(i) as AppCompatCheckBox
                checkbox.setOnClickListener {
                    if ((checkboxesLayout.getChildAt(i) as AppCompatCheckBox).isChecked) viewModel.setSource(
                        Source.values()[i]
                    )
                    for (j in 0 until checkboxesLayout.childCount) {
                        val mChild: AppCompatCheckBox =
                            checkboxesLayout.getChildAt(j) as AppCompatCheckBox
                        mChild.isChecked = i == j
                    }
                }
            }
        }
    }

    private fun setUpButtons(sharedPref: SharedPreferences) {
        viewModel.vibrate.observe(this, { vibrate ->
            val vibrateSaveState1 = sharedPref.getBoolean(getString(R.string.vibrate_key), true)
            setUpSwitch(
                speakerA,
                { viewModel.disableSpeakerA() },
                { viewModel.enableSpeakerA() },
                vibrate
            )
            setUpSwitch(
                speakerB,
                { viewModel.disableSpeakerB() },
                { viewModel.enableSpeakerB() },
                vibrate
            )

            volumeDown.setOnClickListener {
                if (vibrate || vibrateSaveState1) vibratePhone()
                viewModel.volumeDown()
            }

            volumeUp.setOnClickListener {
                if (vibrate || vibrateSaveState1) vibratePhone()
                viewModel.volumeUp()
            }

            powerOn.setOnClickListener {
                if (vibrate || vibrateSaveState1) vibratePhone()
                viewModel.powerOn()
            }

            powerOff.setOnClickListener {
                if (vibrate || vibrateSaveState1) vibratePhone()
                viewModel.powerOff()
            }

            volumeMute.setOnClickListener {
                if (vibrate || vibrateSaveState1) vibratePhone()
                viewModel.toggleMute()
            }
        })
    }


    private fun togglePowerButtons(powerOnButton: AppCompatButton, powerOffButton: AppCompatButton, power: Boolean) {
        if (power) {
            ViewCompat.setBackgroundTintList(
                powerOnButton,
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.green_active
                    )
                )
            )
            ViewCompat.setBackgroundTintList(powerOffButton, null)
        } else {
            ViewCompat.setBackgroundTintList(
                powerOffButton,
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.red_active
                    )
                )
            )
            ViewCompat.setBackgroundTintList(powerOnButton, null)
        }
    }

    private fun setUpSwitch(
        switch: SwitchMaterial,
        checked: () -> Unit,
        unchecked: () -> Unit,
        vibrate: Boolean
    ) {
        switch.setOnClickListener {
            if (vibrate) vibratePhone()
            if (switch.isChecked) unchecked.invoke() else checked.invoke()
        }
    }


    private fun showSettingsDialog() {
        val dialog = SettingsDialog()
        dialog.show(supportFragmentManager, "DIALOG")
    }

    override fun onDialogPositiveClick(ip: String, port: String, btName: String) {
        val success = viewModel.createRetrofitClient(ip, port)
        if (success) {
            val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(getString(R.string.ip_key), ip)
                putString(getString(R.string.port_key), port)
                putString(getString(R.string.btdevice_key), btName)
                apply()
            }
            viewModel.initialize()
        }
    }

    override fun onVibrateClick(vibration: Boolean) {
        if (vibration) vibratePhone()
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.vibrate_key), vibration)
            commit()
        }
        viewModel.setVibration(vibration)
    }

    private fun vibratePhone() {
        val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, 30))
        } else {
            vibrator.vibrate(200)
        }
    }

    private fun connectToBTDevice() {
        if (bluetoothAdapter != null) {

            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
            val btDeviceName = sharedPref.getString(getString(R.string.btdevice_key), null)

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
            val desiredDevice = pairedDevices?.find { it.name == btDeviceName }
            if (desiredDevice == null) {
                Toast.makeText(applicationContext, "No paired device $btDeviceName found", Toast.LENGTH_LONG).show()
            } else {
                bluetoothAdapter!!.getProfileProxy(applicationContext, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        try {
                            val btDeviceWithA2dpProfile = proxy as BluetoothA2dp
                            if (btDeviceWithA2dpProfile.getConnectionState(desiredDevice) != BluetoothA2dp.STATE_CONNECTED) {
                                val connectMethod: Method = btDeviceWithA2dpProfile.javaClass.getMethod("connect", desiredDevice.javaClass)
                                connectMethod.isAccessible = true
                                connectMethod.invoke(proxy, desiredDevice)
                            } else {
                                Toast.makeText(applicationContext, "$btDeviceName already connected", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            Toast.makeText(applicationContext, "BT Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {}
                }, BluetoothA2dp.A2DP)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

