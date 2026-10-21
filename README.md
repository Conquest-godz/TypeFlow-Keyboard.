# TypeFlow Keyboard

TypeFlow is an experimental Android on-screen keyboard (IME) written in Kotlin.

This repository is the shared source of truth for human and AI-assisted development. Please work from the repository rather than from stale ZIP snapshots.

## Current status

**Bootstrap baseline committed; verification is in progress.** The repository was initially empty. The source snapshots available to this workspace were older TypeFlow Alpha-era files, so this repository currently contains a reconstructed starter baseline—not a verified copy of the complete Phase G delivery. The current keyboard implementation is intentionally incomplete; see the limitations below.

## Project goals

- A comfortable, rounded Android keyboard with a dark visual style.
- Correct Shift/Caps Lock and continuous Backspace behaviour.
- Context-aware Enter/Search/Send actions.
- Adjustable height, floating and one-handed layouts, and landscape support.
- Clear settings and accessibility-aware interaction.
- Reproducible builds and regression checks.

## Development

The Android application package is `com.typeflow.keyboard`. The project uses Kotlin and Android's Input Method Service APIs.

This bootstrap currently does not include the Gradle wrapper. With JDK 17, Gradle 8.9, and Android SDK platform/build tools 35 installed, build with:

```bash
gradle assembleDebug --no-daemon
```

The GitHub Actions workflow installs these build dependencies and runs the same assemble task on pushes and pull requests. The debug APK is uploaded as a workflow artifact when the build succeeds.

These commands are instructions, not a claim that a build has passed. Check the [latest workflow run](https://github.com/Conquest-godz/TypeFlow-Keyboard./actions/workflows/android-build.yml) for the actual result.

## Known baseline limitations

- The current code is not a full implementation of every feature previously discussed.
- Continuous Backspace repeat, functional settings persistence, a real emoji picker, floating/one-handed modes, adjustable keyboard height, and a full active Shift/Caps visual state are not implemented yet.
- Unit and instrumentation tests have not yet been added.
- A successful CI build would verify compilation only; it would not establish correct behaviour on a physical device.
- A Gradle wrapper and a project license have not yet been added.

## AI-assisted contributions

Before changing code:

1. Read [AGENTS.md](AGENTS.md).
2. Inspect the current repository and latest commit.
3. Make focused changes and explain the root cause.
4. Run the available checks and report exact commands/results.
5. Do not claim an APK or runtime behaviour was verified unless it actually was.

Keep secrets, signing keys, keystores, local SDK paths, and personal data out of commits.

## License

No license has been selected yet. Until one is added, do not assume this code is licensed for unrestricted reuse or redistribution.
