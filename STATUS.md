# TypeFlow status

## Repository bootstrap

- [x] Public repository populated with an Android/Kotlin starter project.
- [x] Added contributor instructions for humans and AI coding assistants.
- [x] Added Gradle configuration and Android input-method manifest registration.
- [x] Added a GitHub Actions debug-build workflow.
- [ ] Confirm a successful CI build.
- [ ] Add and commit a Gradle wrapper.
- [ ] Decide and add an open-source license.
- [ ] Rename the repository to remove the accidental trailing period in its current name.

## Functional work still required

- [ ] Compare this baseline against the complete Phase G source and reconcile differences.
- [ ] Add reliable Backspace long-press/repeat with lifecycle-safe cancellation.
- [ ] Complete double-tap Shift/Caps Lock state and visible active-state rendering.
- [ ] Implement settings persistence and connect settings to keyboard behaviour.
- [ ] Implement adjustable keyboard height, floating mode, one-handed mode, and landscape-specific layout.
- [ ] Implement context-aware Enter/Search/Send visuals and action handling for all editor types.
- [ ] Add a real emoji/input panel or explicitly defer it in the UI.
- [ ] Add unit tests for pure logic and Android instrumentation/manual test instructions.
- [ ] Install and test on a physical Android device.

## Verification policy

A queued or running GitHub Actions job is not a successful build. A successful debug compilation is not a substitute for physical-device IME testing. Report only checks whose results have actually been observed.
