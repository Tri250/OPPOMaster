//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef Q_OS_ANDROID

#include "utils/android/android_platform.hpp"

#include <QJniObject>
#include <QJniEnvironment>
#include <QAndroidJniEnvironment>
#include <QtAndroid>
#include <QDebug>
#include <QUrl>
#include <QFileInfo>

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

#include <cstring>

#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO,  "AlcedoStudio", __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN,  "AlcedoStudio", __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, "AlcedoStudio", __VA_ARGS__)

namespace alcedo::android {

auto AndroidPlatform::Instance() -> AndroidPlatform& {
  static AndroidPlatform instance;
  return instance;
}

auto AndroidPlatform::GetNativeWindow() -> ANativeWindow* {
  // Obtain the native window from the Qt Android activity's surface.
  // Qt6 provides the surface via QAndroidJniEnvironment.
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGW("AndroidPlatform::GetNativeWindow: failed to get current activity");
    // Try getting the window from the default surface
    QJniObject window_service = QJniObject::callStaticObjectMethod(
        "org/qtproject/qt/android/bindings/QtActivity",
        "currentActivity",
        "()Landroid/app/Activity;");
    return nullptr;
  }

  // Get the window token from the activity
  QJniObject window = activity.callObjectMethod(
      "getWindow",
      "()Landroid/view/Window;");

  if (!window.isValid()) {
    ALOGW("AndroidPlatform::GetNativeWindow: failed to get window from activity");
    return nullptr;
  }

  // Get the DecorView's window token for ANativeWindow
  QJniObject decor_view = window.callObjectMethod(
      "getDecorView",
      "()Landroid/view/View;");

  if (!decor_view.isValid()) {
    ALOGW("AndroidPlatform::GetNativeWindow: failed to get decor view");
    return nullptr;
  }

  // Get the Surface from the view's window
  QJniObject surface_holder = window.callObjectMethod(
      "getHolder",
      "()Landroid/view/SurfaceHolder;");

  if (!surface_holder.isValid()) {
    ALOGW("AndroidPlatform::GetNativeWindow: failed to get SurfaceHolder");
    return nullptr;
  }

  QJniObject surface = surface_holder.callObjectMethod(
      "getSurface",
      "()Landroid/view/Surface;");

  if (!surface.isValid()) {
    ALOGW("AndroidPlatform::GetNativeWindow: failed to get Surface");
    return nullptr;
  }

  QAndroidJniEnvironment env;
  ANativeWindow* native_window = ANativeWindow_fromSurface(env, surface.object());
  if (!native_window) {
    ALOGW("AndroidPlatform::GetNativeWindow: ANativeWindow_fromSurface returned null");
  }

  return native_window;
}

auto AndroidPlatform::GetInternalStoragePath() -> std::string {
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGE("AndroidPlatform::GetInternalStoragePath: failed to get activity");
    return {};
  }

  QJniObject files_dir = activity.callObjectMethod(
      "getFilesDir",
      "()Ljava/io/File;");

  if (!files_dir.isValid()) {
    ALOGE("AndroidPlatform::GetInternalStoragePath: getFilesDir() returned null");
    return {};
  }

  QJniObject path = files_dir.callObjectMethod(
      "getAbsolutePath",
      "()Ljava/lang/String;");

  if (!path.isValid()) {
    ALOGE("AndroidPlatform::GetInternalStoragePath: getAbsolutePath() returned null");
    return {};
  }

  return path.toString().toStdString();
}

auto AndroidPlatform::GetExternalStoragePath() -> std::string {
  // Use Environment.getExternalStorageDirectory() for shared storage
  QJniObject env_class = QJniObject::callStaticObjectMethod(
      "android/os/Environment",
      "getExternalStorageDirectory",
      "()Ljava/io/File;");

  if (!env_class.isValid()) {
    ALOGE("AndroidPlatform::GetExternalStoragePath: Environment.getExternalStorageDirectory() failed");
    return {};
  }

  QJniObject path = env_class.callObjectMethod(
      "getAbsolutePath",
      "()Ljava/lang/String;");

  if (!path.isValid()) {
    ALOGE("AndroidPlatform::GetExternalStoragePath: getAbsolutePath() returned null");
    return {};
  }

  return path.toString().toStdString();
}

