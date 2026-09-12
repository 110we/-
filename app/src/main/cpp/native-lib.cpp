#include <jni.h>
#include <string>
#include <sys/utsname.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_kalidroid_jni_NativeLib_version(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("kalidroid-jni-1.0.0-ops");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_kalidroid_jni_NativeLib_hostFingerprint(JNIEnv *env, jobject /* this */) {
    struct utsname u{};
    if (uname(&u) != 0) {
        return env->NewStringUTF("unknown");
    }
    std::string out = std::string(u.sysname) + " " + u.release + " " + u.machine;
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_kalidroid_jni_NativeLib_denyPrivilegedCall(JNIEnv *env, jobject /* this */, jstring name) {
    const char *raw = env->GetStringUTFChars(name, nullptr);
    std::string msg = std::string("JNI 拒绝特权调用: ") + (raw ? raw : "unknown");
    env->ReleaseStringUTFChars(name, raw);
    return env->NewStringUTF(msg.c_str());
}
