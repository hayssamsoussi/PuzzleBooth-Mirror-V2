package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardThirdBinding

class WizardThirdInfoFragment: BaseFragment<FragmentWizardThirdBinding>(R.layout.fragment_wizard_third) {
    override fun initViewBinding(view: View): FragmentWizardThirdBinding {
        return FragmentWizardThirdBinding.bind(view)
    }
}