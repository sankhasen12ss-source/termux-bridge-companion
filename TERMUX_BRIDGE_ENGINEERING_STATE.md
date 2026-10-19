# Termux Bridge Companion — Engineering State and Handover

**Last updated:** 2026-10-19  
**Purpose:** Preserve the implementation state, known failures, safety constraints, and next-step plan for the Android companion app intended to bridge ChatGPT and Termux.

## 1. Executive status

- The Android companion app is a **prototype**, not yet a completed ChatGPT-to-Termux integration.
- It can launch a **fixed, read-only diagnostic command** through Termux's `RunCommandService` and receive a result through an Android `PendingIntent`/`BroadcastReceiver` callback.
- It does **not** automatically read messages from the ChatGPT app, does not execute text shared from ChatGPT, and does not send Termux output back into the same ChatGPT conversation.
- The local Git histories were diverged. A merge was completed locally on 2026-10-19.
- **Latest known local commit:** `2a06fe6` — `Merge GitHub receiver fixes with callback handling`.
- **Latest known GitHub `origin/main` before the merge:** `6cfaa26` (`Restore receiver and fix Termux result parsing`), preceded by `3a8956f` (`Fix Termux result bundle and error parsing`).
- **Push is still pending/unverified.** The local branch was `ahead 2` after the merge. No successful push was shown.
- A final post-merge source-integrity check was suggested but **was not run**, because Termux crashed/closed.
- No APK build of the merged commit has been confirmed. The previous successful GitHub Actions build predates the current local callback changes.

## 2. Project identity and environment

- Android device: OnePlus Nord, Android 16 / API 36.
- Termux: `0.119.0-beta.3`.
- Local project directory: `$HOME/termux-bridge-companion-upload`
- GitHub repository: `sankhasen12ss-source/termux-bridge-companion`
- Main source file: `app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java`
- Activity: `app/src/main/java/org/example/termuxbridge/MainActivity.java`
- GitHub Actions workflow: `.github/workflows/build.yml`
- Workflow behavior: push to `main`/`master` or manual dispatch; Ubuntu runner, Temurin Java 17, Gradle 8.9, `assembleDebug`, uploads `app/build/outputs/apk/debug/app-debug.apk` as artifact `termux-bridge-debug-apk` for 7 days.
- GitHub Actions page: https://github.com/sankhasen12ss-source/termux-bridge-companion/actions
- Earlier successful build (not the merged source): run ID `39153243028`, commit `6cfaa26`, artifact `termux-bridge-debug-apk`, size 7048 bytes, SHA-256 `9b88b98a75b0b202b3518fc269e4194d7a71cc756d567d7c509cd0d9175d0fbe`, expiry was 2026-10-25. Do not treat this APK as containing the latest local callback fixes.
- GitHub connector actions previously returned 403 `Resource not accessible by integration`; user is pushing manually from Termux.
- No Java/Gradle wrapper or Android SDK was available in Termux in earlier checks, so GitHub Actions is the intended build route.

## 3. Current implementation

### `CommandResultReceiver.java`

The locally merged implementation is intended to include:

- Request-specific callback tracking in a `ConcurrentMap<String, Consumer<String>>`.
- Unique `UUID` request ID per command.
- Unique Intent action containing the request ID and `requestId` extra.
- Incrementing request code for `PendingIntent`.
- `FLAG_ONE_SHOT | FLAG_MUTABLE`.
- A 60-second main-thread `Handler` timeout that removes a pending callback and returns a timeout message.
- Cleanup of callback if `startService` throws.
- Result parsing checks the documented key first:
  `com.termux.app.notify.plugin_result_bundle`
- Compatibility fallback checks `"result"` and then scans nested `Bundle` extras for recognizable result fields.
- Reads `stdout`, `stderr`, `exitCode`, `err`, and `errmsg`.
- Reports missing/unrecognized result bundles rather than silently treating arbitrary extras as the result.
- `looksLikeResult(Bundle)` helper checks for any expected result field.

The command is intentionally fixed and diagnostic-only:

