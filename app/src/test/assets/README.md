Version 1 synthetic fixtures, generated with Pillow and seed 20261008. No personal data.
noise.jpg: seeded RGB noise 640x480; noise.webp: resampled noise 320x240; alpha.png: RGBA gradient 128x96; orientations.png: four-color 120x80 pixel oracle; rotated.jpg: red/blue 120x80, EXIF rotate 90; large.png: solid 4096x2048; corrupt.jpg: truncated JPEG; unsupported.gif: invalid GIF header.

Phase 1.5: detail.png is a seeded (1515) 1920x1600 synthetic RGB texture with
fine lines and text, not an owner camera image. Native benchmark encodes both
modes at 2 MiB / 500 KiB / 100 KiB; reports bytes, dimensions and sampled RGB PSNR
after bilinear reconstruction to source size. Encoder quality is not a quality
metric. Artifacts under build/reports/quality are included in CI reports.
