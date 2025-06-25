# CMake generated Testfile for 
# Source directory: C:/Users/sakib/llama.cpp/examples/eval-callback
# Build directory: C:/Users/sakib/llama.cpp/build-android/examples/eval-callback
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(test-eval-callback "C:/Users/sakib/llama.cpp/build-android/bin/llama-eval-callback" "--hf-repo" "ggml-org/models" "--hf-file" "tinyllamas/stories260K.gguf" "--model" "stories260K.gguf" "--prompt" "hello" "--seed" "42" "-ngl" "0")
set_tests_properties(test-eval-callback PROPERTIES  LABELS "eval-callback;curl" _BACKTRACE_TRIPLES "C:/Users/sakib/llama.cpp/examples/eval-callback/CMakeLists.txt;8;add_test;C:/Users/sakib/llama.cpp/examples/eval-callback/CMakeLists.txt;0;")
