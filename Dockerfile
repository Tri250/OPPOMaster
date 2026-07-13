# =============================================================================
# Alcedo Studio - C++20 RAW Image Editor
# Multi-stage Dockerfile for reproducible builds
# =============================================================================
# 构建阶段: ubuntu:24.04 (包含 GCC 13, CMake 3.28+, 绝大多数依赖可直接 apt 安装)
# 运行阶段: 最小化运行时镜像
# =============================================================================

# ---------------------- 构建阶段 ----------------------
FROM ubuntu:24.04 AS builder

LABEL maintainer="Alcedo Studio Team"
LABEL description="Alcedo Studio - C++20 RAW Image Editor (Build Stage)"
LABEL stage="builder"

# 禁用交互式安装提示，避免 tzdata 等包阻塞构建
ENV DEBIAN_FRONTEND=noninteractive

# 安装基础构建工具与编译器
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    ninja-build \
    g++-13 \
    gcc-13 \
    pkg-config \
    ca-certificates \
    git \
  && rm -rf /var/lib/apt/lists/*

# 设置 GCC 13 为默认编译器 (Ubuntu 24.04 默认即 GCC 13，此处显式设置以确保一致)
RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-13 100 \
  && update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-13 100

# 安装 Qt6 及相关模块
RUN apt-get update && apt-get install -y --no-install-recommends \
    qt6-base-dev \
    qt6-tools-dev \
    qt6-tools-dev-tools \
    qt6-declarative-dev \
    qt6-svg-dev \
    qt6-wayland \
    qt6-l10n-tools \
    libgl-dev \
    libvulkan-dev \
    libxkbcommon-dev \
  && rm -rf /var/lib/apt/lists/*

# 安装图像处理与色彩管理依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libopencv-dev \
    libopencolorio-dev \
    libopenimageio-dev \
    libexiv2-dev \
    liblcms2-dev \
  && rm -rf /var/lib/apt/lists/*

# 安装数据库、数学与工具库
RUN apt-get update && apt-get install -y --no-install-recommends \
    libduckdb-dev \
    libxxhash-dev \
    libeigen3-dev \
    libhwy-dev \
    libglib2.0-dev \
  && rm -rf /var/lib/apt/lists/*

# 安装 protobuf / grpc 与 OpenMP
RUN apt-get update && apt-get install -y --no-install-recommends \
    libprotobuf-dev \
    protobuf-compiler \
    libgrpc++-dev \
    protobuf-compiler-grpc \
    libomp-dev \
  && rm -rf /var/lib/apt/lists/*

# 复制源代码
WORKDIR /src
COPY . .

# CMake 配置与构建
# --preset 选项可替换为项目实际预设名称，如 release 或 default
# 此处使用通用 Ninja 配置，适配大多数 CMakePresets.json 定义
RUN cmake -B build -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX=/opt/alcedo-studio \
  && cmake --build build --parallel "$(nproc)"

# 安装到临时目录，供运行时阶段拷贝
RUN cmake --install build

# ---------------------- 运行时阶段 ----------------------
FROM ubuntu:24.04 AS runtime

LABEL maintainer="Alcedo Studio Team"
LABEL description="Alcedo Studio - C++20 RAW Image Editor (Runtime)"
LABEL org.opencontainers.image.title="Alcedo Studio"
LABEL org.opencontainers.image.version="1.0.0"

ENV DEBIAN_FRONTEND=noninteractive

# 仅安装运行时必需的共享库（不含 -dev 包）
RUN apt-get update && apt-get install -y --no-install-recommends \
    libqt6core6 \
    libqt6gui6 \
    libqt6widgets6 \
    libqt6qml6 \
    libqt6quick6 \
    libqt6network6 \
    libqt6svg6 \
    libqt6opengl6 \
    libqt6waylandclient6 \
    libgl1 \
    libvulkan1 \
    libxkbcommon0 \
    libopencv-core406t64 \
    libopencv-imgproc406t64 \
    libopencv-imgcodecs406t64 \
    libopencolorio2.3t64 \
    libopenimageio2.5t64 \
    libexiv2-27 \
    liblcms2-2 \
    libduckdb-0.10.0 \
    libxxhash0.8.2 \
    libhwy1t64 \
    libglib2.0-0 \
    libprotobuf32t64 \
    libgrpc++1.51 \
    libomp5 \
    libgcc-s1 \
    libstdc++6 \
  && rm -rf /var/lib/apt/lists/*

# 从构建阶段拷贝安装产物
COPY --from=builder /opt/alcedo-studio /opt/alcedo-studio

# 设置可执行文件路径
ENV PATH="/opt/alcedo-studio/bin:${PATH}"
ENV LD_LIBRARY_PATH="/opt/alcedo-studio/lib:${LD_LIBRARY_PATH}"

# 设置工作目录
WORKDIR /opt/alcedo-studio

# 默认入口
ENTRYPOINT ["alcedo-studio"]
CMD []
