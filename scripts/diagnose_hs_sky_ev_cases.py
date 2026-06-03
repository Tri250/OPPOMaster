#!/usr/bin/env python3
"""Simulate highlight compression on measured sky/land EV cases.

The local-tone math mirrors the log-luma path in
alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh.  It uses
synthetic scene-linear AP1 luminance and compares three highlight models:

  legacy:  previous template curve, kept only for comparison.
  current: linear EV/ND-style ramp that reaches full high compression in sky range.
  ev_nd:   alias for the current graduated-ND model.
  ev_hdr:  HDR-merge style, extra roll-down for clouds, halo, and clipped sun.

Photometric anchors used by the cases:
  clear sky average luminance: 8000 cd/m2
  overcast sky average luminance: 2000 cd/m2
  white clouds: up to 30000 cd/m2
  direct sun: 1.6e9 cd/m2
  midday direct sunlight: 100000 lux
  shade illuminated by clear blue sky: 20000 lux
  overcast day: 10000 lux
  grass albedo: 0.20, from the common 0.15-0.25 range

The script converts luminance ratios to EV deltas with log2(L_a / L_b).
For ACEScc code values, one stop in the normal log segment is 1 / 17.52.

Source notes:
  https://docs.acescentral.com/encodings/acescc/
  https://www.neuronsimulator.org/en/8.2.7/guide/data/units.dat.html
  https://publications.idiap.ch/attachments/papers/2019/Wu_THESIS_2019.pdf
  https://www.velux.com/healthy-buildings/research-and-knowledge/deic-basic-book/daylight/daylighting
  https://pvpmc.sandia.gov/modeling-guide/1-weather-design-inputs/plane-of-array-poa-irradiance/calculating-poa-irradiance/poa-ground-reflected/albedo/
"""

from __future__ import annotations

import argparse
import csv
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable

import matplotlib
import numpy as np

matplotlib.use("Agg")
import matplotlib.pyplot as plt


SHADOW_LOG_PIVOT = -3.35
SHADOW_LOG_WIDTH = 0.62
HIGHLIGHT_LOG_PIVOT = -2.80
HIGHLIGHT_LOG_WIDTH = 3.65
MIDDLE_GRAY_LOG2 = math.log2(0.18)
AP1_LUMA = (0.27222872, 0.67408177, 0.05368952)

ACESCC_CODE_PER_EV = 1.0 / 17.52

CLEAR_SKY_LUMINANCE = 8000.0
OVERCAST_SKY_LUMINANCE = 2000.0
WHITE_CLOUD_LUMINANCE = 30000.0
SUN_LUMINANCE = 1.6e9
CLEAR_SUNLIGHT_LUX = 100000.0
CLEAR_SKY_SHADE_LUX = 20000.0
OVERCAST_LUX = 10000.0
GRASS_ALBEDO = 0.20
DARK_LAND_ALBEDO = 0.08
SNOW_ALBEDO = 0.80

MODEL_NAMES = ("legacy", "current", "ev_nd", "ev_hdr")


@dataclass(frozen=True)
class Params:
    shadow_log_pivot: float = SHADOW_LOG_PIVOT
    shadow_log_width: float = SHADOW_LOG_WIDTH
    highlight_log_pivot: float = HIGHLIGHT_LOG_PIVOT
    highlight_log_width: float = HIGHLIGHT_LOG_WIDTH
    nd_stops: float = 2.65
    sky_entry_ev: float = 0.0
    sky_full_ev: float = 4.0
    hdr_target_ev: float = 2.65
    hdr_extra_slope: float = 0.38
    hdr_max_stops: float = 6.0
    combo_highlight_full_ev: float = 5.30
    combo_shadow_lift_scale: float = 0.50


@dataclass
class SyntheticCase:
    name: str
    source_log: np.ndarray
    chroma: np.ndarray
    rgb: np.ndarray
    labels: np.ndarray
    width: int
    height: int
    notes: str

    def __post_init__(self) -> None:
        self.source_log = np.asarray(self.source_log, dtype=np.float64)
        self.chroma = np.asarray(self.chroma, dtype=np.float64)
        self.rgb = np.asarray(self.rgb, dtype=np.float64)
        self.labels = np.asarray(self.labels, dtype=object)


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


def curve_interval_mask(x: float, x0: float, x1: float) -> float:
    return 1.0 if x0 <= x < x1 else 0.0


def curve_lower_mask(x: float, x0: float) -> float:
    return 1.0 if x < x0 else 0.0


def curve_upper_mask(x: float, x0: float) -> float:
    return 1.0 if x >= x0 else 0.0


def curve_segment(x: float, x0: float, y0: float, x1: float, y1: float) -> float:
    t = clamp((x - x0) / (x1 - x0), 0.0, 1.0)
    return curve_interval_mask(x, x0, x1) * (y0 + (y1 - y0) * t)


def shadow_upper_pivot(params: Params) -> float:
    width = max(params.shadow_log_width, 0.35)
    return params.shadow_log_pivot + max(width * 0.40, 0.24)


def shadow_black_floor_weight(mask_ref: float, params: Params) -> float:
    width = max(params.shadow_log_width, 0.35)
    upper = shadow_upper_pivot(params)
    black_start = upper - max(width * 7.20, 4.45)
    black_end = upper - max(width * 5.20, 3.25)
    return 0.30 + 0.70 * smoothstep(black_start, black_end, mask_ref)


def shadow_range_weight(mask_ref: float) -> float:
    relative_ev = mask_ref - MIDDLE_GRAY_LOG2
    return 1.0 - smoothstep(-5.50, -0.50, relative_ev)


def shadow_reference_lift_delta(mask_ref: float) -> float:
    ev = mask_ref - MIDDLE_GRAY_LOG2
    return (
        curve_lower_mask(ev, -1.433) * 1.714
        + curve_segment(ev, -1.433, 1.714, -0.433, 1.676)
        + curve_segment(ev, -0.433, 1.676, 0.152, 1.316)
        + curve_segment(ev, 0.152, 1.316, 0.567, 0.958)
        + curve_segment(ev, 0.567, 0.958, 0.889, 0.718)
        + curve_segment(ev, 0.889, 0.718, 1.152, 0.519)
        + curve_segment(ev, 1.152, 0.519, 1.374, 0.339)
        + curve_segment(ev, 1.374, 0.339, 1.567, 0.179)
        + curve_segment(ev, 1.567, 0.179, 1.737, 0.089)
        + curve_segment(ev, 1.737, 0.089, 1.889, 0.068)
        + curve_segment(ev, 1.889, 0.068, 2.026, 0.054)
        + curve_segment(ev, 2.026, 0.054, 2.152, 0.036)
        + curve_segment(ev, 2.152, 0.036, 2.267, 0.020)
        + curve_segment(ev, 2.267, 0.020, 2.374, 0.009)
        + curve_segment(ev, 2.374, 0.009, 2.474, 0.0)
    )


