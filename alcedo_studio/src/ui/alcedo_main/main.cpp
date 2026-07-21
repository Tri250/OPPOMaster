//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <QApplication>
#include <QCoreApplication>
#include <QDebug>
#include <QFont>
#include <QFontDatabase>
#include <QGuiApplication>
#include <QIcon>
#include <QMessageBox>
#include <QOffscreenSurface>
#include <QOpenGLContext>
#include <QScreen>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>
#include <QString>
#include <QSurfaceFormat>
#include <QTimer>
#include <QtGlobal>

#include <exiv2/error.hpp>
#include <memory>
#include <optional>
#include <string>
#include <string_view>

#include "app/crash_reporter.hpp"
#include "app/update_checker.hpp"
#include "app/export_preset.hpp"
#include "app/credential_portability.hpp"
#include "app/relink_service.hpp"
#include "app/offline_ai_service.hpp"
#include "app/ai_sidecar_runtime_service.hpp"
#include "renderer/memory_budget_manager.hpp"
#include "ui/alcedo_main/album_backend/album_backend.hpp"
#include "ui/alcedo_main/app_theme.hpp"
#include "ui/alcedo_main/language_manager.hpp"
#include "ui/alcedo_main/user_notification.hpp"
#include "ui/alcedo_main/shortcut_definitions.hpp"
#include "ui/alcedo_main/qml_export_preset_manager.hpp"
#include "ui/alcedo_main/qml_credential_portability.hpp"
#include "ui/alcedo_main/qml_relink_service.hpp"
#include "ui/alcedo_main/qml_shortcut_definitions.hpp"
#include "ui/alcedo_main/qml_hdr_manager_bridge.hpp"
#include "ui/edit_viewer/color_manager.hpp"
#include "ui/edit_viewer/hdr_manager.hpp"
#include "edit/operators/operator_registeration.hpp"
#include "utils/diagnostics/app_logging.hpp"
#ifdef HAVE_OPENCL
#include "opencl/opencl_runtime.hpp"
#endif
#include "utils/clock/time_provider.hpp"
#include "utils/font/cjk_font_manager.hpp"
#include "utils/gpu/gpu_capability_detector.hpp"
#include "utils/config/app_config.hpp"

#if defined(Q_OS_WIN) && defined(HAVE_OPENCL)
#include <QtGui/qopenglcontext_platform.h>

#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#include <GL/gl.h>
#endif

namespace {

auto FindArgValue(int argc, char** argv, std::string_view option_name)
    -> std::optional<std::string_view> {
  const std::string opt_eq = std::string(option_name) + "=";
  for (int i = 1; i < argc; ++i) {
    const std::string_view arg(argv[i] ? argv[i] : "");
    if (arg == option_name) {
      if (i + 1 < argc && argv[i + 1]) {
        return std::string_view(argv[i + 1]);
      }
      return std::nullopt;
    }
    if (arg.rfind(opt_eq, 0) == 0) {
      return arg.substr(opt_eq.size());
    }
  }
  return std::nullopt;
}

#if defined(Q_OS_WIN) && defined(HAVE_OPENCL)
class OpenClGlSharingBootstrap {
 public:
  auto Initialize() -> bool {
    if (initialized_) {
      return true;
    }

    context_ = std::make_unique<QOpenGLContext>();
    if (auto* global_share_context = QOpenGLContext::globalShareContext()) {
      context_->setShareContext(global_share_context);
      context_->setFormat(global_share_context->format());
    }
    if (!context_->create()) {
      qWarning("OpenCL/OpenGL bootstrap: failed to create hidden OpenGL context.");
      context_.reset();
      return false;
    }

    surface_ = std::make_unique<QOffscreenSurface>();
    surface_->setFormat(context_->format());
    surface_->create();
    if (!surface_->isValid() || !context_->makeCurrent(surface_.get())) {
      qWarning("OpenCL/OpenGL bootstrap: failed to make hidden OpenGL context current.");
      surface_.reset();
      context_.reset();
      return false;
    }

    auto* native_context = context_->nativeInterface<QNativeInterface::QWGLContext>();
    HGLRC hglrc = native_context ? native_context->nativeContext() : nullptr;
    HDC   hdc   = wglGetCurrentDC();
    if (hglrc == nullptr || hdc == nullptr) {
      qWarning("OpenCL/OpenGL bootstrap: failed to resolve WGL context handles.");
      context_->doneCurrent();
      surface_.reset();
      context_.reset();
      return false;
    }

    alcedo::OpenClInitializationOptions options;
    options.gl_context        = hglrc;
    options.gl_device_context = hdc;
    initialized_ = alcedo::TryInitializeOpenClRuntime(options);
    context_->doneCurrent();

    if (!initialized_) {
      qWarning("OpenCL/OpenGL bootstrap: failed to initialize OpenCL with OpenGL sharing.");
      surface_.reset();
      context_.reset();
    }
    return initialized_;
  }

