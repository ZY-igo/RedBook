/**
 * 文件说明：HomeSpacingDecoration.kt
 * 作用：定义首页双列卡片列表的统一间距装饰。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页列表间距装饰器
 *
 * 负责为首页双列卡片列表添加左右与底部间距，
 * 以保证发现流和关注流拥有一致的视觉留白。
 */
class HomeSpacingDecoration : RecyclerView.ItemDecoration() {
    /**
     * 计算单个条目的外边距。
     *
     * @param outRect 输出的间距矩形。
     * @param view 当前条目视图。
     * @param parent 所属 RecyclerView。
     * @param state RecyclerView 当前状态。
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
