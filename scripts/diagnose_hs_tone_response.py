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

PREVIOUS_SHADOW_LOG_PIVOT = -3.05
PREVIOUS_SHADOW_LOG_WIDTH = 0.62
PREVIOUS_HIGHLIGHT_LOG_PIVOT = -2.80
PREVIOUS_HIGHLIGHT_LOG_WIDTH = 3.35


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


def shadow_range_weight(mask_ref: np.ndarray) -> np.ndarray:
    relative_ev = mask_ref - baseline.MIDDLE_GRAY_LOG2
    return 1.0 - smoothstep(-5.50, -0.50, relative_ev)


def shadow_reference_lift_delta(mask_ref: np.ndarray) -> np.ndarray:
    ev = mask_ref - baseline.MIDDLE_GRAY_LOG2
    xp = np.array(
        [-5.520, -3.935, -2.713, -1.713, -0.997, -0.433, 0.065, 0.475,
         0.850, 1.188, 1.485, 1.760, 2.015, 2.251, 2.474],
        dtype=np.float64,
    )
    fp = np.array(
        [3.170, 3.700, 2.974, 2.100, 1.564, 1.179, 0.807, 0.570,
         0.483, 0.415, 0.355, 0.300, 0.255, 0.220, 0.0],
        dtype=np.float64,
    )
    return np.interp(ev, xp, fp, left=fp[0], right=0.0)


def previous_shadow_upper_pivot() -> float:
    width = max(PREVIOUS_SHADOW_LOG_WIDTH, 0.35)
    return PREVIOUS_SHADOW_LOG_PIVOT + max(width * 0.40, 0.24)


def previous_shadow_black_floor_weight(mask_ref: np.ndarray) -> np.ndarray:
    width = max(PREVIOUS_SHADOW_LOG_WIDTH, 0.35)
    upper = previous_shadow_upper_pivot()
    black_start = upper - max(width * 7.20, 4.45)
    black_end = upper - max(width * 5.20, 3.25)
    return 0.30 + 0.70 * smoothstep(black_start, black_end, mask_ref)


def previous_highlight_zone_weight(mask_ref: np.ndarray) -> np.ndarray:
    t = smoothstep(
        PREVIOUS_HIGHLIGHT_LOG_PIVOT,
        PREVIOUS_HIGHLIGHT_LOG_PIVOT + PREVIOUS_HIGHLIGHT_LOG_WIDTH,
        mask_ref,
    )
    toe = 1.0e-3
    toe_pow = 0.1023292992
    inv_toe_range = 1.1135850
    return np.clip((np.power(t + toe, 0.33) - toe_pow) * inv_toe_range, 0.0, 1.0)


def previous_shadow_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    width = max(PREVIOUS_SHADOW_LOG_WIDTH, 0.35)
    upper_pivot = previous_shadow_upper_pivot()
    lift_pivot = upper_pivot + max(width * 4.00, 2.48)
    distance_to_pivot = np.maximum(lift_pivot - mask_ref, 0.0)
    soft_distance = softplus_distance(distance_to_pivot, max(width * 2.18, 1.35))
    lift_shape = 1.0 - np.exp(-soft_distance / 1.25)
    deep_noise_compression = 1.0 - smoothstep(
        PREVIOUS_SHADOW_LOG_PIVOT - 4.15,
        PREVIOUS_SHADOW_LOG_PIVOT - 2.35,
        mask_ref,
    )
    black_guard = 0.82 + 0.18 * previous_shadow_black_floor_weight(mask_ref)
    highlight_overlap = previous_highlight_zone_weight(mask_ref)
    highlight_active = 1.0 if abs(highlight_amount) > 1.0e-6 else 0.0
    overlap_guard = 1.0 - 0.28 * highlight_active * np.clip(highlight_overlap, 0.0, 1.0)
    lift_amount = max(shadow_amount, 0.0)
    darken_amount = max(-shadow_amount, 0.0)
    lift_delta = lift_amount * (
        0.88 * lift_shape * black_guard * overlap_guard + 0.28 * deep_noise_compression
    )
    darken_delta = darken_amount * 0.34 * soft_distance * (0.85 + 0.15 * black_guard)
    return lift_delta - darken_delta


def previous_highlight_delta(mask_ref: np.ndarray, highlight_amount: float) -> np.ndarray:
    distance_to_pivot = mask_ref - PREVIOUS_HIGHLIGHT_LOG_PIVOT
    width = max(PREVIOUS_HIGHLIGHT_LOG_WIDTH, 0.35)
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


def previous_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    return previous_shadow_delta(mask_ref, shadow_amount, highlight_amount) + previous_highlight_delta(
        mask_ref,
        highlight_amount,
    )


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
    lift_pivot = upper_pivot + max(width * 4.00, 2.48)
    distance_to_pivot = np.maximum(lift_pivot - mask_ref, 0.0)
    soft_distance = softplus_distance(distance_to_pivot, max(width * 2.18, 1.35))
    black_floor = shadow_black_floor_weight(mask_ref)
    black_guard = 0.82 + 0.18 * black_floor
    highlight_overlap = highlight_zone_weight(mask_ref)
    highlight_active = 1.0 if abs(highlight_amount) > 1.0e-6 else 0.0
    overlap_guard = 1.0 - 0.28 * highlight_active * np.clip(highlight_overlap, 0.0, 1.0)
    lift_amount = max(shadow_amount, 0.0)
    darken_amount = max(-shadow_amount, 0.0)
    lift_delta = lift_amount * shadow_reference_lift_delta(mask_ref) * overlap_guard
    darken_delta = darken_amount * 0.34 * soft_distance * (0.85 + 0.15 * black_guard)
    return lift_delta - shadow_range_weight(mask_ref) * darken_delta


