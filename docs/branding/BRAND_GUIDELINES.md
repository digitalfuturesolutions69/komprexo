# Komprexo identity — owner review candidate

Original geometric **K** within deep-blue photo-frame field; one recognizable transformation symbol. Actual SVG/PNG masters/contact/mask sheets in artwork. No trademark clearance or owner aesthetic approval claimed.

| Role | HEX | Usage |
|---|---|---|
| Deep blue | #123B68 | Icon field/light-theme brand text/primary |
| Cyan/teal | #52E0D1 | K against deep blue |
| Off-white | #F5F8FC | Light field/text against deep blue |
| Dark UI accent | #7CDCE8 | Compose dark primary |

Use deep blue on off-white and cyan/off-white on blue; never cyan small text on white. Native text/system bars remain theme-aware.

Wordmark DejaVu Sans capital K/lowercase omprexo outlined into editable SVG paths; actual Bitstream Vera/DejaVu notice in artwork/DEJAVU_LICENSE.txt and app offline notices.

* komprexo-master.svg/icon.svg:512-square editable vector.
* logo-primary/horizontal/light/dark: SVG+PNG wordmark variants.
* play-store-icon-512.png:512x512,8-bit RGBA32-bit,opaque square,no baked mask/badge/shadow.
* Legacy mdpi48/hdpi72/xhdpi96/xxhdpi144/xxxhdpi192 square+round.
* API26 adaptive108dp layers; K vertices inside central66dp safe circle.
* API33 monochrome same geometry/system tint.
* brand-contact-sheet.png/launcher-mask-preview.png: actual variants/circle/rounded/square/safe-zone.

Clear space≥K stem width. Never stretch/tilt/recolor/crowd; preserve ratio. Play/launchers apply masks. Regenerate with scripts/generate-branding.py, CairoSVG2.8.2/fontTools/Pillow and system DejaVu Sans. No embedded/external font in app. scripts/test-phase4-assets.py checks PNG CRC/decompression/mode, SVG self-containment, icon refs/safe zone.

Specs: [adaptive](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [Play icon](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).
