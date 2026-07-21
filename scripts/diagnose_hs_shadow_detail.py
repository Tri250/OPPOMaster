#!/usr/bin/env python3
"""Measure perceived shadow contrast and fill-light detail for shadow tone mapping.

The test compares two shadow-lift curves:

- previous: the linearly growing deep-shadow lift used by the last iteration;
- candidate: the saturated lift now used by diagnose_hs_tone_response.py.

For each synthetic all-shadow patch, the reference is an ideal output with the
same center lift but unchanged local stop differences. The metric is the
standard deviation of OKLab L after tone mapping divided by that ideal output.

The fill-light section then runs the current local-tone detail model. In that
metric, values above 1.0 mean the shadow lift is not merely preserving texture:
it is making usable -1..-4EV shadow texture perceptually stronger, as a fill
light would. Deep shadows are still expected to stay near 1.0 so noise is not
promoted.
"""

from __future__ import annotations

import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagnose_hs_tone_response as tone  # noqa: E402


def previous_shadow_delta(mask_ref: np.ndarray) -> np.ndarray:
    return tone.previous_shadow_delta(mask_ref, 1.0, 0.0)


def candidate_active_limit() -> float:
    width = max(tone.baseline.SHADOW_LOG_WIDTH, 0.35)
    upper_pivot = tone.baseline.shadow_upper_pivot()
    return upper_pivot + max(width * 4.00, 2.48)


def candidate_shadow_delta(mask_ref: np.ndarray) -> np.ndarray:
    return tone.candidate_shadow_delta(mask_ref, 1.0, 0.0)


def practical_shadow_weight(center: np.ndarray) -> np.ndarray:
    pivot = tone.baseline.SHADOW_LOG_PIVOT
    signal_gate = tone.smoothstep(pivot - 2.60, pivot - 1.20, center)
    upper_guard = 1.0 - tone.smoothstep(pivot + 1.05, pivot + 2.10, center)
    return signal_gate * upper_guard


def shadow_zone_weight(mask_ref: np.ndarray) -> np.ndarray:
    width = max(tone.baseline.SHADOW_LOG_WIDTH, 0.35)
    upper_pivot = tone.baseline.shadow_upper_pivot()
    fade_start = upper_pivot - max(width * 3.15, 1.95)
    tonal_weight = 1.0 - tone.smoothstep(fade_start, upper_pivot, mask_ref)
    return np.clip(tonal_weight * tone.shadow_black_floor_weight(mask_ref), 0.0, 1.0)


def compute_masks(
    mask_ref: np.ndarray,
    shadow_amount: float = 1.0,
    highlight_amount: float = 0.0,
) -> tuple[np.ndarray, np.ndarray]:
    raw_shadow = shadow_zone_weight(mask_ref)
    raw_highlight = tone.highlight_zone_weight(mask_ref)
    both_active = abs(shadow_amount) > 1.0e-6 and abs(highlight_amount) > 1.0e-6
    if both_active:
        return raw_shadow * (1.0 - 0.72 * raw_highlight), raw_highlight * (
            1.0 - 0.48 * raw_shadow
        )
    return raw_shadow, raw_highlight


def tonal_reference_mix(base_ref: np.ndarray, source_ref: np.ndarray) -> np.ndarray:
    base_shadow, base_highlight = compute_masks(base_ref)
    source_shadow, source_highlight = compute_masks(source_ref)
    disagreement = np.maximum(
        np.abs(base_shadow - source_shadow),
        0.50 * np.abs(base_highlight - source_highlight),
    )
    return tone.smoothstep(0.025, 0.16, np.clip(disagreement, 0.0, 1.0))


def texture_detail_weight(detail: np.ndarray) -> np.ndarray:
    return 1.0 - tone.smoothstep(0.28, 0.88, np.abs(detail))


def llf_detail_mix(detail: np.ndarray) -> np.ndarray:
    return 1.0 - tone.smoothstep(0.42, 0.95, np.abs(detail))


