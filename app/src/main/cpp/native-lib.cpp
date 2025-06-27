#include <jni.h>
#include <string>
#include <android/log.h>
#include "llama.h"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "llm-native", __VA_ARGS__)

// Text-only
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_localllmapp_MainActivity_runTextOnlyLlama(
        JNIEnv *env,
        jobject,
        jstring jPrompt,
        jstring jModelPath
) {
    const char *prompt = env->GetStringUTFChars(jPrompt, nullptr);
    const char *modelPath = env->GetStringUTFChars(jModelPath, nullptr);

    llama_model_params mparams = llama_model_default_params();
    llama_context_params cparams = llama_context_default_params();

    llama_model *model = llama_load_model_from_file(modelPath, mparams);
    llama_context *ctx = llama_new_context_with_model(model, cparams);

    std::string result;

    llama_token tokens[512];
    int n_tokens = llama_tokenize(ctx, prompt, tokens, 512, true);
    for (int i = 0; i < n_tokens; ++i) {
        const char *token_str = llama_token_to_str(ctx, tokens[i]);
        result += token_str;
    }

    llama_free(ctx);
    llama_free_model(model);

    env->ReleaseStringUTFChars(jPrompt, prompt);
    env->ReleaseStringUTFChars(jModelPath, modelPath);
    return env->NewStringUTF(result.c_str());
}


JNIEXPORT jstring JNICALL
Java_com_example_localllmapp_MainActivity_runMultimodalLlama(
        JNIEnv *env,
        jobject,
        jbyteArray imageData,
        jstring jPrompt,
        jstring jModelPath
) {
    // Get prompt
    const char *prompt = env->GetStringUTFChars(jPrompt, nullptr);
    const char *modelPath = env->GetStringUTFChars(jModelPath, nullptr);

    // Get image byte data
    jbyte *imgBytes = env->GetByteArrayElements(imageData, nullptr);
    jsize imgLength = env->GetArrayLength(imageData);

    // Load model
    llama_model_params mparams = llama_model_default_params();
    llama_context_params cparams = llama_context_default_params();
    llama_model *model = llama_load_model_from_file(modelPath, mparams);
    llama_context *ctx = llama_new_context_with_model(model, cparams);

    // Use image bytes with multimodal logic (requires llava or patch)
    // For now, just simulate response
    std::string result = "[image+text processing not implemented here]";

    // Cleanup
    env->ReleaseByteArrayElements(imageData, imgBytes, JNI_ABORT);
    env->ReleaseStringUTFChars(jPrompt, prompt);
    env->ReleaseStringUTFChars(jModelPath, modelPath);
    llama_free(ctx);
    llama_free_model(model);

    return env->NewStringUTF(result.c_str());
}