def shadow_zone_weight(mask_ref: float, params: Params) -> float:
    width = max(params.shadow_log_width, 0.35)
    upper = shadow_upper_pivot(params)
    fade_start = upper - max(width * 3.15, 1.95)
    tonal_weight = 1.0 - smoothstep(fade_start, upper, mask_ref)
    return clamp(
        tonal_weight * shadow_black_floor_weight(mask_ref, params) * shadow_range_weight(mask_ref),
        0.0,
        1.0,
    )


def shadow_fill_plateau_weight(mask_ref: float, params: Params) -> float:
    lower_gate = smoothstep(params.shadow_log_pivot - 4.05, params.shadow_log_pivot - 1.75, mask_ref)
    upper_gate = 1.0 - 0.45 * smoothstep(
        params.shadow_log_pivot - 1.05, params.shadow_log_pivot + 3.15, mask_ref
    )
    return lower_gate * upper_gate


def highlight_zone_weight(mask_ref: float, params: Params) -> float:
    toe = 1.0e-3
    toe_pow = 0.1023292992
    inv_toe_range = 1.1135850
    t = smoothstep(params.highlight_log_pivot, params.highlight_log_pivot + params.highlight_log_width, mask_ref)
    return clamp((math.pow(t + toe, 0.33) - toe_pow) * inv_toe_range, 0.0, 1.0)


def compute_masks(
    mask_ref: float, shadow_amount: float, highlight_amount: float, params: Params
) -> tuple[float, float]:
    raw_shadow = shadow_zone_weight(mask_ref, params)
    raw_highlight = highlight_zone_weight(mask_ref, params)
    both_active = abs(shadow_amount) > 1.0e-6 and abs(highlight_amount) > 1.0e-6
    if both_active:
        return raw_shadow * (1.0 - 0.72 * raw_highlight), raw_highlight * (
            1.0 - 0.48 * raw_shadow
        )
    return raw_shadow, raw_highlight


def shadow_highlight_conflict(shadow_amount: float, highlight_amount: float) -> float:
    return clamp(min(max(shadow_amount, 0.0), max(highlight_amount, 0.0)), 0.0, 1.0)


def shadow_delta(mask_ref: float, shadow_amount: float, highlight_amount: float, params: Params) -> float:
    _, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, params)
    width = max(params.shadow_log_width, 0.35)
    upper = shadow_upper_pivot(params)
    lift_pivot = upper + max(width * 4.00, 2.48)
    distance_to_pivot = max(lift_pivot - mask_ref, 0.0)
    soft_distance = softplus_distance(distance_to_pivot, max(width * 2.18, 1.35))
    black_floor = shadow_black_floor_weight(mask_ref, params)
    black_guard = 0.82 + 0.18 * black_floor
    highlight_overlap = clamp(highlight_mask, 0.0, 1.0)
    highlight_active = 1.0 if abs(highlight_amount) > 1.0e-6 else 0.0
    overlap_guard = 1.0 - 0.28 * highlight_active * highlight_overlap
    conflict = shadow_highlight_conflict(shadow_amount, highlight_amount)
    lift_amount = max(shadow_amount, 0.0) * (1.0 - params.combo_shadow_lift_scale * conflict)
    darken_amount = max(-shadow_amount, 0.0)
    lift_delta = lift_amount * shadow_reference_lift_delta(mask_ref) * overlap_guard
    darken_delta = darken_amount * 0.34 * soft_distance * (0.85 + 0.15 * black_guard)
    return lift_delta - shadow_range_weight(mask_ref) * darken_delta


def legacy_highlight_reference_reduce_delta(relative_ev: float) -> float:
    return (
        curve_lower_mask(relative_ev, -1.433) * 0.015
        + curve_segment(relative_ev, -1.433, 0.015, -0.433, 0.035)
        + curve_segment(relative_ev, -0.433, 0.035, 0.152, 0.150)
        + curve_segment(relative_ev, 0.152, 0.150, 0.567, 0.115)
        + curve_segment(relative_ev, 0.567, 0.115, 0.889, 0.135)
        + curve_segment(relative_ev, 0.889, 0.135, 1.152, 0.150)
        + curve_segment(relative_ev, 1.152, 0.150, 1.374, 0.155)
        + curve_segment(relative_ev, 1.374, 0.155, 1.567, 0.155)
        + curve_segment(relative_ev, 1.567, 0.155, 1.737, 0.205)
        + curve_segment(relative_ev, 1.737, 0.205, 1.889, 0.295)
        + curve_segment(relative_ev, 1.889, 0.295, 2.026, 0.355)
        + curve_segment(relative_ev, 2.026, 0.355, 2.152, 0.400)
        + curve_segment(relative_ev, 2.152, 0.400, 2.267, 0.390)
        + curve_segment(relative_ev, 2.267, 0.390, 2.374, 0.325)
        + curve_segment(relative_ev, 2.374, 0.325, 2.474, 0.120)
        + curve_upper_mask(relative_ev, 2.474) * 0.120
    )


def highlight_shadow_combo_reduce_delta(relative_ev: float) -> float:
    return (
        curve_lower_mask(relative_ev, -1.124) * 0.266
        + curve_segment(relative_ev, -1.124, 0.266, -0.131, 0.218)
        + curve_segment(relative_ev, -0.131, 0.218, 0.389, 0.100)
        + curve_segment(relative_ev, 0.389, 0.100, 0.739, 0.017)
        + curve_segment(relative_ev, 0.739, 0.017, 1.018, 0.030)
        + curve_segment(relative_ev, 1.018, 0.030, 1.245, 0.025)
        + curve_segment(relative_ev, 1.245, 0.025, 1.435, 0.036)
        + curve_segment(relative_ev, 1.435, 0.036, 1.599, 0.062)
        + curve_segment(relative_ev, 1.599, 0.062, 1.753, 0.117)
        + curve_segment(relative_ev, 1.753, 0.117, 1.901, 0.180)
        + curve_segment(relative_ev, 1.901, 0.180, 2.036, 0.227)
        + curve_segment(relative_ev, 2.036, 0.227, 2.159, 0.262)
        + curve_segment(relative_ev, 2.159, 0.262, 2.271, 0.245)
        + curve_segment(relative_ev, 2.271, 0.245, 2.376, 0.205)
        + curve_segment(relative_ev, 2.376, 0.205, 2.474, 0.056)
        + curve_upper_mask(relative_ev, 2.474) * 0.056
    )


