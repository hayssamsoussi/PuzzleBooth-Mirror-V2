package com.puzzlebooth.server.mosaic

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.ActivityChooseMosaicPictureBinding

class ChooseMosaicPictureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseMosaicPictureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseMosaicPictureBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initViews()
    }

    fun initViews() {

    }


}