#!/usr/bin/env python3
"""Numerically compare highlight/shadow local-tone response variants.

The script uses numpy/matplotlib to make the tradeoffs visible:

- shadow lift range vs. output slope, so shadow contrast loss is measurable;
- highlight reduction range vs. output slope;
- perceived OKLab chroma retention for saturated highlight swatches.
"""

from __future__ import annotations

import math
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagnose_hs_local_tone as baseline  # noqa: E402


def smoothstep(edge0: float, edge1: float, x: np.ndarray) -> np.ndarray:
    t = np.clip((x - edge0) / max(edge1 - edge0, 1.0e-6), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def softplus_distance(distance: np.ndarray, softness: float) -> np.ndarray:
    x = np.clip(distance / max(softness, 1.0e-4), -40.0, 40.0)
    return softness * (np.log1p(np.exp(x)) - math.log(2.0))


def softrelu_distance(
    signed_distance: np.ndarray,
    softness: float,
    onset: float,
) -> np.ndarray:
    x = np.clip((signed_distance - max(onset, 0.0)) / max(softness, 1.0e-4), -40.0, 40.0)
    return softness * np.log1p(np.exp(x))


def shadow_black_floor_weight(mask_ref: np.ndarray) -> np.ndarray:
    width = max(baseline.SHADOW_LOG_WIDTH, 0.35)
    upper = baseline.shadow_upper_pivot()
    black_start = upper - max(width * 7.20, 4.45)
    black_end = upper - max(width * 5.20, 3.25)
    return 0.30 + 0.70 * smoothstep(black_start, black_end, mask_ref)


def highlight_zone_weight(mask_ref: np.ndarray) -> np.ndarray:
    t = smoothstep(
        baseline.HIGHLIGHT_LOG_PIVOT,
        baseline.HIGHLIGHT_LOG_PIVOT + baseline.HIGHLIGHT_LOG_WIDTH,
        mask_ref,
    )
    toe = 1.0e-3
    toe_pow = 0.1023292992
    inv_toe_range = 1.1135850
    return np.clip((np.power(t + toe, 0.33) - toe_pow) * inv_toe_range, 0.0, 1.0)


def candidate_shadow_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    width = max(baseline.SHADOW_LOG_WIDTH, 0.35)
    upper_pivot = baseline.shadow_upper_pivot()
    lift_pivot = upper_pivot + max(width * 1.60, 0.98)
    distance_to_pivot = np.maximum(lift_pivot - mask_ref, 0.0)
    soft_distance = softplus_distance(distance_to_pivot, max(width * 2.18, 1.35))
    black_guard = 0.82 + 0.18 * shadow_black_floor_weight(mask_ref)
    highlight_overlap = highlight_zone_weight(mask_ref)
    overlap_guard = 1.0 - 0.28 * np.clip(highlight_overlap, 0.0, 1.0)
    lift_amount = max(shadow_amount, 0.0)
    darken_amount = max(-shadow_amount, 0.0)
    lift_delta = lift_amount * 0.42 * soft_distance * black_guard * overlap_guard
    darken_delta = darken_amount * 0.34 * soft_distance * (0.85 + 0.15 * black_guard)
    return lift_delta - darken_delta


def candidate_highlight_delta(mask_ref: np.ndarray, highlight_amount: float) -> np.ndarray:
    distance_to_pivot = mask_ref - baseline.HIGHLIGHT_LOG_PIVOT
    width = max(baseline.HIGHLIGHT_LOG_WIDTH, 0.35)
    soft_distance = softrelu_distance(
        distance_to_pivot,
        min(max(width * 0.12, 0.36), 0.55),
        min(max(width * 0.24, 0.72), 1.10),
    )
    reduce_amount = max(highlight_amount, 0.0)
    boost_amount = max(-highlight_amount, 0.0)
    reduce_delta = 1.68 * (1.0 - np.exp(-soft_distance / 1.33))
    boost_delta = 1.24 * (1.0 - np.exp(-soft_distance / 1.45))
    return boost_amount * boost_delta - reduce_amount * reduce_delta


def candidate_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    return candidate_shadow_delta(
        mask_ref,
        shadow_amount,
        highlight_amount,
    ) + candidate_highlight_delta(mask_ref, highlight_amount)


def baseline_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    return np.array(
        [baseline.base_delta(float(x), shadow_amount, highlight_amount, True) for x in mask_ref],
        dtype=np.float64,
    )


def ap1_luminance(ap1: np.ndarray) -> np.ndarray:
    return 0.27222872 * ap1[..., 0] + 0.67408177 * ap1[..., 1] + 0.05368952 * ap1[..., 2]


def ap1_to_oklab(ap1: np.ndarray) -> np.ndarray:
    lms = np.empty_like(ap1)
    lms[..., 0] = (
        0.62217537 * ap1[..., 0] + 0.34268438 * ap1[..., 1] + 0.02339492 * ap1[..., 2]
    )
    lms[..., 1] = (
        0.26593478 * ap1[..., 0] + 0.62930460 * ap1[..., 1] + 0.10828100 * ap1[..., 2]
    )
    lms[..., 2] = (
        0.09725037 * ap1[..., 0] + 0.18525749 * ap1[..., 1] + 0.77254586 * ap1[..., 2]
    )
    lms = np.cbrt(lms)
    lab = np.empty_like(ap1)
    lab[..., 0] = (
        0.2104542553 * lms[..., 0]
        + 0.7936177850 * lms[..., 1]
        - 0.0040720468 * lms[..., 2]
    )
    lab[..., 1] = (
        1.9779984951 * lms[..., 0]
        - 2.4285922050 * lms[..., 1]
        + 0.4505937099 * lms[..., 2]
    )
    lab[..., 2] = (
        0.0259040371 * lms[..., 0]
        + 0.7827717662 * lms[..., 1]
        - 0.8086757660 * lms[..., 2]
    )
    return lab


def oklab_to_ap1(lab: np.ndarray) -> np.ndarray:
    l_ = lab[..., 0] + 0.3963377774 * lab[..., 1] + 0.2158037573 * lab[..., 2]
    m_ = lab[..., 0] - 0.1055613458 * lab[..., 1] - 0.0638541728 * lab[..., 2]
    s_ = lab[..., 0] - 0.0894841775 * lab[..., 1] - 1.2914855480 * lab[..., 2]
    l = l_ * l_ * l_
    m = m_ * m_ * m_
    s = s_ * s_ * s_
    ap1 = np.empty_like(lab)
    ap1[..., 0] = 2.09085732 * l - 1.16812363 * m + 0.10040848 * s
    ap1[..., 1] = -0.87435428 * l + 2.14592958 * m - 0.27429822 * s
    ap1[..., 2] = -0.05353206 * l - 0.36754978 * m + 1.34755888 * s
    return ap1


def fit_ap1_lower_gamut(adjusted: np.ndarray, neutral: np.ndarray) -> np.ndarray:
    scale = np.ones(adjusted.shape[:-1], dtype=np.float64)
    lower = -1.0e-5
    for c in range(3):
        mask = (adjusted[..., c] < lower) & (neutral[..., c] > adjusted[..., c])
        candidate = (neutral[..., c] - lower) / np.maximum(
            neutral[..., c] - adjusted[..., c],
            1.0e-8,
        )
        scale = np.where(mask, np.minimum(scale, candidate), scale)
    scale = np.clip(scale, 0.0, 1.0)
    return neutral + (adjusted - neutral) * scale[..., None]


def preserve_highlight_chroma(
    source_ap1: np.ndarray,
    output_ap1: np.ndarray,
    log_delta: np.ndarray,
) -> np.ndarray:
    source_lab = ap1_to_oklab(source_ap1)
    output_lab = ap1_to_oklab(output_ap1)
    source_chroma = np.hypot(source_lab[..., 1], source_lab[..., 2])
    output_chroma = np.hypot(output_lab[..., 1], output_lab[..., 2])
    highlight_mask = highlight_zone_weight(np.log2(np.maximum(ap1_luminance(source_ap1), 1.0e-8)))
    chroma_conf = smoothstep(0.012, 0.060, source_chroma)
    compression = smoothstep(0.18, 1.15, -log_delta)
    strength = np.clip(0.56 * highlight_mask * compression * chroma_conf, 0.0, 0.62)
    target_chroma = output_chroma + (source_chroma - output_chroma) * strength
    hue = source_lab[..., 1:3] / np.maximum(source_chroma[..., None], 1.0e-5)
    adjusted_lab = output_lab.copy()
    adjusted_lab[..., 1] = hue[..., 0] * target_chroma
    adjusted_lab[..., 2] = hue[..., 1] * target_chroma
    neutral_lab = output_lab.copy()
    neutral_lab[..., 1:3] = 0.0
    return fit_ap1_lower_gamut(oklab_to_ap1(adjusted_lab), oklab_to_ap1(neutral_lab))


def chroma_metrics(log_values: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    swatches = np.array(
        [
            [1.0, 0.34, 0.08],
            [1.0, 0.72, 0.12],
            [0.16, 1.0, 0.20],
            [0.08, 0.72, 1.0],
            [0.16, 0.22, 1.0],
            [1.0, 0.12, 0.82],
        ],
        dtype=np.float64,
    )
    source = []
    for log_y in log_values:
        y = 2.0**log_y
        scaled = swatches * (y / ap1_luminance(swatches))[:, None]
        source.append(scaled)
    source_ap1 = np.stack(source, axis=0)
    baseline_log_delta = baseline_delta(log_values, 0.0, 1.0)[:, None]
    candidate_log_delta = candidate_delta(log_values, 0.0, 1.0)[:, None]
    baseline_ap1 = source_ap1 * np.exp2(baseline_log_delta)[..., None]
    candidate_scaled = source_ap1 * np.exp2(candidate_log_delta)[..., None]
    candidate_ap1 = preserve_highlight_chroma(source_ap1, candidate_scaled, candidate_log_delta)

    source_c = np.hypot(ap1_to_oklab(source_ap1)[..., 1], ap1_to_oklab(source_ap1)[..., 2])
    baseline_c = np.hypot(ap1_to_oklab(baseline_ap1)[..., 1], ap1_to_oklab(baseline_ap1)[..., 2])
    candidate_c = np.hypot(ap1_to_oklab(candidate_ap1)[..., 1], ap1_to_oklab(candidate_ap1)[..., 2])
    return baseline_c / source_c, candidate_c / source_c, np.min(candidate_ap1, axis=-1)


def plot_response(out_dir: Path) -> str:
    x = np.linspace(-8.0, 3.0, 2400)
    shadow_base = baseline_delta(x, 1.0, 0.0)
    shadow_candidate = candidate_delta(x, 1.0, 0.0)
    high_base = baseline_delta(x, 0.0, 1.0)
    high_candidate = candidate_delta(x, 0.0, 1.0)
    shadow_base_slope = np.gradient(x + shadow_base, x)
    shadow_candidate_slope = np.gradient(x + shadow_candidate, x)
    high_base_slope = np.gradient(x + high_base, x)
    high_candidate_slope = np.gradient(x + high_candidate, x)

    fig, axes = plt.subplots(2, 2, figsize=(12, 7), constrained_layout=True)
    axes[0, 0].plot(x, shadow_base, label="baseline")
    axes[0, 0].plot(x, shadow_candidate, label="candidate")
    axes[0, 0].set_title("Shadow lift delta")
    axes[0, 0].set_ylabel("stops")
    axes[0, 0].legend()
    axes[0, 1].plot(x, shadow_base_slope, label="baseline")
    axes[0, 1].plot(x, shadow_candidate_slope, label="candidate")
    axes[0, 1].axhline(0.55, color="0.5", linestyle="--", linewidth=1)
    axes[0, 1].set_title("Shadow output slope")
    axes[1, 0].plot(x, high_base, label="baseline")
    axes[1, 0].plot(x, high_candidate, label="candidate")
    axes[1, 0].set_title("Highlight reduction delta")
    axes[1, 0].set_xlabel("log2 luminance")
    axes[1, 0].set_ylabel("stops")
    axes[1, 1].plot(x, high_base_slope, label="baseline")
    axes[1, 1].plot(x, high_candidate_slope, label="candidate")
    axes[1, 1].axhline(0.35, color="0.5", linestyle="--", linewidth=1)
    axes[1, 1].set_title("Highlight output slope")
    axes[1, 1].set_xlabel("log2 luminance")
    for ax in axes.flat:
        ax.grid(True, alpha=0.25)
    path = out_dir / "tone_response.png"
    fig.savefig(path, dpi=160)
    plt.close(fig)

    log_values = np.linspace(-1.0, 3.0, 64)
    baseline_chroma, candidate_chroma, candidate_min = chroma_metrics(log_values)
    fig, ax = plt.subplots(figsize=(8, 4.5), constrained_layout=True)
    ax.plot(log_values, baseline_chroma.mean(axis=1), label="baseline mean")
    ax.plot(log_values, candidate_chroma.mean(axis=1), label="candidate mean")
    ax.fill_between(
        log_values,
        np.percentile(candidate_chroma, 10, axis=1),
        np.percentile(candidate_chroma, 90, axis=1),
        alpha=0.18,
        label="candidate p10-p90",
    )
    ax.set_title("OKLab chroma retained while reducing highlights")
    ax.set_xlabel("source log2 luminance")
    ax.set_ylabel("C_out / C_source")
    ax.grid(True, alpha=0.25)
    ax.legend()
    chroma_path = out_dir / "highlight_chroma_retention.png"
    fig.savefig(chroma_path, dpi=160)
    plt.close(fig)

    shadow_zone = (x >= -4.75) & (x <= -2.8)
    high_zone = (x >= -2.8) & (x <= 3.0)
    lines = [
        f"shadow_min_slope_baseline={shadow_base_slope[shadow_zone].min():.6f}",
        f"shadow_min_slope_candidate={shadow_candidate_slope[shadow_zone].min():.6f}",
        f"shadow_delta_at_-5_baseline={np.interp(-5.0, x, shadow_base):.6f}",
        f"shadow_delta_at_-5_candidate={np.interp(-5.0, x, shadow_candidate):.6f}",
        f"shadow_delta_at_-3_baseline={np.interp(-3.0, x, shadow_base):.6f}",
        f"shadow_delta_at_-3_candidate={np.interp(-3.0, x, shadow_candidate):.6f}",
        f"highlight_max_reduction_baseline={-high_base[high_zone].min():.6f}",
        f"highlight_max_reduction_candidate={-high_candidate[high_zone].min():.6f}",
        f"highlight_min_slope_baseline={high_base_slope[high_zone].min():.6f}",
        f"highlight_min_slope_candidate={high_candidate_slope[high_zone].min():.6f}",
        f"highlight_chroma_mean_baseline={baseline_chroma.mean():.6f}",
        f"highlight_chroma_mean_candidate={candidate_chroma.mean():.6f}",
        f"candidate_min_ap1_channel={candidate_min.min():.8f}",
    ]
    summary = "\n".join(lines) + "\n"
    (out_dir / "summary.txt").write_text(summary, encoding="utf-8")
    return summary


def main() -> None:
    out_dir = Path("build/diagnostics/hs_tone_response")
    out_dir.mkdir(parents=True, exist_ok=True)
    print(plot_response(out_dir), end="")
    print(f"wrote {out_dir / 'tone_response.png'}")
    print(f"wrote {out_dir / 'highlight_chroma_retention.png'}")


if __name__ == "__main__":
    main()
