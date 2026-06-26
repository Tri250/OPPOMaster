//! Shared HTTP plumbing for the Phase 5c remote image-analysis drivers.
//!
//! Both the OpenRouter Chat provider and the Volcengine Ark Responses provider
//! use the same rustls-backed `reqwest` client, the same image->data-URI encoding,
//! the same bounded retry policy for transient transport / 429 / 5xx failures,
//! the same strict-compatible JSON Schema injection, and the same redaction
//! discipline: the driver never logs the secret, the image bytes / base64, the
//! prompt text, or the raw provider response body. Only status, attempt count,
//! latency, and the provider request id are logged. Driver-specific request
//! building and response parsing live in their own modules; this file holds only
//! the shared pieces, so a change to the retry / redaction / TLS policy applies
//! uniformly to every driver.

use std::time::Duration;

use base64::Engine;
use serde_json::Value;
use tracing::warn;

use crate::service::image_analysis::{ProviderError, Usage};

/// Backoff between retry attempts. Kept small and fixed because a provider call
/// is paid and non-idempotent — the plan's review focus is that retries stay
/// bounded and do not retry aggressively. The outer service timeout bounds the
/// total wall-clock anyway, so a long backoff would just be cut into a
/// `DEADLINE_EXCEEDED` rather than helping.
const RETRY_BACKOFF: Duration = Duration::from_millis(100);

/// Upper bound on a server-supplied `Retry-After` (seconds) that the driver will
/// honor, so a misbehaving or hostile server cannot stall the sidecar for longer
/// than the policy intends.
const MAX_RETRY_AFTER: Duration = Duration::from_secs(5);

/// Maximum number of retries after a transient failure. With the initial attempt
/// this is at most 2 calls for a transient transport / 429 / 5xx failure; a 4xx
/// (non-429) is never retried — it is a non-transient provider error such as a
/// bad request or an unauthorized/forbidden call, and retrying a paid
/// non-idempotent POST would only duplicate cost without changing the outcome.
pub const MAX_TRANSIENT_RETRIES: u32 = 1;

/// Install `ring` as the rustls default crypto provider and build a rustls-backed
/// `reqwest::Client`. `ring` is already used by `ort`'s `tls-rustls`, so this
/// reuses the existing native build rather than introducing `aws-lc-rs`. The
/// client uses the bundled webpki root store (from the `rustls-tls-webpki-roots`
/// feature) for certificate verification; invalid-certificate acceptance and
/// plaintext HTTP for production providers are never enabled (provider config
/// validation already enforces HTTPS-only except localhost dev).
///
/// `install_default` is process-global and idempotent; calling it again after
/// another component installed a provider returns `AlreadyInstalled`, which we
/// treat as success. Providers are constructed at startup on a single thread in
/// `main.rs`; in tests, parallel construction races on `install_default` but the
/// operation is internally synchronized and `AlreadyInstalled` is benign.
pub fn build_rustls_client() -> Result<reqwest::Client, ProviderError> {
    let _ = rustls::crypto::ring::default_provider().install_default();
    reqwest::Client::builder()
        .use_rustls_tls()
        .build()
        .map_err(|_| ProviderError::Provider("failed to build rustls HTTP client".to_string()))
}

/// Encode image bytes as a `data:<mime>;base64,...` URI. Common container formats
/// (PNG / JPEG / WebP / GIF) are detected by magic bytes and passed through
/// verbatim — the host selected the rendition, so the driver sends exactly what
/// it chose. Anything else is decoded with the `image` crate and re-encoded to
/// PNG; if that also fails the driver fails closed with a provider error rather
/// than sending an unrecognizable payload.
pub fn build_image_data_uri(bytes: &[u8]) -> Result<String, ProviderError> {
    let (mime, b64) = detect_image_base64(bytes)?;
    Ok(format!("data:{mime};base64,{b64}"))
}

