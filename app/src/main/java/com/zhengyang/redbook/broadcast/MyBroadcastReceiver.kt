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
