# src/main/cpp/CMakeLists.txt

# Specifies the minimum version of CMake required to build your native library.
cmake_minimum_required(VERSION 3.22.1) # Match the version in build.gradle.kts

# Define your project name. This is good practice.
project(AndroidLLMApp CXX) # CXX indicates it's a C++ project

# Defines a name for your native library. This name will be used when loading the library in Java.
# Example: System.loadLibrary("my_native_lib");
set(MY_NATIVE_LIB_NAME "my_native_lib")

# Adds a native library.
# The first argument is the name of the library (must match the name in System.loadLibrary).
# The second argument specifies the type of library (SHARED for a .so file).
# The remaining arguments are the source files for your library.
add_library(
    ${MY_NATIVE_LIB_NAME}
    SHARED
    # --- IMPORTANT: Verify this path and filename ---
    native_lib.cpp
    # Add other C/C++ files here, e.g., from llama.cpp
    # ../../../../llama.cpp/llama.cpp
    # ../../../../llama.cpp/ggml.c
)

# Searches for the Android log library and links it.
# This is useful for debugging native code using logcat.
find_library(
    log-lib
    log
)

# Links your native library to other libraries.
# The first argument is the target library (your native library).
# The remaining arguments are the libraries to link against.
target_link_libraries(
    ${MY_NATIVE_LIB_NAME}
    # Links the log library for logging from native code.
    ${log-lib}
    # Add other libraries here, e.g., from llama.cpp or system libraries
    # -lm # For math functions
    # -landroid # For Android-specific native APIs
)

# Optional: Set C++ standard
set(CMAKE_CXX_STANDARD 17) # Or 11, 14, 20 depending on your needs
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optional: Add include directories for header files
# include_directories(
#     ${CMAKE_CURRENT_SOURCE_DIR}/path/to/llama.cpp/include
# )