/// Encode image bytes as a `(media_type, base64)` pair for providers that take
/// raw base64 image content — e.g. the Anthropic Messages `image` source block,
/// which uses `source: { type: "base64", media_type, data }`. Same magic-byte
/// pass-through / PNG re-encode policy as [`build_image_data_uri`]; only the
/// packaging differs (the media type and raw base64 are returned separately
/// instead of a `data:` URI).
pub fn build_image_base64(bytes: &[u8]) -> Result<(String, String), ProviderError> {
    detect_image_base64(bytes)
}

/// Detect the image media type by magic bytes (PNG / JPEG / WebP / GIF) and
/// base64-encode the bytes verbatim, or — for an unrecognized container — decode
/// with the `image` crate and re-encode to PNG (lossless, universally accepted by
/// vision providers). Returns `(media_type, base64)`. Empty input fails closed.
fn detect_image_base64(bytes: &[u8]) -> Result<(String, String), ProviderError> {
    if bytes.is_empty() {
        return Err(ProviderError::Provider("image bytes are empty".to_string()));
    }
    if let Some(mime) = detect_mime(bytes) {
        let b64 = base64::engine::general_purpose::STANDARD.encode(bytes);
        return Ok((mime.to_string(), b64));
    }
    // Re-encode to PNG via the `image` crate (lossless, universally accepted by
    // vision providers). The host's container format is not one we recognize by
    // magic bytes, so decode + re-encode rather than guessing a mime.
    let img = image::load_from_memory(bytes)
        .map_err(|_| ProviderError::Provider("could not decode image for provider upload".to_string()))?;
    let mut cursor = std::io::Cursor::new(Vec::new());
    img.write_to(&mut cursor, image::ImageFormat::Png)
        .map_err(|_| ProviderError::Provider("could not encode image for provider upload".to_string()))?;
    let b64 = base64::engine::general_purpose::STANDARD.encode(&cursor.into_inner());
    Ok(("image/png".to_string(), b64))
}

fn detect_mime(bytes: &[u8]) -> Option<&'static str> {
    if bytes.len() >= 8 && &bytes[..8] == b"\x89PNG\r\n\x1a\n" {
        Some("image/png")
    } else if bytes.len() >= 3 && &bytes[..3] == [0xFF, 0xD8, 0xFF] {
        Some("image/jpeg")
    } else if bytes.len() >= 12 && &bytes[..4] == b"RIFF" && &bytes[8..12] == b"WEBP" {
        Some("image/webp")
    } else if bytes.len() >= 6 && (&bytes[..6] == b"GIF89a" || &bytes[..6] == b"GIF87a") {
        Some("image/gif")
    } else {
        None
    }
}

/// JSON Schema keys retained when sanitizing a code-owned schema for strict-mode
/// injection. OpenAI / OpenRouter strict structured output supports a limited
/// subset; unsupported keys (`$schema`, `title`, `minLength`, `minItems`,
/// `minimum`, `maximum`, `pattern`, `format`, ...) are dropped so the provider
/// does not reject the schema. The code-owned validator (`validate_understanding`
/// / `validate_rating`) still enforces the dropped constraints on the parsed
/// response, so fail-closed behavior is preserved — the injected schema only
/// guides the model, the validator decides whether the result is acceptable.
const STRICT_KEEP: &[&str] = &[
    "type",
    "description",
    "properties",
    "required",
    "items",
    "enum",
    "anyOf",
    "oneOf",
    "allOf",
    "additionalProperties",
];

/// Produce a strict-compatible JSON Schema `Value` from a code-owned schema
/// string: drop unsupported keys, recursively, and force `required` to list
/// every property at every object level (strict mode requires every property to
/// be required). `additionalProperties: false` is retained.
pub fn strict_schema_value(schema_json: &str) -> Result<Value, ProviderError> {
    let mut v: Value = serde_json::from_str(schema_json).map_err(|_| {
        ProviderError::Provider("code-owned image-analysis schema is not valid JSON".to_string())
    })?;
    sanitize_strict(&mut v);
    Ok(v)
}

