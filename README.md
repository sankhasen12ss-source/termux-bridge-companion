# Termux Bridge Companion (experimental)

This is a small Android companion app intended to test the Android Share Intent -> Termux `RUN_COMMAND` execution/result path. It does **not** claim to make the ChatGPT Android app invoke commands automatically.

## Safety design
- It does not turn shared text into a shell command.
- The first version only runs one fixed, harmless diagnostic command.
- The result is displayed in the companion app.
- No network listener, polling daemon, arbitrary shell, or remote command endpoint is included.
- Accessibility automation is deliberately not enabled in this first test. It should be added only after the share/command/result path is verified and its privacy implications reviewed.

## Build
The repository includes a GitHub Actions workflow that builds a debug APK on a public repository using a standard GitHub-hosted runner. Public-repository standard runner usage is free under GitHub's published policy. Do not put secrets in this repository.

1. Upload these source files to a **new public repository** you control (do not put them in your Route 1 repository).
2. Push to the default branch or manually run the `Build companion APK` workflow.
3. Download `termux-bridge-debug-apk` from the workflow run's Artifacts.
4. Install the APK on the same phone that has the GitHub-sourced Termux build.

## Phone setup, only after APK is built
1. Open the companion app and grant its requested `Run commands in Termux environment` permission if Android presents it.
2. In Termux, back up `~/.termux/termux.properties` before making any change. The documented RUN_COMMAND service requires `allow-external-apps=true`. Do not enable this until you have reviewed the file and agree to it.
3. Share harmless text to the companion app and tap **Run diagnostic**. It runs a fixed command only.
4. Compare the displayed stdout/stderr/exit code.

## Current limitations
- This is a proof-of-mechanism for Android sharing + Termux execution/result return.
- It does not detect ordinary ChatGPT messages automatically.
- It does not send results back into the originating ChatGPT conversation.
- No AccessibilityService is included yet; automatic UI detection is a separate, higher-risk test.
