package com.example.myapplication

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.appendText

object Logger {

    fun saveLog(context: Context, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date())

        val logMessage = "$time - $message\n"

        val file = File(context.filesDir, "activity_log.txt")
        file.appendText(logMessage)
    }
}
