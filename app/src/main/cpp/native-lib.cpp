#include "llama.h"
#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#define LOG_TAG "LocalLLMApp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper function to convert jstring to std::string
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr) return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

// Helper function to load model from assets
std::string copyAssetToInternalStorage(JNIEnv *env, jobject context, const std::string& assetPath) {
    // Get AssetManager
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getAssetsMethod = env->GetMethodID(contextClass, "getAssets", "()Landroid/content/res/AssetManager;");
    jobject assetManager = env->CallObjectMethod(context, getAssetsMethod);

    // Get internal storage path
    jmethodID getFilesDirMethod = env->GetMethodID(contextClass, "getFilesDir", "()Ljava/io/File;");
    jobject filesDir = env->CallObjectMethod(context, getFilesDirMethod);
    jclass fileClass = env->GetObjectClass(filesDir);
    jmethodID getPathMethod = env->GetMethodID(fileClass, "getPath", "()Ljava/lang/String;");
    jstring pathJString = (jstring)env->CallObjectMethod(filesDir, getPathMethod);
    std::string internalPath = jstring2string(env, pathJString);

    // Extract filename from asset path
    size_t lastSlash = assetPath.find_last_of('/');
    std::string filename = (lastSlash != std::string::npos) ? assetPath.substr(lastSlash + 1) : assetPath;
    std::string destPath = internalPath + "/" + filename;

    // Open asset
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    AAsset* asset = AAssetManager_open(mgr, assetPath.c_str(), AASSET_MODE_STREAMING);
    if (!asset) {
        LOGE("Failed to open asset: %s", assetPath.c_str());
        return "";
    }

    // Copy to internal storage
    FILE* out = fopen(destPath.c_str(), "wb");
    if (!out) {
        LOGE("Failed to open output file: %s", destPath.c_str());
        AAsset_close(asset);
        return "";
    }

    char buf[BUFSIZ];
    int nb_read = 0;
    while ((nb_read = AAsset_read(asset, buf, BUFSIZ)) > 0) {
        fwrite(buf, nb_read, 1, out);
    }

    fclose(out);
    AAsset_close(asset);

    LOGI("Model copied to: %s", destPath.c_str());
    return destPath;
}

// Text generation with llama (simplified version)
std::string generateText(const std::string& prompt, const std::string& modelPath, int max_tokens = 512) {
    LOGI("Loading model from: %s", modelPath.c_str());

    // Initialize llama backend
    llama_backend_init();

    // Load model
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for mobile

    llama_model* model = llama_model_load_from_file(modelPath.c_str(), model_params);
    if (!model) {
        LOGE("Failed to load model");
        llama_backend_free();
        return "Error: Failed to load model";
    }

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4; // Adjust based on device

    llama_context* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_model_free(model);
        llama_backend_free();
        return "Error: Failed to create context";
    }

    // Get vocab
    const llama_vocab* vocab = llama_model_get_vocab(model);

    // Tokenize prompt
    std::vector<llama_token> tokens(prompt.length() + 32);
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens.data(), tokens.size(), true, false);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens.data(), tokens.size(), true, false);
    }
    tokens.resize(n_tokens);

    LOGI("Prompt tokenized to %d tokens", n_tokens);

    // Use the simpler batch creation method
    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);

    // Decode prompt
    if (llama_decode(ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        llama_free(ctx);
        llama_model_free(model);
        llama_backend_free();
        return "Error: Failed to decode prompt";
    }

    // Generate response
    std::string generated_text;
    int n_cur = batch.n_tokens;
    int n_decode = 0;

    // Setup sampling
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(1337));

    // Token buffer for generation
    llama_token new_token_id;

    while (n_decode < max_tokens) {
        // Sample next token
        new_token_id = llama_sampler_sample(smpl, ctx, -1);

        // Check for EOS
        if (llama_vocab_is_eog(vocab, new_token_id)) {
            break;
        }

        // Accept token
        llama_sampler_accept(smpl, new_token_id);

        // Convert token to text
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, false);
        if (n > 0) {
            generated_text.append(buf, n);
        }

        // Prepare next batch with single token
        batch = llama_batch_get_one(&new_token_id, 1);

        // Decode next token
        if (llama_decode(ctx, batch) != 0) {
            LOGE("Failed to decode token");
            break;
        }

        n_cur++;
        n_decode++;
    }

    LOGI("Generated %d tokens", n_decode);

    // Cleanup
    llama_sampler_free(smpl);
    llama_free(ctx);
    llama_model_free(model);
    llama_backend_free();

    return prompt + generated_text;
}

