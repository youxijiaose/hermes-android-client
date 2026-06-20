package com.hermes.client.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 崩溃日志写入工具 - 将异常写入 /sdcard/Download/hermes_crash.log
 */
object CrashLogWriter {
    
    private const val LOG_FILE_NAME = "hermes_crash.log"
    private const val TAG = "CrashLogWriter"
    
    fun init(context: Context) {
        val logFile = getLogFile(context)
        
        // 设置全局未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread: ${thread.name}", throwable)
            writeCrashLog(context, "UNCAUGHT_EXCEPTION", throwable)
            // 让系统默认处理器继续处理（显示崩溃对话框）
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
        
        // 写入启动日志
        writeLog(context, "APP_STARTED", "Hermes Client v1.0.0 started")
    }
    
    fun writeCrashLog(context: Context, errorType: String, throwable: Throwable) {
        val logFile = getLogFile(context)
        val writer = PrintWriter(FileWriter(logFile, true))
        
        writer.println("========================================")
        writer.println("CRASH REPORT - ${getTimestamp()}")
        writer.println("========================================")
        writer.println("Error Type: $errorType")
        writer.println("Message: ${throwable.message}")
        writer.println("Device: ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
        writer.println("Package: ${context.packageName}")
        writer.println("Process: ${getProcessName(context)}")
        writer.println("Thread: ${Thread.currentThread().name} (ID: ${Thread.currentThread().id})")
        writer.println("----------------------------------------")
        writer.println("Stack Trace:")
        throwable.printStackTrace(writer)
        writer.println("========================================")
        writer.println()
        writer.flush()
        writer.close()
    }
    
    fun writeLog(context: Context, tag: String, message: String) {
        val logFile = getLogFile(context)
        val writer = FileWriter(logFile, true)
        writer.println("[${getTimestamp()}] [$tag] $message")
        writer.flush()
        writer.close()
    }
    
    private fun getLogFile(context: Context): File {
        // 优先使用 /sdcard/Download/
        val downloadDir = File("/sdcard/Download/")
        if (downloadDir.exists() || downloadDir.mkdirs()) {
            return File(downloadDir, LOG_FILE_NAME)
        }
        
        // 备选：应用外部存储目录
        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir != null && (externalFilesDir.exists() || externalFilesDir.mkdirs())) {
            return File(externalFilesDir, LOG_FILE_NAME)
        }
        
        // 最后备选：应用内部存储
        val internalDir = context.filesDir
        return File(internalDir, LOG_FILE_NAME)
    }
    
    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA).format(Date())
    }
    
    private fun getProcessName(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Build.getProcessName()
        } else {
            // Fallback for older Android versions
            try {
                Class.forName("android.app.ActivityManager")
                    .getDeclaredMethod("getProcessName")
                    .invoke(null, context.packageName) as? String ?: context.packageName
            } catch (e: Exception) {
                context.packageName
            }
        }
    }
}
