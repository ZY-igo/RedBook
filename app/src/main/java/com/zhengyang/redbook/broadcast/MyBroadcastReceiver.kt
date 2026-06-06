/**
 * 文件说明： MyBroadcastReceiver.kt
 * 作用： 定义当前源码文件的核心实现，承担对应功能模块中的结构声明或行为编排职责。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_CUSTOM_BROADCAST || context == null) {
            return
        }

        val buttonName = intent.getStringExtra(EXTRA_BUTTON_NAME).orEmpty()
        if (buttonName.isNotBlank()) {
            Toast.makeText(context, buttonName, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_CUSTOM_BROADCAST = "com.zhengyang.redbook.CUSTOM_ACTION"
        const val EXTRA_BUTTON_NAME = "buttonName"
    }
}