fn sanitize_strict(v: &mut Value) {
    let Some(map) = v.as_object_mut() else {
        return;
    };
    // Drop unsupported keys.
    let keys: Vec<String> = map.keys().cloned().collect();
    for k in keys {
        if !STRICT_KEEP.contains(&k.as_str()) {
            map.remove(&k);
        }
    }
    // Recurse into container keys.
    if let Some(props) = map.get_mut("properties").and_then(|p| p.as_object_mut()) {
        for sub in props.values_mut() {
            sanitize_strict(sub);
        }
    }
    if let Some(items) = map.get_mut("items") {
        sanitize_strict(items);
    }
    for key in ["anyOf", "oneOf", "allOf"] {
        if let Some(arr) = map.get_mut(key).and_then(|a| a.as_array_mut()) {
            for sub in arr {
                sanitize_strict(sub);
            }
        }
    }
    // Force required = all property keys (strict mode requires every property to
    // be required). Sorted for deterministic output.
    if let Some(props) = map.get("properties").and_then(|p| p.as_object()) {
        // Force required = all property keys (strict mode requires every property to
        // be required). Collect as owned strings, sort, then lift to Values so the
        // comparison is on `&str` (an `Option<&str>` cmp would need an extra borrow).
        let mut required: Vec<String> = props.keys().cloned().collect();
        required.sort();
        let required: Vec<Value> = required.into_iter().map(Value::String).collect();
        map.insert("required".to_string(), Value::Array(required));
    }
}

/// Send a JSON POST with a bounded retry policy. `headers` are attribution /
/// routing headers set by the driver (never `Authorization` or `Content-Type`,
/// which the transport owns). `bearer` is the resolved secret, sent only as a
/// `Bearer` header; it is never logged. Transient transport errors, 429, and
/// 5xx are retried up to `max_retries` times; 4xx (non-429) is not retried. The
/// response body is drained on the error path but never logged.
pub async fn send_with_retry(
    client: &reqwest::Client,
    url: &str,
    body: &Value,
    headers: &[(String, String)],
    bearer: Option<&str>,
    max_retries: u32,
) -> Result<reqwest::Response, ProviderError> {
    let mut attempt = 0u32;
    loop {
        let mut req = client.post(url).json(body);
        for (k, v) in headers {
            req = req.header(k.as_str(), v.as_str());
        }
        if let Some(token) = bearer {
            req = req.bearer_auth(token);
        }
        match req.send().await {
            Ok(resp) => {
                let status = resp.status();
                if status.is_success() {
                    return Ok(resp);
                }
                let code = status.as_u16();
                let retryable = code == 429 || status.is_server_error();
                let header_req_id = resp
                    .headers()
                    .get("x-request-id")
                    .and_then(|h| h.to_str().ok())
                    .map(|s| s.to_string());
                if retryable && attempt < max_retries {
                    warn!(
                        attempt = attempt + 1,
                        status = code,
                        provider_request_id = header_req_id.as_deref().unwrap_or(""),
                        "provider returned a transient status; retrying"
                    );
                    // Read Retry-After before draining the body, since `text()`
                    // consumes `resp` and would make a later `&resp` a use-after-move.
                    let backoff = retry_after(&resp).unwrap_or(RETRY_BACKOFF);
                    // Drain the body without logging it.
                    let _ = resp.text().await;
                    tokio::time::sleep(backoff).await;
                    attempt += 1;
                    continue;
                }
                // Exhausted retries or non-retryable 4xx. Drain without logging.
                let _ = resp.text().await;
                if retryable {
                    return Err(ProviderError::Transient);
                }
                return Err(ProviderError::Provider(format!(
                    "provider returned HTTP {code}"
                )));
            }
            Err(err) => {
                if attempt < max_retries {
                    warn!(
                        attempt = attempt + 1,
                        category = transport_error_category(&err),
                        "transport error calling provider; retrying"
                    );
                    tokio::time::sleep(RETRY_BACKOFF).await;
                    attempt += 1;
                    continue;
                }
                return Err(ProviderError::Transient);
            }
        }
    }
}

