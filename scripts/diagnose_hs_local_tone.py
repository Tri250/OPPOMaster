#!/usr/bin/env python3
"""Reproduce the highlight/shadow local-tone mask-edge halo on a 1D ramp.

The model copies the log-luma part of color.cuh/color.cl. It intentionally skips
ACES/AP1 conversion and feeds synthetic log2 luminance directly into the local
tone equation, which is enough to expose mask-reference inversions.
"""

from __future__ import annotations

import argparse
import csv
import math
from pathlib import Path


SHADOW_LOG_PIVOT = -3.05
SHADOW_LOG_WIDTH = 0.62
HIGHLIGHT_LOG_PIVOT = -2.80
HIGHLIGHT_LOG_WIDTH = 3.35


def clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def smoothstep(edge0: float, edge1: float, x: float) -> float:
    denom = max(edge1 - edge0, 1.0e-6)
    t = clamp((x - edge0) / denom, 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def shadow_upper_pivot() -> float:
    width = max(SHADOW_LOG_WIDTH, 0.35)
    return SHADOW_LOG_PIVOT + max(width * 0.40, 0.24)


def shadow_black_floor_weight(mask_ref: float) -> float:
    width = max(SHADOW_LOG_WIDTH, 0.35)
    upper = shadow_upper_pivot()
    black_start = upper - max(width * 7.20, 4.45)
    black_end = upper - max(width * 5.20, 3.25)
    return 0.30 + 0.70 * smoothstep(black_start, black_end, mask_ref)


def shadow_zone_weight(mask_ref: float) -> float:
    width = max(SHADOW_LOG_WIDTH, 0.35)
    upper = shadow_upper_pivot()
    fade_start = upper - max(width * 3.15, 1.95)
    tonal_weight = 1.0 - smoothstep(fade_start, upper, mask_ref)
    return clamp(tonal_weight * shadow_black_floor_weight(mask_ref), 0.0, 1.0)


def shadow_base_distance(mask_ref: float) -> float:
    return clamp(shadow_upper_pivot() - mask_ref, 0.0, 3.65)


def shadow_deep_recovery_weight(mask_ref: float) -> float:
    width = max(SHADOW_LOG_WIDTH, 0.35)
    upper = shadow_upper_pivot()
    deep_start = upper - max(width * 5.80, 3.60)
    deep_full = upper - max(width * 7.60, 4.70)
    return 1.0 - smoothstep(deep_full, deep_start, mask_ref)


def highlight_zone_weight(mask_ref: float, fixed: bool) -> float:
    t = smoothstep(HIGHLIGHT_LOG_PIVOT, HIGHLIGHT_LOG_PIVOT + HIGHLIGHT_LOG_WIDTH, mask_ref)
    if not fixed:
        return math.pow(t, 0.33)

    toe = 1.0e-3
    toe_pow = 0.1023292992
    inv_toe_range = 1.1135850
    return clamp((math.pow(t + toe, 0.33) - toe_pow) * inv_toe_range, 0.0, 1.0)


def compute_masks(
    mask_ref: float, shadow_amount: float, highlight_amount: float, fixed: bool
) -> tuple[float, float]:
    raw_shadow = shadow_zone_weight(mask_ref)
    raw_highlight = highlight_zone_weight(mask_ref, fixed)
    both_active = abs(shadow_amount) > 1.0e-6 and abs(highlight_amount) > 1.0e-6
    if both_active:
        return raw_shadow * (1.0 - 0.72 * raw_highlight), raw_highlight * (
            1.0 - 0.48 * raw_shadow
        )
    return raw_shadow, raw_highlight


def shadow_delta(
    mask_ref: float, shadow_amount: float, highlight_amount: float, fixed: bool
) -> float:
    _, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, fixed)
    shadow_weight = shadow_zone_weight(mask_ref)
    distance_to_pivot = shadow_base_distance(mask_ref)
    highlight_overlap = clamp(highlight_mask, 0.0, 1.0)
    lift_amount = max(shadow_amount, 0.0)
    darken_amount = max(-shadow_amount, 0.0)
    lift_delta = (
        lift_amount * 0.56 * distance_to_pivot * shadow_weight * (1.0 - 0.35 * highlight_overlap)
    )
    deep_lift_delta = (
        lift_amount
        * 0.26
        * distance_to_pivot
        * shadow_deep_recovery_weight(mask_ref)
        * (1.0 - 0.45 * highlight_overlap)
    )
    darken_delta = darken_amount * 0.42 * (0.30 + 0.70 * distance_to_pivot) * shadow_weight
    return lift_delta + deep_lift_delta - darken_delta


