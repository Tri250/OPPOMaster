#!/usr/bin/env python3
"""Reproduce highlight/shadow local-tone mask-edge halos on 1D ramps.

The model mirrors the log-luma part of color.cuh/color.cl. It intentionally
skips ACEScc conversion and either feeds synthetic log2 luminance directly into
the local-tone equation or synthesizes RGB channel offsets that mimic
chromatic-aberration fringes at a sky/land edge.
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
AP1_LUMA = (0.27222872, 0.67408177, 0.05368952)


def clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def smoothstep(edge0: float, edge1: float, x: float) -> float:
    denom = max(edge1 - edge0, 1.0e-6)
    t = clamp((x - edge0) / denom, 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def softplus_distance(distance: float, softness: float) -> float:
    safe_softness = max(softness, 1.0e-4)
    x = distance / safe_softness
    if x > 20.0:
        return distance - safe_softness * math.log(2.0)
    return safe_softness * (math.log1p(math.exp(x)) - math.log(2.0))


def softrelu_distance(signed_distance: float, softness: float, onset: float) -> float:
    safe_softness = max(softness, 1.0e-4)
    safe_onset = max(onset, 0.0)
    x = (signed_distance - safe_onset) / safe_softness
    if x > 20.0:
        return signed_distance - safe_onset
    if x < -20.0:
        return safe_softness * math.exp(x)
    return safe_softness * math.log1p(math.exp(x))


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


def highlight_zone_weight(mask_ref: float, guarded: bool) -> float:
    _ = guarded
    t = smoothstep(HIGHLIGHT_LOG_PIVOT, HIGHLIGHT_LOG_PIVOT + HIGHLIGHT_LOG_WIDTH, mask_ref)
    toe = 1.0e-3
    toe_pow = 0.1023292992
    inv_toe_range = 1.1135850
    return clamp((math.pow(t + toe, 0.33) - toe_pow) * inv_toe_range, 0.0, 1.0)


def compute_masks(
    mask_ref: float, shadow_amount: float, highlight_amount: float, guarded: bool
) -> tuple[float, float]:
    raw_shadow = shadow_zone_weight(mask_ref)
    raw_highlight = highlight_zone_weight(mask_ref, guarded)
    both_active = abs(shadow_amount) > 1.0e-6 and abs(highlight_amount) > 1.0e-6
    if both_active:
        return raw_shadow * (1.0 - 0.72 * raw_highlight), raw_highlight * (
            1.0 - 0.48 * raw_shadow
        )
    return raw_shadow, raw_highlight


def shadow_delta(
    mask_ref: float, shadow_amount: float, highlight_amount: float, guarded: bool
) -> float:
    _, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, guarded)
    width = max(SHADOW_LOG_WIDTH, 0.35)
    upper = shadow_upper_pivot()
    lift_pivot = upper + max(width * 4.00, 2.48)
    distance_to_pivot = max(lift_pivot - mask_ref, 0.0)
    soft_distance = softplus_distance(distance_to_pivot, max(width * 2.18, 1.35))
    lift_shape = 1.0 - math.exp(-soft_distance / 1.25)
    deep_noise_compression = 1.0 - smoothstep(
        SHADOW_LOG_PIVOT - 4.15, SHADOW_LOG_PIVOT - 2.35, mask_ref
    )
    black_guard = 0.82 + 0.18 * shadow_black_floor_weight(mask_ref)
    highlight_overlap = clamp(highlight_mask, 0.0, 1.0)
    highlight_active = 1.0 if abs(highlight_amount) > 1.0e-6 else 0.0
    overlap_guard = 1.0 - 0.28 * highlight_active * highlight_overlap
    lift_amount = max(shadow_amount, 0.0)
    darken_amount = max(-shadow_amount, 0.0)
    lift_delta = lift_amount * (
        0.88 * lift_shape * black_guard * overlap_guard + 0.28 * deep_noise_compression
    )
    darken_delta = darken_amount * 0.34 * soft_distance * (0.85 + 0.15 * black_guard)
    return lift_delta - darken_delta


def highlight_delta(
    mask_ref: float, shadow_amount: float, highlight_amount: float, guarded: bool
) -> float:
    _ = shadow_amount, guarded
    width = max(HIGHLIGHT_LOG_WIDTH, 0.35)
    soft_distance = softrelu_distance(
        mask_ref - HIGHLIGHT_LOG_PIVOT,
        min(max(width * 0.12, 0.36), 0.55),
        min(max(width * 0.24, 0.72), 1.10),
    )
    reduce_amount = max(highlight_amount, 0.0)
    boost_amount = max(-highlight_amount, 0.0)
    reduce_delta = 1.68 * (1.0 - math.exp(-soft_distance / 1.33))
    boost_delta = 1.24 * (1.0 - math.exp(-soft_distance / 1.45))
    return boost_amount * boost_delta - reduce_amount * reduce_delta


def base_delta(
    mask_ref: float, shadow_amount: float, highlight_amount: float, guarded: bool
) -> float:
    return shadow_delta(mask_ref, shadow_amount, highlight_amount, guarded) + highlight_delta(
        mask_ref, shadow_amount, highlight_amount, guarded
    )


def curve_slope(
    fn, mask_ref: float, shadow_amount: float, highlight_amount: float, guarded: bool
) -> float:
    eps_stops = 0.08
    delta_lo = fn(mask_ref - eps_stops, shadow_amount, highlight_amount, guarded)
    delta_hi = fn(mask_ref + eps_stops, shadow_amount, highlight_amount, guarded)
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
    tonal_weight = shadow_zone_weight(mask_ref)
    signal_gate = smoothstep(SHADOW_LOG_PIVOT - 2.60, SHADOW_LOG_PIVOT - 1.20, mask_ref)
    upper_guard = 1.0 - smoothstep(SHADOW_LOG_PIVOT + 1.05, SHADOW_LOG_PIVOT + 2.10, mask_ref)
    practical_shadow = signal_gate * upper_guard
    return max(tonal_weight * signal_gate, 0.72 * practical_shadow)


def shadow_fill_light_weight(mask_ref: float) -> float:
    signal_gate = smoothstep(SHADOW_LOG_PIVOT - 2.45, SHADOW_LOG_PIVOT - 1.05, mask_ref)
    upper_guard = 1.0 - smoothstep(SHADOW_LOG_PIVOT + 1.55, SHADOW_LOG_PIVOT + 2.50, mask_ref)
    return signal_gate * upper_guard


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
    alpha = 1.0 - 0.38 * lift_amount
    remapped_mag = sigma_r_stops * math.pow(x, alpha)
    remap_gain = remapped_mag / max(mag, 1.0e-4)
    limited_gain = clamp(remap_gain - 1.0, 0.0, 0.38)
    mix = lift_amount * shadow_zone * noise_gate * fine_detail_gate
    return 1.0 + limited_gain * mix


def ap1_to_oklab(ap1: tuple[float, float, float]) -> tuple[float, float, float]:
    lms_l = 0.62217537 * ap1[0] + 0.34268438 * ap1[1] + 0.02339492 * ap1[2]
    lms_m = 0.26593478 * ap1[0] + 0.62930460 * ap1[1] + 0.10828100 * ap1[2]
    lms_s = 0.09725037 * ap1[0] + 0.18525749 * ap1[1] + 0.77254586 * ap1[2]
    l_ = math.copysign(abs(lms_l) ** (1.0 / 3.0), lms_l)
    m_ = math.copysign(abs(lms_m) ** (1.0 / 3.0), lms_m)
    s_ = math.copysign(abs(lms_s) ** (1.0 / 3.0), lms_s)
    return (
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_,
    )


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
    base_ref: float, source_ref: float, shadow_amount: float, highlight_amount: float, guarded: bool
) -> float:
    base_shadow, base_highlight = compute_masks(base_ref, shadow_amount, highlight_amount, guarded)
    source_shadow, source_highlight = compute_masks(
        source_ref, shadow_amount, highlight_amount, guarded
    )
    disagreement = 0.0
    if abs(shadow_amount) > 1.0e-6:
        disagreement = max(disagreement, abs(base_shadow - source_shadow))
        disagreement = max(disagreement, 0.50 * abs(base_highlight - source_highlight))
    if abs(highlight_amount) > 1.0e-6:
        disagreement = max(disagreement, abs(base_highlight - source_highlight))
    return smoothstep(0.025, 0.16, clamp(disagreement, 0.0, 1.0))


def local_detail_reference_guard(detail: float, ref_mix: float) -> float:
    edge_reference = smoothstep(0.42, 0.95, abs(detail))
    return 1.0 - ref_mix * edge_reference


def chromatic_fringe_guard(
    source_chroma: float,
    detail: float,
    ref_mix: float,
    active_highlight_mask: float,
    shadow_amount: float,
    highlight_amount: float,
) -> float:
    if highlight_amount <= 1.0e-6 and shadow_amount <= 1.0e-6:
        return 1.0
    chroma_gate = smoothstep(0.012, 0.060, source_chroma)
    detail_gate = smoothstep(0.055, 0.34, abs(detail)) * (
        1.0 - smoothstep(0.82, 1.42, abs(detail))
    )
    edge_gate = smoothstep(0.020, 0.12, ref_mix)
    highlight_gate = 0.35 + 0.65 * active_highlight_mask
    strength = 0.82 * chroma_gate * detail_gate * edge_gate * highlight_gate
    return 1.0 - clamp(strength, 0.0, 0.82)


def chromatic_local_mix_guard(
    source_chroma: float,
    detail: float,
    local_delta: float,
    source_delta: float,
    active_highlight_mask: float,
    shadow_amount: float,
    highlight_amount: float,
) -> float:
    if highlight_amount <= 1.0e-6 and shadow_amount <= 1.0e-6:
        return 1.0
    chroma_gate = smoothstep(0.012, 0.055, source_chroma)
    detail_gate = smoothstep(0.050, 0.20, abs(detail)) * (
        1.0 - smoothstep(0.85, 1.45, abs(detail))
    )
    mismatch_gate = smoothstep(0.055, 0.16, abs(local_delta - source_delta))
    highlight_gate = 0.35 + 0.65 * active_highlight_mask
    both_active_gate = 0.55 + 0.45 * clamp(
        min(max(shadow_amount, 0.0), max(highlight_amount, 0.0)), 0.0, 1.0
    )
    strength = 0.78 * chroma_gate * detail_gate * mismatch_gate * highlight_gate * both_active_gate
    return 1.0 - clamp(strength, 0.0, 0.78)


def apply_local_tone(
    source_log_y: float,
    base_log_y: float,
    shadow_amount: float,
    highlight_amount: float,
    guarded: bool,
    source_chroma: float = 0.0,
) -> dict[str, float]:
    detail = source_log_y - base_log_y
    ref_mix = tonal_reference_mix(base_log_y, source_log_y, shadow_amount, highlight_amount, guarded)
    mask_ref = base_log_y + (source_log_y - base_log_y) * ref_mix
    shadow_mask, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, guarded)

    bd = base_delta(mask_ref, shadow_amount, highlight_amount, guarded)
    base_curve_slope = clamp(
        curve_slope(base_delta, mask_ref, shadow_amount, highlight_amount, guarded), 0.42, 1.65
    )
    base_contrast_loss = clamp(1.0 / base_curve_slope - 1.0, 0.0, 0.62)
    shadow_curve_slope = clamp(
        curve_slope(shadow_delta, mask_ref, shadow_amount, highlight_amount, guarded), 0.58, 1.35
    )
    shadow_contrast_loss = clamp(1.0 / shadow_curve_slope - 1.0, 0.0, 0.46)
    highlight_curve_slope = clamp(
        curve_slope(highlight_delta, mask_ref, shadow_amount, highlight_amount, guarded),
        0.50,
        1.20,
    )
    highlight_contrast_loss = clamp(1.0 / highlight_curve_slope - 1.0, 0.0, 0.42)

    shadow_texture_zone = shadow_detail_weight(mask_ref)
    texture_detail = texture_detail_weight(detail)
    shadow_detail_zone = shadow_texture_zone * texture_detail * shadow_detail_sign_weight(detail)
    shadow_detail_preserve = shadow_detail_preserve_weight(detail)
    shadow_fill_light_zone = shadow_fill_light_weight(mask_ref)
    active_highlight_mask = clamp(highlight_mask, 0.0, 1.0) if abs(highlight_amount) > 1.0e-6 else 0.0
    fill_highlight_guard = 1.0 - 0.35 * active_highlight_mask
    fill_detail_polarity = 1.0 if detail >= 0.0 else 0.68
    highlight_detail_zone = highlight_detail_weight(mask_ref, highlight_mask, detail) * texture_detail
    fringe_guard = (
        chromatic_fringe_guard(
            source_chroma,
            detail,
            ref_mix,
            active_highlight_mask,
            shadow_amount,
            highlight_amount,
        )
        if guarded
        else 1.0
    )
    contrast_recovery = 0.035 + 0.075 * max(base_contrast_loss, shadow_contrast_loss)
    fill_light_recovery = 0.075 + 0.085 * max(base_contrast_loss, shadow_contrast_loss)
    shadow_detail_scale = (
        max(shadow_amount, 0.0) * shadow_detail_zone * shadow_detail_preserve * contrast_recovery
        - 0.018 * max(shadow_amount, 0.0) * shadow_texture_zone * smoothstep(0.85, 1.80, -detail)
        + max(shadow_amount, 0.0)
        * shadow_fill_light_zone
        * fill_highlight_guard
        * texture_detail
        * shadow_detail_preserve
        * fill_detail_polarity
        * fill_light_recovery
        + 0.025
        * max(-shadow_amount, 0.0)
        * shadow_detail_zone
        * max(base_contrast_loss, shadow_contrast_loss)
    ) * fringe_guard
    highlight_detail_scale = (
        max(highlight_amount, 0.0)
        * highlight_detail_zone
        * (0.035 + 0.12 * highlight_contrast_loss)
        * fringe_guard
    )
    raw_llf_gain = shadow_llf_detail_gain(
        detail,
        shadow_amount,
        max(shadow_texture_zone, 0.86 * shadow_fill_light_zone * fill_highlight_guard),
    )
    llf_gain = 1.0 + (raw_llf_gain - 1.0) * fringe_guard
    raw_detail_scale = clamp(
        (1.0 + shadow_detail_scale + highlight_detail_scale) * llf_gain,
        0.97,
        1.30,
    )
    detail_scale = 1.0 + (raw_detail_scale - 1.0) * llf_detail_mix(detail)
    local_delta = bd + detail * (detail_scale - 1.0)
    source_delta = base_delta(source_log_y, shadow_amount, highlight_amount, guarded)
    mix_guard = (
        chromatic_local_mix_guard(
            source_chroma,
            detail,
            local_delta,
            source_delta,
            active_highlight_mask,
            shadow_amount,
            highlight_amount,
        )
        if guarded
        else 1.0
    )
    mix = (
        local_tone_mix(detail, local_delta, source_delta)
        * local_detail_reference_guard(detail, ref_mix)
        * fringe_guard
        * mix_guard
    )
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
        "fringe_guard": fringe_guard,
        "mix_guard": mix_guard,
        "source_chroma": source_chroma,
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


def make_source(case: str, width: int) -> tuple[list[float], list[float], list[tuple[float, float, float]]]:
    rgb: list[tuple[float, float, float]] = []
    if case == "hard":
        source = [-4.0 if x < width // 2 else 0.0 for x in range(width)]
        return source, [0.0] * width, [(2.0**v, 2.0**v, 2.0**v) for v in source]
    if case == "soft":
        lo, hi = -4.0, 0.0
        edge_width = max(12, width // 7)
    elif case == "pivot":
        lo, hi = -3.5, -1.0
        edge_width = max(24, width // 17)
    elif case == "ca-edge":
        lo, hi = -4.25, -0.55
        edge_width = max(34, width // 9)
    else:
        raise ValueError(f"unknown case: {case}")

    start = width // 2 - edge_width // 2
    source: list[float] = []
    chroma: list[float] = []
    for x in range(width):
        if case == "ca-edge":
            t_r = smoothstep(0.0, edge_width - 1.0, x - start - 2.5)
            t_g = smoothstep(0.0, edge_width - 1.0, x - start)
            t_b = smoothstep(0.0, edge_width - 1.0, x - start + 3.0)
            r = 2.0 ** (lo + (hi - lo) * t_r)
            g = 2.0 ** (lo + (hi - lo) * t_g)
            b = 2.0 ** (lo + (hi - lo) * t_b)
            y = AP1_LUMA[0] * r + AP1_LUMA[1] * g + AP1_LUMA[2] * b
            lab = ap1_to_oklab((r, g, b))
            c = math.hypot(lab[1], lab[2])
            rgb.append((r, g, b))
            source.append(math.log2(max(y, 1.0e-8)))
            chroma.append(c)
        else:
            if x < start:
                v = lo
            elif x >= start + edge_width:
                v = hi
            else:
                v = lo + (hi - lo) * (x - start) / max(edge_width - 1, 1)
            source.append(v)
            chroma.append(0.0)
            rgb.append((2.0**v, 2.0**v, 2.0**v))
    return source, chroma, rgb


def to_gray(log_y: float) -> int:
    return int(round(255.0 * clamp((log_y + 5.0) / 6.0, 0.0, 1.0)))


def to_rgb(rgb: tuple[float, float, float]) -> tuple[int, int, int]:
    return tuple(int(round(255.0 * clamp(math.log2(max(c, 1.0e-8) + 5.0) / 6.0, 0.0, 1.0))) for c in rgb)


def artifact_color(extra: float) -> tuple[int, int, int]:
    v = clamp(abs(extra) / 0.25, 0.0, 1.0)
    if extra >= 0.0:
        return int(255 * v), int(255 * (1.0 - v)), int(255 * (1.0 - v))
    return int(255 * (1.0 - v)), int(255 * (1.0 - v)), int(255 * v)


def write_ppm(
    path: Path,
    source_rgb: list[tuple[float, float, float]],
    source_log: list[float],
    current: list[dict[str, float]],
    fixed: list[dict[str, float]],
) -> None:
    band_height = 32
    width = len(source_log)
    height = band_height * 4
    pixels: list[tuple[int, int, int]] = []
    for y in range(height):
        band = y // band_height
        for x, src in enumerate(source_log):
            if band == 0:
                pixels.append(to_rgb(source_rgb[x]))
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


def write_csv(path: Path, source: list[float], chroma: list[float], base: list[float], current, fixed) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(
            [
                "x",
                "source_log_y",
                "source_chroma",
                "base_log_y",
                "current_final_delta",
                "current_source_delta",
                "current_extra",
                "fixed_final_delta",
                "fixed_source_delta",
                "fixed_extra",
                "fixed_ref_mix",
                "fixed_mask_ref",
                "fixed_fringe_guard",
                "fixed_mix_guard",
            ]
        )
        for x, src in enumerate(source):
            current_extra = current[x]["final_delta"] - current[x]["source_delta"]
            fixed_extra = fixed[x]["final_delta"] - fixed[x]["source_delta"]
            writer.writerow(
                [
                    x,
                    src,
                    chroma[x],
                    base[x],
                    current[x]["final_delta"],
                    current[x]["source_delta"],
                    current_extra,
                    fixed[x]["final_delta"],
                    fixed[x]["source_delta"],
                    fixed_extra,
                    fixed[x]["ref_mix"],
                    fixed[x]["mask_ref"],
                    fixed[x]["fringe_guard"],
                    fixed[x]["mix_guard"],
                ]
            )


def summarize(name: str, results: list[dict[str, float]]) -> str:
    extras = [r["final_delta"] - r["source_delta"] for r in results]
    guards = [r["fringe_guard"] for r in results]
    max_pos = max(extras)
    min_neg = min(extras)
    max_abs = max(extras, key=lambda v: abs(v))
    mean_guard = sum(guards) / max(len(guards), 1)
    return (
        f"{name}: max_pos={max_pos:.6f}, min_neg={min_neg:.6f}, "
        f"max_abs={max_abs:.6f}, mean_fringe_guard={mean_guard:.6f}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--case", choices=["hard", "soft", "pivot", "ca-edge"], default="pivot")
    parser.add_argument("--width", type=int, default=512)
    parser.add_argument("--shadow", type=float, default=1.0)
    parser.add_argument("--highlight", type=float, default=1.0)
    parser.add_argument("--out", type=Path, default=Path("build/diagnostics/hs_local_tone_repro"))
    args = parser.parse_args()

    source, chroma, source_rgb = make_source(args.case, args.width)
    base = build_base(source)
    current = [
        apply_local_tone(s, b, args.shadow, args.highlight, False, c)
        for s, b, c in zip(source, base, chroma)
    ]
    fixed = [
        apply_local_tone(s, b, args.shadow, args.highlight, True, c)
        for s, b, c in zip(source, base, chroma)
    ]

    args.out.mkdir(parents=True, exist_ok=True)
    write_csv(args.out / "profile.csv", source, chroma, base, current, fixed)
    write_ppm(args.out / "profile.ppm", source_rgb, source, current, fixed)
    summary = "\n".join([summarize("current", current), summarize("fixed", fixed)])
    (args.out / "summary.txt").write_text(summary + "\n", encoding="utf-8")
    print(summary)
    print(f"wrote {args.out / 'profile.csv'}")
    print(f"wrote {args.out / 'profile.ppm'}")


if __name__ == "__main__":
    main()
