/**
 * 文件说明：MyBroadcastReceiver.kt
 * 作用：处理系统或应用广播触发的 My Broadcast Receiver 相关逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
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
