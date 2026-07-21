# AlcedoStudio Performance & Memory Benchmarks
#
# These targets are NOT unit tests — they measure wall-clock timing and memory
# usage under realistic workloads.  Build Release for authoritative numbers:
#
#   cmake --build build --target PipelineBenchmark
#   cmake --build build --target MemoryBenchmark

set(ALCEDO_BENCH_DIR "${CMAKE_CURRENT_SOURCE_DIR}")
set(ALCEDO_SRC_DIR   "${CMAKE_CURRENT_LIST_DIR}/../../src")
set(ALCEDO_INCLUDE_DIR "${ALCEDO_SRC_DIR}/include")

# Common OpenCV deps shared by both benchmarks
set(ALCEDO_BENCH_OPENCV_DEPS
    opencv_core
    opencv_imgproc
    opencv_imgcodecs
)

# ---------------------------------------------------------------------------
# Pipeline Performance Benchmark
# ---------------------------------------------------------------------------
add_executable(PipelineBenchmark
    pipeline_benchmark.cpp
)
target_include_directories(PipelineBenchmark
    PUBLIC
        ${ALCEDO_INCLUDE_DIR}
        ${ALCEDO_BENCH_DIR}
)
target_link_libraries(PipelineBenchmark PRIVATE
    ${ALCEDO_BENCH_OPENCV_DEPS}
    Qt6::Core
)

# Enable full-project mode (real RAW decode, real AI inference) when the
# full application libraries are available.  Without it, the benchmarks
# use OpenCV-based simulations that still stress the same code paths.
if(TARGET RawProcessor)
    target_compile_definitions(PipelineBenchmark PRIVATE ALCEDO_FULL_PROJECT)
    target_link_libraries(PipelineBenchmark PRIVATE
        RawProcessor
        EditPipeline
    )
endif()

if(TARGET AiSidecarRuntimeService)
    target_link_libraries(PipelineBenchmark PRIVATE
        AiSidecarRuntimeService
    )
endif()

# ---------------------------------------------------------------------------
# Memory Benchmark
# ---------------------------------------------------------------------------
add_executable(MemoryBenchmark
    memory_benchmark.cpp
)
target_include_directories(MemoryBenchmark
    PUBLIC
        ${ALCEDO_INCLUDE_DIR}
        ${ALCEDO_BENCH_DIR}
)
target_link_libraries(MemoryBenchmark PRIVATE
    ${ALCEDO_BENCH_OPENCV_DEPS}
    Qt6::Core
    Threads::Threads
)

# ---------------------------------------------------------------------------
# Optional CUDA GPU timing support
# ---------------------------------------------------------------------------
if(ALCEDO_CUDA_ENABLED)
    target_compile_definitions(PipelineBenchmark PRIVATE HAVE_CUDA)
    target_compile_definitions(MemoryBenchmark PRIVATE HAVE_CUDA)
    target_link_libraries(PipelineBenchmark PRIVATE CUDA::cudart)
    target_link_libraries(MemoryBenchmark PRIVATE CUDA::cudart)
endif()

# ---------------------------------------------------------------------------
# Register with ctest label (opt-in, not run by default)
# ---------------------------------------------------------------------------
option(ALCEDO_ENABLE_BENCHMARK_TESTS
       "Register benchmark executables with ctest under the 'benchmark' label." OFF)

if(ALCEDO_ENABLE_BENCHMARK_TESTS)
    add_test(
        NAME PipelineBenchmark
        COMMAND $<TARGET_FILE:PipelineBenchmark>
    )
    set_tests_properties(PipelineBenchmark PROPERTIES
        LABELS "benchmark"
        TIMEOUT 3600
    )

    add_test(
        NAME MemoryBenchmark
        COMMAND $<TARGET_FILE:MemoryBenchmark>
    )
    set_tests_properties(MemoryBenchmark PROPERTIES
        LABELS "benchmark"
        TIMEOUT 3600
    )
endif()

# Add to the top-level benchmark custom target
if(NOT TARGET alcedo_benchmarks_all)
    add_custom_target(alcedo_benchmarks_all)
endif()
add_dependencies(alcedo_benchmarks_all PipelineBenchmark MemoryBenchmark)