auto AndroidPlatform::GetCachePath() -> std::string {
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGE("AndroidPlatform::GetCachePath: failed to get activity");
    return {};
  }

  QJniObject cache_dir = activity.callObjectMethod(
      "getCacheDir",
      "()Ljava/io/File;");

  if (!cache_dir.isValid()) {
    ALOGE("AndroidPlatform::GetCachePath: getCacheDir() returned null");
    return {};
  }

  QJniObject path = cache_dir.callObjectMethod(
      "getAbsolutePath",
      "()Ljava/lang/String;");

  if (!path.isValid()) {
    ALOGE("AndroidPlatform::GetCachePath: getAbsolutePath() returned null");
    return {};
  }

  return path.toString().toStdString();
}

auto AndroidPlatform::RequestStoragePermission() -> bool {
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGE("AndroidPlatform::RequestStoragePermission: failed to get activity");
    return false;
  }

  // Check if permission is already granted
  QJniObject context = activity.callObjectMethod(
      "getApplicationContext",
      "()Landroid/content/Context;");

  if (!context.isValid()) {
    ALOGE("AndroidPlatform::RequestStoragePermission: failed to get context");
    return false;
  }

  // Determine which permission to check based on API level
  QJniObject build_version = QJniObject::getStaticObjectField(
      "android/os/Build$VERSION",
      "SDK_INT",
      "I");

  // For API 33+, use READ_MEDIA_IMAGES; for older, use READ_EXTERNAL_STORAGE
  QString permission;
  int sdk_int = build_version.isValid() ? build_version.toInt() : 0;
  if (sdk_int >= 33) {
    permission = QStringLiteral("android.permission.READ_MEDIA_IMAGES");
  } else {
    permission = QStringLiteral("android.permission.READ_EXTERNAL_STORAGE");
  }

  // Check if the permission is already granted
  QJniObject result = context.callObjectMethod(
      "checkSelfPermission",
      "(Ljava/lang/String;)I",
      QJniObject::fromString(permission).object());

  if (result.isValid() && result.toInt() == 0) {
    // PERMISSION_GRANTED = 0
    ALOGI("Storage permission already granted");
    return true;
  }

  // Request the permission
  QJniObject permission_string = QJniObject::fromString(permission);
  QJniObject permission_array = QJniObject::callStaticObjectMethod(
      "java/lang/reflect/Array",
      "newInstance",
      "(Ljava/lang/Class;I)Ljava/lang/Object;",
      QJniObject::getStaticObjectField(
          "java/lang/String",
          "class",
          "Ljava/lang/Class;").object(),
      1);

  QAndroidJniEnvironment env;
  jobjectArray arr = permission_array.object<jobjectArray>();
  env->SetObjectArrayElement(arr, 0, permission_string.object<jstring>());

  activity.callMethod<void>(
      "requestPermissions",
      "([Ljava/lang/String;I)V",
      arr,
      1001);  // Request code

  ALOGI("Storage permission requested: %s", permission.toUtf8().constData());

  // Note: The result comes asynchronously via onRequestPermissionsResult.
  // For synchronous check, re-verify the permission state.
  QJniObject result_after = context.callObjectMethod(
      "checkSelfPermission",
      "(Ljava/lang/String;)I",
      QJniObject::fromString(permission).object());

  return result_after.isValid() && result_after.toInt() == 0;
}

auto AndroidPlatform::GetDisplayInfo() -> DisplayInfo {
  DisplayInfo info;

  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGE("AndroidPlatform::GetDisplayInfo: failed to get activity");
    return info;
  }

  // Get display from WindowManager
  QJniObject window_manager = activity.callObjectMethod(
      "getSystemService",
      "(Ljava/lang/String;)Ljava/lang/Object;",
      QJniObject::fromString(QStringLiteral("window")).object());

  if (!window_manager.isValid()) {
    // Try getWindowManager() instead
    QJniObject wm = activity.callObjectMethod(
        "getWindowManager",
        "()Landroid/view/WindowManager;");
    window_manager = wm;
  }

  if (!window_manager.isValid()) {
    ALOGE("AndroidPlatform::GetDisplayInfo: failed to get WindowManager");
    return info;
  }

  QJniObject display = window_manager.callObjectMethod(
      "getDefaultDisplay",
      "()Landroid/view/Display;");

  if (!display.isValid()) {
    ALOGE("AndroidPlatform::GetDisplayInfo: failed to get default display");
    return info;
  }

  // Get display metrics
  QJniObject metrics = QJniObject("android/util/DisplayMetrics");
  display.callMethod<void>(
      "getMetrics",
      "(Landroid/util/DisplayMetrics;)V",
      metrics.object());

  if (metrics.isValid()) {
    info.density_dpi = metrics.getField<float>("densityDpi");
    info.density_scale = metrics.getField<float>("density");

    // Get refresh rate from the Display object
    QJniObject refresh_rate = display.callObjectMethod(
        "getRefreshRate",
        "()F");
    if (refresh_rate.isValid()) {
      // getRefreshRate returns a float directly
      info.refresh_rate_hz = display.callMethod<float>("getRefreshRate");
    }
  }

  // Get real display metrics for accurate size
  QJniObject real_metrics = QJniObject("android/util/DisplayMetrics");
  QJniObject display_obj = window_manager.callObjectMethod(
      "getDefaultDisplay",
      "()Landroid/view/Display;");

  if (display_obj.isValid()) {
    display_obj.callMethod<void>(
        "getRealMetrics",
        "(Landroid/util/DisplayMetrics;)V",
        real_metrics.object());

    if (real_metrics.isValid()) {
      info.width_pixels = real_metrics.getField<int>("widthPixels");
      info.height_pixels = real_metrics.getField<int>("heightPixels");
    }
  }

  return info;
}

