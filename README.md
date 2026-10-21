# TypeFlow Keyboard

TypeFlow is an experimental Android on-screen keyboard (IME) written in Kotlin.

This repository is the shared source of truth for human and AI-assisted development. Please work from the repository rather than from stale ZIP snapshots.

## Current status

**Repository bootstrap in progress.** The repository was initially empty. The source snapshots available to this workspace are older TypeFlow Alpha-era files; they must not be mistaken for a complete, build-verified Phase G delivery. Build and device verification are still required.

## Project goals

- A comfortable, rounded Android keyboard with a dark visual style.
- Correct Shift/Caps Lock and continuous Backspace behaviour.
- Context-aware Enter/Search/Send actions.
- Adjustable height, floating and one-handed layouts, and landscape support.
- Clear settings and accessibility-aware interaction.
- Reproducible builds and regression checks.

## Development

The Android application package is intended to be `com.typeflow.keyboard`. The project uses Kotlin and Android's Input Method Service APIs.

Once the Gradle project files are in place, build with:

```bash
./gradlew assembleDebug
./gradlew test
```

These commands are instructions, not a claim that they have passed. Use a compatible JDK and install the Android SDK platform/build-tools required by the Gradle configuration.

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
