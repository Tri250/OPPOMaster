//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENGL_ES

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <memory>
#include <sstream>
#include <stdexcept>
#include <vector>

#include <GLES3/gl3.h>

#include "edit/operators/op_base.hpp"
#include "edit/pipeline/pipeline_gpu_wrapper.hpp"
#include "image/image_buffer.hpp"
#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

namespace {

// ── GLES Shader Sources ─────────────────────────────────────────

// Vertex shader: full-screen triangle pair
const char* kGlesVertexShader = R"glsl(
#version 300 es
precision highp float;
layout(location = 0) in vec2 a_position;
out vec2 v_texCoord;
void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    v_texCoord = (a_position + 1.0) * 0.5;
}
)glsl";

// Basic color space conversion: linear → sRGB with exposure
const char* kGlesColorSpaceConvFrag = R"glsl(
#version 300 es
precision highp float;
precision highp sampler2D;
in vec2 v_texCoord;
out vec4 fragColor;
uniform sampler2D u_input;
uniform float u_exposure;
uniform float u_contrast;
uniform float u_saturation;

vec3 linearToSrgb(vec3 linear) {
    vec3 srgb_low = linear * 12.92;
    vec3 srgb_high = 1.055 * pow(linear, vec3(1.0 / 2.4)) - 0.055;
    vec3 selector = step(0.0031308, linear);
    return mix(srgb_low, srgb_high, selector);
}

void main() {
    vec4 px = texture(u_input, v_texCoord);
    vec3 color = px.rgb;

    // Exposure
    color *= pow(2.0, u_exposure);

    // Contrast (using Reinhard-style)
    float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(lum), color, u_contrast);

    // Saturation
    color = mix(vec3(lum), color, u_saturation);

    // Tone mapping: simple Reinhard
    color = color / (color + vec3(1.0));

    // Linear to sRGB
    color = linearToSrgb(color);

    fragColor = vec4(color, px.a);
}
)glsl";

// Tone mapping fragment shader
const char* kGlesToneMappingFrag = R"glsl(
#version 300 es
precision highp float;
precision highp sampler2D;
in vec2 v_texCoord;
out vec4 fragColor;
uniform sampler2D u_input;
uniform float u_highlight;
uniform float u_shadow;
uniform float u_white;
uniform float u_black;
uniform float u_vibrance;

// ACES Filmic tone mapping
vec3 acesFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 px = texture(u_input, v_texCoord);
    vec3 color = px.rgb;

    // White / Black point
    color = (color - u_black) / max(u_white - u_black, 0.001);

    // Highlight recovery
    float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float highlight_mask = smoothstep(0.5, 1.0, lum);
    color = mix(color, color * (1.0 - u_highlight * 0.5), highlight_mask);

    // Shadow recovery
    float shadow_mask = 1.0 - smoothstep(0.0, 0.5, lum);
    color = mix(color, color + u_shadow * 0.3, shadow_mask);

    // ACES tone mapping
    color = acesFilmic(color);

    // Vibrance (saturation weighted by low saturation)
    float max_c = max(color.r, max(color.g, color.b));
    float min_c = min(color.r, min(color.g, color.b));
    float sat = (max_c - min_c) / max(max_c, 0.001);
    color = mix(vec3(lum), color, 1.0 + u_vibrance * (1.0 - sat));

    fragColor = vec4(clamp(color, 0.0, 1.0), px.a);
}
)glsl";

// ── GLES Utility Functions ───────────────────────────────────────

auto CheckGlError(const char* operation) -> bool {
  GLint err = glGetError();
  if (err != GL_NO_ERROR) {
    APP_LOG_WARN_DEFAULT("GLES error after %s: 0x%x", operation, err);
    return true;
  }
  return false;
}

auto CompileShader(GLenum type, const char* source) -> GLuint {
  GLuint shader = glCreateShader(type);
  glShaderSource(shader, 1, &source, nullptr);
  glCompileShader(shader);

  GLint success = 0;
  glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
  if (!success) {
    GLchar log[512];
    glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
    APP_LOG_WARN_DEFAULT("GLES shader compile failed: %s", log);
    glDeleteShader(shader);
    return 0;
  }
  return shader;
}