def local_detail_reference_guard(
    detail: np.ndarray,
    reference_mix: np.ndarray,
    fill_light_candidate: bool,
) -> np.ndarray:
    if not fill_light_candidate:
        return 1.0 - reference_mix
    edge_reference = tone.smoothstep(0.42, 0.95, np.abs(detail))
    return 1.0 - reference_mix * edge_reference


def shadow_detail_preserve_weight(detail: np.ndarray) -> np.ndarray:
    mag = np.abs(detail)
    noise_gate = tone.smoothstep(0.045, 0.15, mag)
    edge_guard = 1.0 - tone.smoothstep(0.78, 1.35, mag)
    return noise_gate * edge_guard


def shadow_detail_sign_weight(detail: np.ndarray) -> np.ndarray:
    return np.where(detail >= 0.0, 1.0, 1.0 - 0.65 * tone.smoothstep(0.18, 0.72, -detail))


def shadow_llf_detail_gain(
    detail: np.ndarray,
    shadow_zone: np.ndarray,
    fill_light_candidate: bool,
) -> np.ndarray:
    mag = np.abs(detail)
    noise_gate = tone.smoothstep(0.035, 0.11, mag)
    fine_detail_gate = 1.0 - tone.smoothstep(0.38, 0.82, mag)
    sigma_r_stops = 0.42
    x = np.clip(mag / sigma_r_stops, 1.0e-4, 1.0)
    strength = 0.44 if fill_light_candidate else 0.34
    limit = 0.50 if fill_light_candidate else 0.34
    alpha = 1.0 - strength
    remapped_mag = sigma_r_stops * np.power(x, alpha)
    remap_gain = remapped_mag / np.maximum(mag, 1.0e-4)
    limited_gain = np.clip(remap_gain - 1.0, 0.0, limit)
    active = (shadow_zone > 1.0e-6) & (noise_gate > 0.0) & (fine_detail_gate > 0.0)
    return np.where(active, 1.0 + limited_gain * shadow_zone * noise_gate * fine_detail_gate, 1.0)


def local_tone_mix(detail: np.ndarray, local_delta: np.ndarray, source_delta: np.ndarray) -> np.ndarray:
    mag = np.abs(detail)
    edge_weight = tone.smoothstep(0.62, 1.55, mag)
    delta_mismatch = tone.smoothstep(0.16, 0.52, np.abs(local_delta - source_delta))
    guard = np.clip(
        edge_weight * (0.82 + 0.18 * delta_mismatch) + 0.18 * edge_weight * edge_weight,
        0.0,
        1.0,
    )
    return 1.0 - guard


def shadow_curve_slope(mask_ref: np.ndarray) -> np.ndarray:
    eps = 0.08
    delta_lo = tone.candidate_shadow_delta(mask_ref - eps, 1.0, 0.0)
    delta_hi = tone.candidate_shadow_delta(mask_ref + eps, 1.0, 0.0)
    return 1.0 + (delta_hi - delta_lo) / (2.0 * eps)


def base_curve_slope(mask_ref: np.ndarray) -> np.ndarray:
    eps = 0.08
    delta_lo = tone.candidate_delta(mask_ref - eps, 1.0, 0.0)
    delta_hi = tone.candidate_delta(mask_ref + eps, 1.0, 0.0)
    return 1.0 + (delta_hi - delta_lo) / (2.0 * eps)


def shadow_detail_weight(mask_ref: np.ndarray) -> np.ndarray:
    tonal_weight = shadow_zone_weight(mask_ref)
    pivot = tone.baseline.SHADOW_LOG_PIVOT
    signal_gate = tone.smoothstep(pivot - 2.60, pivot - 1.20, mask_ref)
    upper_guard = 1.0 - tone.smoothstep(pivot + 1.05, pivot + 2.10, mask_ref)
    practical_shadow = signal_gate * upper_guard
    return np.maximum(tonal_weight * signal_gate, 0.72 * practical_shadow)


