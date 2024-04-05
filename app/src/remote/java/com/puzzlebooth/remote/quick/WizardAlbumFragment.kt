package com.puzzlebooth.remote.quick

import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding
import com.puzzlebooth.server.databinding.FragmentWizardAlbumBinding

class WizardAlbumFragment: BaseFragment<FragmentWizardAlbumBinding>(R.layout.fragment_wizard_album) {
    override fun initViewBinding(view: View): FragmentWizardAlbumBinding {
        return FragmentWizardAlbumBinding.bind(view)
    }
}