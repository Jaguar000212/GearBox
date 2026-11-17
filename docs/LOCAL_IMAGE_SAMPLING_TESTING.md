# Color Picker: local image sampling

The image sampler remains part of the existing Color Picker. Choose one image through Android’s system visual picker; GearBox requests no broad photo-library permission and takes no persistable URI permission. It reads only the selected content URI and writes neither the URI nor image bytes into app storage/history. Applied RGB values use the existing Color Picker flow and can be recorded under the existing color-history preference.

## Bounds and interpretation

- Read at most 12 MiB; reject larger, empty or unsupported images without applying a color.
- Reject source dimensions above 12,000 pixels on either axis or 40 million pixels total.
- Decode on an IO dispatcher to software sRGB pixels, with a maximum 1,024-pixel preview edge. ImageDecoder handles orientation; partial/corrupt images are rejected.
- Transparency is composited on white. Animated formats use the decoded first frame. This is a screen-preview pixel, not an original full-resolution or physically calibrated color measurement.
- Preview height is bounded to 320 dp. Coordinates are zero-based in the decoded preview, not screen coordinates or original-image coordinates.
- The preview and coordinates are transient, not saved-instance state. Cancel/reselect/leave must not apply a stale color; picking does not apply any color until a tap or explicit coordinate sample.

## Device checks (not yet executed)

1. Choose a small known PNG with distinct red/green/blue corners. Tap each displayed corner; verify applied HEX and RGB. Use X/Y fields and Apply sampled color with TalkBack as well.
2. Choose a half-transparent red PNG. Verify the preview and sampled result are the same pink-on-white composite, not transparent black or uncomposited red.
3. Test portrait EXIF rotation, wide-gamut image conversion, a large downsampled photo, and an animated image. Compare the sampled pixel to the displayed preview, not an original-image editor.
4. Test extremely thin/wide images. Preview must stay bounded; accessible coordinate controls must remain usable.
5. Select an image, cancel a replacement picker, quickly reselect during loading, rotate, and leave the tool during loading. No previous decode may apply a color or recreate a discarded preview.
6. Test corrupt/truncated images, a file above 12 MiB, dimensions above the limits, and a provider that revokes access. Show an error without changing the selected color.
7. With history on/off, verify only applied color data follows the existing policy. Image URI/bytes must not appear in history, presets or Workbench.

Added JVM tests cover dimension bounds/downsample factors and pixel mapping. Android tests cover white alpha compositing, invalid/oversized input and cancellation checkpoints. Compiling instrumentation tests does not mean these device checks ran.
