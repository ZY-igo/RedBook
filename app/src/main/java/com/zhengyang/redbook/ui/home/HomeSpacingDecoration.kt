/**
 * 文件说明： HomeSpacingDecoration.kt
 * 作用： 承载首页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.zhengyang.redbook.utils.dpToPx

class HomeSpacingDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = 1.dpToPx()
        outRect.right = 1.dpToPx()
        outRect.bottom = 6.dpToPx()
    }
}
