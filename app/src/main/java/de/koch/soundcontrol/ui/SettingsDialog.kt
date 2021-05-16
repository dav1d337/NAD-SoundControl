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

    private lateinit var listener: IPDialogListener

    interface IPDialogListener {
        fun onDialogPositiveClick(ip: String, port: String)
        fun onVibrateClick(vibration: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val sharedPref = it.getPreferences(Context.MODE_PRIVATE)
            val builder = AlertDialog.Builder(it)

            val ipSave = sharedPref.getString(getString(R.string.ip_key), "")
            val portSave = sharedPref.getString(getString(R.string.port_key), "")

            val linLayout = LinearLayout(context)
            linLayout.orientation = LinearLayout.VERTICAL
            val ipInput = EditText(context)
            ipInput.setText(ipSave)
            ipInput.hint = "IP"
            val portInput = EditText(context)
            portInput.setText(portSave)
            portInput.hint = "Port"
            val vibrateCheckbox = CheckBox(context)
            vibrateCheckbox.text = "Enable Vibration"

            val checked = sharedPref.getBoolean(getString(R.string.vibrate_key), true)
            vibrateCheckbox.isChecked = checked
            vibrateCheckbox.setOnClickListener {v->
                listener.onVibrateClick((v as CheckBox).isChecked)
            }

            linLayout.addView(ipInput)
            linLayout.addView(portInput)
            linLayout.addView(vibrateCheckbox)
            builder.setView(linLayout)

            builder.setMessage("IP Config")
                .setPositiveButton("Set",
                    DialogInterface.OnClickListener { dialog, id ->
                        listener.onDialogPositiveClick(ipInput.text.toString().trim(), portInput.text.toString().trim())
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User cancelled the dialog
                    })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as IPDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }
}