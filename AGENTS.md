# TypeFlow contributor instructions

These instructions apply to humans and AI coding assistants working in this repository.

## Source of truth

- Always inspect the current branch and repository files before editing.
- Treat committed files as authoritative; conversation summaries and old ZIPs may be stale.
- Do not invent existing APIs or claim code was read if it was not.
- Preserve the package namespace `com.typeflow.keyboard` unless a deliberate migration is documented.

## Android IME correctness

- Test against real `EditorInfo` input types and IME action flags.
- Do not send passwords or other typed content to logs, analytics, network services, or clipboard history.
- Handle a null or stale `InputConnection` safely.
- Ensure key repeat/touch callbacks are cancelled when the view detaches or input finishes.
- Verify shift state, double-tap Caps Lock, backspace repeat, punctuation, composing text, cursor movement, and editor action behaviour.
- Keep keyboard layout and state changes accessible and responsive to portrait/landscape changes.
- Avoid blocking the main thread.

## Changes and validation

- Make small, focused changes and explain the root cause being addressed.
- Add or update tests for logic that can be tested without a device.
- Run Gradle checks when the project configuration is available.
- If the Android SDK, Gradle distribution, or network is unavailable, state exactly what could not be run.
- Never claim a build, test, APK install, or device test passed unless its result was observed.
- Do not commit generated build output, local IDE state, signing material, API keys, tokens, or personal data.

## Pull requests

Include:
- Summary of the change and why it is needed.
- Files affected and relevant trade-offs.
- Exact validation commands and their results.
- Remaining risks or device checks required.
