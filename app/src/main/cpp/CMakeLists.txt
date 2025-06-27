cmake_minimum_required(VERSION 3.22.1)
project("LocalLLMApp")

# Your JNI library
add_library(${PROJECT_NAME} SHARED
        native-lib.cpp
)

# Prebuilt llama.cpp library
add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES
        IMPORTED_LOCATION
        "C:/Users/sakib/llama.cpp/build-android/bin/libllama.so"
)

# Include llama headers
target_include_directories(${PROJECT_NAME} PRIVATE
        "C:/Users/sakib/llama.cpp/include"
        "C:/Users/sakib/llama.cpp/ggml/include"
)

# Link your native code against llama
target_link_libraries(${PROJECT_NAME}
        llama
        android
        log
)

set_target_properties(${PROJECT_NAME} PROPERTIES
        CXX_STANDARD 11
        CXX_STANDARD_REQUIRED ON
)
