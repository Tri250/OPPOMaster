# Compatibility test suite for AlcedoStudio
# Tests platform-specific features, RAW format support, and cross-platform behavior.

if(NOT ALCEDO_BUILD_COMPAT_TESTS)
  return()
endif()

set(ALCEDO_COMPAT_DIR "${CMAKE_CURRENT_SOURCE_DIR}")

# ---- Platform Compatibility Test ----
add_executable(PlatformCompatTest
    platform_compat_test.cpp
)
target_include_directories(PlatformCompatTest
    PUBLIC ${ALCEDO_COMPAT_DIR}
)
target_link_libraries(PlatformCompatTest
    PRIVATE
        GTest::gtest_main
)
gtest_discover_tests(PlatformCompatTest TEST_PREFIX "PlatformCompatTest.")

# ---- RAW Format Compatibility Test ----
add_executable(RawFormatCompatTest
    raw_format_test.cpp
)
target_include_directories(RawFormatCompatTest
    PUBLIC ${ALCEDO_COMPAT_DIR}
)
target_link_libraries(RawFormatCompatTest
    PRIVATE
        GTest::gtest_main
)
gtest_discover_tests(RawFormatCompatTest TEST_PREFIX "RawFormatCompatTest.")