 private:
  std::unique_ptr<QOpenGLContext>  context_;
  std::unique_ptr<QOffscreenSurface> surface_;
  bool                             initialized_ = false;
};
#endif

}  // namespace

// P1-8: Configure Qt application-level color space support.
// Sets the default QSurfaceFormat to request sRGB-capable framebuffer,
// and registers display profile change callbacks so the color management
// pipeline can react when the user changes monitor ICC profiles.
static void ApplyUiColorManagement(QApplication& app) {
  // P1-8: Request sRGB-capable default framebuffer for correct gamma rendering.
  // Without this, Qt may create linear framebuffer on some platforms,
  // causing UI elements to appear washed out or over-saturated.
  QSurfaceFormat format = QSurfaceFormat::defaultFormat();
  format.setColorSpace(QSurfaceFormat::sRGBColorSpace);
  QSurfaceFormat::setDefaultFormat(format);

  // P1-8: Register for display profile changes so the rendering pipeline
  // can invalidate cached color transforms when the monitor ICC changes.
  QObject::connect(&app, &QGuiApplication::screenAdded, &app, [](QScreen* screen) {
    if (!screen) return;
    QObject::connect(screen, &QScreen::logicalDotsPerInchChanged, &app, [](qreal) {
      // Screen properties changed — may indicate profile or DPI change.
      // Invalidate any cached display color transforms.
      alcedo::ColorManager::ClearCache();
    });
    QObject::connect(screen, &QScreen::geometryChanged, &app, [](const QRect&) {
      // Geometry change may indicate monitor reconfiguration.
      alcedo::ColorManager::ClearCache();
    });
  });

  // P1-8: Set rendering intent for UI color management to perceptual
  // (best for photographic content displayed in the UI).
  // This is a hint for the OS-level color management; actual rendering
  // intent is controlled by the OCIO/pipeline color manager.
  alcedo::ColorManager::SetDisplayProfileChangeCallback(
      [](const std::wstring& new_profile_path) {
        qCInfo(alcedo::diag::appLog).noquote()
            << QStringLiteral("app.display_profile_changed path=%1")
                   .arg(QString::fromWCharArray(new_profile_path.c_str()));
      });
}

