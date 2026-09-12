package com.kalidroid.jni

object NativeLib {
    init {
        runCatching { System.loadLibrary("kalidroid") }
    }

    external fun version(): String
    external fun hostFingerprint(): String
    external fun denyPrivilegedCall(name: String): String
}
