package com.zhengyang.redbook.ui.my

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zhengyang.redbook.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonPreview.setOnClickListener { }
    }
}
