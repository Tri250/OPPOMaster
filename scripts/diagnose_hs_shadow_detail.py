#!/usr/bin/env python3
"""Measure perceived shadow contrast retention for highlight/shadow tone mapping.

The test compares two shadow-lift curves:

- previous: the linearly growing deep-shadow lift used by the last iteration;
- candidate: the saturated lift now used by diagnose_hs_tone_response.py.

For each synthetic all-shadow patch, the reference is an ideal output with the
same center lift but unchanged local stop differences. The metric is the
standard deviation of OKLab L after tone mapping divided by that ideal output.
"""

from __future__ import annotations

import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagnose_hs_tone_response as tone  # noqa: E402


def previous_shadow_delta(mask_ref: np.ndarray) -> np.ndarray:
    width = max(tone.baseline.SHADOW_LOG_WIDTH, 0.35)
    upper_pivot = tone.baseline.shadow_upper_pivot()
    lift_pivot = upper_pivot + max(width * 1.60, 0.98)
    distance_to_pivot = np.maximum(lift_pivot - mask_ref, 0.0)
    soft_distance = tone.softplus_distance(distance_to_pivot, max(width * 2.18, 1.35))
    black_guard = 0.82 + 0.18 * tone.shadow_black_floor_weight(mask_ref)
    highlight_overlap = tone.highlight_zone_weight(mask_ref)
    overlap_guard = 1.0 - 0.28 * np.clip(highlight_overlap, 0.0, 1.0)
    return 0.42 * soft_distance * black_guard * overlap_guard


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


def make_grid(delta_fn, centers: np.ndarray, half_ranges: np.ndarray) -> np.ndarray:
    upper = candidate_active_limit()
    grid = np.full((len(half_ranges), len(centers)), np.nan, dtype=np.float64)
    for row, half_range in enumerate(half_ranges):
        for col, center in enumerate(centers):
            if center + half_range > upper:
                continue
            grid[row, col] = contrast_retention(delta_fn, float(center), float(half_range))
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
    ]
    for center, half_range in [(-5.5, 0.50), (-4.5, 0.50), (-3.5, 0.40), (-2.5, 0.30)]:
        prev = contrast_retention(previous_shadow_delta, center, half_range)
        cand = contrast_retention(candidate_shadow_delta, center, half_range)
        lines.append(f"contrast_c{center}_a{half_range}_previous={prev:.6f}")
        lines.append(f"contrast_c{center}_a{half_range}_candidate={cand:.6f}")
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
    print(f"wrote {out_dir / 'shadow_texture_lines.png'}")


if __name__ == "__main__":
    main()
