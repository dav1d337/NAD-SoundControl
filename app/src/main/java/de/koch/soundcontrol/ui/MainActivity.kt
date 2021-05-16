package de.koch.soundcontrol.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koch.soundcontrol.R
import de.koch.soundcontrol.api.Source


class MainActivity : AppCompatActivity(), SettingsDialog.IPDialogListener {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val ipSaveState = sharedPref.getString(getString(R.string.ip_key), " ") ?: " "
        val portSaveState = sharedPref.getString(getString(R.string.port_key), " ") ?: " "

        if (ipSaveState == " " && portSaveState == " ") {
            Toast.makeText(
                applicationContext,
                "Set your IP Config",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.initNetwork("0000", "9000")
        } else {
            viewModel.initNetwork(ipSaveState, portSaveState)
            viewModel.initialize()
        }

        viewModel.error.observe(this, {
            Snackbar.make(findViewById(R.id.rootView), it, Snackbar.LENGTH_LONG).show()
            findViewById<TextView>(R.id.errorText).text = it
        })

        val speakerASwitch = findViewById<SwitchMaterial>(R.id.speakerA)
        val speakerBSwitch = findViewById<SwitchMaterial>(R.id.speakerB)
        val volumeDownButton = findViewById<ImageButton>(R.id.volumeDown)
        val volumeUpButton = findViewById<ImageButton>(R.id.volumeUp)
        val powerOnButton = findViewById<AppCompatButton>(R.id.powerOn)
        val powerOffButton = findViewById<AppCompatButton>(R.id.powerOff)
        val muteButton = findViewById<ImageButton>(R.id.volumeMute)
        val checkboxes = findViewById<LinearLayoutCompat>(R.id.linearLayoutCompat)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)


        var vibrateSaveState = sharedPref.getBoolean(getString(R.string.vibrate_key), true)
        viewModel.setVibration(vibrateSaveState)

        viewModel.power.observe(this, {
            if (it) {
                viewModel.getMuteStatus()
                viewModel.getStatusSpeakerA()
                viewModel.getStatusSpeakerB()
                viewModel.getSource()
            }
            togglePowerButtons(powerOnButton, powerOffButton, it)
        })

        viewModel.mute.observe(this, {
            if (it) {
                muteButton.setImageResource(R.drawable.ic_baseline_volume_off_24)
            } else {
                muteButton.setImageResource(R.drawable.ic_baseline_volume_up_24)
            }
        })

        viewModel.speakerA.observe(this, {
            speakerASwitch.isChecked = it
        })

        viewModel.speakerB.observe(this, {
            speakerBSwitch.isChecked = it
        })

        viewModel.source.observe(this, {
            if (it == null) {
                for (i in 0 until checkboxes.childCount) {
                    val checkbox = checkboxes.getChildAt(i) as AppCompatCheckBox
                    checkbox.isChecked = false
                }
            } else {
                for (i in 0 until checkboxes.childCount) {
                    val checkbox = checkboxes.getChildAt(i) as AppCompatCheckBox
                    checkbox.isChecked = i == it.ordinal
                }
            }
        })

        viewModel.vibrate.observe(this, { vibrate ->
            vibrateSaveState = sharedPref.getBoolean(getString(R.string.vibrate_key), true)
            setUpSwitch(
                speakerASwitch,
                { viewModel.disableSpeakerA() },
                { viewModel.enableSpeakerA() },
                vibrate
            )
            setUpSwitch(
                speakerBSwitch,
                { viewModel.disableSpeakerB() },
                { viewModel.enableSpeakerB() },
                vibrate
            )
            volumeDownButton.setOnClickListener {
                if (vibrate || vibrateSaveState) vibratePhone()
                viewModel.volumeDown()
            }
            volumeUpButton.setOnClickListener {
                if (vibrate || vibrateSaveState) vibratePhone()
                viewModel.volumeUp()
            }
            powerOnButton.setOnClickListener {
                if (vibrate || vibrateSaveState) vibratePhone()
                viewModel.powerOn()
            }

            powerOffButton.setOnClickListener {
                if (vibrate || vibrateSaveState) vibratePhone()
                viewModel.powerOff()
            }

            muteButton.setOnClickListener {
                if (vibrate || vibrateSaveState) vibratePhone()
                viewModel.toggleMute()
            }
        })

        viewModel.loading.observe(this, {
            if (it) progressBar.visibility = View.VISIBLE else progressBar.visibility =
                View.INVISIBLE
        })

        for (i in 0 until checkboxes.childCount) {
            if (checkboxes.getChildAt(i) is AppCompatCheckBox) {
                val checkbox: AppCompatCheckBox = checkboxes.getChildAt(i) as AppCompatCheckBox
                checkbox.setOnClickListener {
                    if ((checkboxes.getChildAt(i) as AppCompatCheckBox).isChecked) viewModel.setSource(
                        Source.values()[i]
                    )
                    for (j in 0 until checkboxes.childCount) {
                        val mChild: AppCompatCheckBox =
                            checkboxes.getChildAt(j) as AppCompatCheckBox
                        mChild.isChecked = i == j
                    }
                }
            }
        }
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

    private fun showSettingsDialog() {
        val dialog = SettingsDialog()
        dialog.show(supportFragmentManager, "DIALOG")
    }

    override fun onDialogPositiveClick(ip: String, port: String) {
        val success = viewModel.createRetrofitClient(ip, port)
        if (success) {
            val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(getString(R.string.ip_key), ip)
                putString(getString(R.string.port_key), port)
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
}