def highlight_delta(
    mask_ref: float, shadow_amount: float, highlight_amount: float, fixed: bool
) -> float:
    _, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, fixed)
    return -highlight_amount * 1.04 * highlight_mask


def base_delta(
    mask_ref: float, shadow_amount: float, highlight_amount: float, fixed: bool
) -> float:
    return shadow_delta(mask_ref, shadow_amount, highlight_amount, fixed) + highlight_delta(
        mask_ref, shadow_amount, highlight_amount, fixed
    )


def curve_slope(
    fn, mask_ref: float, shadow_amount: float, highlight_amount: float, fixed: bool
) -> float:
    eps_stops = 0.08
    delta_lo = fn(mask_ref - eps_stops, shadow_amount, highlight_amount, fixed)
    delta_hi = fn(mask_ref + eps_stops, shadow_amount, highlight_amount, fixed)
    return 1.0 + (delta_hi - delta_lo) / (2.0 * eps_stops)


def texture_detail_weight(detail: float) -> float:
    return 1.0 - smoothstep(0.28, 0.88, abs(detail))


def llf_detail_mix(detail: float) -> float:
    return 1.0 - smoothstep(0.42, 0.95, abs(detail))


def shadow_detail_preserve_weight(detail: float) -> float:
    mag = abs(detail)
    return smoothstep(0.045, 0.15, mag) * (1.0 - smoothstep(0.78, 1.35, mag))


def shadow_detail_sign_weight(detail: float) -> float:
    if detail >= 0.0:
        return 1.0
    return 1.0 - 0.65 * smoothstep(0.18, 0.72, -detail)


def shadow_detail_weight(mask_ref: float) -> float:
    noise_floor = smoothstep(SHADOW_LOG_PIVOT - 3.5, SHADOW_LOG_PIVOT - 1.75, mask_ref)
    return shadow_zone_weight(mask_ref) * noise_floor


def highlight_detail_weight(mask_ref: float, highlight_mask: float, detail: float) -> float:
    width = max(HIGHLIGHT_LOG_WIDTH, 0.35)
    tonal_weight = clamp(highlight_mask, 0.0, 1.0)
    noise_gate = smoothstep(0.035, 0.12, abs(detail))
    edge_guard = 1.0 - smoothstep(0.78, 1.45, abs(detail))
    clipped_guard = 1.0 - smoothstep(
        HIGHLIGHT_LOG_PIVOT + width * 1.15, HIGHLIGHT_LOG_PIVOT + width * 2.35, mask_ref
    )
    return tonal_weight * noise_gate * edge_guard * clipped_guard


def shadow_llf_detail_gain(detail: float, shadow_amount: float, shadow_zone: float) -> float:
    lift_amount = max(shadow_amount, 0.0)
    if lift_amount <= 1.0e-6 or shadow_zone <= 1.0e-6:
        return 1.0
    mag = abs(detail)
    noise_gate = smoothstep(0.035, 0.11, mag)
    fine_detail_gate = 1.0 - smoothstep(0.38, 0.82, mag)
    if noise_gate <= 0.0 or fine_detail_gate <= 0.0:
        return 1.0
    sigma_r_stops = 0.42
    x = clamp(mag / sigma_r_stops, 1.0e-4, 1.0)
    alpha = 1.0 - 0.34 * lift_amount
    remapped_mag = sigma_r_stops * math.pow(x, alpha)
    remap_gain = remapped_mag / max(mag, 1.0e-4)
    limited_gain = clamp(remap_gain - 1.0, 0.0, 0.34)
    mix = lift_amount * shadow_zone * noise_gate * fine_detail_gate
    return 1.0 + limited_gain * mix


