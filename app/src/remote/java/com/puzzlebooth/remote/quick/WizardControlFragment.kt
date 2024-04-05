package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardControlBinding

class WizardControlFragment: BaseFragment<FragmentWizardControlBinding>(R.layout.fragment_wizard_control) {
    override fun initViewBinding(view: View): FragmentWizardControlBinding {
        return FragmentWizardControlBinding.bind(view)
    }
}