```sh
printf 'TERMUX_BRIDGE_OK\n'; printf 'user='; id -un; printf 'time='; date -Iseconds
```

It runs through `/data/data/com.termux/files/usr/bin/sh` with the Termux home directory as working directory. It is not intended to execute arbitrary text shared from ChatGPT.

### `MainActivity.java`

Local commit `46e1b0e` added a busy-button guard:

- The “Run fixed Termux diagnostic” button is disabled while a request is pending.
- The callback posts the result to the UI thread and re-enables the button.
- Shared text received through `Intent.EXTRA_TEXT` is displayed as data; it is not executed.

## 4. Git history and current known state

The original shared base was `c96f119` (`Add Termux bridge companion prototype`).

Before integration:
- Local commit: `46e1b0e` — `Fix Termux command result callback handling`.
- GitHub `origin/main`: `3a8956f` and `6cfaa26`.
- `git rev-list --left-right --count origin/main...main` returned `2 1`.
- GitHub and local changes both touched `CommandResultReceiver.java`; local also changed `MainActivity.java`.
- A safety branch was created:
  `safety/local-before-integration-20261019`
  pointing to `46e1b0e`.

The merge was started with `git merge --no-commit --no-ff origin/main`. Git reported the expected content conflict in `CommandResultReceiver.java`. The combined receiver implementation was then written and staged, and the merge commit was created successfully.

Latest observed history:

```text
*   2a06fe6 (HEAD -> main) Merge GitHub receiver fixes with callback handling
|\
| * 6cfaa26 (origin/main, origin/HEAD) Restore receiver and fix Termux result parsing
| * 3a8956f Fix Termux result bundle and error parsing
* | 46e1b0e (safety/local-before-integration-20261019) Fix Termux command result callback handling
|/
* c96f119 Add Termux bridge companion prototype
```

Latest observed status after merge:

```text
## main...origin/main [ahead 2]
?? app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java.before-callback-fix.20261019-001718
?? app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java.before-key-correction.20261019-000306
?? app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java.before-request-code-fix.20261019-002058
?? app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java.before-result-fix.20261018-235118
?? app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java.before-sed-fix.20261019-000347
?? app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java.before-uuid-timeout.20261019-001955
?? app/src/main/java/org/example/termuxbridge/MainActivity.java.before-callback-fix.20261019-001718
```

All seven backup files were visible and should be retained. They are untracked backup files, not changes to commit. Do not delete them.

### Credential/push history

- User entered GitHub username `sankhasen12ss-source` and a PAT at the HTTPS Git prompt.
- The attempted push was rejected with `(fetch first)` because GitHub had two commits missing locally.
- `git fetch origin` succeeded.
- Do not ask the user to paste the PAT. Do not store the PAT in project files, command strings, or this handover.
- After the local merge, the branch was `ahead 2`; a successful push has **not** been confirmed.

## 5. Checks already observed vs checks still pending

### Observed

- Before merge, `git diff --check` was clean.
- Before merge, the source structure showed callback handling and the busy-button change.
- `git fetch origin` succeeded and retrieved the remote commits.
- The safety branch exists and points to the local callback-fix commit.
- The merge commit `2a06fe6` was created.
- After merge, `git status` reported `main...origin/main [ahead 2]`.
- The seven backup files remained present.

### Not verified yet

- The post-merge `git diff --check`/source-integrity check was not run.
- Java compilation of `2a06fe6` has not been confirmed.
- GitHub push of `2a06fe6` and `46e1b0e` has not been confirmed.
- GitHub Actions build for the merged commit has not been confirmed.
- Installation and runtime behavior of the merged APK have not been confirmed.
- A real Termux callback has not been confirmed against this merged version.
- No evidence yet that Android process death/recreation is handled. The static callback map and timeout are in-process only.
- The current app still has no automatic ChatGPT message intake or response delivery mechanism.

## 6. Immediate recovery plan after Termux closed

Reopen Termux and run the following **read-only** block first. It should be safe even if the app/terminal closed during the previous check.