def local_tone_mix(detail: float, local_delta: float, source_delta: float) -> float:
    mag = abs(detail)
    edge_weight = smoothstep(0.62, 1.55, mag)
    delta_mismatch = smoothstep(0.16, 0.52, abs(local_delta - source_delta))
    guard = clamp(
        edge_weight * (0.82 + 0.18 * delta_mismatch) + 0.18 * edge_weight * edge_weight,
        0.0,
        1.0,
    )
    return 1.0 - guard


def tonal_reference_mix(
    base_ref: float, source_ref: float, shadow_amount: float, highlight_amount: float, fixed: bool
) -> float:
    base_shadow, base_highlight = compute_masks(base_ref, shadow_amount, highlight_amount, fixed)
    source_shadow, source_highlight = compute_masks(
        source_ref, shadow_amount, highlight_amount, fixed
    )
    disagreement = 0.0
    if abs(shadow_amount) > 1.0e-6:
        disagreement = max(disagreement, abs(base_shadow - source_shadow))
        disagreement = max(disagreement, 0.50 * abs(base_highlight - source_highlight))
    if abs(highlight_amount) > 1.0e-6:
        disagreement = max(disagreement, abs(base_highlight - source_highlight))
    return smoothstep(0.025, 0.16, clamp(disagreement, 0.0, 1.0))


def apply_local_tone(
    source_log_y: float,
    base_log_y: float,
    shadow_amount: float,
    highlight_amount: float,
    fixed: bool,
) -> dict[str, float]:
    detail = source_log_y - base_log_y
    ref_mix = (
        tonal_reference_mix(base_log_y, source_log_y, shadow_amount, highlight_amount, fixed)
        if fixed
        else 0.0
    )
    mask_ref = base_log_y + (source_log_y - base_log_y) * ref_mix
    shadow_mask, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, fixed)

    bd = base_delta(mask_ref, shadow_amount, highlight_amount, fixed)
    base_curve_slope = clamp(
        curve_slope(base_delta, mask_ref, shadow_amount, highlight_amount, fixed), 0.42, 1.65
    )
    base_contrast_loss = clamp(1.0 / base_curve_slope - 1.0, 0.0, 0.62)
    shadow_curve_slope = clamp(
        curve_slope(shadow_delta, mask_ref, shadow_amount, highlight_amount, fixed), 0.58, 1.35
    )
    shadow_contrast_loss = clamp(1.0 / shadow_curve_slope - 1.0, 0.0, 0.46)
    highlight_curve_slope = clamp(
        curve_slope(highlight_delta, mask_ref, shadow_amount, highlight_amount, fixed), 0.50, 1.20
    )
    highlight_contrast_loss = clamp(1.0 / highlight_curve_slope - 1.0, 0.0, 0.42)

    shadow_texture_zone = shadow_detail_weight(mask_ref)
    texture_detail = texture_detail_weight(detail)
    shadow_detail_zone = shadow_texture_zone * texture_detail * shadow_detail_sign_weight(detail)
    highlight_detail_zone = (
        highlight_detail_weight(mask_ref, highlight_mask, detail) * texture_detail
    )
    contrast_recovery = 0.035 + 0.075 * max(base_contrast_loss, shadow_contrast_loss)
    shadow_detail_scale = (
        max(shadow_amount, 0.0)
        * shadow_detail_zone
        * shadow_detail_preserve_weight(detail)
        * contrast_recovery
        - 0.018 * max(shadow_amount, 0.0) * shadow_texture_zone * smoothstep(0.85, 1.80, -detail)
        + 0.025
        * max(-shadow_amount, 0.0)
        * shadow_detail_zone
        * max(base_contrast_loss, shadow_contrast_loss)
    )
    highlight_detail_scale = max(highlight_amount, 0.0) * highlight_detail_zone * (
        0.035 + 0.12 * highlight_contrast_loss
    )
    raw_detail_scale = clamp(
        (1.0 + shadow_detail_scale + highlight_detail_scale)
        * shadow_llf_detail_gain(detail, shadow_amount, shadow_texture_zone),
        0.97,
        1.24,
    )
    detail_scale = 1.0 + (raw_detail_scale - 1.0) * llf_detail_mix(detail)
    local_delta = bd + detail * (detail_scale - 1.0)
    source_delta = base_delta(source_log_y, shadow_amount, highlight_amount, fixed)
    mix = local_tone_mix(detail, local_delta, source_delta) * (1.0 - ref_mix)
    final_delta = source_delta + (local_delta - source_delta) * mix

    return {
        "detail": detail,
        "mask_ref": mask_ref,
        "ref_mix": ref_mix,
        "shadow_mask": shadow_mask,
        "highlight_mask": highlight_mask,
        "source_delta": source_delta,
        "local_delta": local_delta,
        "final_delta": final_delta,
    }


