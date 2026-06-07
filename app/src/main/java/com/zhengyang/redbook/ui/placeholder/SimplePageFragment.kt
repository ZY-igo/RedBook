/**
 * 文件说明：SimplePageFragment.kt
 * 作用：承载 Simple Page Fragment 相关页面区块的视图渲染、状态呈现与交互逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
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