```bash
cd "$HOME/termux-bridge-companion-upload" || exit 1

echo "=== BRANCH AND STATUS ==="
git status --short --branch

echo
echo "=== CURRENT COMMIT ==="
git --no-pager log -1 --oneline

echo
echo "=== CONFIRM SAFETY BRANCH ==="
git show-ref --verify refs/heads/safety/local-before-integration-20261019

echo
echo "=== SOURCE INTEGRITY ==="
FILE="app/src/main/java/org/example/termuxbridge/CommandResultReceiver.java"
git diff HEAD^ HEAD --check || exit 1

echo
echo "=== REQUIRED RECEIVER LOGIC ==="
grep -nE 'com\.termux\.app\.notify\.plugin_result_bundle|CALLBACKS|postDelayed|looksLikeResult|requestId' "$FILE"

echo
echo "=== CONFLICT MARKER CHECK ==="
if grep -nE '^(<<<<<<<|=======|>>>>>>>)' "$FILE"; then
  echo "ERROR: conflict markers found."
  exit 1
else
  echo "No conflict markers found."
fi
```

If the branch is still at `2a06fe6`, the source check passes, and no unexpected changes appear, the next planned action is:

```bash
git push origin main
```

If prompted, enter username `sankhasen12ss-source`; paste the PAT only into the password prompt. Never send it in chat. Do not force-push.

If the push succeeds, verify the remote commit ID:

```bash
git rev-parse HEAD
git ls-remote origin refs/heads/main
git status --short --branch
```

The local `HEAD` commit ID and the remote `refs/heads/main` ID should match. Then inspect GitHub Actions for a run corresponding to the merged commit and wait for the build result before downloading/installing an APK.

If the source check fails or status differs unexpectedly, stop and report the output rather than trying resets, cleanups, or force-pushes.

## 7. Overall architecture goal: connect ChatGPT and Termux

The intended end goal is a controlled workflow in which the user can ask ChatGPT to perform an operation that requires Termux, execute the operation locally on Android with the user's approval, and return its result to the conversation.

Current architecture is only a local prototype:

1. User opens the Android companion app.
2. User taps “Run fixed Termux diagnostic”.
3. App starts Termux `RunCommandService` with a fixed diagnostic command.
4. Termux returns output through a `PendingIntent`.
5. `CommandResultReceiver` maps the request ID to the matching callback.
6. `MainActivity` displays the returned text.

Not implemented:
- Automatic detection or reading of ChatGPT conversation messages.
- Automatic execution of arbitrary ChatGPT-generated shell commands.
- Sending Termux output back into the same ChatGPT conversation.
- A verified ChatGPT app deep-link/share-back workflow.
- A secure command approval and policy layer for general-purpose operations.
- A persistent queue or process-death recovery mechanism.

### Recommended staged plan

**Stage A — Stabilize the diagnostic prototype**
1. Run the recovery/source-integrity checks.
2. Push the merged commits normally.
3. Verify GitHub Actions builds the merged commit.
4. Install that exact APK and test the fixed diagnostic on the device.
5. Record the actual displayed output and any Android/Termux errors.

**Stage B — Define the ChatGPT handoff mechanism**
1. Decide how the user explicitly sends a task from ChatGPT to the companion app. Candidate mechanisms must be tested on the actual Android app/device; do not assume ChatGPT supports a particular callback or deep-link.
2. Prefer explicit user share/action or a deliberate copy-and-open workflow over accessibility scraping or background monitoring of ChatGPT UI.
3. Keep received ChatGPT text as data. Do not execute arbitrary shared text automatically.
4. Define a strict request envelope with request ID, allowed operation, arguments, and expected output.
5. Show the proposed operation and require user approval before execution.

**Stage C — Execute constrained operations**
1. Use an allowlist of named operations rather than raw shell text wherever possible.
2. Validate inputs, impose timeouts/output limits, and capture exit code/stdout/stderr.
3. Return each result to the matching request ID; handle cancellation, timeout, and duplicate callbacks.
4. Consider Android process death, stale callbacks, and lifecycle behavior.