def shadow_fill_light_weight(mask_ref: np.ndarray) -> np.ndarray:
    pivot = tone.baseline.SHADOW_LOG_PIVOT
    signal_gate = tone.smoothstep(pivot - 2.45, pivot - 1.05, mask_ref)
    upper_guard = 1.0 - tone.smoothstep(pivot + 1.55, pivot + 2.50, mask_ref)
    return signal_gate * upper_guard


def shadow_fill_plateau_weight(mask_ref: np.ndarray) -> np.ndarray:
    pivot = tone.baseline.SHADOW_LOG_PIVOT
    lower_gate = tone.smoothstep(pivot - 4.05, pivot - 1.75, mask_ref)
    upper_gate = 1.0 - 0.45 * tone.smoothstep(pivot - 1.05, pivot + 3.15, mask_ref)
    return lower_gate * upper_gate


def neutral_oklab_l(log_y: np.ndarray) -> np.ndarray:
    y = np.exp2(log_y)
    ap1 = np.stack([y, y, y], axis=-1)
    return tone.ap1_to_oklab(ap1)[..., 0]


def contrast_retention(delta_fn, center: float, half_range: float) -> float:
    phase = np.linspace(0.0, 2.0 * np.pi, 1024, endpoint=False)
    source = center + half_range * np.sin(phase)
    output = source + delta_fn(source)
    ideal = source + float(delta_fn(np.array([center], dtype=np.float64))[0])
    ideal_std = np.std(neutral_oklab_l(ideal))
    if ideal_std <= 1.0e-10:
        return 1.0
    return float(np.std(neutral_oklab_l(output)) / ideal_std)


def local_tone_output(
    center: float,
    half_range: float,
    fill_light_candidate: bool,
) -> tuple[np.ndarray, np.ndarray]:
    phase = np.linspace(0.0, 2.0 * np.pi, 2048, endpoint=False)
    source = center + half_range * np.sin(phase)
    base = np.full_like(source, center)
    detail = source - base
    reference_mix = tonal_reference_mix(base, source)
    mask_ref = base + detail * reference_mix

    base_delta = tone.candidate_delta(mask_ref, 1.0, 0.0)
    base_slope = np.clip(base_curve_slope(mask_ref), 0.42, 1.65)
    base_contrast_loss = np.clip(1.0 / base_slope - 1.0, 0.0, 0.62)
    shadow_slope = np.clip(shadow_curve_slope(mask_ref), 0.58, 1.35)
    shadow_contrast_loss = np.clip(1.0 / shadow_slope - 1.0, 0.0, 0.46)
    contrast_loss = np.maximum(base_contrast_loss, shadow_contrast_loss)

    shadow_texture_zone = shadow_detail_weight(mask_ref)
    texture_detail = texture_detail_weight(detail)
    shadow_detail_zone = (
        shadow_texture_zone * texture_detail * shadow_detail_sign_weight(detail)
    )
    shadow_detail_preserve = shadow_detail_preserve_weight(detail)
    shadow_fill_plateau_zone = shadow_fill_plateau_weight(mask_ref)
    dark_valley = tone.smoothstep(0.85, 1.80, -detail)
    contrast_recovery = 0.045 + 0.090 * contrast_loss
    shadow_detail_scale = (
        shadow_detail_zone * shadow_detail_preserve * contrast_recovery
        - 0.018 * shadow_texture_zone * dark_valley
    )

    llf_shadow_zone = shadow_texture_zone
    raw_detail_cap = 1.24
    if fill_light_candidate:
        fill_zone = shadow_fill_light_weight(mask_ref)
        fill_detail_polarity = np.where(detail >= 0.0, 1.0, 0.68)
        fill_light_recovery = 0.095 + 0.105 * contrast_loss
        fill_plateau_recovery = 0.10 + 0.16 * contrast_loss
        shadow_detail_scale += (
            fill_zone
            * texture_detail
            * shadow_detail_preserve
            * fill_detail_polarity
            * fill_light_recovery
        )
        shadow_detail_scale += (
            shadow_fill_plateau_zone
            * texture_detail
            * shadow_detail_preserve
            * fill_detail_polarity
            * fill_plateau_recovery
        )
        llf_shadow_zone = np.maximum.reduce(
            [llf_shadow_zone, 0.86 * fill_zone, 0.42 * shadow_fill_plateau_zone]
        )
        raw_detail_cap = 1.38

    raw_detail_scale = np.clip(
        (1.0 + shadow_detail_scale)
        * shadow_llf_detail_gain(detail, llf_shadow_zone, fill_light_candidate),
        0.97,
        raw_detail_cap,
    )
    detail_scale = 1.0 + (raw_detail_scale - 1.0) * llf_detail_mix(detail)
    local_delta = base_delta + detail * (detail_scale - 1.0)
    source_delta = tone.candidate_delta(source, 1.0, 0.0)
    mix = local_tone_mix(detail, local_delta, source_delta) * local_detail_reference_guard(
        detail,
        reference_mix,
        fill_light_candidate,
    )
    output = source + source_delta + (local_delta - source_delta) * mix
    return source, output