// Multimodal generation for Gemma-based models (simplified)
std::string generateMultimodal(const std::vector<uint8_t>& imageData, const std::string& prompt, const std::string& modelPath) {
    LOGI("Multimodal generation with Gemma model");

    // Initialize llama backend
    llama_backend_init();

    // Load model
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for mobile

    llama_model* model = llama_model_load_from_file(modelPath.c_str(), model_params);
    if (!model) {
        LOGE("Failed to load multimodal model");
        llama_backend_free();
        return "Error: Failed to load multimodal model";
    }

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 4096; // Larger context for multimodal
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;

    llama_context* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_model_free(model);
        llama_backend_free();
        return "Error: Failed to create context";
    }

    const llama_vocab* vocab = llama_model_get_vocab(model);

    // Format prompt for multimodal
    std::string formatted_prompt = "User: [Image provided] " + prompt + "\nAssistant:";

    // Tokenize prompt
    std::vector<llama_token> tokens(formatted_prompt.length() * 2);
    int n_tokens = llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.length(),
                                  tokens.data(), tokens.size(), true, false);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.length(),
                                  tokens.data(), tokens.size(), true, false);
    }
    tokens.resize(n_tokens);

    LOGI("Multimodal prompt tokenized to %d tokens", n_tokens);

    // Use the simpler batch creation
    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);

    // Decode
    if (llama_decode(ctx, batch) != 0) {
        LOGE("Failed to decode multimodal prompt");
        llama_free(ctx);
        llama_model_free(model);
        llama_backend_free();
        return "Error: Failed to decode prompt";
    }

    // Generate response
    std::string generated_text;
    int n_decode = 0;
    int max_tokens = 256;

    // Setup sampling
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(1337));

    llama_token new_token_id;

    while (n_decode < max_tokens) {
        new_token_id = llama_sampler_sample(smpl, ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token_id)) {
            break;
        }

        llama_sampler_accept(smpl, new_token_id);

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, false);
        if (n > 0) {
            generated_text.append(buf, n);
        }

        batch = llama_batch_get_one(&new_token_id, 1);

        if (llama_decode(ctx, batch) != 0) {
            break;
        }

        n_decode++;
    }

    LOGI("Generated %d tokens for multimodal response", n_decode);

    // Cleanup
    llama_sampler_free(smpl);
    llama_free(ctx);
    llama_model_free(model);
    llama_backend_free();

    // Return formatted response
    return "Image Analysis:\n" + generated_text +
           "\n\n[Note: Full multimodal processing requires vision encoder integration. " +
           "Image size: " + std::to_string(imageData.size()) + " bytes]";
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_localllmapp_MainActivity_runTextOnlyLlama(
        JNIEnv *env,
        jobject thiz,
        jstring prompt,
        jstring model_path) {

    std::string promptStr = jstring2string(env, prompt);
    std::string modelPathStr = jstring2string(env, model_path);

    // If the path starts with "C:", it's likely an asset path that needs correction
    if (modelPathStr.find("C:/") == 0 || modelPathStr.find("C:\\") == 0) {
        // Extract just the filename
        size_t lastSlash = modelPathStr.find_last_of("/\\");
        std::string filename = (lastSlash != std::string::npos) ? modelPathStr.substr(lastSlash + 1) : modelPathStr;

        // Copy from assets to internal storage
        modelPathStr = copyAssetToInternalStorage(env, thiz, filename);
        if (modelPathStr.empty()) {
            return env->NewStringUTF("Error: Failed to load model from assets");
        }
    }

    LOGI("Running text-only LLaMA with prompt: %s", promptStr.c_str());

    try {
        std::string result = generateText(promptStr, modelPathStr);
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& e) {
        LOGE("Exception: %s", e.what());
        return env->NewStringUTF(("Error: " + std::string(e.what())).c_str());
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_localllmapp_MainActivity_runMultimodalLlama(
        JNIEnv *env,
        jobject thiz,
        jbyteArray image_data,
        jstring prompt,
        jstring model_path) {

    // Convert inputs
    jsize imageLen = env->GetArrayLength(image_data);
    std::vector<uint8_t> imageVec(imageLen);
    env->GetByteArrayRegion(image_data, 0, imageLen, reinterpret_cast<jbyte*>(imageVec.data()));

    std::string promptStr = jstring2string(env, prompt);
    std::string modelPathStr = jstring2string(env, model_path);

    LOGI("Running multimodal LLaMA with image size: %d bytes", imageLen);

    try {
        std::string result = generateMultimodal(imageVec, promptStr, modelPathStr);
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& e) {
        LOGE("Exception: %s", e.what());
        return env->NewStringUTF(("Error: " + std::string(e.what())).c_str());
    }
}

} // extern "C"