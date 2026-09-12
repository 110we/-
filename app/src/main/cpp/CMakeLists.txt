cmake_minimum_required(VERSION 3.22.1)
project("kalidroid")

add_library(kalidroid SHARED native-lib.cpp)
find_library(log-lib log)
target_link_libraries(kalidroid ${log-lib})