def ev_nd_reduce_delta(relative_ev: float, params: Params, conflict: float = 0.0) -> float:
    sky_full_ev = params.sky_full_ev + (params.combo_highlight_full_ev - params.sky_full_ev) * conflict
    zone = clamp((relative_ev - params.sky_entry_ev) / max(sky_full_ev - params.sky_entry_ev, 1.0e-6), 0.0, 1.0)
    return params.nd_stops * zone


def ev_hdr_reduce_delta(relative_ev: float, params: Params) -> float:
    sky_zone = smoothstep(params.sky_entry_ev, params.sky_full_ev, relative_ev)
    base_nd = min(params.nd_stops, params.hdr_target_ev) * sky_zone
    extra = max(relative_ev - params.hdr_target_ev, 0.0) * (1.0 - params.hdr_extra_slope)
    return clamp(base_nd + extra, 0.0, params.hdr_max_stops)


def highlight_delta(
    mask_ref: float,
    shadow_amount: float,
    highlight_amount: float,
    params: Params,
    model: str,
) -> float:
    width = max(params.highlight_log_width, 0.35)
    soft_distance = softrelu_distance(
        mask_ref - params.highlight_log_pivot,
        min(max(width * 0.12, 0.36), 0.55),
        min(max(width * 0.24, 0.62), 1.00),
    )
    reduce_amount = max(highlight_amount, 0.0)
    boost_amount = max(-highlight_amount, 0.0)
    reduce_delta = 0.0
    if reduce_amount > 1.0e-6:
        shadow_lift_delta = 0.0
        if shadow_amount > 1.0e-6:
            shadow_lift_delta = max(shadow_delta(mask_ref, shadow_amount, highlight_amount, params), 0.0)
        lifted_relative_ev = mask_ref + 0.18 * shadow_lift_delta - MIDDLE_GRAY_LOG2
        if model == "legacy":
            reference = legacy_highlight_reference_reduce_delta(lifted_relative_ev)
            combo = highlight_shadow_combo_reduce_delta(lifted_relative_ev)
            reduce_delta = reference + (combo - reference) * clamp(shadow_amount, 0.0, 1.0)
        elif model in ("current", "ev_nd"):
            conflict = shadow_highlight_conflict(shadow_amount, highlight_amount)
            reduce_delta = ev_nd_reduce_delta(lifted_relative_ev, params, conflict)
        elif model == "ev_hdr":
            reduce_delta = ev_hdr_reduce_delta(lifted_relative_ev, params)
        else:
            raise ValueError(f"unknown model: {model}")
    boost_delta = 1.24 * (1.0 - math.exp(-soft_distance / 1.45))
    return boost_amount * boost_delta - reduce_amount * reduce_delta


def base_delta(
    mask_ref: float,
    shadow_amount: float,
    highlight_amount: float,
    params: Params,
    model: str,
) -> float:
    return shadow_delta(mask_ref, shadow_amount, highlight_amount, params) + highlight_delta(
        mask_ref, shadow_amount, highlight_amount, params, model
    )


def curve_slope(
    fn: Callable[[float, float, float, Params, str], float],
    mask_ref: float,
    shadow_amount: float,
    highlight_amount: float,
    params: Params,
    model: str,
) -> float:
    eps_stops = 0.08
    delta_lo = fn(mask_ref - eps_stops, shadow_amount, highlight_amount, params, model)
    delta_hi = fn(mask_ref + eps_stops, shadow_amount, highlight_amount, params, model)
    return 1.0 + (delta_hi - delta_lo) / (2.0 * eps_stops)


def shadow_delta_wrapper(
    mask_ref: float, shadow_amount: float, highlight_amount: float, params: Params, model: str
) -> float:
    _ = model
    return shadow_delta(mask_ref, shadow_amount, highlight_amount, params)


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


def shadow_detail_weight(mask_ref: float, params: Params) -> float:
    tonal_weight = shadow_zone_weight(mask_ref, params)
    signal_gate = smoothstep(params.shadow_log_pivot - 2.60, params.shadow_log_pivot - 1.20, mask_ref)
    upper_guard = 1.0 - smoothstep(
        params.shadow_log_pivot + 1.05, params.shadow_log_pivot + 2.10, mask_ref
    )
    practical_shadow = signal_gate * upper_guard
    return max(tonal_weight * signal_gate, 0.72 * practical_shadow)


def shadow_fill_light_weight(mask_ref: float, params: Params) -> float:
    signal_gate = smoothstep(params.shadow_log_pivot - 2.45, params.shadow_log_pivot - 1.05, mask_ref)
    upper_guard = 1.0 - smoothstep(
        params.shadow_log_pivot + 1.55, params.shadow_log_pivot + 2.50, mask_ref
    )
    return signal_gate * upper_guard


