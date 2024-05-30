package com.puzzlebooth.main.base

import android.app.ProgressDialog
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.puzzlebooth.server.network.APIService
import com.puzzlebooth.server.network.RetrofitInstance

data class MessageEvent(val text: String)

abstract class BaseFragment<VB : ViewBinding>(
    @LayoutRes val layoutRes: Int
) : Fragment() {

    var progressDialog: ProgressDialog? = null

    lateinit var sharedPreferences: SharedPreferences
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    open val service = RetrofitInstance.getRetrofitInstance().create(APIService::class.java)

    abstract fun initViewBinding(view: View): VB

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layoutRes, container, false)
        _binding = initViewBinding(view)
        sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
        return view
    }

    fun isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun hideProgress() {
        progressDialog?.let { progress ->
            progress.hide()
        }
    }

    fun showProgress() {
        if(progressDialog != null) {
            progressDialog?.show()
        } else {
            progressDialog = ProgressDialog(requireContext())
            progressDialog?.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding when the view is destroyed
    }
}