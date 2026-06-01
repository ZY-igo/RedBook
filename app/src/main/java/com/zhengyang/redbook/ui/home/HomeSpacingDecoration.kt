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
