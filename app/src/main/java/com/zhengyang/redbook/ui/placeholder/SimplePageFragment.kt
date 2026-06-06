/**
 * 文件说明： SimplePageFragment.kt
 * 作用： 提供占位页面实现，用于补齐导航流程或演示界面。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.placeholder

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.zhengyang.redbook.R

class SimplePageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            gravity = Gravity.CENTER
            text = requireArguments().getString(ARG_TITLE).orEmpty()
            textSize = 22f
            setTextColor(ContextCompat.getColor(context, R.color.xhs_text_primary))
            setBackgroundColor(ContextCompat.getColor(context, R.color.xhs_bg))
        }
    }

    companion object {
        private const val ARG_TITLE = "title"

        fun newInstance(title: String) = SimplePageFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
            }
        }
    }
}
