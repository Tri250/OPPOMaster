use std::path::PathBuf;
use std::sync::OnceLock;

use tracing_appender::non_blocking::WorkerGuard;
use tracing_subscriber::{EnvFilter, fmt, layer::SubscriberExt, util::SubscriberInitExt};

static LOG_GUARD: OnceLock<WorkerGuard> = OnceLock::new();

fn env_filter() -> EnvFilter {
    EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info"))
}

fn executable_log_dir() -> PathBuf {
    std::env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(|parent| parent.join("log")))
        .unwrap_or_else(|| {
            std::env::current_dir()
                .unwrap_or_else(|_| PathBuf::from("."))
                .join("log")
        })
}

pub fn init_logging() {
    let stdout_layer = fmt::layer().with_target(false);
    let log_dir = executable_log_dir();

    if std::fs::create_dir_all(&log_dir).is_ok() {
        let file_appender = tracing_appender::rolling::daily(&log_dir, "alcedo_mind.log");
        let (file_writer, guard) = tracing_appender::non_blocking(file_appender);
        let _ = LOG_GUARD.set(guard);
        let file_layer = fmt::layer()
            .with_ansi(false)
            .with_target(true)
            .with_writer(file_writer);

        let _ = tracing_subscriber::registry()
            .with(env_filter())
            .with(stdout_layer)
            .with(file_layer)
            .try_init();
        tracing::info!(path = %log_dir.display(), "file logging initialized");
        return;
    }

    let _ = tracing_subscriber::registry()
        .with(env_filter())
        .with(stdout_layer)
        .try_init();
}
