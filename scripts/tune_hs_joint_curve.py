#!/usr/bin/env python3
"""Tune the combined Highlight/Shadow response curve.

This script mirrors the GPU local-tone scalar response and focuses on the
problem case where shadow lift and highlight reduction are both active.  It
can validate the active joint curve or fit a new one by limiting highlight
reduction slope against the shadow curve's local output slope.
"""

from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

import matplotlib
import numpy as np

matplotlib.use("Agg")
import matplotlib.pyplot as plt

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagnose_hs_local_tone as local_tone  # noqa: E402
import diagnose_hs_tone_response as tone_response  # noqa: E402


ACTIVE_JOINT_POINTS = (
    (0.0, 0.0),
    (0.5, 0.06),
    (1.0, 0.42),
    (1.5, 0.85),
    (2.0, 1.30),
    (2.35, 1.20),
    (2.65, 1.08),
    (3.0, 0.96),
    (4.0, 0.68),
    (5.0, 0.50),
    (6.0, 0.40),
)


def single_reduce_delta(relative_ev: np.ndarray) -> np.ndarray:
    return 3.0 * np.clip(relative_ev / 4.0, 0.0, 1.0)


def piecewise_reduce_delta(relative_ev: np.ndarray, points: tuple[tuple[float, float], ...]) -> np.ndarray:
    xp = np.array([x for x, _ in points], dtype=np.float64)
    fp = np.array([y for _, y in points], dtype=np.float64)
    return np.interp(relative_ev, xp, fp, left=fp[0], right=fp[-1])


def fit_joint_points(
    anchors: tuple[float, ...],
    min_combined_slope: float,
    max_transition_slope: float,
    max_sky_slope: float,
    sky_slope_start_ev: float,
    samples: int,
) -> tuple[tuple[float, float], ...]:
    middle_gray = local_tone.MIDDLE_GRAY_LOG2
    x = np.linspace(middle_gray - 1.0, middle_gray + 6.0, samples)
    relative_ev = x - middle_gray
    shadow_delta = tone_response.candidate_shadow_delta(x, 1.0, 1.0)
    shadow_output_slope = np.gradient(x + shadow_delta, x)

    values = [0.0]
    for start_ev, end_ev in zip(anchors, anchors[1:]):
        interval = (relative_ev >= start_ev) & (relative_ev <= end_ev)
        shadow_limit = float(shadow_output_slope[interval].min() - min_combined_slope)
        stage_limit = max_sky_slope if start_ev >= sky_slope_start_ev else max_transition_slope
        allowed_slope = max(0.0, min(stage_limit, shadow_limit))
        target = float(single_reduce_delta(np.array([end_ev]))[0])
        values.append(min(target, values[-1] + allowed_slope * (end_ev - start_ev)))

    return tuple((float(x), float(y)) for x, y in zip(anchors, values))


