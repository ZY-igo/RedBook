/**
 * 文件说明：EditProfileActivity.kt
 * 作用：承载 Edit Profile Activity 相关页面的界面初始化、状态呈现与交互逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
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
