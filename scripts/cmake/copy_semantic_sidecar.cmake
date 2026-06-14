if(NOT DEFINED ALCEDO_MIND_BINARY OR ALCEDO_MIND_BINARY STREQUAL "")
    message(FATAL_ERROR "ALCEDO_MIND_BINARY is required")
endif()

if(NOT DEFINED ALCEDO_MIND_TARGET_DIR OR ALCEDO_MIND_TARGET_DIR STREQUAL "")
    message(FATAL_ERROR "ALCEDO_MIND_TARGET_DIR is required")
endif()

if(NOT DEFINED ALCEDO_MIND_DEST_DIR OR ALCEDO_MIND_DEST_DIR STREQUAL "")
    message(FATAL_ERROR "ALCEDO_MIND_DEST_DIR is required")
endif()

if(NOT EXISTS "${ALCEDO_MIND_BINARY}")
    message(FATAL_ERROR "Semantic sidecar binary was not built: ${ALCEDO_MIND_BINARY}")
endif()

file(MAKE_DIRECTORY "${ALCEDO_MIND_DEST_DIR}")
file(COPY "${ALCEDO_MIND_BINARY}" DESTINATION "${ALCEDO_MIND_DEST_DIR}")

file(GLOB _alcedo_mind_runtime_dlls "${ALCEDO_MIND_TARGET_DIR}/*.dll")
foreach(_alcedo_mind_runtime_dll IN LISTS _alcedo_mind_runtime_dlls)
    file(COPY "${_alcedo_mind_runtime_dll}" DESTINATION "${ALCEDO_MIND_DEST_DIR}")
endforeach()