def candidate_highlight_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    distance_to_pivot = mask_ref - baseline.HIGHLIGHT_LOG_PIVOT
    width = max(baseline.HIGHLIGHT_LOG_WIDTH, 0.35)
    soft_distance = softrelu_distance(
        distance_to_pivot,
        min(max(width * 0.12, 0.36), 0.55),
        min(max(width * 0.24, 0.62), 1.00),
    )
    reduce_amount = max(highlight_amount, 0.0)
    boost_amount = max(-highlight_amount, 0.0)
    reduce_delta = np.zeros_like(mask_ref)
    if reduce_amount > 1.0e-6:
        shadow_lift_delta = np.zeros_like(mask_ref)
        if shadow_amount > 1.0e-6:
            shadow_lift_delta = np.maximum(
                candidate_shadow_delta(mask_ref, shadow_amount, highlight_amount),
                0.0,
            )
        lifted_relative_ev = mask_ref + 0.18 * shadow_lift_delta - baseline.MIDDLE_GRAY_LOG2
        highlight_shelf = 0.60 * smoothstep(-2.80, 0.50, lifted_relative_ev)
        highlight_peak = 0.50 * smoothstep(-1.35, 1.60, lifted_relative_ev) * (
            1.0 - smoothstep(2.25, 4.90, lifted_relative_ev)
        )
        extreme_high_tail = 0.23 * smoothstep(3.00, 5.15, lifted_relative_ev)
        reduce_delta = highlight_shelf + highlight_peak + extreme_high_tail
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
    ) + candidate_highlight_delta(mask_ref, shadow_amount, highlight_amount)


def baseline_delta(
    mask_ref: np.ndarray,
    shadow_amount: float,
    highlight_amount: float,
) -> np.ndarray:
    return previous_delta(mask_ref, shadow_amount, highlight_amount)


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
    candidate_ap1 = source_ap1 * np.exp2(candidate_log_delta)[..., None]

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
    combined_candidate = candidate_delta(x, 1.0, 1.0)
    shadow_base_slope = np.gradient(x + shadow_base, x)
    shadow_candidate_slope = np.gradient(x + shadow_candidate, x)
    high_base_slope = np.gradient(x + high_base, x)
    high_candidate_slope = np.gradient(x + high_candidate, x)
    combined_candidate_slope = np.gradient(x + combined_candidate, x)

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
    middle_gray_log2 = math.log2(0.18)
    middle_gray_oklab_l = 0.18 ** (1.0 / 3.0)
    lines = [
        f"middle_gray_log2={middle_gray_log2:.6f}",
        f"middle_gray_oklab_l={middle_gray_oklab_l:.6f}",
        f"shadow_min_slope_baseline={shadow_base_slope[shadow_zone].min():.6f}",
        f"shadow_min_slope_candidate={shadow_candidate_slope[shadow_zone].min():.6f}",
        f"shadow_delta_at_-5_baseline={np.interp(-5.0, x, shadow_base):.6f}",
        f"shadow_delta_at_-5_candidate={np.interp(-5.0, x, shadow_candidate):.6f}",
        f"shadow_delta_at_-3_baseline={np.interp(-3.0, x, shadow_base):.6f}",
        f"shadow_delta_at_-3_candidate={np.interp(-3.0, x, shadow_candidate):.6f}",
        f"shadow_delta_at_middle_gray_baseline={np.interp(middle_gray_log2, x, shadow_base):.6f}",
        f"shadow_delta_at_middle_gray_candidate={np.interp(middle_gray_log2, x, shadow_candidate):.6f}",
        f"highlight_reduction_at_middle_gray_baseline={-np.interp(middle_gray_log2, x, high_base):.6f}",
        f"highlight_reduction_at_middle_gray_candidate={-np.interp(middle_gray_log2, x, high_candidate):.6f}",
        f"highlight_reduction_at_plus1ev_baseline={-np.interp(middle_gray_log2 + 1.0, x, high_base):.6f}",
        f"highlight_reduction_at_plus1ev_candidate={-np.interp(middle_gray_log2 + 1.0, x, high_candidate):.6f}",
        f"highlight_reduction_at_plus2ev_baseline={-np.interp(middle_gray_log2 + 2.0, x, high_base):.6f}",
        f"highlight_reduction_at_plus2ev_candidate={-np.interp(middle_gray_log2 + 2.0, x, high_candidate):.6f}",
        f"highlight_reduction_at_plus3ev_baseline={-np.interp(middle_gray_log2 + 3.0, x, high_base):.6f}",
        f"highlight_reduction_at_plus3ev_candidate={-np.interp(middle_gray_log2 + 3.0, x, high_candidate):.6f}",
        f"highlight_reduction_at_plus4ev_baseline={-np.interp(middle_gray_log2 + 4.0, x, high_base):.6f}",
        f"highlight_reduction_at_plus4ev_candidate={-np.interp(middle_gray_log2 + 4.0, x, high_candidate):.6f}",
        f"highlight_reduction_at_plus5ev_baseline={-np.interp(middle_gray_log2 + 5.0, x, high_base):.6f}",
        f"highlight_reduction_at_plus5ev_candidate={-np.interp(middle_gray_log2 + 5.0, x, high_candidate):.6f}",
        f"highlight_max_reduction_baseline={-high_base[high_zone].min():.6f}",
        f"highlight_max_reduction_candidate={-high_candidate[high_zone].min():.6f}",
        f"highlight_min_slope_baseline={high_base_slope[high_zone].min():.6f}",
        f"highlight_min_slope_candidate={high_candidate_slope[high_zone].min():.6f}",
        f"highlight_shadow_combined_min_slope_candidate={combined_candidate_slope.min():.6f}",
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
