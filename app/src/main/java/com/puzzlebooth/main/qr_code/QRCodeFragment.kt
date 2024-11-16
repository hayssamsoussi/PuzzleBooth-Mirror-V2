package com.puzzlebooth.main.qr_code

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.puzzlebooth.server.R


class QRCodeFragment: DialogFragment() {

    companion object {
        fun newInstance(text: String): QRCodeFragment {
            val args = Bundle()
            args.putString("text", text)
            val fragment = QRCodeFragment()
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

            // Adjust other attributes if needed
            dialog.window?.setDimAmount(0f)

            val width: Int = size.x

            //window.setLayout((width * 1).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.TOP or Gravity.END)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.setCanceledOnTouchOutside(false);
        return inflater.inflate(R.layout.fragment_qr_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getExtras()
        setUpViews()
    }

    private fun getExtras() {
        val args = arguments
        val text = args?.getString("text")

        if(!text.isNullOrEmpty()) {
            val bitmap = net.glxn.qrgen.android.QRCode.from(text).withSize(200,200).bitmap()
            println("hhh text is not empty ${text}")
            Glide.with(requireContext())
                .load(bitmap)
                .into(requireView().findViewById<ImageView>(R.id.ivQrCode))

            requireView().findViewById<ImageView>(R.id.ivQrCode).setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(Uri.parse(text))
                startActivity(i)
            }
        }
    }

    private fun setUpViews() {
        requireView().findViewById<ImageView>(R.id.ivQrCode).rootView.setOnClickListener {
            dismiss()
        }
    }
}