//! Env-gated live smoke tests against the real OpenRouter, Volcengine Ark, and
//! Volcengine Ark Coding Plan (Anthropic-compatible) APIs.
//!
//! These are NOT part of the default CI gate — they call real provider endpoints
//! and may incur cost. Each test loads a gitignored `.env.test` (via `dotenvy`) and
//! skips (prints + returns) when its API key is unset or empty, so `cargo test`
//! without credentials is a clean no-op pass. To run them, copy
//! `.env.test.example` to `.env.test`, fill in real keys, and run `cargo test`.
//!
//! Purpose: the mock-server tests in `openrouter.rs` / `volcengine_ark.rs` /
//! `anthropic_messages.rs` cover the drivers' request/response shapes against
//! `wiremock` fixtures. These live smokes
//! are the ground-truth check that the documented shapes match the real providers —
//! if a provider changes its envelope, the smoke fails and the parser/fixture gap
//! surfaces here rather than in production. On success they assert the parsed
//! outcome validates against the code-owned Alcedo contract (no active annotation
//! on a malformed real response), and print usage + provider request id for manual
//! inspection.

use crate::service::credential_vault::SecretString;
use crate::service::image_analysis::{
    ImageAnalysisProvider, validate_rating, validate_understanding,
};
use crate::service::provider_config::load_provider_configs;
use crate::service::providers::anthropic_messages::AnthropicMessagesProvider;
use crate::service::providers::openrouter::OpenRouterChatProvider;
use crate::service::providers::volcengine_ark::VolcengineArkResponsesProvider;

/// Return the first non-empty env var from `keys`, or `None` after printing a
/// skip line. Existing process env vars take precedence over `.env.test`
/// (`dotenvy` does not override already-set vars).
fn env_or_skip(keys: &[&str]) -> Option<String> {
    for k in keys {
        if let Ok(v) = std::env::var(k) {
            let v = v.trim().to_string();
            if !v.is_empty() {
                return Some(v);
            }
        }
    }
    eprintln!(
        "skip: {} not set (live smoke; set it in .env.test to enable)",
        keys.join(" or ")
    );
    None
}

/// A small 32x32 RGB gradient PNG. Large enough for a vision model to describe
/// meaningfully, small enough to upload quickly and cheaply on every smoke run.
fn smoke_image_png() -> Vec<u8> {
    let (w, h) = (32u32, 32u32);
    let mut img = image::RgbImage::new(w, h);
    for y in 0..h {
        for x in 0..w {
            let r = (x * 8) as u8;
            let g = (y * 8) as u8;
            let b = ((x + y) * 4) as u8;
            img.put_pixel(x, y, image::Rgb([r, g, b]));
        }
    }
    let mut cursor = std::io::Cursor::new(Vec::new());
    image::DynamicImage::ImageRgb8(img)
        .write_to(&mut cursor, image::ImageFormat::Png)
        .expect("encode smoke png");
    cursor.into_inner()
}

#[tokio::test]
async fn live_openrouter_smoke_describe_and_score() {
    let _ = dotenvy::from_filename(".env.test").ok();
    let key = env_or_skip(&["ALCEDO_OPENROUTER_API_KEY", "OPENROUTER_API_KEY"]);
    let Some(key) = key else {
        return;
    };

    let config = load_provider_configs(None)
        .expect("built-ins load")
        .get("openrouter")
        .expect("openrouter built-in")
        .clone();
    let provider = OpenRouterChatProvider::new(config).expect("provider builds");
    let secret = SecretString::new(key);
    let img = smoke_image_png();

    let out = provider
        .describe_image(&img, "", "alcedo-live-smoke", "", Some(&secret))
        .await
        .expect("live OpenRouter describe succeeded");
    eprintln!(
        "live openrouter describe: caption={:?} tags={:?} scene={:?} confidence={} usage={:?} req_id={}",
        out.caption, out.tags, out.scene, out.confidence, out.usage, out.provider_request_id
    );
    validate_understanding(&out).expect("live outcome validates against the Alcedo contract");

    let score = provider
        .score_image(
            &img,
            "",
            "alcedo-live-smoke",
            "alcedo-default-v1",
            "",
            "",
            Some(&secret),
        )
        .await
        .expect("live OpenRouter score succeeded");
    eprintln!(
        "live openrouter score: rating={} rubric={} usage={:?} req_id={}",
        score.rating, score.rubric_id, score.usage, score.provider_request_id
    );
    validate_rating(&score).expect("live rating validates against the Alcedo contract");
}

