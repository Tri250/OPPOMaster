# Resolves the aria2c executable for the current platform into the cache
# variable ALCEDO_ARIA2C_BINARY. The ModelDownloadService spawns this binary
# as an --enable-rpc daemon to accelerate Hugging Face model downloads.
#
# Official aria2 releases only ship a Windows binary (1.37.0) and source; the
# last prebuilt macOS build is 1.35.0 (x86_64, runs under Rosetta on Apple
# Silicon). We fetch the prebuilt binary per platform so end users do not need
# to install aria2 themselves.

set(_aria2_stamp_dir "${CMAKE_BINARY_DIR}/aria2-download")
file(MAKE_DIRECTORY "${_aria2_stamp_dir}")

if(WIN32)
    set(ALCEDO_ARIA2C_VERSION "1.37.0")
    set(_aria2_url "https://github.com/aria2/aria2/releases/download/release-${ALCEDO_ARIA2C_VERSION}/aria2-${ALCEDO_ARIA2C_VERSION}-win-64bit-build1.zip")
    set(_aria2_archive "${_aria2_stamp_dir}/aria2-${ALCEDO_ARIA2C_VERSION}-win-64bit.zip")
    set(_aria2_extracted "${_aria2_stamp_dir}/aria2-win")
    set(_aria2_glob_names "aria2c.exe")
else()
    set(ALCEDO_ARIA2C_VERSION "1.35.0")
    set(_aria2_url "https://github.com/aria2/aria2/releases/download/release-${ALCEDO_ARIA2C_VERSION}/aria2-${ALCEDO_ARIA2C_VERSION}-osx-darwin.tar.bz2")
    set(_aria2_archive "${_aria2_stamp_dir}/aria2-${ALCEDO_ARIA2C_VERSION}-osx.tar.bz2")
    set(_aria2_extracted "${_aria2_stamp_dir}/aria2-osx")
    set(_aria2_glob_names "aria2c")
endif()

option(ALCEDO_BUILD_ARIA2C_FETCH
    "Download and bundle aria2c next to alcedo_main for model downloads." ON)

if(NOT ALCEDO_BUILD_ARIA2C_FETCH)
    return()
endif()

# Allow a manual override (e.g. a system aria2c or a pre-placed binary).
if(DEFINED ALCEDO_ARIA2C_BINARY AND EXISTS "${ALCEDO_ARIA2C_BINARY}")
    return()
endif()

set(_aria2_resolved_binary "")

if(EXISTS "${_aria2_extracted}")
    file(GLOB_RECURSE _aria2_candidates
        "${_aria2_extracted}/${_aria2_glob_names}"
        "${_aria2_extracted}/*/${_aria2_glob_names}")
    if(_aria2_candidates)
        list(GET _aria2_candidates 0 _aria2_resolved_binary)
    endif()
endif()

if(_aria2_resolved_binary STREQUAL "")
    message(STATUS "Fetching aria2c ${ALCEDO_ARIA2C_VERSION} for ${CMAKE_SYSTEM_NAME}")
    if(NOT EXISTS "${_aria2_archive}")
        file(DOWNLOAD "${_aria2_url}" "${_aria2_archive}"
            STATUS _aria2_dl_status SHOW_PROGRESS)
        list(GET _aria2_dl_status 0 _aria2_dl_rc)
        if(_aria2_dl_rc)
            list(GET _aria2_dl_status 1 _aria2_dl_msg)
            message(WARNING
                "Failed to download aria2c (${_aria2_dl_msg}). Models will not be downloadable. "
                "Set ALCEDO_ARIA2C_BINARY to a local aria2c or disable ALCEDO_BUILD_ARIA2C_FETCH.")
            unset(ALCEDO_ARIA2C_BINARY CACHE)
            return()
        endif()
    endif()
    file(REMOVE_RECURSE "${_aria2_extracted}")
    file(ARCHIVE_EXTRACT INPUT "${_aria2_archive}" DESTINATION "${_aria2_extracted}")
    file(GLOB_RECURSE _aria2_candidates
        "${_aria2_extracted}/${_aria2_glob_names}"
        "${_aria2_extracted}/*/${_aria2_glob_names}")
    if(_aria2_candidates)
        list(GET _aria2_candidates 0 _aria2_resolved_binary)
    endif()
endif()

if(_aria2_resolved_binary STREQUAL "")
    message(WARNING "aria2c binary was not found after extracting ${_aria2_archive}.")
    unset(ALCEDO_ARIA2C_BINARY CACHE)
    return()
endif()

set(ALCEDO_ARIA2C_BINARY "${_aria2_resolved_binary}" CACHE FILEPATH
    "Path to the aria2c binary bundled next to alcedo_main.")
message(STATUS "aria2c binary: ${ALCEDO_ARIA2C_BINARY}")
