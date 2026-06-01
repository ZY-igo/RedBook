package com.zhengyang.redbook

import androidx.appcompat.app.AppCompatDelegate

class RedBookApplication : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
