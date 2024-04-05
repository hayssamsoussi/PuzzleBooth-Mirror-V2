package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardSecondBinding

class WizardSecondInfoFragment: BaseFragment<FragmentWizardSecondBinding>(R.layout.fragment_wizard_second) {
    override fun initViewBinding(view: View): FragmentWizardSecondBinding {
        return FragmentWizardSecondBinding.bind(view)
    }
}