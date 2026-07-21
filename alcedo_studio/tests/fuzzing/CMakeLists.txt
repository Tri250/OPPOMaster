# ── Fuzzing tests for AlcedoStudio ──────────────────────────────────────────
# Copyright 2026 Yurun Zi
# SPDX-License-Identifier: GPL-3.0-only

cmake_minimum_required(VERSION 3.20)

project(AlcedoStudioFuzzing LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ── Dependencies ─────────────────────────────────────────────────────────────

find_package(GTest REQUIRED)

# ── Fuzz test sources ────────────────────────────────────────────────────────

set(FUZZ_SOURCES
    fuzz_raw_decoder.cpp
    fuzz_image_pipeline.cpp
    fuzz_project_file.cpp
)

# ── Fuzz test executable ─────────────────────────────────────────────────────

add_executable(alcedo_fuzz_tests ${FUZZ_SOURCES})

target_link_libraries(alcedo_fuzz_tests
    PRIVATE
        GTest::GTest
        GTest::Main
)

target_include_directories(alcedo_fuzz_tests
    PRIVATE
        ${CMAKE_SOURCE_DIR}/src/include
)

# ── Compiler flags for fuzzing ──────────────────────────────────────────────

if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    target_compile_options(alcedo_fuzz_tests
        PRIVATE
            -Wall -Wextra -Wpedantic
            -Wno-unused-parameter
            # Address sanitizer for fuzzing — catches memory errors
            -fsanitize=address,undefined
            -fno-omit-frame-pointer
    )
    target_link_options(alcedo_fuzz_tests
        PRIVATE
            -fsanitize=address,undefined
    )
endif()

# ── Optional: libFuzzer integration ─────────────────────────────────────────
# If building with libFuzzer support, define ALBEDO_USE_LIBFUZZER and
# link against -fsanitize=fuzzer. This replaces the GTest entry point
# with libFuzzer's main() for continuous fuzzing.
#
# if(ALBEDO_USE_LIBFUZZER)
#     target_compile_options(alcedo_fuzz_tests PRIVATE -fsanitize=fuzzer)
#     target_link_options(alcedo_fuzz_tests PRIVATE -fsanitize=fuzzer)
# endif()

# ── Register with CTest ─────────────────────────────────────────────────────

include(GoogleTest)
gtest_discover_tests(alcedo_fuzz_tests
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    PROPERTIES
        TIMEOUT 60
        LABELS "fuzzing"
)