int main(int argc, char* argv[]) {
#if QT_VERSION < QT_VERSION_CHECK(6, 0, 0)
  QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
  QCoreApplication::setAttribute(Qt::AA_UseHighDpiPixmaps);
#else
  QGuiApplication::setHighDpiScaleFactorRoundingPolicy(
      Qt::HighDpiScaleFactorRoundingPolicy::PassThrough);
#endif
#if defined(Q_OS_WIN) && defined(HAVE_OPENCL)
  QCoreApplication::setAttribute(Qt::AA_ShareOpenGLContexts);
#endif

  // ── Initialize CrashReporter early (before UI) ───────────────
  alcedo::CrashReporter crash_reporter;
  crash_reporter.Initialize();
  crash_reporter.CheckForPendingCrashes();

  alcedo::TimeProvider::Refresh();
  alcedo::RegisterAllOperators();
  Exiv2::LogMsg::setLevel(Exiv2::LogMsg::Level::error);

  QApplication app(argc, argv);
  QCoreApplication::setOrganizationName(QStringLiteral("Alcedo"));
  QCoreApplication::setOrganizationDomain(QStringLiteral("alcedo.app"));
  QCoreApplication::setApplicationName(QStringLiteral("Alcedo"));

  // P1-8: Apply UI color management before any windows are created.
  ApplyUiColorManagement(app);

  const QString log_path = alcedo::diag::InitializeApplicationLogging();
  qCInfo(alcedo::diag::appLog).noquote()
      << QStringLiteral("app.start log_path=%1").arg(log_path);
  app.setWindowIcon(QIcon(QStringLiteral(":/ICON/alcedo_icon.png")));
  {
    QFont default_font = app.font();
    default_font.setStyleStrategy(QFont::PreferAntialias);
    app.setFont(default_font);
  }
  alcedo::ui::AppTheme::RegisterFonts();
  if (const auto arg = FindArgValue(argc, argv, "--font"); arg.has_value()) {
    alcedo::ui::AppTheme::TryRegisterUiFontOverride(QString::fromUtf8(arg->data(), arg->size()));
  } else if (const auto env = qEnvironmentVariable("ALCEDO_FONT_PATH"); !env.isEmpty()) {
    alcedo::ui::AppTheme::TryRegisterUiFontOverride(env);
  }
  alcedo::ui::LanguageManager language_manager(&app);
  alcedo::ui::AppTheme::SetEffectiveLanguageCode(language_manager.EffectiveLanguageCode());
  alcedo::ui::AppTheme::ApplyApplicationFont(app);
  QObject::connect(&language_manager, &alcedo::ui::LanguageManager::EffectiveLanguageCodeChanged,
                   &app, [&app, &language_manager]() {
                     alcedo::ui::AppTheme::SetEffectiveLanguageCode(
                         language_manager.EffectiveLanguageCode());
                     alcedo::ui::AppTheme::ApplyApplicationFont(app);
                   });
  QQuickStyle::setStyle("Material");

  // Initialize CJK font fallback chain for optimal Chinese/Japanese/Korean rendering.
  alcedo::font::CjkFontManager::Instance().Initialize();
  alcedo::font::CjkFontManager::Instance().ApplyCjkFont(app);

  // Detect GPU capability and warn the user if acceleration is limited.
  {
    auto gpu_info = alcedo::gpu::GpuCapabilityDetector::Detect();
    qCInfo(alcedo::diag::appLog).noquote()
        << QStringLiteral("app.gpu capability=%1 backend=%2 adapter=%3 detail=%4")
               .arg(static_cast<int>(gpu_info.capability_level))
               .arg(static_cast<int>(gpu_info.recommended_backend))
               .arg(QString::fromStdString(gpu_info.gpu_adapter_name))
               .arg(QString::fromStdString(gpu_info.detail));

    if ((gpu_info.IsLimited() || gpu_info.IsSoftwareOnly())
        && !alcedo::gpu::GpuCapabilityDetector::IsDriverWarningSuppressed()) {
      QMessageBox::warning(
          nullptr,
          QCoreApplication::translate("GpuWarning", "GPU Acceleration Warning"),
          QString::fromStdString(
              alcedo::gpu::GpuCapabilityDetector::BuildDriverWarningMessage(gpu_info)));
    }
  }

#if defined(Q_OS_WIN) && defined(HAVE_OPENCL)
  OpenClGlSharingBootstrap opencl_gl_bootstrap;
  (void)opencl_gl_bootstrap.Initialize();
#endif

  // ── Initialize singletons and services ────────────────────────

  // Load AppConfig singleton
  auto& app_config = alcedo::AppConfig::Instance();
  app_config.LoadFromFile(app_config.ConfigFilePath());

  // Initialize MemoryBudgetManager singleton
  alcedo::MemoryBudgetManager::Instance().RefreshMemoryStatus();

  // Initialize UserNotificationManager singleton
  auto& notification_mgr = alcedo::ui::UserNotificationManager::Instance();

  // Initialize ColorManager singleton
  alcedo::ColorManager::GetInstance();

  // Initialize ExportPresetManager
  alcedo::ExportPresetManager export_preset_mgr;
  export_preset_mgr.LoadCustomPresets();

  // Initialize CredentialPortability (will be properly wired when project opens)
  // The credential store and profile controller come from AlbumBackend at project-open time.
  // Until then, the QML wrapper returns errors gracefully.
  alcedo::CredentialPortability credential_portability(nullptr, nullptr);

  // Initialize UpdateChecker
  alcedo::UpdateChecker update_checker;

  alcedo::ui::AlbumBackend backend;

  // ── Create QML wrapper objects ─────────────────────────────────
  alcedo::ui::QmlExportPresetManager qml_export_preset_mgr(&export_preset_mgr, &app);
  alcedo::ui::QmlCredentialPortability qml_credential_portability(&credential_portability, &app);
  alcedo::ui::QmlShortcutDefinitions qml_shortcut_defs(&app);

  QQmlApplicationEngine engine;
  engine.addImportPath("qrc:/");
  language_manager.AttachEngine(&engine);

  // ── Register context properties for QML ───────────────────────
  engine.rootContext()->setContextProperty("albumBackend", &backend);
  engine.rootContext()->setContextProperty("appTheme", &alcedo::ui::AppTheme::Instance());
  engine.rootContext()->setContextProperty("languageManager", &language_manager);
  engine.rootContext()->setContextProperty("crashReporter", &crash_reporter);
  engine.rootContext()->setContextProperty("updateChecker", &update_checker);
  engine.rootContext()->setContextProperty("exportPresetManager", &qml_export_preset_mgr);
  engine.rootContext()->setContextProperty("credentialPortability", &qml_credential_portability);
  engine.rootContext()->setContextProperty("userNotificationManager", &notification_mgr);
  engine.rootContext()->setContextProperty("appConfig", &app_config);
  engine.rootContext()->setContextProperty("shortcutDefinitions", &qml_shortcut_defs);

  // ── Register HDRManager context ───────────────────────────────
  alcedo::ui::QmlHdrManagerBridge hdr_manager_bridge(&app);
  engine.rootContext()->setContextProperty("hdrManager", &hdr_manager_bridge);

  QObject::connect(&engine, &QQmlApplicationEngine::objectCreationFailed, &app,
                   []() { QCoreApplication::exit(-1); }, Qt::QueuedConnection);

  engine.loadFromModule("Alcedo.Main", "Main");

  // ── Trigger update check after window shows ───────────────────
  QTimer::singleShot(2000, &update_checker, &alcedo::UpdateChecker::MaybeCheckForUpdate);

  const int exit_code = app.exec();
  qCInfo(alcedo::diag::appLog) << "app.exit code=" << exit_code;
  alcedo::diag::ShutdownApplicationLogging();
  return exit_code;
}