def fill_light_contrast(center: float, half_range: float, fill_light_candidate: bool) -> float:
    source, output = local_tone_output(center, half_range, fill_light_candidate)
    ideal = source + float(tone.candidate_delta(np.array([center]), 1.0, 0.0)[0])
    ideal_std = np.std(neutral_oklab_l(ideal))
    if ideal_std <= 1.0e-10:
        return 1.0
    return float(np.std(neutral_oklab_l(output)) / ideal_std)


def make_grid(delta_fn, centers: np.ndarray, half_ranges: np.ndarray) -> np.ndarray:
    upper = candidate_active_limit()
    grid = np.full((len(half_ranges), len(centers)), np.nan, dtype=np.float64)
    for row, half_range in enumerate(half_ranges):
        for col, center in enumerate(centers):
            if center + half_range > upper:
                continue
            grid[row, col] = contrast_retention(delta_fn, float(center), float(half_range))
    return grid


def make_fill_light_grid(
    centers: np.ndarray,
    half_ranges: np.ndarray,
    fill_light_candidate: bool,
) -> np.ndarray:
    upper = candidate_active_limit()
    grid = np.full((len(half_ranges), len(centers)), np.nan, dtype=np.float64)
    for row, half_range in enumerate(half_ranges):
        for col, center in enumerate(centers):
            if center + half_range > upper:
                continue
            grid[row, col] = fill_light_contrast(
                float(center),
                float(half_range),
                fill_light_candidate,
            )
    return grid


def selected_lines(delta_fn, center: float, half_range: float) -> tuple[np.ndarray, np.ndarray]:
    x = np.linspace(0.0, 1.0, 512)
    source = center + half_range * np.sin(2.0 * np.pi * x)
    output = source + delta_fn(source)
    return x, neutral_oklab_l(output)


