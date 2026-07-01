use std::env;

#[derive(Debug)]
pub struct AppConfig {
    pub host: String,
    pub port: u16,
    pub semantic: SemanticConfig,
    pub max_message_bytes: usize,
    pub credential_ttl_ms: u64,
    /// Optional directory of user-provider JSON configs, loaded after the built-in
    /// configs and allowed to add providers or override model defaults. `None`
    /// means built-ins only. See `service::provider_config`.
    pub provider_config_dir: Option<String>,
}

#[derive(Debug, Clone)]
pub struct SemanticConfig {
    pub model_id: String,
    pub revision: String,
    pub model_root: String,
    pub hf_endpoint: String,
    pub device: String,
    pub allow_download: bool,
    pub batch_cap: usize,
    pub batch_wait_ms: u64,
}

impl AppConfig {
    pub fn load() -> anyhow::Result<Self> {
        Self::from_args(env::args().skip(1))
    }

    pub fn from_args<I, S>(args: I) -> anyhow::Result<Self>
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        let mut config = Self::from_env()?;
        let mut args = args.into_iter().map(Into::into);

        while let Some(arg) = args.next() {
            match arg.as_str() {
                "--host" => config.host = next_value(&mut args, "--host")?,
                "--port" => config.port = parse_next(&mut args, "--port")?,
                "--model-root" => {
                    config.semantic.model_root = next_value(&mut args, "--model-root")?
                }
                "--model-id" => config.semantic.model_id = next_value(&mut args, "--model-id")?,
                "--revision" => config.semantic.revision = next_value(&mut args, "--revision")?,
                "--hf-endpoint" => {
                    config.semantic.hf_endpoint = next_value(&mut args, "--hf-endpoint")?
                }
                "--device" => config.semantic.device = next_value(&mut args, "--device")?,
                "--no-download" => config.semantic.allow_download = false,
                "--allow-download" => config.semantic.allow_download = true,
                "--batch-cap" => config.semantic.batch_cap = parse_next(&mut args, "--batch-cap")?,
                "--batch-wait-ms" => {
                    config.semantic.batch_wait_ms = parse_next(&mut args, "--batch-wait-ms")?
                }
                "--max-message-bytes" => {
                    config.max_message_bytes = parse_next(&mut args, "--max-message-bytes")?
                }
                "--credential-ttl-ms" => {
                    config.credential_ttl_ms = parse_next(&mut args, "--credential-ttl-ms")?
                }
                "--provider-config-dir" => {
                    config.provider_config_dir =
                        Some(next_value(&mut args, "--provider-config-dir")?)
                }
                "--help" | "-h" => anyhow::bail!("{}", Self::usage()),
                other => anyhow::bail!("unknown argument {other:?}\n{}", Self::usage()),
            }
        }

        config.validate()?;
        Ok(config)
    }

    fn from_env() -> anyhow::Result<Self> {
        Ok(Self {
            host: env_value("ALCEDO_MIND_HOST", "127.0.0.1"),
            port: parse_env("ALCEDO_MIND_PORT", 50051)?,
            max_message_bytes: parse_env("ALCEDO_MIND_MAX_MESSAGE_BYTES", 16 * 1024 * 1024)?,
            semantic: SemanticConfig {
                model_id: env_value("ALCEDO_MIND_MODEL_ID", "plhery/mobileclip2-onnx:s2"),
                revision: env_value(
                    "ALCEDO_MIND_REVISION",
                    crate::service::model_assets::MOBILECLIP2_ONNX_REVISION,
                ),
                model_root: env_value("ALCEDO_MIND_MODEL_ROOT", "./models/mobileclip2-s2-openclip"),
                hf_endpoint: env_value("ALCEDO_MIND_HF_ENDPOINT", "https://hf-mirror.com"),
                device: env_value("ALCEDO_MIND_DEVICE", "auto"),
                allow_download: parse_bool_env("ALCEDO_MIND_ALLOW_DOWNLOAD", false)?,
                batch_cap: parse_env("ALCEDO_MIND_BATCH_CAP", 512)?,
                batch_wait_ms: parse_env("ALCEDO_MIND_BATCH_WAIT_MS", 25)?,
            },
            credential_ttl_ms: parse_env("ALCEDO_MIND_CREDENTIAL_TTL_MS", 3_600_000)?,
            provider_config_dir: env::var("ALCEDO_MIND_PROVIDER_CONFIG_DIR")
                .ok()
                .filter(|s| !s.trim().is_empty()),
        })
    }

    pub fn listen_addr(&self) -> String {
        format!("{}:{}", self.host, self.port)
    }

    fn validate(&self) -> anyhow::Result<()> {
        if self.host.trim().is_empty() {
            anyhow::bail!("host must not be empty");
        }
        if self.semantic.model_root.trim().is_empty() {
            anyhow::bail!("model-root must not be empty");
        }
        if self.semantic.model_id.trim().is_empty() {
            anyhow::bail!("model-id must not be empty");
        }
        if self.semantic.revision.trim().is_empty() {
            anyhow::bail!("revision must not be empty");
        }
        if self.semantic.hf_endpoint.trim().is_empty() {
            anyhow::bail!("hf-endpoint must not be empty");
        }
        if self.semantic.batch_cap == 0 {
            anyhow::bail!("batch-cap must be greater than zero");
        }
        if self.max_message_bytes == 0 {
            anyhow::bail!("max-message-bytes must be greater than zero");
        }
        Ok(())
    }

    fn usage() -> &'static str {
        "usage: alcedo_mind [--host HOST] [--port PORT] [--model-root PATH] [--model-id ID] [--revision REV] [--hf-endpoint URL] [--device auto|cpu|directml[:N]|coreml[:MODE]] [--no-download] [--allow-download] [--batch-cap N] [--batch-wait-ms N] [--max-message-bytes N] [--credential-ttl-ms N] [--provider-config-dir PATH]"
    }
}

