package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardStartBinding

class WizardStartStepFragment: BaseFragment<FragmentWizardStartBinding>(R.layout.fragment_wizard_start) {
    override fun initViewBinding(view: View): FragmentWizardStartBinding {
        return FragmentWizardStartBinding.bind(view)
    }
}