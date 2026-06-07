/**
 * 文件说明：DimensionExt.kt
 * 作用：定义当前文件在项目中的核心实现与职责。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.utils

import android.content.res.Resources
import kotlin.math.roundToInt

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).roundToInt()
