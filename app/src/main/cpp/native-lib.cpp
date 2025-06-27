#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>
#include "llama.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "llm-native", __VA_ARGS__)

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

    // ✅ NEW API
    llama_model *model = llama_model_load_from_file(modelPath, mparams);
    llama_context *ctx = llama_init_from_model(model, cparams);

    const llama_vocab *vocab = llama_model_get_vocab(model);

    llama_token tokens[512];
    int n_tokens = llama_tokenize(
            vocab,
            prompt,
            strlen(prompt),
            tokens,
            512,
            true,   // add_special
            true    // parse_special
    );

    std::string result;
    char piece_buf[512];

    for (int i = 0; i < n_tokens; ++i) {
        int written = llama_token_to_piece(
                vocab,
                tokens[i],
                piece_buf,
                sizeof(piece_buf),
                0,
                true
        );
        if (written > 0 && written < sizeof(piece_buf)) {
            result.append(piece_buf, written);
        }
    }

    llama_free(ctx);
    llama_model_free(model); // ✅ NEW API

    env->ReleaseStringUTFChars(jPrompt, prompt);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_localllmapp_MainActivity_runMultimodalLlama(
        JNIEnv *env,
        jobject,
        jbyteArray imageData,
        jstring jPrompt,
        jstring jModelPath
) {
    const char *prompt = env->GetStringUTFChars(jPrompt, nullptr);
    const char *modelPath = env->GetStringUTFChars(jModelPath, nullptr);

    jbyte *imgBytes = env->GetByteArrayElements(imageData, nullptr);
    jsize imgLength = env->GetArrayLength(imageData);

    llama_model_params mparams = llama_model_default_params();
    llama_context_params cparams = llama_context_default_params();

    // ✅ NEW API
    llama_model *model = llama_model_load_from_file(modelPath, mparams);
    llama_context *ctx = llama_init_from_model(model, cparams);

    std::string result = "[image+text processing not implemented here]";

    env->ReleaseByteArrayElements(imageData, imgBytes, JNI_ABORT);
    env->ReleaseStringUTFChars(jPrompt, prompt);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    llama_free(ctx);
    llama_model_free(model); // ✅ NEW API

    return env->NewStringUTF(result.c_str());
}
