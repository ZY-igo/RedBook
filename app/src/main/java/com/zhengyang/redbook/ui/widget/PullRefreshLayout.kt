/**
 * 文件说明： PullRefreshLayout.kt
 * 作用： 定义可复用的自定义控件和跨页面共享的界面组件。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.children
import com.zhengyang.redbook.R
import kotlin.math.max
import kotlin.math.min

class PullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val indicatorSize = dp(15)
    private val headerHeight = dp(52)
    private val triggerOffset = dp(72)
    private val refreshingOffset = dp(56)
    private val maxOffset = dp(124)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val decelerateInterpolator = DecelerateInterpolator()

    private val indicator = ProgressBar(context).apply {
        layoutParams = LayoutParams(indicatorSize, indicatorSize)
        indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.xhs_refresh_secondary))
        alpha = 0f
        visibility = View.INVISIBLE
    }

    private var targetView: View? = null
    private var refreshListener: (() -> Unit)? = null
    private var animator: ValueAnimator? = null
    private var initialDownY = 0f
    private var initialMotionY = 0f
    private var isBeingDragged = false
    private var currentOffset = 0f

    var isRefreshing: Boolean = false
        private set

    init {
        clipToPadding = false
        clipChildren = false
        addView(
            indicator,
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        targetView = children.firstOrNull { it !== indicator }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || isRefreshing || canChildScrollUp()) {
            return false
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialDownY = ev.y
                initialMotionY = 0f
                isBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                val yDiff = ev.y - initialDownY
                if (yDiff > touchSlop && !isBeingDragged) {
                    initialMotionY = initialDownY + touchSlop
                    isBeingDragged = true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
            }
        }
        return isBeingDragged
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!isBeingDragged) {
                    val yDiff = event.y - initialDownY
                    if (yDiff > touchSlop && !canChildScrollUp()) {
                        initialMotionY = initialDownY + touchSlop
                        isBeingDragged = true
                    }
                }
                if (isBeingDragged) {
                    val dragDistance = max(0f, event.y - initialMotionY)
                    moveSpinner(calculateOffset(dragDistance))
                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) {
                    finishSpinner()
                    isBeingDragged = false
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val indicatorLeft = (width - indicator.measuredWidth) / 2
        val indicatorTop = ((currentOffset - headerHeight) / 2f + (headerHeight - indicator.measuredHeight) / 2f)
            .toInt()
        indicator.layout(
            indicatorLeft,
            indicatorTop,
            indicatorLeft + indicator.measuredWidth,
            indicatorTop + indicator.measuredHeight
        )
    }

    fun setOnRefreshListener(listener: (() -> Unit)?) {
        refreshListener = listener
    }

    fun setRefreshing(refreshing: Boolean) {
        if (refreshing == isRefreshing) return
        isRefreshing = refreshing
        if (refreshing) {
            indicator.visibility = View.VISIBLE
            animateOffsetTo(refreshingOffset.toFloat())
            refreshListener?.invoke()
        } else {
            animateOffsetTo(0f)
        }
    }

    private fun finishSpinner() {
        if (currentOffset >= triggerOffset) {
            isRefreshing = true
            indicator.visibility = View.VISIBLE
            animateOffsetTo(refreshingOffset.toFloat()) {
                refreshListener?.invoke()
            }
        } else {
            animateOffsetTo(0f)
        }
    }

    private fun moveSpinner(offset: Float) {
        currentOffset = offset
        targetView?.translationY = offset
        indicator.alpha = min(1f, offset / triggerOffset)
        indicator.visibility = if (offset > 0f || isRefreshing) View.VISIBLE else View.INVISIBLE
        requestLayout()
    }

    private fun animateOffsetTo(targetOffset: Float, endAction: (() -> Unit)? = null) {
        animator?.cancel()
        val startOffset = currentOffset
        animator = ValueAnimator.ofFloat(startOffset, targetOffset).apply {
            duration = if (targetOffset == 0f) 220L else 180L
            interpolator = decelerateInterpolator
            addUpdateListener { animation ->
                moveSpinner(animation.animatedValue as Float)
            }
            doOnEnd {
                if (targetOffset == 0f) {
                    indicator.alpha = 0f
                    indicator.visibility = View.INVISIBLE
                    targetView?.translationY = 0f
                    currentOffset = 0f
                    requestLayout()
                } else {
                    indicator.alpha = 1f
                    indicator.visibility = View.VISIBLE
                }
                endAction?.invoke()
            }
            start()
        }
    }

    private fun calculateOffset(dragDistance: Float): Float {
        val dragPercent = min(1f, dragDistance / triggerOffset)
        val extra = max(0f, dragDistance - triggerOffset)
        val tension = min(extra, triggerOffset.toFloat() * 1.8f) / triggerOffset
        val tensionOffset = triggerOffset * tension * 0.35f
        return min(maxOffset.toFloat(), triggerOffset * dragPercent + tensionOffset)
    }

    private fun canChildScrollUp(): Boolean {
        return targetView?.canScrollVertically(-1) == true
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun ValueAnimator.doOnEnd(action: () -> Unit) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) = Unit
            override fun onAnimationEnd(animation: android.animation.Animator) = action()
            override fun onAnimationCancel(animation: android.animation.Animator) = Unit
            override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
        })
    }
}