**Stage D — Return the result to ChatGPT**
1. Test an explicit share/copy-back path first.
2. Only consider automated delivery if a supported and authorized ChatGPT integration path exists and has been demonstrated; do not claim that the companion can write into the current conversation without proof.
3. Keep sensitive output, credentials, and personal files out of returned results unless specifically needed and approved.

**Stage E — Harden before routine automation**
- Test missing Termux, disabled Termux service, permission denial, timeout, non-zero exit code, empty result, malformed bundle, repeated taps, delayed callback, duplicate callback, app recreation, and Android process death.
- Verify no arbitrary command execution from incoming text.
- Keep an audit trail without logging secrets.
- Do not connect this prototype to N8n or other automation until execution permissions and command policy are defined and tested.

## 8. Important boundaries and working rules

- Do not use `git reset`, `git restore`, or `git clean`.
- Do not force-push.
- Do not delete timestamped backup files.
- Do not overwrite or disturb the separate Route 1 API/reverse-SSH tunnel project while working on this companion app.
- Avoid Codex for routine repository inspection; prefer one consolidated Termux command and one consolidated report.
- Explain a command's purpose before asking the user to run it.
- Do not claim a test, build, push, install, or live operation succeeded unless the output proves it.
- Never ask the user to paste GitHub PATs, passwords, private keys, or other secrets into chat.
- Termux has closed/crashed during recent command sequences. Keep commands consolidated but avoid unnecessarily huge repeated outputs; if it closes again, resume from the current commit and inspect status rather than restarting the whole investigation.

## 9. Handover for the next ChatGPT conversation

You are continuing work on the Termux Bridge Companion project. Read this engineering state first and do not restart earlier investigations.

**Project:** `$HOME/termux-bridge-companion-upload`  
**Repository:** `sankhasen12ss-source/termux-bridge-companion`  
**Current expected local commit:** `2a06fe6` — merge commit, `Merge GitHub receiver fixes with callback handling`  
**Safety branch:** `safety/local-before-integration-20261019` points to `46e1b0e`  
**Remote before merge:** `origin/main` at `6cfaa26`; it had two commits not in local history.  
**Last known status:** local `main` ahead of `origin/main` by 2 commits; seven untracked backup files present.  
**Latest blocker:** Termux closed before the final post-merge source check. Push and build remain unverified.

Start by asking the user to reopen Termux and run the read-only recovery block in Section 6. Inspect its output. If it confirms `2a06fe6`, the merge is clean, and no unexpected changes are present, run the source checks, then push normally and verify the remote commit ID. Do not force-push or reset. After the push, verify GitHub Actions builds the merged commit; then test the installed APK's fixed diagnostic. The ultimate ChatGPT-to-Termux workflow is not implemented yet; follow the staged plan in Section 7, prioritizing explicit user consent and constrained operations. Preserve all backups and avoid touching the separate Route 1 tunnel/API project.

## 2026-10-19 — Installed APK diagnostic acceptance

- GitHub Actions build for commit `bfb5b18` completed successfully.
- Installed APK launched the fixed Termux diagnostic successfully on the OnePlus Nord running Android 16 (API 36).
- Termux required the Android `RUN_COMMAND` permission and `allow-external-apps=true` in `~/.termux/termux.properties`; both prerequisites were addressed.
- Verified diagnostic output:
  - `TERMUX_BRIDGE_OK`
  - `user=u0_a453`
  - Timestamp: `2026-10-19T07:54:54+05:30`
  - Exit code: `0`
  - Standard error: empty
- The app received and displayed the command result. Service error code was `-1`, despite the successful command exit and expected output.
- Stage A fixed-diagnostic device test: PASS.
- Remaining limitations: automatic ChatGPT message capture, arbitrary command execution, and automatic return of results to the same ChatGPT conversation are not implemented.
- Next stage: investigate an explicit, user-triggered Android share handoff. Preserve the fixed-command safety boundary.