#[tokio::test]
async fn live_volcengine_ark_smoke_describe_and_score() {
    let _ = dotenvy::from_filename(".env.test").ok();
    let key = env_or_skip(&["ALCEDO_VOLCENGINE_ARK_API_KEY", "ALCEDO_ARK_API_KEY"]);
    let Some(key) = key else {
        return;
    };

    let config = load_provider_configs(None)
        .expect("built-ins load")
        .get("volcengine_ark")
        .expect("volcengine_ark built-in")
        .clone();
    let provider = VolcengineArkResponsesProvider::new(config).expect("provider builds");
    let secret = SecretString::new(key);
    let img = smoke_image_png();

    let out = provider
        .describe_image(&img, "", "alcedo-live-smoke", "", Some(&secret))
        .await
        .expect("live Volcengine Ark describe succeeded");
    eprintln!(
        "live volcengine_ark describe: caption={:?} tags={:?} scene={:?} confidence={} usage={:?} req_id={}",
        out.caption, out.tags, out.scene, out.confidence, out.usage, out.provider_request_id
    );
    validate_understanding(&out).expect("live outcome validates against the Alcedo contract");

    let score = provider
        .score_image(
            &img,
            "",
            "alcedo-live-smoke",
            "alcedo-default-v1",
            "",
            "",
            Some(&secret),
        )
        .await
        .expect("live Volcengine Ark score succeeded");
    eprintln!(
        "live volcengine_ark score: rating={} rubric={} usage={:?} req_id={}",
        score.rating, score.rubric_id, score.usage, score.provider_request_id
    );
    validate_rating(&score).expect("live rating validates against the Alcedo contract");
}

#[tokio::test]
async fn live_volcengine_ark_coding_smoke_describe_and_score() {
    // Ground-truth check for the Anthropic Messages driver against the Volcengine
    // Ark Coding Plan endpoint (`/api/coding/v1/messages`). Reuses the same Ark
    // API key as `live_volcengine_ark_smoke_*` — the Coding Plan is reached with
    // the same account credential, just a different base URL + Anthropic wire
    // shape. See `anthropic_messages.rs` for the Coding Plan ToS + vision caveats:
    // this is a validation smoke, NOT a production path. If the live call 401s,
    // flip `auth.type` in `volcengine_ark_coding.json` to `api_key_header`; if it
    // 404s on the model or rejects the image, adjust the `slug` to a confirmed
    // vision-capable Coding Plan model.
    let _ = dotenvy::from_filename(".env.test").ok();
    let key = env_or_skip(&["ALCEDO_VOLCENGINE_ARK_API_KEY", "ALCEDO_ARK_API_KEY"]);
    let Some(key) = key else {
        return;
    };

    let config = load_provider_configs(None)
        .expect("built-ins load")
        .get("volcengine_ark_coding")
        .expect("volcengine_ark_coding built-in")
        .clone();
    let provider = AnthropicMessagesProvider::new(config).expect("provider builds");
    let secret = SecretString::new(key);
    let img = smoke_image_png();

    let out = provider
        .describe_image(&img, "", "alcedo-live-smoke", "", Some(&secret))
        .await
        .expect("live Volcengine Ark Coding Plan describe succeeded");
    eprintln!(
        "live volcengine_ark_coding describe: caption={:?} tags={:?} scene={:?} confidence={} usage={:?} req_id={}",
        out.caption, out.tags, out.scene, out.confidence, out.usage, out.provider_request_id
    );
    validate_understanding(&out).expect("live outcome validates against the Alcedo contract");

    let score = provider
        .score_image(
            &img,
            "",
            "alcedo-live-smoke",
            "alcedo-default-v1",
            "",
            "",
            Some(&secret),
        )
        .await
        .expect("live Volcengine Ark Coding Plan score succeeded");
    eprintln!(
        "live volcengine_ark_coding score: rating={} rubric={} usage={:?} req_id={}",
        score.rating, score.rubric_id, score.usage, score.provider_request_id
    );
    validate_rating(&score).expect("live rating validates against the Alcedo contract");
}