auto LinkProgram(GLuint vert_shader, GLuint frag_shader) -> GLuint {
  GLuint program = glCreateProgram();
  glAttachShader(program, vert_shader);
  glAttachShader(program, frag_shader);
  glLinkProgram(program);

  GLint success = 0;
  glGetProgramiv(program, GL_LINK_STATUS, &success);
  if (!success) {
    GLchar log[512];
    glGetProgramInfoLog(program, sizeof(log), nullptr, log);
    APP_LOG_WARN_DEFAULT("GLES program link failed: %s", log);
    glDeleteProgram(program);
    return 0;
  }
  return program;
}

// ── GLESPipeline ─────────────────────────────────────────────────

class GLESPipeline final : public GPUPipelineImpl {
 public:
  GLESPipeline()
      : input_img_(nullptr),
        cpu_params_(nullptr),
        frame_sink_(nullptr),
        color_conv_program_(0),
        tone_mapping_program_(0),
        vao_(0),
        vbo_(0),
        input_texture_(0),
        intermediate_texture_(0),
        intermediate_fbo_(0),
        output_texture_(0),
        output_fbo_(0),
        width_(0),
        height_(0),
        initialized_(false) {}

  ~GLESPipeline() override { ReleaseResources(); }

  void SetInputImage(std::shared_ptr<ImageBuffer> input_img) override {
    input_img_ = std::move(input_img);
  }

  void SetParams(OperatorParams& params) override { cpu_params_ = &params; }

  void SetFrameSink(IFrameSink* frame_sink) override { frame_sink_ = frame_sink; }

  void Execute(std::shared_ptr<ImageBuffer> output) override {
    if (!input_img_) {
      throw std::runtime_error("GLES pipeline: input image is null.");
    }

    // Ensure input is on CPU
    if (!input_img_->cpu_data_valid_ && input_img_->gpu_data_valid_) {
      input_img_->SyncToCPU();
    }
    if (!input_img_->cpu_data_valid_) {
      throw std::runtime_error("GLES pipeline: input image has no valid data.");
    }

    // Initialize OpenGL resources on first execution
    if (!initialized_) {
      InitializeGL();
    }

    // Update image dimensions
    const auto& cpu_data = input_img_->GetCPUData();
    const int new_width = cpu_data.cols;
    const int new_height = cpu_data.rows;
    if (new_width != width_ || new_height != height_) {
      width_ = new_width;
      height_ = new_height;
      ResizeTextures();
    }

    // Upload input data to texture
    UploadInput();

    // Run color space conversion pass
    glUseProgram(color_conv_program_);
    glBindFramebuffer(GL_FRAMEBUFFER, intermediate_fbo_);
    glViewport(0, 0, width_, height_);

    // Set uniforms for color space conversion
    GLint u_exposure = glGetUniformLocation(color_conv_program_, "u_exposure");
    GLint u_contrast = glGetUniformLocation(color_conv_program_, "u_contrast");
    GLint u_saturation = glGetUniformLocation(color_conv_program_, "u_saturation");

    float exposure = 0.0f;
    float contrast = 1.0f;
    float saturation = 1.0f;
    if (cpu_params_) {
      exposure = cpu_params_->exposure_offset_;
      contrast = 1.0f + cpu_params_->contrast_scale_;
    }

    glUniform1f(u_exposure, exposure);
    glUniform1f(u_contrast, contrast);
    glUniform1f(u_saturation, saturation);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, input_texture_);
    glUniform1i(glGetUniformLocation(color_conv_program_, "u_input"), 0);

    DrawFullScreenQuad();

    // Run tone mapping pass
    glUseProgram(tone_mapping_program_);
    glBindFramebuffer(GL_FRAMEBUFFER, output_fbo_);
    glViewport(0, 0, width_, height_);

    // Set tone mapping uniforms
    float highlight = 0.0f;
    float shadow = 0.0f;
    float white = 1.0f;
    float black = 0.0f;
    float vibrance = 0.0f;
    if (cpu_params_) {
      shadow = cpu_params_->tone_mapping_.shadow_amount_;
      highlight = cpu_params_->tone_mapping_.highlight_amount_;
    }