def gaussian_weights(sigma: float) -> list[float]:
    radius = max(1, math.ceil(3.0 * sigma))
    weights = [math.exp(-(tap * tap) * 0.5 / (sigma * sigma)) for tap in range(radius + 1)]
    norm = weights[0] + 2.0 * sum(weights[1:])
    return [w / norm for w in weights]


def range_weight(center: float, sample: float) -> float:
    edge_delta = max(abs(sample - center) - 0.24, 0.0)
    d = edge_delta / 0.48
    return math.pow(2.0, -(d * d))


def build_base(source: list[float], sigma: float = 18.0) -> list[float]:
    weights = gaussian_weights(sigma)
    out: list[float] = []
    last = len(source) - 1
    for x, center in enumerate(source):
        total = center * weights[0]
        weight_sum = weights[0]
        for tap in range(1, len(weights)):
            for sx in (min(x + tap, last), max(x - tap, 0)):
                w = weights[tap] * range_weight(center, source[sx])
                total += source[sx] * w
                weight_sum += w
        out.append(total / max(weight_sum, 1.0e-6))
    return out


def make_source(case: str, width: int) -> list[float]:
    if case == "hard":
        return [-4.0 if x < width // 2 else 0.0 for x in range(width)]
    if case == "soft":
        lo, hi = -4.0, 0.0
        edge_width = max(12, width // 7)
        start = width // 2 - edge_width // 2
        out = []
        for x in range(width):
            if x < start:
                out.append(lo)
            elif x >= start + edge_width:
                out.append(hi)
            else:
                out.append(lo + (hi - lo) * (x - start) / max(edge_width - 1, 1))
        return out
    if case == "pivot":
        lo, hi = -3.5, -1.0
        edge_width = max(24, width // 17)
        start = width // 2 - edge_width // 2
        out = []
        for x in range(width):
            if x < start:
                out.append(lo)
            elif x >= start + edge_width:
                out.append(hi)
            else:
                out.append(lo + (hi - lo) * (x - start) / max(edge_width - 1, 1))
        return out
    raise ValueError(f"unknown case: {case}")


def to_gray(log_y: float) -> int:
    return int(round(255.0 * clamp((log_y + 5.0) / 6.0, 0.0, 1.0)))


def artifact_color(extra: float) -> tuple[int, int, int]:
    v = clamp(abs(extra) / 0.25, 0.0, 1.0)
    if extra >= 0.0:
        return int(255 * v), int(255 * (1.0 - v)), int(255 * (1.0 - v))
    return int(255 * (1.0 - v)), int(255 * (1.0 - v)), int(255 * v)


def write_ppm(
    path: Path,
    source: list[float],
    current: list[dict[str, float]],
    fixed: list[dict[str, float]],
) -> None:
    band_height = 32
    width = len(source)
    height = band_height * 4
    pixels: list[tuple[int, int, int]] = []
    for y in range(height):
        band = y // band_height
        for x, src in enumerate(source):
            if band == 0:
                g = to_gray(src)
                pixels.append((g, g, g))
            elif band == 1:
                g = to_gray(src + current[x]["final_delta"])
                pixels.append((g, g, g))
            elif band == 2:
                g = to_gray(src + fixed[x]["final_delta"])
                pixels.append((g, g, g))
            else:
                current_extra = current[x]["final_delta"] - current[x]["source_delta"]
                fixed_extra = fixed[x]["final_delta"] - fixed[x]["source_delta"]
                pixels.append(artifact_color(current_extra - fixed_extra))
    with path.open("wb") as f:
        f.write(f"P6\n{width} {height}\n255\n".encode("ascii"))
        for r, g, b in pixels:
            f.write(bytes((r, g, b)))


def write_csv(path: Path, source: list[float], base: list[float], current, fixed) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(
            [
                "x",
                "source_log_y",
                "base_log_y",
                "current_final_delta",
                "current_source_delta",
                "current_extra",
                "fixed_final_delta",
                "fixed_source_delta",
                "fixed_extra",
                "fixed_ref_mix",
                "fixed_mask_ref",
            ]
        )
        for x, src in enumerate(source):
            current_extra = current[x]["final_delta"] - current[x]["source_delta"]
            fixed_extra = fixed[x]["final_delta"] - fixed[x]["source_delta"]
            writer.writerow(
                [
                    x,
                    src,
                    base[x],
                    current[x]["final_delta"],
                    current[x]["source_delta"],
                    current_extra,
                    fixed[x]["final_delta"],
                    fixed[x]["source_delta"],
                    fixed_extra,
                    fixed[x]["ref_mix"],
                    fixed[x]["mask_ref"],
                ]
            )


def summarize(name: str, results: list[dict[str, float]]) -> str:
    extras = [r["final_delta"] - r["source_delta"] for r in results]
    max_pos = max(extras)
    min_neg = min(extras)
    max_abs = max(extras, key=lambda v: abs(v))
    return f"{name}: max_pos={max_pos:.6f}, min_neg={min_neg:.6f}, max_abs={max_abs:.6f}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--case", choices=["hard", "soft", "pivot"], default="pivot")
    parser.add_argument("--width", type=int, default=512)
    parser.add_argument("--out", type=Path, default=Path("build/diagnostics/hs_local_tone_repro"))
    args = parser.parse_args()

    source = make_source(args.case, args.width)
    base = build_base(source)
    shadow_amount = 1.0
    highlight_amount = 1.0
    current = [
        apply_local_tone(s, b, shadow_amount, highlight_amount, False)
        for s, b in zip(source, base)
    ]
    fixed = [
        apply_local_tone(s, b, shadow_amount, highlight_amount, True)
        for s, b in zip(source, base)
    ]

    args.out.mkdir(parents=True, exist_ok=True)
    write_csv(args.out / "profile.csv", source, base, current, fixed)
    write_ppm(args.out / "profile.ppm", source, current, fixed)
    summary = "\n".join([summarize("current", current), summarize("fixed", fixed)])
    (args.out / "summary.txt").write_text(summary + "\n", encoding="utf-8")
    print(summary)
    print(f"wrote {args.out / 'profile.csv'}")
    print(f"wrote {args.out / 'profile.ppm'}")


if __name__ == "__main__":
    main()
