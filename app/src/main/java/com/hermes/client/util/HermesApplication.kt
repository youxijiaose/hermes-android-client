package com.hermes.client.util

import android.app.Application
import android.util.Log

class HermesApplication : Application() {

    companion object {
        private const val TAG = "HermesApplication"
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // 初始化崩溃日志系统（使用 Application 上下文，避免 Activity 相关崩溃）
        try {
            CrashLogWriter.init(this)
            Log.d(TAG, "CrashLogWriter initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CrashLogWriter", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HermesApplication created")
    }
}
