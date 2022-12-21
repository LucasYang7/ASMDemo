package com.xiaozhezhe.asmdemo

import android.util.Log

class KtStringTest {
    companion object {
        const val TAG = "stringLog"
        const val LOG_CONTENT = "This is KtStringTest log content"
    }

    var testString = "testString"

    fun test(): String {
        Log.d(TAG, LOG_CONTENT)
        Log.d("stringLog2","This is KtStringTest log content2")
        return LOG_CONTENT
    }
}