def highlight_detail_weight(mask_ref: float, highlight_mask: float, detail: float, params: Params) -> float:
    width = max(params.highlight_log_width, 0.35)
    tonal_weight = clamp(highlight_mask, 0.0, 1.0)
    noise_gate = smoothstep(0.035, 0.12, abs(detail))
    edge_guard = 1.0 - smoothstep(0.78, 1.45, abs(detail))
    clipped_guard = 1.0 - smoothstep(
        params.highlight_log_pivot + width * 1.15,
        params.highlight_log_pivot + width * 2.35,
        mask_ref,
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
    alpha = 1.0 - 0.44 * lift_amount
    remapped_mag = sigma_r_stops * math.pow(x, alpha)
    remap_gain = remapped_mag / max(mag, 1.0e-4)
    limited_gain = clamp(remap_gain - 1.0, 0.0, 0.50)
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
    base_ref: float, source_ref: float, shadow_amount: float, highlight_amount: float, params: Params
) -> float:
    base_shadow, base_highlight = compute_masks(base_ref, shadow_amount, highlight_amount, params)
    source_shadow, source_highlight = compute_masks(source_ref, shadow_amount, highlight_amount, params)
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
    params: Params,
    model: str,
    source_chroma: float = 0.0,
) -> dict[str, float]:
    detail = source_log_y - base_log_y
    ref_mix = tonal_reference_mix(base_log_y, source_log_y, shadow_amount, highlight_amount, params)
    mask_ref = base_log_y + (source_log_y - base_log_y) * ref_mix
    shadow_mask, highlight_mask = compute_masks(mask_ref, shadow_amount, highlight_amount, params)

    bd = base_delta(mask_ref, shadow_amount, highlight_amount, params, model)
    base_curve_slope = clamp(
        curve_slope(base_delta, mask_ref, shadow_amount, highlight_amount, params, model), 0.42, 1.65
    )
    base_contrast_loss = clamp(1.0 / base_curve_slope - 1.0, 0.0, 0.62)
    shadow_curve_slope = clamp(
        curve_slope(shadow_delta_wrapper, mask_ref, shadow_amount, highlight_amount, params, model),
        0.58,
        1.35,
    )
    shadow_contrast_loss = clamp(1.0 / shadow_curve_slope - 1.0, 0.0, 0.46)
    highlight_curve_slope = clamp(
        curve_slope(highlight_delta, mask_ref, shadow_amount, highlight_amount, params, model),
        0.50,
        1.20,
    )
    highlight_contrast_loss = clamp(1.0 / highlight_curve_slope - 1.0, 0.0, 0.42)

    shadow_texture_zone = shadow_detail_weight(mask_ref, params)
    texture_detail = texture_detail_weight(detail)
    shadow_detail_zone = shadow_texture_zone * texture_detail * shadow_detail_sign_weight(detail)
    shadow_detail_preserve = shadow_detail_preserve_weight(detail)
    shadow_fill_light_zone = shadow_fill_light_weight(mask_ref, params)
    shadow_fill_plateau_zone = shadow_fill_plateau_weight(mask_ref, params)
    active_highlight_mask = clamp(highlight_mask, 0.0, 1.0) if abs(highlight_amount) > 1.0e-6 else 0.0
    conflict = shadow_highlight_conflict(shadow_amount, highlight_amount)
    conflict_detail_guard = 1.0 - conflict * smoothstep(0.05, 0.45, active_highlight_mask)
    fill_highlight_guard = 1.0 - 0.35 * active_highlight_mask
    fill_detail_polarity = 1.0 if detail >= 0.0 else 0.68
    highlight_detail_zone = highlight_detail_weight(mask_ref, highlight_mask, detail, params) * texture_detail
    fringe_guard = chromatic_fringe_guard(
        source_chroma,
        detail,
        ref_mix,
        active_highlight_mask,
        shadow_amount,
        highlight_amount,
    )
    contrast_recovery = 0.045 + 0.090 * max(base_contrast_loss, shadow_contrast_loss)
    fill_light_recovery = 0.095 + 0.105 * max(base_contrast_loss, shadow_contrast_loss)
    fill_plateau_recovery = 0.10 + 0.16 * max(base_contrast_loss, shadow_contrast_loss)
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
        + max(shadow_amount, 0.0)
        * shadow_fill_plateau_zone
        * fill_highlight_guard
        * texture_detail
        * shadow_detail_preserve
        * fill_detail_polarity
        * fill_plateau_recovery
        + 0.025
        * max(-shadow_amount, 0.0)
        * shadow_detail_zone
        * max(base_contrast_loss, shadow_contrast_loss)
    ) * fringe_guard * conflict_detail_guard
    highlight_detail_scale = (
        max(highlight_amount, 0.0)
        * highlight_detail_zone
        * (0.035 + 0.12 * highlight_contrast_loss)
        * fringe_guard
        * conflict_detail_guard
    )
    raw_llf_gain = shadow_llf_detail_gain(
        detail,
        shadow_amount,
        max(
            shadow_texture_zone,
            0.86 * shadow_fill_light_zone * fill_highlight_guard,
            0.42 * shadow_fill_plateau_zone * fill_highlight_guard,
        ),
    )
    llf_gain = 1.0 + (raw_llf_gain - 1.0) * fringe_guard * conflict_detail_guard
    raw_detail_scale = clamp(
        (1.0 + shadow_detail_scale + highlight_detail_scale) * llf_gain,
        0.97,
        1.38,
    )
    detail_scale = 1.0 + (raw_detail_scale - 1.0) * llf_detail_mix(detail)
    local_delta = bd + detail * (detail_scale - 1.0)
    source_delta = base_delta(source_log_y, shadow_amount, highlight_amount, params, model)
    mix_guard = chromatic_local_mix_guard(
        source_chroma,
        detail,
        local_delta,
        source_delta,
        active_highlight_mask,
        shadow_amount,
        highlight_amount,
    )
    mix = (
        local_tone_mix(detail, local_delta, source_delta)
        * local_detail_reference_guard(detail, ref_mix)
        * fringe_guard
        * mix_guard
        * conflict_detail_guard
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


def grass_luminance(illuminance_lux: float, albedo: float = GRASS_ALBEDO) -> float:
    return illuminance_lux * albedo / math.pi


def ev_delta(a_luminance: float, b_luminance: float) -> float:
    return math.log2(a_luminance / b_luminance)


def log_from_relative_ev(ev: float) -> float:
    return MIDDLE_GRAY_LOG2 + ev


def scaled_rgb_for_log_luma(
    rgb: tuple[float, float, float], log_y: float
) -> tuple[float, float, float]:
    y = max(AP1_LUMA[0] * rgb[0] + AP1_LUMA[1] * rgb[1] + AP1_LUMA[2] * rgb[2], 1.0e-8)
    scale = (2.0**log_y) / y
    return rgb[0] * scale, rgb[1] * scale, rgb[2] * scale


def chroma_from_rgb(rgb: tuple[float, float, float]) -> float:
    lab = ap1_to_oklab(rgb)
    return math.hypot(lab[1], lab[2])


def gaussian_weights(sigma: float) -> list[float]:
    radius = max(1, math.ceil(3.0 * sigma))
    weights = [math.exp(-(tap * tap) * 0.5 / (sigma * sigma)) for tap in range(radius + 1)]
    norm = weights[0] + 2.0 * sum(weights[1:])
    return [w / norm for w in weights]


def range_weight(center: float, sample: float) -> float:
    edge_delta = max(abs(sample - center) - 0.24, 0.0)
    d = edge_delta / 0.48
    return math.pow(2.0, -(d * d))


def at(buf: list[float], width: int, height: int, x: int, y: int) -> float:
    return buf[clamp_int(y, 0, height - 1) * width + clamp_int(x, 0, width - 1)]


def clamp_int(x: int, lo: int, hi: int) -> int:
    return max(lo, min(hi, x))


def build_base_2d(source: np.ndarray, width: int, height: int, sigma: float) -> np.ndarray:
    weights = gaussian_weights(sigma)
    source_2d = np.asarray(source, dtype=np.float64).reshape(height, width)
    horizontal = np.empty_like(source_2d)
    for y in range(height):
        for x in range(width):
            center = source_2d[y, x]
            total = center * weights[0]
            weight_sum = weights[0]
            for tap in range(1, len(weights)):
                for sx in (min(x + tap, width - 1), max(x - tap, 0)):
                    sample = source_2d[y, sx]
                    w = weights[tap] * range_weight(center, sample)
                    total += sample * w
                    weight_sum += w
            horizontal[y, x] = total / max(weight_sum, 1.0e-6)

    out = np.empty_like(source_2d)
    for y in range(height):
        for x in range(width):
            center_guidance = source_2d[y, x]
            center = horizontal[y, x]
            total = center * weights[0]
            weight_sum = weights[0]
            for tap in range(1, len(weights)):
                for sy in (min(y + tap, height - 1), max(y - tap, 0)):
                    sample = horizontal[sy, x]
                    guidance = source_2d[sy, x]
                    w = weights[tap] * range_weight(center_guidance, guidance)
                    total += sample * w
                    weight_sum += w
            out[y, x] = total / max(weight_sum, 1.0e-6)
    return out.reshape(-1)


def add_pixel(
    source: list[float],
    chroma: list[float],
    rgb: list[tuple[float, float, float]],
    labels: list[str],
    log_y: float,
    color: tuple[float, float, float],
    label: str,
) -> None:
    scaled = scaled_rgb_for_log_luma(color, log_y)
    source.append(log_y)
    chroma.append(chroma_from_rgb(scaled))
    rgb.append(scaled)
    labels.append(label)


def wave(x: float, y: float, ax: float, ay: float, phase: float = 0.0) -> float:
    return math.sin(x * ax + y * ay + phase)


def make_layered_case(
    name: str,
    width: int,
    height: int,
    sky_ev: float,
    land_ev: float,
    notes: str,
    overcast: bool = False,
    dark_land: bool = False,
) -> SyntheticCase:
    source: list[float] = []
    chroma: list[float] = []
    rgb: list[tuple[float, float, float]] = []
    labels: list[str] = []
    horizon = int(height * 0.58)
    sky_color = (0.42, 0.70, 1.35) if not overcast else (0.82, 0.88, 0.95)
    land_color = (0.48, 0.70, 0.32) if not dark_land else (0.26, 0.24, 0.20)
    for y in range(height):
        for x in range(width):
            if y < horizon:
                grad = 0.30 * (1.0 - y / max(horizon - 1, 1))
                texture = 0.05 * wave(x, y, 0.045, 0.017)
                add_pixel(
                    source,
                    chroma,
                    rgb,
                    labels,
                    log_from_relative_ev(sky_ev + grad + texture),
                    sky_color,
                    "blue_sky" if not overcast else "overcast_sky",
                )
            else:
                texture = 0.10 * wave(x, y, 0.070, 0.055, 1.4)
                add_pixel(
                    source,
                    chroma,
                    rgb,
                    labels,
                    log_from_relative_ev(land_ev + texture),
                    land_color,
                    "land",
                )
    return SyntheticCase(name, source, chroma, rgb, labels, width, height, notes)


def make_cloud_sun_case(width: int, height: int, clipped_sun_ev: float) -> SyntheticCase:
    shaded_land_l = grass_luminance(CLEAR_SKY_SHADE_LUX)
    blue_sky_ev = ev_delta(CLEAR_SKY_LUMINANCE, shaded_land_l)
    lit_cloud_ev = ev_delta(WHITE_CLOUD_LUMINANCE, shaded_land_l)
    unlit_cloud_ev = ev_delta(OVERCAST_SKY_LUMINANCE, shaded_land_l)
    source: list[float] = []
    chroma: list[float] = []
    rgb: list[tuple[float, float, float]] = []
    labels: list[str] = []
    horizon = int(height * 0.62)
    sun_cx, sun_cy = int(width * 0.72), int(height * 0.18)
    lit_cx, lit_cy = int(width * 0.43), int(height * 0.26)
    unlit_cx, unlit_cy = int(width * 0.30), int(height * 0.43)
    for y in range(height):
        for x in range(width):
            if y >= horizon:
                texture = 0.12 * wave(x, y, 0.060, 0.040, 2.1)
                add_pixel(source, chroma, rgb, labels, log_from_relative_ev(texture), (0.48, 0.70, 0.32), "land")
                continue

            dx, dy = x - sun_cx, y - sun_cy
            sun_r = math.hypot(dx, dy)
            if sun_r < 5.0:
                add_pixel(
                    source,
                    chroma,
                    rgb,
                    labels,
                    log_from_relative_ev(clipped_sun_ev),
                    (1.0, 0.97, 0.88),
                    "clipped_sun",
                )
                continue
            if sun_r < 34.0:
                halo_t = 1.0 - smoothstep(6.0, 34.0, sun_r)
                halo_ev = blue_sky_ev + (7.0 - blue_sky_ev) * halo_t
                add_pixel(source, chroma, rgb, labels, log_from_relative_ev(halo_ev), (1.0, 0.88, 0.62), "sun_halo")
                continue

            lit_d = ((x - lit_cx) / 58.0) ** 2 + ((y - lit_cy) / 18.0) ** 2
            unlit_d = ((x - unlit_cx) / 72.0) ** 2 + ((y - unlit_cy) / 22.0) ** 2
            if lit_d < 1.0:
                edge = 1.0 - smoothstep(0.62, 1.0, lit_d)
                texture = 0.10 * wave(x, y, 0.11, 0.07)
                ev = blue_sky_ev + (lit_cloud_ev - blue_sky_ev) * edge + texture
                add_pixel(source, chroma, rgb, labels, log_from_relative_ev(ev), (1.0, 0.97, 0.86), "lit_cloud")
            elif unlit_d < 1.0:
                edge = 1.0 - smoothstep(0.55, 1.0, unlit_d)
                texture = 0.08 * wave(x, y, 0.08, 0.05, 3.0)
                ev = blue_sky_ev + (unlit_cloud_ev - blue_sky_ev) * edge + texture
                add_pixel(source, chroma, rgb, labels, log_from_relative_ev(ev), (0.70, 0.76, 0.82), "unlit_cloud")
            else:
                grad = 0.22 * (1.0 - y / max(horizon - 1, 1))
                texture = 0.04 * wave(x, y, 0.045, 0.017)
                add_pixel(source, chroma, rgb, labels, log_from_relative_ev(blue_sky_ev + grad + texture), (0.42, 0.70, 1.35), "blue_sky")
    notes = (
        f"blue sky +{blue_sky_ev:.2f} EV, lit cloud +{lit_cloud_ev:.2f} EV, "
        f"unlit cloud +{unlit_cloud_ev:.2f} EV vs shaded grass; sun clipped at +{clipped_sun_ev:.2f} EV"
    )
    return SyntheticCase("clipped_sun_halo_clouds", source, chroma, rgb, labels, width, height, notes)


def make_snow_case(width: int, height: int) -> SyntheticCase:
    sunlit_snow_l = grass_luminance(CLEAR_SUNLIGHT_LUX, SNOW_ALBEDO)
    sky_ev_vs_snow = ev_delta(CLEAR_SKY_LUMINANCE, sunlit_snow_l)
    return make_layered_case(
        "snow_field_blue_sky",
        width,
        height,
        sky_ev=sky_ev_vs_snow,
        land_ev=0.0,
        notes=f"clear sky is {sky_ev_vs_snow:.2f} EV below sunlit snow using snow albedo 0.80",
        dark_land=False,
    )


def make_cases(width: int, height: int, clipped_sun_ev: float) -> list[SyntheticCase]:
    sunlit_grass = grass_luminance(CLEAR_SUNLIGHT_LUX)
    shaded_grass = grass_luminance(CLEAR_SKY_SHADE_LUX)
    overcast_grass = grass_luminance(OVERCAST_LUX)
    dark_shaded_land = grass_luminance(CLEAR_SKY_SHADE_LUX, DARK_LAND_ALBEDO)
    return [
        make_layered_case(
            "clear_sky_sunlit_land",
            width,
            height,
            sky_ev=ev_delta(CLEAR_SKY_LUMINANCE, sunlit_grass),
            land_ev=0.0,
            notes="clear blue sky vs sunlit grass; usually only a small EV separation",
        ),
        make_layered_case(
            "clear_sky_shaded_land",
            width,
            height,
            sky_ev=ev_delta(CLEAR_SKY_LUMINANCE, shaded_grass),
            land_ev=0.0,
            notes="clear blue sky vs shaded grass; main graduated-ND landscape case",
        ),
        make_layered_case(
            "overcast_sky_land",
            width,
            height,
            sky_ev=ev_delta(OVERCAST_SKY_LUMINANCE, overcast_grass),
            land_ev=0.0,
            notes="overcast sky vs grass under overcast illuminance",
            overcast=True,
        ),
        make_layered_case(
            "clear_sky_dark_foreground",
            width,
            height,
            sky_ev=ev_delta(CLEAR_SKY_LUMINANCE, dark_shaded_land),
            land_ev=0.0,
            notes="clear blue sky vs dark shaded foreground/asphalt-like albedo",
            dark_land=True,
        ),
        make_cloud_sun_case(width, height, clipped_sun_ev),
        make_snow_case(width, height),
    ]


def apply_case(
    case: SyntheticCase,
    model: str,
    params: Params,
    shadow_amount: float,
    highlight_amount: float,
    sigma: float,
) -> tuple[np.ndarray, list[dict[str, float]]]:
    base = build_base_2d(case.source_log, case.width, case.height, sigma)
    rows = [
        apply_local_tone(src, b, shadow_amount, highlight_amount, params, model, c)
        for src, b, c in zip(case.source_log, base, case.chroma)
    ]
    adjusted = case.source_log + np.asarray([row["final_delta"] for row in rows], dtype=np.float64)
    return adjusted, rows


def percentile(values: np.ndarray | list[float], p: float) -> float:
    arr = np.asarray(values, dtype=np.float64)
    if arr.size == 0:
        return 0.0
    return float(np.percentile(arr, p * 100.0, method="linear"))


def max_same_label_step(
    values: np.ndarray | list[float], labels: np.ndarray, width: int, height: int, label: str
) -> float:
    field = np.asarray(values, dtype=np.float64).reshape(height, width)
    label_field = np.asarray(labels, dtype=object).reshape(height, width)
    mask = label_field == label
    steps: list[np.ndarray] = []
    horizontal_mask = mask[:, 1:] & mask[:, :-1]
    vertical_mask = mask[1:, :] & mask[:-1, :]
    if np.any(horizontal_mask):
        steps.append(np.abs(np.diff(field, axis=1))[horizontal_mask])
    if np.any(vertical_mask):
        steps.append(np.abs(np.diff(field, axis=0))[vertical_mask])
    if not steps:
        return 0.0
    return float(max(np.max(step) for step in steps))


def same_label_reversal_stats(
    source: np.ndarray | list[float],
    adjusted: np.ndarray | list[float],
    labels: np.ndarray,
    width: int,
    height: int,
    label: str,
) -> tuple[int, float, float]:
    source_field = np.asarray(source, dtype=np.float64).reshape(height, width)
    adjusted_field = np.asarray(adjusted, dtype=np.float64).reshape(height, width)
    label_field = np.asarray(labels, dtype=object).reshape(height, width)
    mask = label_field == label
    reversal_count = 0
    pair_count = 0
    worst_reversal = 0.0
    min_local_slope = float("inf")

    for axis in (0, 1):
        source_diff = np.diff(source_field, axis=axis)
        adjusted_diff = np.diff(adjusted_field, axis=axis)
        if axis == 0:
            pair_mask = mask[1:, :] & mask[:-1, :]
        else:
            pair_mask = mask[:, 1:] & mask[:, :-1]
        valid = pair_mask & (np.abs(source_diff) > 0.02)
        if not np.any(valid):
            continue
        pair_count += int(np.count_nonzero(valid))
        slopes = adjusted_diff[valid] / source_diff[valid]
        min_local_slope = min(min_local_slope, float(np.min(slopes)))
        reversed_pairs = slopes < 0.0
        reversal_count += int(np.count_nonzero(reversed_pairs))
        if np.any(reversed_pairs):
            worst_reversal = max(worst_reversal, float(np.max(-slopes[reversed_pairs])))

    if min_local_slope == float("inf"):
        min_local_slope = 0.0
    reversal_fraction = reversal_count / max(pair_count, 1)
    return reversal_count, reversal_fraction, min_local_slope


def region_stats(
    case: SyntheticCase,
    adjusted: np.ndarray,
    rows: list[dict[str, float]],
    label: str,
) -> dict[str, float]:
    idx = np.flatnonzero(case.labels == label)
    source = case.source_log[idx] - MIDDLE_GRAY_LOG2
    out = adjusted[idx] - MIDDLE_GRAY_LOG2
    delta = np.asarray([rows[int(i)]["final_delta"] for i in idx], dtype=np.float64)
    src_range = percentile(source, 0.95) - percentile(source, 0.05)
    out_range = percentile(out, 0.95) - percentile(out, 0.05)
    contrast_ratio = out_range / max(src_range, 1.0e-6)
    return {
        "count": float(idx.size),
        "source_p50_ev": percentile(source, 0.50),
        "source_range_ev": src_range,
        "delta_mean_ev": float(np.mean(delta)) if delta.size else 0.0,
        "delta_min_ev": float(np.min(delta)) if delta.size else 0.0,
        "delta_max_ev": float(np.max(delta)) if delta.size else 0.0,
        "positive_delta_frac": float(np.mean(delta > 0.01)) if delta.size else 0.0,
        "adjusted_range_ev": out_range,
        "contrast_ratio": contrast_ratio,
        "max_same_label_delta_step_ev": max_same_label_step(
            np.asarray([row["final_delta"] for row in rows], dtype=np.float64),
            case.labels,
            case.width,
            case.height,
            label,
        ),
        "max_same_label_adjusted_step_ev": max_same_label_step(
            adjusted, case.labels, case.width, case.height, label
        ),
        "reversal_count": same_label_reversal_stats(
            case.source_log, adjusted, case.labels, case.width, case.height, label
        )[0],
        "reversal_fraction": same_label_reversal_stats(
            case.source_log, adjusted, case.labels, case.width, case.height, label
        )[1],
        "min_local_slope": same_label_reversal_stats(
            case.source_log, adjusted, case.labels, case.width, case.height, label
        )[2],
    }


def write_summary_csv(path: Path, rows: Iterable[dict[str, object]]) -> None:
    fieldnames = [
        "case",
        "model",
        "label",
        "count",
        "source_p50_ev",
        "source_range_ev",
        "delta_mean_ev",
        "delta_min_ev",
        "delta_max_ev",
        "positive_delta_frac",
        "adjusted_range_ev",
        "contrast_ratio",
        "max_same_label_delta_step_ev",
        "max_same_label_adjusted_step_ev",
        "reversal_count",
        "reversal_fraction",
        "min_local_slope",
        "notes",
    ]
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def write_pixels_csv(
    path: Path,
    case: SyntheticCase,
    model_outputs: dict[str, tuple[np.ndarray, list[dict[str, float]]]],
) -> None:
    fields = ["x", "y", "label", "source_ev"]
    for model in model_outputs:
        fields.extend([f"{model}_adjusted_ev", f"{model}_delta_ev", f"{model}_mask_ref_ev"])
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for y in range(case.height):
            for x in range(case.width):
                i = y * case.width + x
                row: dict[str, object] = {
                    "x": x,
                    "y": y,
                    "label": case.labels[i],
                    "source_ev": case.source_log[i] - MIDDLE_GRAY_LOG2,
                }
                for model, (adjusted, details) in model_outputs.items():
                    row[f"{model}_adjusted_ev"] = adjusted[i] - MIDDLE_GRAY_LOG2
                    row[f"{model}_delta_ev"] = details[i]["final_delta"]
                    row[f"{model}_mask_ref_ev"] = details[i]["mask_ref"] - MIDDLE_GRAY_LOG2
                writer.writerow(row)


def tone_map_image(rgb: np.ndarray, exposure_bias: float = -1.5) -> np.ndarray:
    scaled = np.maximum(rgb * (2.0**exposure_bias), 0.0)
    mapped = scaled / (1.0 + scaled)
    return np.clip(mapped ** (1.0 / 2.2), 0.0, 1.0)


def reshape_case_field(case: SyntheticCase, values: np.ndarray | list[float]) -> np.ndarray:
    return np.asarray(values, dtype=np.float64).reshape(case.height, case.width)


def adjusted_rgb_image(case: SyntheticCase, adjusted_log: np.ndarray) -> np.ndarray:
    source_log = reshape_case_field(case, case.source_log)
    adjusted = reshape_case_field(case, adjusted_log)
    scale = np.exp2(adjusted - source_log)[..., np.newaxis]
    return case.rgb.reshape(case.height, case.width, 3) * scale


def delta_field(case: SyntheticCase, details: list[dict[str, float]]) -> np.ndarray:
    return np.asarray([row["final_delta"] for row in details], dtype=np.float64).reshape(
        case.height, case.width
    )


def write_case_png(
    path: Path,
    case: SyntheticCase,
    model_outputs: dict[str, tuple[np.ndarray, list[dict[str, float]]]],
) -> None:
    models = list(model_outputs)
    nrows = 1 + len(models)
    fig, axes = plt.subplots(
        nrows,
        2,
        figsize=(11, max(3.2, nrows * 2.35)),
        constrained_layout=True,
        width_ratios=(1.0, 1.08),
    )
    axes = np.asarray(axes)
    fig.suptitle(f"{case.name}: {case.notes}", fontsize=10)

    source_rgb = case.rgb.reshape(case.height, case.width, 3)
    axes[0, 0].imshow(tone_map_image(source_rgb))
    axes[0, 0].set_title("source", loc="left", fontsize=9)
    axes[0, 0].axis("off")

    source_ev = reshape_case_field(case, case.source_log - MIDDLE_GRAY_LOG2)
    source_im = axes[0, 1].imshow(source_ev, cmap="viridis")
    axes[0, 1].set_title("source relative EV", loc="left", fontsize=9)
    axes[0, 1].axis("off")
    fig.colorbar(source_im, ax=axes[0, 1], fraction=0.035, pad=0.012, label="EV")

    all_deltas = [delta_field(case, details) for _, details in model_outputs.values()]
    max_abs_delta = max(0.25, *(float(np.max(np.abs(d))) for d in all_deltas))
    for i, model in enumerate(models):
        adjusted, details = model_outputs[model]
        image_ax = axes[i + 1, 0]
        delta_ax = axes[i + 1, 1]
        image_ax.imshow(tone_map_image(adjusted_rgb_image(case, adjusted)))
        image_ax.set_title(f"{model}: adjusted", loc="left", fontsize=9)
        image_ax.axis("off")

        delta = delta_field(case, details)
        im = delta_ax.imshow(delta, cmap="coolwarm", vmin=-max_abs_delta, vmax=max_abs_delta)
        delta_ax.set_title(f"{model}: EV delta", loc="left", fontsize=9)
        delta_ax.axis("off")
        fig.colorbar(im, ax=delta_ax, fraction=0.035, pad=0.012, label="EV")

    fig.savefig(path, dpi=160)
    plt.close(fig)


def write_response_plot(
    path: Path,
    models: list[str],
    params: Params,
    shadow_amount: float,
    highlight_amount: float,
) -> None:
    relative_ev = np.linspace(-2.0, 12.0, 600)
    fig, axes = plt.subplots(2, 1, figsize=(9, 7), constrained_layout=True)
    for model in models:
        deltas = np.asarray(
            [
                base_delta(log_from_relative_ev(float(ev)), shadow_amount, highlight_amount, params, model)
                for ev in relative_ev
            ],
            dtype=np.float64,
        )
        axes[0].plot(relative_ev, deltas, label=model)
        axes[1].plot(relative_ev, relative_ev + deltas, label=model)

    anchors = {
        "clear sky / shaded grass": ev_delta(CLEAR_SKY_LUMINANCE, grass_luminance(CLEAR_SKY_SHADE_LUX)),
        "white cloud / shaded grass": ev_delta(WHITE_CLOUD_LUMINANCE, grass_luminance(CLEAR_SKY_SHADE_LUX)),
        "sun clip test": 10.0,
    }
    for ax in axes:
        for label, ev in anchors.items():
            ax.axvline(ev, color="0.72", linewidth=0.8, linestyle="--")
            ax.text(ev, ax.get_ylim()[1], label, rotation=90, va="top", ha="right", fontsize=7)
        ax.grid(True, color="0.88", linewidth=0.7)
        ax.legend(loc="best")
        ax.set_xlabel("source relative EV from middle gray")
    axes[0].set_ylabel("applied delta (EV)")
    axes[0].set_title("Highlight compression response")
    axes[1].set_ylabel("output relative EV")
    axes[1].set_title("Output EV after compression")
    fig.savefig(path, dpi=160)
    plt.close(fig)


def print_ev_reference() -> None:
    sunlit_grass = grass_luminance(CLEAR_SUNLIGHT_LUX)
    shaded_grass = grass_luminance(CLEAR_SKY_SHADE_LUX)
    overcast_grass = grass_luminance(OVERCAST_LUX)
    dark_shaded = grass_luminance(CLEAR_SKY_SHADE_LUX, DARK_LAND_ALBEDO)
    snow = grass_luminance(CLEAR_SUNLIGHT_LUX, SNOW_ALBEDO)
    rows = [
        ("clear sky vs sunlit grass", ev_delta(CLEAR_SKY_LUMINANCE, sunlit_grass)),
        ("clear sky vs shaded grass", ev_delta(CLEAR_SKY_LUMINANCE, shaded_grass)),
        ("overcast sky vs overcast grass", ev_delta(OVERCAST_SKY_LUMINANCE, overcast_grass)),
        ("clear sky vs dark shaded land", ev_delta(CLEAR_SKY_LUMINANCE, dark_shaded)),
        ("white cloud vs shaded grass", ev_delta(WHITE_CLOUD_LUMINANCE, shaded_grass)),
        ("direct sun vs shaded grass", ev_delta(SUN_LUMINANCE, shaded_grass)),
        ("clear sky vs sunlit snow", ev_delta(CLEAR_SKY_LUMINANCE, snow)),
    ]
    print("EV references from luminance ratios:")
    for name, value in rows:
        print(f"  {name:34s} {value:+6.2f} EV")
    print(f"ACEScc normal log segment: 1 EV = {ACESCC_CODE_PER_EV:.8f} code value")


def parse_models(value: str) -> list[str]:
    if value == "all":
        return list(MODEL_NAMES)
    models = [part.strip() for part in value.split(",") if part.strip()]
    for model in models:
        if model not in MODEL_NAMES:
            raise argparse.ArgumentTypeError(f"unknown model {model}; use {', '.join(MODEL_NAMES)}")
    return models


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--width", type=int, default=64)
    parser.add_argument("--height", type=int, default=48)
    parser.add_argument("--sigma", type=float, default=18.0)
    parser.add_argument("--shadow", type=float, default=0.0)
    parser.add_argument("--highlight", type=float, default=1.0)
    parser.add_argument("--models", type=parse_models, default=list(MODEL_NAMES))
    parser.add_argument("--nd-stops", type=float, default=2.65)
    parser.add_argument("--sky-entry-ev", type=float, default=0.0)
    parser.add_argument("--sky-full-ev", type=float, default=4.0)
    parser.add_argument("--hdr-target-ev", type=float, default=2.65)
    parser.add_argument("--hdr-extra-slope", type=float, default=0.38)
    parser.add_argument("--hdr-max-stops", type=float, default=6.0)
    parser.add_argument("--combo-highlight-full-ev", type=float, default=5.30)
    parser.add_argument("--combo-shadow-lift-scale", type=float, default=0.50)
    parser.add_argument("--clipped-sun-ev", type=float, default=10.0)
    parser.add_argument("--out-dir", type=Path, default=Path("artifacts/hs_sky_ev_cases"))
    args = parser.parse_args()

    params = Params(
        nd_stops=args.nd_stops,
        sky_entry_ev=args.sky_entry_ev,
        sky_full_ev=args.sky_full_ev,
        hdr_target_ev=args.hdr_target_ev,
        hdr_extra_slope=args.hdr_extra_slope,
        hdr_max_stops=args.hdr_max_stops,
        combo_highlight_full_ev=args.combo_highlight_full_ev,
        combo_shadow_lift_scale=args.combo_shadow_lift_scale,
    )
    args.out_dir.mkdir(parents=True, exist_ok=True)
    print_ev_reference()

    summary_rows: list[dict[str, object]] = []
    cases = make_cases(args.width, args.height, args.clipped_sun_ev)
    write_response_plot(
        args.out_dir / "highlight_response.png",
        args.models,
        params,
        args.shadow,
        args.highlight,
    )
    for case in cases:
        outputs: dict[str, tuple[np.ndarray, list[dict[str, float]]]] = {}
        for model in args.models:
            outputs[model] = apply_case(case, model, params, args.shadow, args.highlight, args.sigma)
            adjusted, details = outputs[model]
            for label in sorted(set(case.labels)):
                stats = region_stats(case, adjusted, details, label)
                summary_rows.append(
                    {
                        "case": case.name,
                        "model": model,
                        "label": label,
                        "notes": case.notes,
                        **stats,
                    }
                )
        write_pixels_csv(args.out_dir / f"{case.name}.csv", case, outputs)
        write_case_png(args.out_dir / f"{case.name}.png", case, outputs)

    write_summary_csv(args.out_dir / "summary.csv", summary_rows)
    print(f"Wrote {len(cases)} cases to {args.out_dir}")
    print("Key columns: delta_max_ev <= 0 means the whole label darkened; "
          "contrast_ratio near 1 preserves local contrast; max_same_label_delta_step_ev flags jumps.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