fn transport_error_category(err: &reqwest::Error) -> &'static str {
    if err.is_connect() {
        "connect"
    } else if err.is_timeout() {
        "timeout"
    } else if err.is_decode() {
        "body-decode"
    } else {
        "other"
    }
}

/// Parse a `Retry-After` header (seconds form only; the HTTP-date form is
/// intentionally not supported to keep the policy simple and bounded). Capped at
/// `MAX_RETRY_AFTER` so a server cannot stall the sidecar.
fn retry_after(resp: &reqwest::Response) -> Option<Duration> {
    let header = resp.headers().get("retry-after")?.to_str().ok()?;
    let secs: u64 = header.trim().parse().ok()?;
    Some(Duration::from_secs(secs).min(MAX_RETRY_AFTER))
}

/// Parse a content string (the model's JSON text) into a `Value`. A non-JSON
/// content string maps to `SchemaValidation` so the service reports a
/// payload-decode failure and creates no active annotation.
pub fn parse_content_json(content: &str) -> Result<Value, ProviderError> {
    serde_json::from_str::<Value>(content).map_err(|_| ProviderError::SchemaValidation)
}

/// Coerce a model-emitted `rating` value to the 1..=5 integer the contract
/// requires, WITHOUT silently truncating a fractional float.
///
/// The remote LLM is asked for an `integer` rating, but some models still emit
/// an integer-valued float (e.g. `4.0`); that form is accepted and coerced to
/// the integer 4. A genuinely fractional float (e.g. `4.9`) is schema-invalid
/// — truncating it to 4 would silently normalize bad output into a valid
/// rating, so it is rejected here (returns `None`). The caller maps `None` to
/// the out-of-contract sentinel 0, which `validate_rating` then turns into a
/// `SchemaValidation` error (fail closed, no active annotation). Non-numeric
/// values are likewise `None`.
pub fn parse_rating_int(value: &Value) -> Option<i32> {
    // An exact integer (JSON `4`) — the common, contract-correct case.
    if let Some(i) = value.as_i64() {
        return Some(i as i32);
    }
    // An integer-valued float (JSON `4.0`) — accept and coerce. A fractional
    // float (JSON `4.9`) has `fract() != 0.0` and falls through to `None`,
    // so it is NOT truncated into a valid rating.
    if let Some(f) = value.as_f64() {
        if f.fract() == 0.0 && f >= i32::MIN as f64 && f <= i32::MAX as f64 {
            return Some(f as i32);
        }
    }
    None
}

/// Tolerantly extract usage from a provider usage object. OpenRouter follows
/// OpenAI Chat (`prompt_tokens` / `completion_tokens` / `total_tokens`); Ark
/// Responses follows OpenAI Responses (`input_tokens` / `output_tokens` /
/// `total_tokens`). Accept either field name; 0 means "not reported".
pub fn extract_usage(usage: Option<&Value>) -> Usage {
    let Some(usage) = usage else { return Usage::default() };
    let input = usage
        .get("input_tokens")
        .or_else(|| usage.get("prompt_tokens"))
        .and_then(|v| v.as_i64())
        .unwrap_or(0);
    let output = usage
        .get("output_tokens")
        .or_else(|| usage.get("completion_tokens"))
        .and_then(|v| v.as_i64())
        .unwrap_or(0);
    let total = usage.get("total_tokens").and_then(|v| v.as_i64()).unwrap_or(0);
    Usage {
        input_tokens: input,
        output_tokens: output,
        total_tokens: total,
    }
}