def evaluate(
    points: tuple[tuple[float, float], ...],
    out_dir: Path,
    samples: int,
) -> str:
    middle_gray = local_tone.MIDDLE_GRAY_LOG2
    x = np.linspace(middle_gray - 7.0, middle_gray + 6.0, samples)
    relative_ev = x - middle_gray

    shadow_only = tone_response.candidate_shadow_delta(x, 1.0, 0.0)
    high_single = -single_reduce_delta(relative_ev)
    high_joint = -piecewise_reduce_delta(relative_ev, points)
    combined = tone_response.candidate_shadow_delta(x, 1.0, 1.0) + high_joint

    high_single_slope = np.gradient(x + high_single, x)
    high_joint_slope = np.gradient(x + high_joint, x)
    combined_slope = np.gradient(x + combined, x)
    shadow_slope = np.gradient(x + shadow_only, x)

    out_dir.mkdir(parents=True, exist_ok=True)
    with (out_dir / "joint_curve.csv").open("w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(
            [
                "relative_ev",
                "shadow_only_delta",
                "highlight_single_delta",
                "highlight_joint_delta",
                "combined_delta",
                "shadow_output_slope",
                "highlight_joint_output_slope",
                "combined_output_slope",
            ]
        )
        for row in zip(
            relative_ev,
            shadow_only,
            high_single,
            high_joint,
            combined,
            shadow_slope,
            high_joint_slope,
            combined_slope,
        ):
            writer.writerow([f"{value:.8f}" for value in row])

    fig, axes = plt.subplots(2, 2, figsize=(12, 7), constrained_layout=True)
    axes[0, 0].plot(relative_ev, -high_single, label="highlight single")
    axes[0, 0].plot(relative_ev, -high_joint, label="highlight joint")
    axes[0, 0].set_title("Highlight reduction")
    axes[0, 0].set_ylabel("stops")
    axes[0, 0].legend()
    axes[0, 1].plot(relative_ev, shadow_only, label="shadow only")
    axes[0, 1].plot(relative_ev, combined, label="shadow + joint highlight")
    axes[0, 1].set_title("Base delta")
    axes[0, 1].legend()
    axes[1, 0].plot(relative_ev, high_single_slope, label="highlight single")
    axes[1, 0].plot(relative_ev, high_joint_slope, label="highlight joint")
    axes[1, 0].set_title("Highlight output slope")
    axes[1, 0].set_xlabel("relative EV from middle gray")
    axes[1, 0].legend()
    axes[1, 1].plot(relative_ev, combined_slope, label="combined")
    axes[1, 1].axhline(0.0, color="0.5", linewidth=1, linestyle="--")
    axes[1, 1].set_title("Combined output slope")
    axes[1, 1].set_xlabel("relative EV from middle gray")
    axes[1, 1].legend()
    for ax in axes.flat:
        ax.grid(True, alpha=0.25)
    fig.savefig(out_dir / "joint_curve_response.png", dpi=160)
    plt.close(fig)

    anchor_lines = ["active_joint_points:"]
    for x_ev, y in points:
        anchor_lines.append(f"  {x_ev:5.2f} -> {y:.6f}")
    code_lines = ["c_style_segments:"]
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        code_lines.append(f"  HS_JOINT_SEG({x0:.3f}f, {y0:.6f}f, {x1:.3f}f, {y1:.6f}f);")

    probe_lines = ["probe_reductions:"]
    for ev in (0.0, 0.5, 1.0, 2.0, 2.35, 2.65, 3.0, 4.0, 5.0, 6.0):
        reduction = float(np.interp(ev, relative_ev, -high_joint))
        probe_lines.append(f"  +{ev:.2f}EV -> {reduction:.6f} stops")

    summary_lines = [
        f"min_shadow_only_slope={shadow_slope.min():.6f}",
        f"min_highlight_single_slope={high_single_slope[(relative_ev >= 0.0) & (relative_ev <= 5.0)].min():.6f}",
        f"min_highlight_joint_slope={high_joint_slope[(relative_ev >= 0.0) & (relative_ev <= 5.0)].min():.6f}",
        f"max_highlight_joint_slope={high_joint_slope[(relative_ev >= 0.0) & (relative_ev <= 6.0)].max():.6f}",
        f"min_combined_slope={combined_slope.min():.6f}",
        f"max_combined_slope={combined_slope[(relative_ev >= 0.0) & (relative_ev <= 6.0)].max():.6f}",
        *anchor_lines,
        *probe_lines,
        *code_lines,
        f"wrote={out_dir / 'joint_curve.csv'}",
        f"wrote={out_dir / 'joint_curve_response.png'}",
    ]
    summary = "\n".join(summary_lines) + "\n"
    (out_dir / "summary.txt").write_text(summary, encoding="utf-8")
    return summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out-dir", type=Path, default=Path("build/diagnostics/hs_joint_curve"))
    parser.add_argument("--fit", action="store_true", help="fit monotone anchors from slope constraints")
    parser.add_argument("--samples", type=int, default=6000)
    parser.add_argument("--min-combined-slope", type=float, default=0.02)
    parser.add_argument("--max-transition-slope", type=float, default=0.75)
    parser.add_argument("--max-sky-slope", type=float, default=0.635)
    parser.add_argument("--sky-slope-start-ev", type=float, default=2.5)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.fit:
        anchors = tuple(x for x, _ in ACTIVE_JOINT_POINTS)
        points = fit_joint_points(
            anchors,
            args.min_combined_slope,
            args.max_transition_slope,
            args.max_sky_slope,
            args.sky_slope_start_ev,
            args.samples,
        )
    else:
        points = ACTIVE_JOINT_POINTS
    print(evaluate(points, args.out_dir, args.samples), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