auto AndroidPlatform::IsHdrSupported() -> bool {
  // Check HDR support via Display.HdrCapabilities (API 24+)
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    return false;
  }

  QJniObject window_manager = activity.callObjectMethod(
      "getWindowManager",
      "()Landroid/view/WindowManager;");

  if (!window_manager.isValid()) {
    return false;
  }

  QJniObject display = window_manager.callObjectMethod(
      "getDefaultDisplay",
      "()Landroid/view/Display;");

  if (!display.isValid()) {
    return false;
  }

  // Display.isHdr() requires API 26+
  QJniObject build_version = QJniObject::getStaticObjectField(
      "android/os/Build$VERSION",
      "SDK_INT",
      "I");
  int sdk_int = build_version.isValid() ? build_version.toInt() : 0;

  if (sdk_int >= 26) {
    // Check if any HDR capability is supported
    QJniObject hdr_caps = display.callObjectMethod(
        "getHdrCapabilities",
        "()Landroid/view/Display$HdrCapabilities;");

    if (hdr_caps.isValid()) {
      // getSupportedHdrTypes returns int[], non-empty means HDR is supported
      QJniObject types = hdr_caps.callObjectMethod(
          "getSupportedHdrTypes",
          "()[I");

      if (types.isValid()) {
        QAndroidJniEnvironment env;
        jintArray arr = types.object<jintArray>();
        jsize len = env->GetArrayLength(arr);
        return len > 0;
      }
    }
  }

  return false;
}

void AndroidPlatform::Vibrate(int duration_ms) {
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGW("AndroidPlatform::Vibrate: failed to get activity");
    return;
  }

  QJniObject vibrator = activity.callObjectMethod(
      "getSystemService",
      "(Ljava/lang/String;)Ljava/lang/Object;",
      QJniObject::fromString(QStringLiteral("vibrator")).object());

  if (!vibrator.isValid()) {
    ALOGW("AndroidPlatform::Vibrate: Vibrator service not available");
    return;
  }

  // Check if vibrator has amplitude control (API 26+)
  QJniObject build_version = QJniObject::getStaticObjectField(
      "android/os/Build$VERSION",
      "SDK_INT",
      "I");
  int sdk_int = build_version.isValid() ? build_version.toInt() : 0;

  if (sdk_int >= 26) {
    // Use vibrate(VibrationEffect) for API 26+
    QJniObject effect = QJniObject::callStaticObjectMethod(
        "android/os/VibrationEffect",
        "createOneShot",
        "(JI)Landroid/os/VibrationEffect;",
        static_cast<jlong>(duration_ms),
        -1);  // DEFAULT_AMPLITUDE

    if (effect.isValid()) {
      vibrator.callMethod<void>(
          "vibrate",
          "(Landroid/os/VibrationEffect;)V",
          effect.object());
    }
  } else {
    // Deprecated but works on API < 26
    vibrator.callMethod<void>(
        "vibrate",
        "(J)V",
        static_cast<jlong>(duration_ms));
  }
}

