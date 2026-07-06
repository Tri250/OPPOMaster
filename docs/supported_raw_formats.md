# Supported RAW and Image Formats

Alcedo Studio can import the RAW and general image formats listed below. The RAW pipeline is built on a patched fork of [LibRaw](https://github.com/zidage/LibRaw), which provides the underlying decoder, metadata extraction, and camera-specific handling.

## Import File Extensions

The following extensions are recognized at import time (case-insensitive):

| Extension | Format |
| --- | --- |
| `.3fr` | Hasselblad RAW |
| `.arw` | Sony α RAW |
| `.cr2` | Canon RAW 2 |
| `.cr3` | Canon RAW 3 |
| `.dng` | Adobe Digital Negative |
| `.fff` | Hasselblad 3F / Imacon RAW |
| `.nef` | Nikon Electronic Format |
| `.raf` | Fujifilm RAW |
| `.raw` | Generic / Panasonic / Leica RAW |
| `.rw2` | Panasonic RAW |

## Underlying RAW Decoder Support

The bundled LibRaw fork covers the major vendor formats, including:

- **Canon**: CR2, CR3, CRN (embedded RAW), sRAW/mRAW
- **Nikon**: NEF, NEFX (Pixel Shift merged), and **Nikon High-Efficiency (HE / HE\*) NEF**
- **Sony**: ARW, compressed/uncompressed, and YCC pseudo-RAW
- **Fujifilm**: RAF, including X-Trans and 14-bit high-resolution files
- **Panasonic**: RW2 / .raw, including Panasonic encoding 8
- **Olympus / OM System**: ORF / high-resolution files
- **Leica, Hasselblad, Phase One, Pentax, Sigma, Samsung, Blackmagic, and others**
- **DNG 1.7** (including JPEG-XL compressed DNG when compiled with Adobe DNG SDK)
- **Smartphone and drone DNGs**: Apple, Google, DJI, Skydio, GoPro (via GoPro SDK)

> For camera-model-level compatibility, see [supported_cameras.md](supported_cameras.md).

## Exclusive: Nikon HE / HE\* NEF Support

Nikon High-Efficiency (`HE`) and High-Efficiency★ (`HE*`) NEFs use a JPEG-XS-like compressed codestream inside the standard TIFF/NEF container. As of the upstream LibRaw 0.22 release, these files are **not supported** for the affected cameras.

Alcedo Studio ships a patched LibRaw fork that decodes HE / HE\* NEFs without relying on Nikon NX Studio or any closed SDK. Validated samples include:

- Nikon Z 8
- Nikon Z 9
- Nikon Z 6 III
- Nikon Z 50 II

The decoder handles the dynamic per-precinct `Bp/Br` regimes that Nikon uses across these models, so both the smaller `HE` files and the even more compact `HE*` files can be opened, demosaiced, and graded inside Alcedo Studio.

The patched decoder lives in the project's LibRaw fork:  
**https://github.com/zidage/LibRaw**

## Export Formats

For export, Alcedo Studio supports:

| Format | Notes |
| --- | --- |
| JPEG | 8-bit, quality-controlled |
| PNG | 8/16/32-bit, compression level |
| TIFF | 8/16/32-bit, LZW/ZIP/no compression |
| WEBP | 8-bit, quality-controlled |
| Ultra HDR | JPEG with gain-map (Android Ultra HDR) |

HDR export can be written as an Ultra HDR gain-map JPEG or as an embedded-profile image, depending on the chosen export mode.
