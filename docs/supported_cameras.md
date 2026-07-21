# Supported Cameras

Alcedo Studio uses a patched fork of [LibRaw](https://github.com/zidage/LibRaw) as its RAW decoder. The fork is based on LibRaw 0.22+ and supports approximately **1,284 camera models** across about **84 brands** when compiled with all features.

This page lists the major supported brands and highlights recently added models. It is not exhaustive; for the complete official list, see the [LibRaw supported cameras page](https://www.libraw.org/supported-cameras).

## Major Supported Brands

- Canon
- Nikon
- Sony
- Fujifilm
- Panasonic
- Olympus / OM System
- Leica
- Hasselblad
- Phase One
- Pentax
- Sigma
- Samsung
- Apple
- Google
- DJI
- Skydio
- GoPro
- Blackmagic
- RED (legacy models)
- And many others

## Recently Added Camera Models (LibRaw 0.22 cycle)

### Canon
- EOS R1
- EOS R5 Mark II
- EOS R5 C
- EOS R6 Mark II
- EOS R8
- EOS R50
- EOS R100
- EOS Ra

### Fujifilm
- X-T50
- GFX 100S II
- GFX100-II
- X-T5
- X-S20
- X-H2
- X-H2S

### GoPro
- HERO11
- HERO12

### Hasselblad
- CFV-50c
- CFV-100c
- X2D-100c

### Leica
- Q3 43
- D-Lux8
- SL3
- Q3
- M11 Monochrom

### Nikon
- Z 6 III
- Z f
- Z 30
- Z 8
- Z 9
- Z 50 II

### Olympus / OM System
- OM-1 Mark II
- TG-7
- OM-5

### Panasonic
- GH7
- S9
- DC-G9 II
- DC-ZS200D / ZS220D
- DC-TZ200D / TZ202D / TZ220D
- DC-S5-II
- DC-GH6

### Pentax
- KF
- K III Monochrome

### Sony
- ZV-E10M2
- UMC-R10C
- A9-III
- ILX-LR1
- A7C-II
- A7CR
- ILCE-6700
- ZV-1M2
- ZV-E1
- ILCE-7RM5 (A7R-V)
- ILME-FX30
- A1

## Exclusive: Nikon HE / HE\* NEF Support

Upstream LibRaw 0.22 lists the following Nikon models with the note *"HE/HE* formats are not supported yet"*:

- Nikon Z 6 III
- Nikon Z 8
- Nikon Z 9
- Nikon Z f

Alcedo Studio's LibRaw fork removes this limitation for the validated models below:

| Camera | HE | HE\* | Notes |
| --- | --- | --- | --- |
| Nikon Z 8 | ✅ | ✅ | Full-frame samples validated |
| Nikon Z 9 | ✅ | ✅ | Mixed-Bp and low-Bp regimes validated |
| Nikon Z 6 III | ✅ | ✅ | Dynamic Bp=6 regimes validated |
| Nikon Z 50 II | ✅ | ✅ | DX samples validated |

This means you can import, demosaic, edit, and export Nikon High-Efficiency NEFs directly in Alcedo Studio without converting them through Nikon NX Studio or another tool first.

For the decoder implementation details, see the project's LibRaw fork:  
**https://github.com/zidage/LibRaw**