void AndroidPlatform::ShareImage(const std::string& file_path) {
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGE("AndroidPlatform::ShareImage: failed to get activity");
    return;
  }

  // Build an ACTION_SEND intent
  QJniObject intent("android/content/Intent");
  intent.callObjectMethod(
      "setAction",
      "(Ljava/lang/String;)Landroid/content/Intent;",
      QJniObject::fromString(QStringLiteral("android.intent.action.SEND")).object());

  intent.callObjectMethod(
      "setType",
      "(Ljava/lang/String;)Landroid/content/Intent;",
      QJniObject::fromString(QStringLiteral("image/*")).object());

  // Create a content URI using FileProvider
  QJniObject file = QJniObject::callStaticObjectMethod(
      "java/io/File",
      "<init>",
      "(Ljava/lang/String;)V",
      QJniObject::fromString(QString::fromStdString(file_path)).object());

  if (!file.isValid()) {
    ALOGE("AndroidPlatform::ShareImage: failed to create File object");
    return;
  }

  // Use FileProvider to get a content:// URI
  QJniObject authority = QJniObject::fromString(
      QStringLiteral("studio.alcedo.AlcedoStudio.fileprovider"));

  QJniObject uri = QJniObject::callStaticObjectMethod(
      "androidx/core/content/FileProvider",
      "getUriForFile",
      "(Landroid/content/Context;Ljava/lang/String;Ljava/io/File;)Landroid/net/Uri;",
      activity.object(),
      authority.object(),
      file.object());

  if (!uri.isValid()) {
    // Fallback: try the support library version
    uri = QJniObject::callStaticObjectMethod(
        "android/support/v4/content/FileProvider",
        "getUriForFile",
        "(Landroid/content/Context;Ljava/lang/String;Ljava/io/File;)Landroid/net/Uri;",
        activity.object(),
        authority.object(),
        file.object());
  }

  if (!uri.isValid()) {
    ALOGE("AndroidPlatform::ShareImage: failed to get content URI from FileProvider");
    return;
  }

  intent.callObjectMethod(
      "putExtra",
      "(Ljava/lang/String;Landroid/os/Parcelable;)Landroid/content/Intent;",
      QJniObject::fromString(QStringLiteral("android.intent.extra.STREAM")).object(),
      uri.object());

  intent.callObjectMethod(
      "addFlags",
      "(I)Landroid/content/Intent;",
      0x00000001);  // FLAG_GRANT_READ_URI_PERMISSION

  // Create chooser and start activity
  QJniObject chooser = QJniObject::callStaticObjectMethod(
      "android/content/Intent",
      "createChooser",
      "(Landroid/content/Intent;Ljava/lang/CharSequence;)Landroid/content/Intent;",
      intent.object(),
      QJniObject::fromString(QStringLiteral("Share Image")).object());

  activity.callMethod<void>(
      "startActivity",
      "(Landroid/content/Intent;)V",
      chooser.object());

  ALOGI("ShareImage: shared %s", file_path.c_str());
}

void AndroidPlatform::OpenInGallery(const std::string& file_path) {
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    ALOGE("AndroidPlatform::OpenInGallery: failed to get activity");
    return;
  }

  // Build an ACTION_VIEW intent for the image
  QJniObject intent("android/content/Intent");
  intent.callObjectMethod(
      "setAction",
      "(Ljava/lang/String;)Landroid/content/Intent;",
      QJniObject::fromString(QStringLiteral("android.intent.action.VIEW")).object());

  // Create a content URI using FileProvider
  QJniObject file = QJniObject::callStaticObjectMethod(
      "java/io/File",
      "<init>",
      "(Ljava/lang/String;)V",
      QJniObject::fromString(QString::fromStdString(file_path)).object());

  QJniObject authority = QJniObject::fromString(
      QStringLiteral("studio.alcedo.AlcedoStudio.fileprovider"));

  QJniObject uri = QJniObject::callStaticObjectMethod(
      "androidx/core/content/FileProvider",
      "getUriForFile",
      "(Landroid/content/Context;Ljava/lang/String;Ljava/io/File;)Landroid/net/Uri;",
      activity.object(),
      authority.object(),
      file.object());

  if (!uri.isValid()) {
    ALOGE("AndroidPlatform::OpenInGallery: failed to get content URI");
    return;
  }

  intent.callObjectMethod(
      "setDataAndType",
      "(Landroid/net/Uri;Ljava/lang/String;)Landroid/content/Intent;",
      uri.object(),
      QJniObject::fromString(QStringLiteral("image/*")).object());

  intent.callObjectMethod(
      "addFlags",
      "(I)Landroid/content/Intent;",
      0x00000001);  // FLAG_GRANT_READ_URI_PERMISSION

  activity.callMethod<void>(
      "startActivity",
      "(Landroid/content/Intent;)V",
      intent.object());

  ALOGI("OpenInGallery: opened %s", file_path.c_str());
}

}  // namespace alcedo::android

#endif  // Q_OS_ANDROID
