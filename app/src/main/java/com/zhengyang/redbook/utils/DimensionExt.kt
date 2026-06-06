/**
 * 文件说明： DimensionExt.kt
 * 作用： 提供跨模块复用的工具方法与扩展函数。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.utils

import android.content.res.Resources
import kotlin.math.roundToInt

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).roundToInt()
