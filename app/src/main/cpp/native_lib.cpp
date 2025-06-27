// src/main/cpp/native-lib.cpp

#include <jni.h> // Essential JNI header
#include <string>
#include <android/log.h> // For Android logging

// Define a tag for logging, similar to Java's Log.d()
#define LOG_TAG "MyNativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// This is the implementation of your native method declared in Java/Kotlin.
// The function name follows a specific JNI convention:
// Java_com_yourpackage_yourapp_YourClass_yourMethodName
// (Replace com_yourpackage_yourapp_YourClass with your actual package and class name)
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_llmapp_MainActivity_stringFromJNI(
JNIEnv* env, // Pointer to the JNI environment, used to interact with Java
jobject /* this */) { // Reference to the Java object that called this method

std::string hello = "Hello from C++ (JNI)!";
LOGD("Native method 'stringFromJNI' called. Returning: %s", hello.c_str());
return env->NewStringUTF(hello.c_str());
}

// You can add more native functions here as needed for llama.cpp integration.
// For example:
// extern "C" JNIEXPORT jlong JNICALL
// Java_com_example_llmapp_MainActivity_loadModel(JNIEnv* env, jobject thiz, jstring modelPath) {
//     // Implementation to load llama.cpp model
//     // Return a pointer to the loaded model as a jlong
// }

// extern "C" JNIEXPORT jstring JNICALL
// Java_com_example_llmapp_MainActivity_generateText(JNIEnv* env, jobject thiz, jlong modelPtr, jstring prompt) {
//     // Implementation to generate text using llama.cpp
// }