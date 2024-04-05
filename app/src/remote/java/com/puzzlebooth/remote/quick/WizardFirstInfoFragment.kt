package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardFirstBinding

class WizardFirstInfoFragment: BaseFragment<FragmentWizardFirstBinding>(R.layout.fragment_wizard_first) {
    override fun initViewBinding(view: View): FragmentWizardFirstBinding {
        return FragmentWizardFirstBinding.bind(view)
    }
}