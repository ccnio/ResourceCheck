package com.ccnio.plugin.utils

import java.text.SimpleDateFormat

class Logger {
    private static final enable = false
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")

    static void log(String tag, String msg) {
        if (enable) println(format.format(System.currentTimeMillis()) + " " + tag + ": " + msg)
    }

    static void log(String msg) {
        if (enable) println(format.format(System.currentTimeMillis()) + " " + msg)
    }
}