    glUniform1f(glGetUniformLocation(tone_mapping_program_, "u_highlight"), highlight);
    glUniform1f(glGetUniformLocation(tone_mapping_program_, "u_shadow"), shadow);
    glUniform1f(glGetUniformLocation(tone_mapping_program_, "u_white"), white);
    glUniform1f(glGetUniformLocation(tone_mapping_program_, "u_black"), black);
    glUniform1f(glGetUniformLocation(tone_mapping_program_, "u_vibrance"), vibrance);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, intermediate_texture_);
    glUniform1i(glGetUniformLocation(tone_mapping_program_, "u_input"), 0);

    DrawFullScreenQuad();

    // Read back the result to CPU output
    glBindFramebuffer(GL_FRAMEBUFFER, output_fbo_);
    ReadBackOutput(output);

    // Restore default framebuffer
    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    // Mark output as CPU-valid
    output->cpu_data_valid_ = true;
    output->gpu_data_valid_ = false;
    output->SetGPUDataValid(false);
  }

  void ReleaseScratchBuffers() override {
    // Release intermediate textures
    if (intermediate_texture_) {
      glDeleteTextures(1, &intermediate_texture_);
      intermediate_texture_ = 0;
    }
    if (intermediate_fbo_) {
      glDeleteFramebuffers(1, &intermediate_fbo_);
      intermediate_fbo_ = 0;
    }
  }

  void ReleaseResources() override {
    if (color_conv_program_) {
      glDeleteProgram(color_conv_program_);
      color_conv_program_ = 0;
    }
    if (tone_mapping_program_) {
      glDeleteProgram(tone_mapping_program_);
      tone_mapping_program_ = 0;
    }
    if (input_texture_) {
      glDeleteTextures(1, &input_texture_);
      input_texture_ = 0;
    }
    if (output_texture_) {
      glDeleteTextures(1, &output_texture_);
      output_texture_ = 0;
    }
    if (output_fbo_) {
      glDeleteFramebuffers(1, &output_fbo_);
      output_fbo_ = 0;
    }
    if (vao_) {
      glDeleteVertexArrays(1, &vao_);
      vao_ = 0;
    }
    if (vbo_) {
      glDeleteBuffers(1, &vbo_);
      vbo_ = 0;
    }
    ReleaseScratchBuffers();
    initialized_ = false;
  }

  [[nodiscard]] auto DebugGetAllocatedScratchBytes() const -> size_t override {
    size_t bytes = 0;
    if (width_ > 0 && height_ > 0) {
      // RGBA32F = 16 bytes per pixel
      bytes += static_cast<size_t>(width_) * height_ * 16 * 3;  // 3 textures
    }
    return bytes;
  }

 private:
  void InitializeGL() {
    // Compile shaders and create programs
    GLuint vert = CompileShader(GL_VERTEX_SHADER, kGlesVertexShader);
    if (!vert) {
      throw std::runtime_error("GLES pipeline: failed to compile vertex shader.");
    }

    // Color space conversion program
    GLuint frag_cs = CompileShader(GL_FRAGMENT_SHADER, kGlesColorSpaceConvFrag);
    if (frag_cs) {
      color_conv_program_ = LinkProgram(vert, frag_cs);
      glDeleteShader(frag_cs);
    }

    // Tone mapping program
    GLuint frag_tm = CompileShader(GL_FRAGMENT_SHADER, kGlesToneMappingFrag);
    if (frag_tm) {
      tone_mapping_program_ = LinkProgram(vert, frag_tm);
      glDeleteShader(frag_tm);
    }

    glDeleteShader(vert);

    if (!color_conv_program_ || !tone_mapping_program_) {
      throw std::runtime_error("GLES pipeline: failed to create shader programs.");
    }

    // Create full-screen quad VAO/VBO
    // Two triangles covering the screen: (-1,-1), (1,-1), (-1,1), (-1,1), (1,-1), (1,1)
    static const float kQuadVertices[] = {
        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
        -1.0f,  1.0f, 1.0f, -1.0f,  1.0f, 1.0f,
    };

    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &vbo_);

    glBindVertexArray(vao_);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferData(GL_ARRAY_BUFFER, sizeof(kQuadVertices), kQuadVertices, GL_STATIC_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(float), nullptr);
    glBindVertexArray(0);

    // Create input texture
    glGenTextures(1, &input_texture_);
    glBindTexture(GL_TEXTURE_2D, input_texture_);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_2D, 0);

    initialized_ = true;
  }

  void ResizeTextures() {
    // Intermediate texture (RGBA32F for HDR data)
    if (intermediate_texture_) glDeleteTextures(1, &intermediate_texture_);
    if (intermediate_fbo_) glDeleteFramebuffers(1, &intermediate_fbo_);
    if (output_texture_) glDeleteTextures(1, &output_texture_);
    if (output_fbo_) glDeleteFramebuffers(1, &output_fbo_);

    // Intermediate texture
    glGenTextures(1, &intermediate_texture_);
    glBindTexture(GL_TEXTURE_2D, intermediate_texture_);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width_, height_, 0, GL_RGBA, GL_FLOAT, nullptr);

    glGenFramebuffers(1, &intermediate_fbo_);
    glBindFramebuffer(GL_FRAMEBUFFER, intermediate_fbo_);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                           intermediate_texture_, 0);

    // Output texture
    glGenTextures(1, &output_texture_);
    glBindTexture(GL_TEXTURE_2D, output_texture_);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width_, height_, 0, GL_RGBA, GL_FLOAT, nullptr);

    glGenFramebuffers(1, &output_fbo_);
    glBindFramebuffer(GL_FRAMEBUFFER, output_fbo_);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                           output_texture_, 0);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
  }

  void UploadInput() {
    // Get the CPU mat from the input image
    const auto& mat = input_img_->GetCPUData();
    glBindTexture(GL_TEXTURE_2D, input_texture_);
    // Upload as RGBA8 or RGBA32F depending on the mat type
    int mat_type = mat.type();
    if (mat_type == CV_32FC4) {
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width_, height_, 0,
                   GL_RGBA, GL_FLOAT, mat.data);
    } else if (mat_type == CV_8UC4) {
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width_, height_, 0,
                   GL_RGBA, GL_UNSIGNED_BYTE, mat.data);
    } else if (mat_type == CV_16UC4) {
      // Upload 16-bit as RGBA16F
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width_, height_, 0,
                   GL_RGBA, GL_UNSIGNED_SHORT, mat.data);
    } else {
      // Convert to 32FC4 and upload
      cv::Mat converted;
      mat.convertTo(converted, CV_32FC4, 1.0 / 255.0);
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width_, height_, 0,
                   GL_RGBA, GL_FLOAT, converted.data);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
  }

  void DrawFullScreenQuad() {
    glBindVertexArray(vao_);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);
  }

  void ReadBackOutput(std::shared_ptr<ImageBuffer> output) {
    // Read pixels from the output framebuffer
    auto& out_mat = output->GetCPUData();
    if (out_mat.empty() || out_mat.cols != width_ || out_mat.rows != height_) {
      out_mat.create(height_, width_, CV_32FC4);
    }

    glReadPixels(0, 0, width_, height_, GL_RGBA, GL_FLOAT, out_mat.data);

    // OpenGL reads bottom-to-top; flip vertically
    cv::flip(out_mat, out_mat, 0);
  }

 private:
  std::shared_ptr<ImageBuffer> input_img_;
  OperatorParams*              cpu_params_;
  IFrameSink*                  frame_sink_;

  GLuint  color_conv_program_     = 0;
  GLuint  tone_mapping_program_  = 0;
  GLuint  vao_                   = 0;
  GLuint  vbo_                   = 0;
  GLuint  input_texture_         = 0;
  GLuint  intermediate_texture_  = 0;
  GLuint  intermediate_fbo_      = 0;
  GLuint  output_texture_        = 0;
  GLuint  output_fbo_            = 0;

  int     width_                 = 0;
  int     height_                = 0;
  bool    initialized_           = false;
};

}  // namespace

auto CreateGlesGPUPipeline() -> std::unique_ptr<GPUPipelineImpl> {
  return std::make_unique<GLESPipeline>();
}

}  // namespace alcedo

#endif  // HAVE_OPENGL_ES
