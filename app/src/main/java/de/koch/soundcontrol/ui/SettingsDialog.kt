package de.koch.soundcontrol.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import de.koch.soundcontrol.R

class SettingsDialog : DialogFragment() {

    private lateinit var listener: SettingsDialogListener

    interface SettingsDialogListener {
        fun onDialogPositiveClick(ip: String, port: String, btName: String)
        fun onVibrateClick(vibration: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val sharedPref = it.getPreferences(Context.MODE_PRIVATE)
            val builder = AlertDialog.Builder(it)

            val ipSave = sharedPref.getString(getString(R.string.ip_key), "")
            val portSave = sharedPref.getString(getString(R.string.port_key), "")
            val btSave = sharedPref.getString(getString(R.string.btdevice_key), "")

            val linLayout = LinearLayout(context)
            linLayout.orientation = LinearLayout.VERTICAL
            val ipInput = EditText(context)
            ipInput.setText(ipSave)
            ipInput.hint = "IP"
            val portInput = EditText(context)
            portInput.setText(portSave)
            portInput.hint = "Port"
            val btInput = EditText(context)
            btInput.setText(btSave)
            btInput.hint = "Bluetooth Device Name"
            val vibrateCheckbox = CheckBox(context)
            vibrateCheckbox.text = "Enable Vibration"

            val checked = sharedPref.getBoolean(getString(R.string.vibrate_key), true)
            vibrateCheckbox.isChecked = checked
            vibrateCheckbox.setOnClickListener { v ->
                listener.onVibrateClick((v as CheckBox).isChecked)
            }

            linLayout.addView(ipInput)
            linLayout.addView(portInput)
            linLayout.addView(btInput)
            linLayout.addView(vibrateCheckbox)
            builder.setView(linLayout)

            builder.setMessage("Settings")
                .setPositiveButton(
                    "Apply"
                ) { dialog, id ->
                    listener.onDialogPositiveClick(ipInput.text.toString().trim(), portInput.text.toString().trim(), btInput.text.toString())
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, id ->
                    // User cancelled the dialog
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SettingsDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                (context.toString() +
                        " must implement IPDialogListener")
            )
        }
    }
}