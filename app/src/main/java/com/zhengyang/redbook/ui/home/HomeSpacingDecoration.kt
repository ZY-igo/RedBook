package com.zhengyang.redbook.ui.home

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页列表间距装饰器。
 *
 * 给双列卡片列表提供统一的左右和底部留白。
 */
class HomeSpacingDecoration : RecyclerView.ItemDecoration() {

    /**
     * 计算单个列表项的外边距。
     */
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
