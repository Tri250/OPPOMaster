# AlcedoStudio — License Compliance Report

## 1. Overview

AlcedoStudio is a C++/Qt6/QML desktop image editor that links against several
third-party libraries with different open-source licenses. This document
analyses the license obligations, explains why non-commercial distribution is
compatible, and provides recommendations for replacing GPL components if
broader distribution is desired.

## 2. GPL Dependencies

The following libraries used by AlcedoStudio are licensed under the GNU General
Public License (GPL):

| Library | License | Version | Purpose |
|---------|---------|---------|---------|
| **Exiv2** | GPL-2.0-or-later | 0.28.x | EXIF/IPTC/XMP metadata reading and writing |

Exiv2 is the **only** GPL dependency in the project. It is used for reading
and writing image metadata (EXIF, IPTC, XMP profiles). The library is linked
dynamically.

## 3. Implications of GPL-2.0-or-later

The GNU General Public License v2 (or later) imposes the following obligations
on distributors:

### 3.1 Copyleft Requirement
- Any work that **links to** or **derives from** a GPL-licensed library must
  itself be distributed under the GPL (or a compatible license).
- This applies to **dynamic linking** as well as static linking.
- The AlcedoStudio source code is already licensed under GPL-3.0-only with an
  additional permission under section 7, which satisfies this requirement.

### 3.2 Source Code Offering
- Distributors must provide the **complete corresponding source code** for the
  application, including any modifications to GPL-licensed libraries.
- Source code must be offered **at no additional charge** (beyond reasonable
  distribution costs) and must remain available for at least **three years**
  after the last binary distribution.
- Acceptable methods include:
  - Bundling the source with the binary distribution
  - A written offer to provide the source on physical media
  - Providing a publicly accessible download URL

### 3.3 License Notices
- All GPL license texts must be included in the distribution.
- Copyright notices must be preserved.
- Any modifications to GPL-licensed files must be clearly marked.

### 3.4 No Additional Restrictions
- Distributors may not impose additional restrictions on the recipient's
  exercise of the rights granted by the GPL (e.g., cannot add DRM that
  prevents modification).

## 4. Non-Commercial Distribution Compatibility

AlcedoStudio is distributed **non-commercially** (free of charge, without
subscription or licensing fees). This is fully compatible with the GPL:

- **GPL does not prohibit non-commercial distribution.** In fact, the GPL was
  designed to ensure that recipients of software can freely use, study, modify,
  and redistribute it — regardless of whether the distribution is commercial or
  not.
- **Non-commercial distribution simplifies compliance** because:
  - There is no need to worry about "selling" the software (which the GPL
    permits, but which triggers additional requirements about price and
    source-code availability).
  - The source code offering can be satisfied by publishing on GitHub.
  - No patent licensing concerns arise from non-commercial distribution.

The key compliance requirement remains: **source code must be made available
to all recipients of the binary**. Publishing the source on the project's
GitHub repository satisfies this requirement.

## 5. Source Code Offering Requirements

For each binary distribution of AlcedoStudio:

| Requirement | How We Satisfy It |
|-------------|-------------------|
| Source code availability | Published on GitHub at `https://github.com/alcedo-studio/alcedo` |
| Corresponding source for Exiv2 | Available from Exiv2 upstream + any patches in our repo |
| Availability duration | GitHub provides indefinite availability |
| No additional charge | Source is freely downloadable |
| License text included | `third_party_licenses/Exiv2-LICENSE.txt` and `Exiv2-COPYING-GPL-2.0.txt` shipped with binary |

## 6. Recommendations for Replacing GPL Components

If the project needs to transition to a non-copyleft distribution model (e.g.,
proprietary licensing, or permissive-only open source), Exiv2 must be replaced.
Options include:

### 6.1 Replace Exiv2 with a Permissive Alternative

| Library | License | EXIF Read | EXIF Write | IPTC | XMP | Notes |
|---------|---------|-----------|------------|------|-----|-------|
| **libexif** | LGPL-2.1-or-later | ✅ | ✅ | ❌ | ❌ | Mature, lightweight; no XMP/IPTC |
| **ExifTool** (via IPC) | Artistic/Perl | ✅ | ✅ | ✅ | ✅ | Requires Perl runtime; IPC overhead |
| **Adobe XMP SDK** | BSD-3-Clause | ✅ | ✅ | ✅ | ✅ | XMP only; large SDK; CLA required |
| **Custom EXIF parser** | Proprietary | ✅ | ✅ | ❌ | ❌ | Significant development effort |

### 6.2 Recommended Replacement Strategy

1. **Short term (maintain GPL compliance):** Continue using Exiv2 with full
   source-code distribution. Ensure every binary release includes a
   `LICENSES/` directory with all license texts and a `SOURCE_OFFER` file
   pointing to the GitHub repository.

2. **Medium term (dual-path):** Implement a metadata abstraction layer that
   allows swapping the backend. Use Exiv2 by default but allow building
   against libexif for permissive-only distributions.

3. **Long term (GPL-free):** Replace Exiv2 with libexif (EXIF only) + Adobe
   XMP SDK (XMP only). IPTC support can be dropped or implemented via a
   custom lightweight parser, as the IPTC IIM specification is simple.

### 6.3 Effort Estimate

| Approach | Development Effort | Risk | Feature Parity |
|----------|-------------------|------|----------------|
| libexif only | 2–4 weeks | Low | EXIF only; no IPTC/XMP |
| libexif + XMP SDK | 4–8 weeks | Medium | EXIF + XMP; no IPTC |
| Full custom | 8–16 weeks | High | Depends on scope |

## 7. Other License Obligations

The project also uses libraries under the following non-GPL licenses:

| License | Libraries | Obligations |
|---------|-----------|-------------|
| LGPL-2.1/LGPL-3.0 | Qt6, LibRaw | Dynamic linking; license text; allow relinking |
| BSD-2-Clause/BSD-3-Clause | OpenCV (partially), nlohmann_json, others | License text; copyright notice |
| MPL-2.0 | Eigen | License text; file-level copyleft only |
| MIT | DuckDB, xxHash, utfcpp, others | License text; copyright notice |
| Apache-2.0 | OpenColorIO (if used) | License text; NOTICE file; copyright notice |
| CC-BY-SA-3.0 | Lensfun data | Attribution; share-alike for derivatives |

All license texts are collected in the `third_party_licenses/` directory and
shipped with every binary distribution.

## 8. Compliance Checklist

- [x] Source code publicly available on GitHub
- [x] GPL license text included in distribution
- [x] All third-party license texts included
- [x] Copyright notices preserved in source files
- [x] No additional restrictions imposed on recipients
- [x] Source code offering statement included with binary distributions
- [ ] Verify Exiv2 version matches source offered (on each release)
- [ ] Include `SOURCE_OFFER.txt` in packaged binaries
