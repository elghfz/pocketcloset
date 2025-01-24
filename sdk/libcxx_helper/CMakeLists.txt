cmake_minimum_required(VERSION 3.6)

project(opencv_jni_shared)

# Use libc++_shared as the standard C++ library
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -fexceptions -frtti")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -stdlib=libc++")

# Create a dummy library to include libc++_shared.so
add_library(opencv_jni_shared STATIC dummy.cpp)

# Link libc++_shared
target_link_libraries(opencv_jni_shared c++_shared)
