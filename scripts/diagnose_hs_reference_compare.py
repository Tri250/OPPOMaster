#!/usr/bin/env python3
"""Compare current highlight/shadow exports against reference PNGs.

This script focuses on the failure modes called out during review:
tone placement, local contrast, highlight chroma inflation, shadow
noise, edge halos, and smooth-region discontinuities/banding.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from pathlib import Path

import cv2
import matplotlib
import numpy as np
from scipy import ndimage

matplotlib.use("Agg")
import matplotlib.pyplot as plt


DEFAULT_REFERENCE_DIR = Path(r"D:\素材\照片\2026-6-3-reference")
DEFAULT_ACTUAL_DIR = Path("build/diagnostics/hs_reference_exports")
DEFAULT_OUT_DIR = Path("build/diagnostics/hs_reference_compare")


@dataclass
class CaseMetrics:
    case: str
    width: int
    height: int
    luma_mae: float
    luma_p95_abs: float
    luma_median_delta: float
    chroma_mae: float
    chroma_mean_ratio: float
    highlight_chroma_ratio: float
    shadow_noise_ratio: float
    gradient_p90_ratio: float
    halo_score: float
    smooth_break_score: float


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--actual-dir", type=Path, default=DEFAULT_ACTUAL_DIR)
    parser.add_argument("--reference-dir", type=Path, default=DEFAULT_REFERENCE_DIR)
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_DIR)
    parser.add_argument("--gamma", type=float, default=2.2)
    parser.add_argument(
        "--files",
        nargs="*",
        default=[],
        help="Specific PNG filenames to compare. Defaults to every reference PNG in the folder.",
    )
    return parser.parse_args()


def load_rgb_image(path: Path) -> np.ndarray:
    image = cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
    if image is None:
        raise FileNotFoundError(f"Failed to read image: {path}")
    if image.ndim == 2:
        image = cv2.cvtColor(image, cv2.COLOR_GRAY2RGB)
    if image.shape[2] == 4:
        image = cv2.cvtColor(image, cv2.COLOR_BGRA2RGB)
    else:
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    if image.dtype == np.uint8:
        scale = 255.0
    elif image.dtype == np.uint16:
        scale = 65535.0
    else:
        scale = float(np.max(image)) if np.max(image) > 1.0 else 1.0
    return np.clip(image.astype(np.float32) / scale, 0.0, 1.0)


def gamma_decode(rgb: np.ndarray, gamma: float) -> np.ndarray:
    return np.power(np.clip(rgb, 0.0, 1.0), gamma, dtype=np.float32)


def resize_like_reference(actual_rgb: np.ndarray, reference_rgb: np.ndarray) -> np.ndarray:
    if actual_rgb.shape[:2] == reference_rgb.shape[:2]:
        return actual_rgb

    ref_h, ref_w = reference_rgb.shape[:2]
    src_h, src_w = actual_rgb.shape[:2]
    scale_x = ref_w / max(src_w, 1)
    scale_y = ref_h / max(src_h, 1)
    interpolation = cv2.INTER_AREA if max(scale_x, scale_y) < 1.0 else cv2.INTER_LINEAR
    return cv2.resize(actual_rgb, (ref_w, ref_h), interpolation=interpolation)


def rgb_to_oklab(linear_rgb: np.ndarray) -> np.ndarray:
    l = 0.4122214708 * linear_rgb[..., 0] + 0.5363325363 * linear_rgb[..., 1] + 0.0514459929 * linear_rgb[..., 2]
    m = 0.2119034982 * linear_rgb[..., 0] + 0.6806995451 * linear_rgb[..., 1] + 0.1073969566 * linear_rgb[..., 2]
    s = 0.0883024619 * linear_rgb[..., 0] + 0.2817188376 * linear_rgb[..., 1] + 0.6299787005 * linear_rgb[..., 2]

    l_ = np.cbrt(np.clip(l, 0.0, None))
    m_ = np.cbrt(np.clip(m, 0.0, None))
    s_ = np.cbrt(np.clip(s, 0.0, None))

    return np.stack(
        (
            0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
            1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
            0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_,
        ),
        axis=-1,
    )


def luma(linear_rgb: np.ndarray) -> np.ndarray:
    return (
        0.2126 * linear_rgb[..., 0]
        + 0.7152 * linear_rgb[..., 1]
        + 0.0722 * linear_rgb[..., 2]
    )


def robust_std(values: np.ndarray) -> float:
    median = np.median(values)
    mad = np.median(np.abs(values - median))
    return float(1.4826 * mad)


def sobel_magnitude(image: np.ndarray) -> np.ndarray:
    gx = ndimage.sobel(image, axis=1, mode="reflect")
    gy = ndimage.sobel(image, axis=0, mode="reflect")
    return np.hypot(gx, gy)


def compute_case_metrics(filename: str, actual_rgb: np.ndarray, reference_rgb: np.ndarray, gamma: float) -> tuple[CaseMetrics, dict[str, np.ndarray]]:
    actual_rgb = resize_like_reference(actual_rgb, reference_rgb)
    if actual_rgb.shape != reference_rgb.shape:
        raise ValueError(f"Shape mismatch for {filename}: {actual_rgb.shape} vs {reference_rgb.shape}")

    actual_lin = gamma_decode(actual_rgb, gamma)
    reference_lin = gamma_decode(reference_rgb, gamma)

    actual_luma = luma(actual_lin)
    reference_luma = luma(reference_lin)
    diff_luma = actual_luma - reference_luma

    actual_oklab = rgb_to_oklab(actual_lin)
    reference_oklab = rgb_to_oklab(reference_lin)
    actual_chroma = np.hypot(actual_oklab[..., 1], actual_oklab[..., 2])
    reference_chroma = np.hypot(reference_oklab[..., 1], reference_oklab[..., 2])
    diff_chroma = actual_chroma - reference_chroma

    grad_actual = sobel_magnitude(actual_luma)
    grad_reference = sobel_magnitude(reference_luma)
    grad_diff = np.abs(grad_actual - grad_reference)

    highlight_mask = reference_luma >= np.quantile(reference_luma, 0.9)
    shadow_mask = reference_luma <= np.quantile(reference_luma, 0.25)
    smooth_mask = grad_reference <= np.quantile(grad_reference, 0.25)
    shadow_smooth_mask = shadow_mask & smooth_mask
    mid_smooth_mask = smooth_mask & (reference_luma >= 0.10) & (reference_luma <= 0.75)
    edge_mask = grad_reference >= np.quantile(grad_reference, 0.97)
    edge_band = ndimage.binary_dilation(edge_mask, iterations=2)

    actual_highpass = actual_luma - ndimage.gaussian_filter(actual_luma, 1.2, mode="reflect")
    reference_highpass = reference_luma - ndimage.gaussian_filter(reference_luma, 1.2, mode="reflect")
    shadow_noise_actual = robust_std(actual_highpass[shadow_smooth_mask])
    shadow_noise_reference = max(robust_std(reference_highpass[shadow_smooth_mask]), 1.0e-6)

    halo_map = np.abs(ndimage.laplace(diff_luma, mode="reflect"))
    smooth_break_map = np.abs(ndimage.laplace(diff_luma, mode="reflect"))

    metrics = CaseMetrics(
        case=filename,
        width=int(actual_rgb.shape[1]),
        height=int(actual_rgb.shape[0]),
        luma_mae=float(np.mean(np.abs(diff_luma))),
        luma_p95_abs=float(np.quantile(np.abs(diff_luma), 0.95)),
        luma_median_delta=float(np.median(diff_luma)),
        chroma_mae=float(np.mean(np.abs(diff_chroma))),
        chroma_mean_ratio=float(np.mean(actual_chroma) / max(np.mean(reference_chroma), 1.0e-6)),
        highlight_chroma_ratio=float(
            np.mean(actual_chroma[highlight_mask]) / max(np.mean(reference_chroma[highlight_mask]), 1.0e-6)
        ),
        shadow_noise_ratio=float(shadow_noise_actual / shadow_noise_reference),
        gradient_p90_ratio=float(
            np.quantile(grad_actual, 0.90) / max(np.quantile(grad_reference, 0.90), 1.0e-6)
        ),
        halo_score=float(np.mean(halo_map[edge_band])),
        smooth_break_score=float(np.mean(smooth_break_map[mid_smooth_mask])),
    )

    debug = {
        "actual_luma": actual_luma,
        "reference_luma": reference_luma,
        "diff_luma": diff_luma,
        "actual_chroma": actual_chroma,
        "reference_chroma": reference_chroma,
        "grad_actual": grad_actual,
        "grad_reference": grad_reference,
        "grad_diff": grad_diff,
    }
    return metrics, debug


def select_windows(score_map: np.ndarray, window: int, count: int = 3) -> list[tuple[int, int]]:
    pooled = ndimage.uniform_filter(score_map, size=window, mode="reflect")
    working = pooled.copy()
    coords: list[tuple[int, int]] = []
    radius = window // 2
    for _ in range(count):
        y, x = np.unravel_index(np.argmax(working), working.shape)
        coords.append((int(y), int(x)))
        y0 = max(0, y - radius)
        y1 = min(working.shape[0], y + radius)
        x0 = max(0, x - radius)
        x1 = min(working.shape[1], x + radius)
        working[y0:y1, x0:x1] = -1.0
    return coords


def save_case_report(out_dir: Path, filename: str, actual_rgb: np.ndarray, reference_rgb: np.ndarray, debug: dict[str, np.ndarray], metrics: CaseMetrics) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    stem = Path(filename).stem

    diff_vis = np.clip(np.abs(debug["diff_luma"]) * 6.0, 0.0, 1.0)
    score_map = np.abs(debug["diff_luma"]) + 0.25 * debug["grad_diff"] + 0.15 * np.abs(
        debug["actual_chroma"] - debug["reference_chroma"]
    )
    window = max(128, min(actual_rgb.shape[0], actual_rgb.shape[1]) // 5)
    coords = select_windows(score_map, window)

    fig, axes = plt.subplots(2, 3, figsize=(14, 8), constrained_layout=True)
    axes[0, 0].imshow(reference_rgb)
    axes[0, 0].set_title("Reference")
    axes[0, 1].imshow(actual_rgb)
    axes[0, 1].set_title("Current")
    axes[0, 2].imshow(diff_vis, cmap="magma")
    axes[0, 2].set_title("Luma diff x6")
    axes[1, 0].hist(debug["reference_luma"].ravel(), bins=256, alpha=0.7, label="ref")
    axes[1, 0].hist(debug["actual_luma"].ravel(), bins=256, alpha=0.5, label="current")
    axes[1, 0].set_title("Luma histogram")
    axes[1, 0].legend()
    axes[1, 1].hist(debug["reference_chroma"].ravel(), bins=256, alpha=0.7, label="ref")
    axes[1, 1].hist(debug["actual_chroma"].ravel(), bins=256, alpha=0.5, label="current")
    axes[1, 1].set_title("OKLab chroma histogram")
    axes[1, 1].legend()
    axes[1, 2].scatter(
        debug["reference_luma"].ravel()[::32],
        debug["actual_luma"].ravel()[::32],
        s=1,
        alpha=0.1,
    )
    axes[1, 2].plot([0.0, 1.0], [0.0, 1.0], color="white", linewidth=1.0)
    axes[1, 2].set_xlim(0.0, 1.0)
    axes[1, 2].set_ylim(0.0, 1.0)
    axes[1, 2].set_title("Ref vs current luma")
    for ax in axes.flat:
        ax.set_xticks([])
        ax.set_yticks([])
    fig.suptitle(stem)
    fig.savefig(out_dir / f"{stem}_summary.png", dpi=150)
    plt.close(fig)

    crop_fig, crop_axes = plt.subplots(3, len(coords), figsize=(5 * len(coords), 9), constrained_layout=True)
    if len(coords) == 1:
        crop_axes = np.array(crop_axes).reshape(3, 1)
    radius = window // 2
    for col, (cy, cx) in enumerate(coords):
        y0 = max(0, cy - radius)
        y1 = min(actual_rgb.shape[0], cy + radius)
        x0 = max(0, cx - radius)
        x1 = min(actual_rgb.shape[1], cx + radius)
        crop_axes[0, col].imshow(reference_rgb[y0:y1, x0:x1])
        crop_axes[0, col].set_title(f"Reference crop {col + 1}")
        crop_axes[1, col].imshow(actual_rgb[y0:y1, x0:x1])
        crop_axes[1, col].set_title(f"Current crop {col + 1}")
        crop_axes[2, col].imshow(diff_vis[y0:y1, x0:x1], cmap="magma")
        crop_axes[2, col].set_title(f"Diff crop {col + 1}")
        for row in range(3):
            crop_axes[row, col].set_xticks([])
            crop_axes[row, col].set_yticks([])
    crop_fig.savefig(out_dir / f"{stem}_crops.png", dpi=150)
    plt.close(crop_fig)

    (out_dir / f"{stem}_metrics.json").write_text(
        json.dumps(asdict(metrics), indent=2, ensure_ascii=False), encoding="utf-8"
    )


def summarize(metrics: list[CaseMetrics]) -> str:
    lines = ["# Highlight/Shadow Reference Comparison", ""]
    for item in metrics:
        lines.extend(
            [
                f"## {item.case}",
                f"- luma_mae: {item.luma_mae:.6f}",
                f"- luma_p95_abs: {item.luma_p95_abs:.6f}",
                f"- luma_median_delta: {item.luma_median_delta:.6f}",
                f"- chroma_mae: {item.chroma_mae:.6f}",
                f"- chroma_mean_ratio: {item.chroma_mean_ratio:.4f}",
                f"- highlight_chroma_ratio: {item.highlight_chroma_ratio:.4f}",
                f"- shadow_noise_ratio: {item.shadow_noise_ratio:.4f}",
                f"- gradient_p90_ratio: {item.gradient_p90_ratio:.4f}",
                f"- halo_score: {item.halo_score:.6f}",
                f"- smooth_break_score: {item.smooth_break_score:.6f}",
                "",
            ]
        )

    if metrics:
        aggregate = {
            "mean_luma_mae": float(np.mean([m.luma_mae for m in metrics])),
            "mean_chroma_mae": float(np.mean([m.chroma_mae for m in metrics])),
            "mean_highlight_chroma_ratio": float(np.mean([m.highlight_chroma_ratio for m in metrics])),
            "mean_shadow_noise_ratio": float(np.mean([m.shadow_noise_ratio for m in metrics])),
            "mean_halo_score": float(np.mean([m.halo_score for m in metrics])),
            "mean_smooth_break_score": float(np.mean([m.smooth_break_score for m in metrics])),
        }
        lines.extend(
            [
                "## Aggregate",
                *[f"- {key}: {value:.6f}" for key, value in aggregate.items()],
                "",
            ]
        )
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    args.out_dir.mkdir(parents=True, exist_ok=True)

    if args.files:
        filenames = args.files
    else:
        filenames = sorted(p.name for p in args.reference_dir.glob("*.png"))

    reports: list[CaseMetrics] = []
    for filename in filenames:
        actual_path = args.actual_dir / filename
        reference_path = args.reference_dir / filename
        if not actual_path.exists():
            raise FileNotFoundError(f"Missing actual export: {actual_path}")
        if not reference_path.exists():
            raise FileNotFoundError(f"Missing reference export: {reference_path}")

        actual_rgb = load_rgb_image(actual_path)
        reference_rgb = load_rgb_image(reference_path)
        actual_rgb = resize_like_reference(actual_rgb, reference_rgb)
        metrics, debug = compute_case_metrics(filename, actual_rgb, reference_rgb, args.gamma)
        save_case_report(args.out_dir, filename, actual_rgb, reference_rgb, debug, metrics)
        reports.append(metrics)
        print(json.dumps(asdict(metrics), ensure_ascii=False))

    summary_md = summarize(reports)
    (args.out_dir / "summary.md").write_text(summary_md, encoding="utf-8")
    (args.out_dir / "metrics.json").write_text(
        json.dumps([asdict(item) for item in reports], indent=2, ensure_ascii=False),
        encoding="utf-8",
    )
    print(f"Wrote {args.out_dir / 'summary.md'}")
    print(f"Wrote {args.out_dir / 'metrics.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
