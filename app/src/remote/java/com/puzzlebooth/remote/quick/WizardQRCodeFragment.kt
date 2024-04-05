package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardQrCodeBinding

class WizardQRCodeFragment: BaseFragment<FragmentWizardQrCodeBinding>(R.layout.fragment_wizard_qr_code) {
    override fun initViewBinding(view: View): FragmentWizardQrCodeBinding {
        return FragmentWizardQrCodeBinding.bind(view)
    }
}