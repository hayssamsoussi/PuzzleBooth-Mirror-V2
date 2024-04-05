package com.puzzlebooth.remote.add_number

import android.app.Dialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.puzzlebooth.server.R

interface RemoteAddNumberListener {
    fun onSubmit(phone: String)
    fun onSkip()
}
class RemoteAddNumberFragment: DialogFragment() {

    var listener: RemoteAddNumberListener? = null
    companion object {
        fun newInstance(): RemoteAddNumberFragment {
            val args = Bundle()
            val fragment = RemoteAddNumberFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = getDialog()!!.window
            val size = Point()

            val display = window!!.windowManager.defaultDisplay
            display.getSize(size)

            val width: Int = size.x

            window.setLayout((width * 0.90).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.setCanceledOnTouchOutside(false);
        getExtras()

        return inflater.inflate(R.layout.fragment_remote_phone_number, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpViews()
    }

    private fun getExtras() {
        val args = arguments
        val fileName = args?.getString("fileName")
    }

    private fun setUpViews() {
        val nameEt = requireView().findViewById<EditText>(R.id.et_name)
        val phoneEt = requireView().findViewById<EditText>(R.id.et_phone)
        val emailEt = requireView().findViewById<EditText>(R.id.et_email)
        val buttonSubmit = requireView().findViewById<Button>(R.id.btn_submit)
        val buttonSkip = requireView().findViewById<Button>(R.id.btn_skip)


        buttonSubmit.setOnClickListener {
            val text = "${nameEt.text.toString()};${emailEt.text.toString()};${phoneEt.text.toString()}"
            listener?.onSubmit(text)
            dismiss()
        }

        buttonSkip.setOnClickListener {
            listener?.onSkip()
            dismiss()
        }
    }
}