def plot(out_dir: Path) -> str:
    centers = np.linspace(-7.0, -1.35, 84)
    half_ranges = np.linspace(0.12, 1.20, 45)
    previous = make_grid(previous_shadow_delta, centers, half_ranges)
    candidate = make_grid(candidate_shadow_delta, centers, half_ranges)
    improvement = candidate - previous
    priority = practical_shadow_weight(centers)[None, :]
    previous_practical = previous * priority
    candidate_practical = candidate * priority
    practical_improvement = candidate_practical - previous_practical

    fig, axes = plt.subplots(1, 3, figsize=(14, 4.5), constrained_layout=True)
    extent = [centers[0], centers[-1], half_ranges[0], half_ranges[-1]]
    for ax, data, title, vmin, vmax in [
        (axes[0], previous, "previous OKLab-L contrast retained", 0.55, 1.02),
        (axes[1], candidate, "candidate OKLab-L contrast retained", 0.55, 1.02),
        (axes[2], improvement, "candidate - previous", -0.05, 0.32),
    ]:
        image = ax.imshow(
            data,
            origin="lower",
            aspect="auto",
            extent=extent,
            vmin=vmin,
            vmax=vmax,
            cmap="viridis" if "candidate -" not in title else "magma",
        )
        ax.set_title(title)
        ax.set_xlabel("shadow patch center, log2 luminance")
        ax.set_ylabel("local half range, stops")
        fig.colorbar(image, ax=ax, shrink=0.88)
    heatmap_path = out_dir / "shadow_perceptual_contrast.png"
    fig.savefig(heatmap_path, dpi=160)
    plt.close(fig)

    fig, axes = plt.subplots(1, 3, figsize=(14, 4.5), constrained_layout=True)
    for ax, data, title, vmin, vmax in [
        (axes[0], previous_practical, "previous camera-weighted contrast", 0.0, 1.02),
        (axes[1], candidate_practical, "candidate camera-weighted contrast", 0.0, 1.02),
        (axes[2], practical_improvement, "candidate - previous, weighted", -0.05, 0.32),
    ]:
        image = ax.imshow(
            data,
            origin="lower",
            aspect="auto",
            extent=extent,
            vmin=vmin,
            vmax=vmax,
            cmap="viridis" if "candidate -" not in title else "magma",
        )
        ax.set_title(title)
        ax.set_xlabel("shadow patch center, log2 luminance")
        ax.set_ylabel("local half range, stops")
        fig.colorbar(image, ax=ax, shrink=0.88)
    practical_path = out_dir / "shadow_camera_weighted_contrast.png"
    fig.savefig(practical_path, dpi=160)
    plt.close(fig)

    fill_centers = np.linspace(-6.2, -0.85, 86)
    fill_half_ranges = np.linspace(0.08, 0.75, 43)
    previous_fill = make_fill_light_grid(fill_centers, fill_half_ranges, False)
    candidate_fill = make_fill_light_grid(fill_centers, fill_half_ranges, True)
    fill_improvement = candidate_fill - previous_fill
    fill_extent = [
        fill_centers[0],
        fill_centers[-1],
        fill_half_ranges[0],
        fill_half_ranges[-1],
    ]
    fig, axes = plt.subplots(1, 3, figsize=(14, 4.5), constrained_layout=True)
    for ax, data, title, vmin, vmax in [
        (axes[0], previous_fill, "previous local fill-light contrast", 0.88, 1.30),
        (axes[1], candidate_fill, "candidate local fill-light contrast", 0.88, 1.30),
        (axes[2], fill_improvement, "candidate - previous", -0.04, 0.22),
    ]:
        image = ax.imshow(
            data,
            origin="lower",
            aspect="auto",
            extent=fill_extent,
            vmin=vmin,
            vmax=vmax,
            cmap="viridis" if "candidate -" not in title else "magma",
        )
        ax.axvspan(-4.0, -1.0, color="white", alpha=0.08, linewidth=0)
        ax.set_title(title)
        ax.set_xlabel("shadow patch center, log2 luminance")
        ax.set_ylabel("local half range, stops")
        fig.colorbar(image, ax=ax, shrink=0.88)
    fill_path = out_dir / "shadow_fill_light_contrast.png"
    fig.savefig(fill_path, dpi=160)
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(8, 4), constrained_layout=True)
    for center, half_range in [(-5.5, 0.50), (-4.5, 0.50), (-3.5, 0.40), (-2.5, 0.30)]:
        x, prev_l = selected_lines(previous_shadow_delta, center, half_range)
        _, cand_l = selected_lines(candidate_shadow_delta, center, half_range)
        ax.plot(x, cand_l - cand_l.mean(), label=f"candidate c={center}, a={half_range}")
        ax.plot(x, prev_l - prev_l.mean(), linestyle="--", alpha=0.72)
    ax.set_title("Perceived dark texture contrast after shadow lift")
    ax.set_xlabel("normalized position")
    ax.set_ylabel("OKLab L, mean removed")
    ax.grid(True, alpha=0.25)
    ax.legend(fontsize=8)
    strip_path = out_dir / "shadow_texture_lines.png"
    fig.savefig(strip_path, dpi=160)
    plt.close(fig)

    valid_previous = previous[np.isfinite(previous)]
    valid_candidate = candidate[np.isfinite(candidate)]
    finite = np.isfinite(candidate)
    weighted_den = np.nansum(priority * finite)
    previous_weighted_mean = np.nansum(previous_practical) / max(weighted_den, 1.0e-8)
    candidate_weighted_mean = np.nansum(candidate_practical) / max(weighted_den, 1.0e-8)
    deep_cols = centers < -5.50
    previous_deep = previous[:, deep_cols]
    candidate_deep = candidate[:, deep_cols]
    fill_usable_cols = (fill_centers >= -4.0) & (fill_centers <= -1.0)
    fill_deep_cols = fill_centers < -5.50
    previous_fill_usable = previous_fill[:, fill_usable_cols]
    candidate_fill_usable = candidate_fill[:, fill_usable_cols]
    previous_fill_deep = previous_fill[:, fill_deep_cols]
    candidate_fill_deep = candidate_fill[:, fill_deep_cols]
    stops = np.array([-8.0, -6.0, -5.0, -4.0, -3.0, -2.5])
    previous_lift = previous_shadow_delta(stops)
    candidate_lift = candidate_shadow_delta(stops)
    lines = [
        f"previous_contrast_min={valid_previous.min():.6f}",
        f"candidate_contrast_min={valid_candidate.min():.6f}",
        f"previous_contrast_mean={valid_previous.mean():.6f}",
        f"candidate_contrast_mean={valid_candidate.mean():.6f}",
        f"previous_camera_weighted_mean={previous_weighted_mean:.6f}",
        f"candidate_camera_weighted_mean={candidate_weighted_mean:.6f}",
        f"previous_deep_noise_contrast_mean={np.nanmean(previous_deep):.6f}",
        f"candidate_deep_noise_contrast_mean={np.nanmean(candidate_deep):.6f}",
        f"previous_fill_usable_mean={np.nanmean(previous_fill_usable):.6f}",
        f"candidate_fill_usable_mean={np.nanmean(candidate_fill_usable):.6f}",
        f"previous_fill_deep_mean={np.nanmean(previous_fill_deep):.6f}",
        f"candidate_fill_deep_mean={np.nanmean(candidate_fill_deep):.6f}",
    ]
    for center, half_range in [(-5.5, 0.50), (-4.5, 0.50), (-3.5, 0.40), (-2.5, 0.30)]:
        prev = contrast_retention(previous_shadow_delta, center, half_range)
        cand = contrast_retention(candidate_shadow_delta, center, half_range)
        lines.append(f"contrast_c{center}_a{half_range}_previous={prev:.6f}")
        lines.append(f"contrast_c{center}_a{half_range}_candidate={cand:.6f}")
    for center, half_range in [
        (-4.5, 0.50),
        (-4.0, 0.45),
        (-3.5, 0.40),
        (-3.0, 0.35),
        (-2.5, 0.30),
        (-1.5, 0.20),
        (-1.0, 0.18),
    ]:
        prev = fill_light_contrast(center, half_range, False)
        cand = fill_light_contrast(center, half_range, True)
        lines.append(f"fill_light_c{center}_a{half_range}_previous={prev:.6f}")
        lines.append(f"fill_light_c{center}_a{half_range}_candidate={cand:.6f}")
    for stop, prev, cand in zip(stops, previous_lift, candidate_lift):
        lines.append(f"lift_at_{stop:.1f}_previous={prev:.6f}")
        lines.append(f"lift_at_{stop:.1f}_candidate={cand:.6f}")
    summary = "\n".join(lines) + "\n"
    (out_dir / "summary.txt").write_text(summary, encoding="utf-8")
    return summary


def main() -> None:
    out_dir = Path("build/diagnostics/hs_shadow_detail")
    out_dir.mkdir(parents=True, exist_ok=True)
    print(plot(out_dir), end="")
    print(f"wrote {out_dir / 'shadow_perceptual_contrast.png'}")
    print(f"wrote {out_dir / 'shadow_camera_weighted_contrast.png'}")
    print(f"wrote {out_dir / 'shadow_fill_light_contrast.png'}")
    print(f"wrote {out_dir / 'shadow_texture_lines.png'}")


if __name__ == "__main__":
    main()
