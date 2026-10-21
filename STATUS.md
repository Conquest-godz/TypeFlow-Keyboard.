# TypeFlow status

## Verified baseline

- [x] Android/Kotlin starter project committed.
- [x] GitHub Actions Android debug build passed for commit `846056472d3f5ee6c0a993578027c604028655b7`.
- [x] Backspace long-press repeat implemented with lifecycle-safe cancellation.
- [x] Initial local SQLite clipboard-history store added in commit `670c3a147d1814db7fb86bd81961dcce02f21ca3`.
- [ ] Verify the clipboard-history store in CI after the latest commit.
- [ ] Test IME behavior on a physical Android device.

## TypeFlow Studio — target scope

The product direction is a full Keys Cafe-inspired customization suite, implemented as TypeFlow's own design rather than a copy of Samsung's proprietary assets or code.

- [ ] Layout Studio: editable key positions, sizes, row geometry, spacing, and alternate layouts.
- [ ] Theme Studio: per-key colors, text colors, backgrounds, borders, shapes, and gradients.
- [ ] Effects Studio: configurable press animations and visual effects.
- [ ] Feedback controls: haptics, sound, and preview behavior.
- [ ] Keyboard profiles: create, duplicate, rename, switch, import/export, and restore defaults.
- [ ] Live preview while editing.
- [ ] Persist all customizations and apply them to the active IME.
- [ ] Clipboard panel in the keyboard with searchable history, pinning, insert-on-tap, per-item deletion, and clear-all.
- [ ] Clipboard history with no app-imposed word-count or entry-count limit; practical OS, database, and storage limits still apply.
- [ ] Privacy controls: local-only history by default, pause capture, and clear history.
- [ ] Accessibility and touch-target checks across portrait, landscape, and different display sizes.
- [ ] Add regression tests and verify on physical Android devices.

## Clipboard design constraints

Clipboard history must be local by default. Android restricts clipboard access in some app states, so reliable capture of every copy from every app cannot be promised. TypeFlow can save clips it can observe while the input method is active and clips the user explicitly saves. Long text must not be truncated by an arbitrary product-level word limit. A user-visible clear-history action and capture toggle are required.

## Verification policy

A passing CI compilation does not establish correct IME behavior, touch interaction, clipboard capture coverage, or UI quality. Report each as verified only after the relevant check has actually run.