/// Resolve a JSON Pointer to a `&Value`. An empty pointer means the whole
/// document. Returns `None` if the pointer does not resolve.
pub fn json_pointer_str<'a>(root: &'a Value, pointer: &str) -> Option<&'a Value> {
    if pointer.is_empty() {
        Some(root)
    } else {
        root.pointer(pointer)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_common_container_mimes() {
        // PNG magic bytes.
        let png = b"\x89PNG\r\n\x1a\n\x00\x00\x00";
        assert_eq!(detect_mime(png), Some("image/png"));
        // JPEG magic bytes.
        let jpeg = [0xFFu8, 0xD8, 0xFF, 0xE0, 0x00, 0x10];
        assert_eq!(detect_mime(&jpeg), Some("image/jpeg"));
        // Unknown bytes -> None (caller re-encodes via the image crate).
        assert_eq!(detect_mime(&[0u8, 1, 2, 3, 4, 5, 6, 7]), None);
    }

    #[test]
    fn build_image_base64_returns_mime_and_base64() {
        use base64::Engine;
        // A real 2x2 PNG: magic-byte pass-through yields ("image/png", <STANDARD b64>),
        // and the data-uri form packages exactly that base64.
        let img = image::RgbImage::from_pixel(2, 2, image::Rgb([1, 2, 3]));
        let mut cursor = std::io::Cursor::new(Vec::new());
        image::DynamicImage::ImageRgb8(img)
            .write_to(&mut cursor, image::ImageFormat::Png)
            .expect("encode png");
        let png = cursor.into_inner();
        let (mime, b64) = build_image_base64(&png).expect("png passes through");
        assert_eq!(mime, "image/png");
        assert_eq!(
            b64,
            base64::engine::general_purpose::STANDARD.encode(&png)
        );
        assert_eq!(
            build_image_data_uri(&png).unwrap(),
            format!("data:image/png;base64,{b64}")
        );
    }

    #[test]
    fn build_data_uri_passes_through_png() {
        // A real 2x2 PNG encoded by the image crate.
        let img = image::RgbImage::from_pixel(2, 2, image::Rgb([1, 2, 3]));
        let mut cursor = std::io::Cursor::new(Vec::new());
        image::DynamicImage::ImageRgb8(img)
            .write_to(&mut cursor, image::ImageFormat::Png)
            .expect("encode png");
        let png = cursor.into_inner();
        let uri = build_image_data_uri(&png).expect("png passes through");
        assert!(uri.starts_with("data:image/png;base64,"), "{uri}");
    }

    #[test]
    fn build_data_uri_reencodes_unknown_bytes_to_png() {
        // Not a recognized container; the image crate cannot decode arbitrary
        // bytes, so this fails closed rather than sending garbage.
        let err = build_image_data_uri(&[0u8; 16]).unwrap_err();
        assert!(matches!(err, ProviderError::Provider(_)));
    }

    #[test]
    fn build_data_uri_rejects_empty_bytes() {
        let err = build_image_data_uri(&[]).unwrap_err();
        assert!(matches!(err, ProviderError::Provider(_)));
    }

    #[test]
    fn strict_schema_drops_unsupported_keys_and_forces_required() {
        let v = strict_schema_value(crate::service::image_analysis::IMAGE_UNDERSTANDING_SCHEMA)
            .expect("understanding schema sanitizes");
        // $schema and title dropped.
        assert!(v.get("$schema").is_none());
        assert!(v.get("title").is_none());
        // Every property is required (sorted).
        let required: Vec<&str> = v["required"]
            .as_array()
            .unwrap()
            .iter()
            .map(|x| x.as_str().unwrap())
            .collect();
        assert_eq!(required, vec!["caption", "confidence", "scene", "tags"]);
        // additionalProperties retained.
        assert_eq!(v["additionalProperties"], false);
        // minItems / minLength / minimum / maximum dropped.
        assert!(v["properties"]["tags"].get("minItems").is_none());
        assert!(v["properties"]["confidence"].get("maximum").is_none());
    }

    #[test]
    fn strict_schema_forces_required_on_rating_properties() {
        let v = strict_schema_value(crate::service::image_analysis::IMAGE_RATING_SCHEMA)
            .expect("rating schema sanitizes");
        // The rating contract is a flat object: rating + rubric_id + rubric_version
        // + reasons. Strict mode forces every property to be required (sorted), and
        // drops the `minimum`/`maximum` range constraints (the code-owned
        // `validate_rating` re-enforces 1..=5 on the parsed response, so fail-closed
        // behavior is preserved — the injected schema only guides the model).
        let required: Vec<&str> = v["required"]
            .as_array()
            .unwrap()
            .iter()
            .map(|x| x.as_str().unwrap())
            .collect();
        assert_eq!(required, vec!["rating", "reasons", "rubric_id", "rubric_version"]);
        assert_eq!(v["additionalProperties"], false);
        assert_eq!(v["properties"]["rating"]["type"], "integer");
        assert!(
            v["properties"]["rating"].get("minimum").is_none(),
            "minimum dropped by strict sanitization"
        );
        assert!(
            v["properties"]["rating"].get("maximum").is_none(),
            "maximum dropped by strict sanitization"
        );
        // No nested `scores` array and no `confidence` remain after sanitization.
        assert!(v["properties"].get("scores").is_none(), "scores array still present");
        assert!(v["properties"].get("confidence").is_none(), "confidence still present");
    }

    #[test]
    fn extract_usage_tolerates_openai_chat_and_responses_field_names() {
        // OpenAI Chat shape.
        let chat: Value = serde_json::from_str(
            r#"{"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}"#,
        )
        .unwrap();
        let u = extract_usage(Some(&chat));
        assert_eq!(u.input_tokens, 10);
        assert_eq!(u.output_tokens, 5);
        assert_eq!(u.total_tokens, 15);

        // OpenAI Responses shape.
        let resp: Value =
            serde_json::from_str(r#"{"input_tokens": 7, "output_tokens": 3, "total_tokens": 10}"#)
                .unwrap();
        let u = extract_usage(Some(&resp));
        assert_eq!(u.input_tokens, 7);
        assert_eq!(u.output_tokens, 3);
        assert_eq!(u.total_tokens, 10);

        // Missing usage -> default zeros.
        assert_eq!(extract_usage(None), Usage::default());
    }

    #[test]
    fn parse_content_json_rejects_non_json() {
        let err = parse_content_json("not json").unwrap_err();
        assert_eq!(err, ProviderError::SchemaValidation);
        let ok = parse_content_json(r#"{"caption": "c"}"#).expect("valid json");
        assert_eq!(ok["caption"], "c");
    }

    #[test]
    fn json_pointer_resolves_root_and_paths() {
        let v: Value = serde_json::from_str(r#"{"a": {"b": 1}}"#).unwrap();
        assert_eq!(json_pointer_str(&v, "").unwrap(), &v);
        assert_eq!(json_pointer_str(&v, "/a/b").unwrap(), &1);
        assert!(json_pointer_str(&v, "/missing").is_none());
    }

    #[test]
    fn parse_rating_int_accepts_integer_and_integer_valued_float() {
        // Exact integer (the contract-correct form).
        assert_eq!(parse_rating_int(&serde_json::json!(4)), Some(4));
        // Integer-valued float a model may emit despite the `integer` schema.
        assert_eq!(parse_rating_int(&serde_json::json!(5.0)), Some(5));
        assert_eq!(parse_rating_int(&serde_json::json!(1.0)), Some(1));
    }

    #[test]
    fn parse_rating_int_rejects_fractional_float_without_truncation() {
        // A fractional rating is schema-invalid. It must NOT be truncated to 4
        // and pass validation — fail closed (None -> 0 -> SchemaValidation).
        assert_eq!(parse_rating_int(&serde_json::json!(4.9)), None);
        assert_eq!(parse_rating_int(&serde_json::json!(0.5)), None);
        assert_eq!(parse_rating_int(&serde_json::json!(-1.2)), None);
    }

    #[test]
    fn parse_rating_int_rejects_non_numeric() {
        assert_eq!(parse_rating_int(&serde_json::json!("4")), None);
        assert_eq!(parse_rating_int(&serde_json::json!(null)), None);
        assert_eq!(parse_rating_int(&serde_json::json!({"rating": 4})), None);
    }
}