fn env_value(name: &str, default: &str) -> String {
    env::var(name).unwrap_or_else(|_| default.to_string())
}

fn parse_env<T>(name: &str, default: T) -> anyhow::Result<T>
where
    T: std::str::FromStr,
    T::Err: std::fmt::Display,
{
    match env::var(name) {
        Ok(value) => value
            .parse()
            .map_err(|err| anyhow::anyhow!("failed to parse {name}={value:?}: {err}")),
        Err(env::VarError::NotPresent) => Ok(default),
        Err(err) => Err(anyhow::anyhow!("failed to read {name}: {err}")),
    }
}

fn parse_bool_env(name: &str, default: bool) -> anyhow::Result<bool> {
    match env::var(name) {
        Ok(value) => match value.to_ascii_lowercase().as_str() {
            "1" | "true" | "yes" | "on" => Ok(true),
            "0" | "false" | "no" | "off" => Ok(false),
            _ => anyhow::bail!("failed to parse {name}={value:?} as bool"),
        },
        Err(env::VarError::NotPresent) => Ok(default),
        Err(err) => Err(anyhow::anyhow!("failed to read {name}: {err}")),
    }
}

fn next_value(args: &mut impl Iterator<Item = String>, flag: &str) -> anyhow::Result<String> {
    args.next()
        .ok_or_else(|| anyhow::anyhow!("missing value after {flag}"))
}

fn parse_next<T>(args: &mut impl Iterator<Item = String>, flag: &str) -> anyhow::Result<T>
where
    T: std::str::FromStr,
    T::Err: std::fmt::Display,
{
    let value = next_value(args, flag)?;
    value
        .parse()
        .map_err(|err| anyhow::anyhow!("failed to parse {flag} value {value:?}: {err}"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_cli_overrides_and_keeps_validate_only_default() {
        let config = AppConfig::from_args([
            "--host",
            "0.0.0.0",
            "--port",
            "5555",
            "--model-root",
            "C:/models/mobileclip",
            "--model-id",
            "repo/model:s2",
            "--revision",
            "abc123",
            "--hf-endpoint",
            "https://example.invalid",
            "--device",
            "cpu",
            "--no-download",
            "--batch-cap",
            "8",
            "--batch-wait-ms",
            "3",
            "--max-message-bytes",
            "4096",
        ])
        .expect("config should parse");

        assert_eq!(config.host, "0.0.0.0");
        assert_eq!(config.port, 5555);
        assert_eq!(config.semantic.model_root, "C:/models/mobileclip");
        assert_eq!(config.semantic.model_id, "repo/model:s2");
        assert_eq!(config.semantic.revision, "abc123");
        assert_eq!(config.semantic.hf_endpoint, "https://example.invalid");
        assert_eq!(config.semantic.device, "cpu");
        assert!(!config.semantic.allow_download);
        assert_eq!(config.semantic.batch_cap, 8);
        assert_eq!(config.semantic.batch_wait_ms, 3);
        assert_eq!(config.max_message_bytes, 4096);
    }

    #[test]
    fn parses_explicit_download_opt_in() {
        let config = AppConfig::from_args(["--allow-download"]).expect("config should parse");
        assert!(config.semantic.allow_download);
        assert_eq!(config.semantic.hf_endpoint, "https://hf-mirror.com");
    }

    #[test]
    fn rejects_empty_host() {
        let err = AppConfig::from_args(["--host", "  "]).expect_err("empty host rejected");
        assert!(err.to_string().contains("host must not be empty"));
    }

    #[test]
    fn rejects_empty_model_root() {
        let err =
            AppConfig::from_args(["--model-root", ""]).expect_err("empty model-root rejected");
        assert!(err.to_string().contains("model-root must not be empty"));
    }

    #[test]
    fn rejects_unknown_argument() {
        let err = AppConfig::from_args(["--nonsense"]).expect_err("unknown arg rejected");
        assert!(err.to_string().contains("unknown argument"));
    }

    #[test]
    fn rejects_missing_value_after_flag() {
        let err = AppConfig::from_args(["--port"]).expect_err("missing value rejected");
        assert!(err.to_string().contains("missing value after --port"));
    }

    #[test]
    fn rejects_non_numeric_port() {
        let err =
            AppConfig::from_args(["--port", "not-a-port"]).expect_err("non-numeric port rejected");
        assert!(err.to_string().contains("failed to parse --port"));
    }

    #[test]
    fn rejects_zero_batch_cap() {
        let err = AppConfig::from_args(["--batch-cap", "0"]).expect_err("zero batch-cap rejected");
        assert!(
            err.to_string()
                .contains("batch-cap must be greater than zero")
        );
    }

    #[test]
    fn parses_provider_config_dir_override() {
        let config =
            AppConfig::from_args(["--provider-config-dir", "C:/Users/me/alcedo/providers"])
                .expect("config should parse");
        assert_eq!(
            config.provider_config_dir.as_deref(),
            Some("C:/Users/me/alcedo/providers")
        );
    }

    #[test]
    fn provider_config_dir_defaults_to_none() {
        let config = AppConfig::from_env().expect("env config should parse");
        assert!(config.provider_config_dir.is_none());
    }
}
