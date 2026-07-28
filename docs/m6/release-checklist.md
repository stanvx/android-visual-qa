# Release Checklist — M6

Checklist for releasing Android Visual QA M6 (Enterprise Release Wiring).

---

## Pre-Release

### Source & Build

- [ ] All unit tests pass: `./gradlew test --no-daemon`
- [ ] All instrumented tests pass: `./gradlew connectedCheck --no-daemon`
- [ ] All benchmarks within budget (see [perf-budgets.md](perf-budgets.md))
- [ ] R8 release build succeeds: `./gradlew :app:assembleRelease --no-daemon`
- [ ] R8 debug build succeeds: `./gradlew :app:assembleDebug --no-daemon`
- [ ] Baseline profile is generated and bundled in the release APK
- [ ] SBOM generated: `bash scripts/generate-sbom.sh`
- [ ] SBOM validates as CycloneDX 1.5 JSON
- [ ] No lint errors: `./gradlew lint --no-daemon`
- [ ] Dependency license report generated: `./gradlew licenseReport --no-daemon`
- [ ] `proguard-rules.pro` reviewed — no unnecessary keep rules
- [ ] `app/build.gradle.kts` — signing config points to the real release
      keystore (not the debug placeholder)

### Documentation

- [ ] MDM configuration tested on at least one MDM platform
- [ ] S Pen validation completed (minimum 3 device models)
- [ ] Performance budgets documented and reviewed
- [ ] Release notes drafted

### Security

- [ ] ProGuard/R8 mapping file retained (`build/outputs/mapping/release/mapping.txt`)
- [ ] No secrets, keys, or tokens in the repo
- [ ] AccessibilityService privacy disclosure reviewed
- [ ] Network security policy reviewed (no cleartext traffic to production)
- [ ] Audit logging verified functional

### Verification

- [ ] Smoke test: launch, capture, annotate, save, share
- [ ] Smoke test: launch in work profile
- [ ] Smoke test: orientation change during annotation
- [ ] Smoke test: low-storage scenario (< 100 MB free)
- [ ] Smoke test: quick tile captured from another app

---

## During Release

- [ ] **Tag the release:**
  ```bash
  git tag -a m6-0.1.0 -m "M6 Enterprise release 0.1.0"
  git push origin m6-0.1.0
  ```

- [ ] **Build signed release APK / AAB:**
  ```bash
  ./gradlew :app:bundleRelease --no-daemon
  ```
  Output: `app/build/outputs/bundle/release/app-release.aab`

- [ ] **Sign with the release keystore** (if not configured in Gradle):
  ```bash
  jarsigner -keystore release.keystore \
    app/build/outputs/bundle/release/app-release.aab \
    release-key-alias
  ```

- [ ] **Verify alignment and signature:**
  ```bash
  ./gradlew :app:verifyReleaseBundle --no-daemon
  ```

- [ ] **Push to distribution channel (internal track):**
  - Upload AAB to Managed Google Play internal track.
  - Verify the release is available to test devices within 5 minutes.

- [ ] **Canary deployment:**
  - Distribute to 5 % of test devices.
  - Monitor crash rate for 24 hours.
  - No new crash clusters → promote to production track.

- [ ] **Upload ProGuard mapping file:**
  - Upload `build/outputs/mapping/release/mapping.txt` to the crash
    reporting service (or store it alongside the release tag).

- [ ] **Upload SBOM:**
  - Attach `build/sbom/sbom.cdx.json` to the release notes or store in
    a secure artifact repository.

---

## Post-Release

- [ ] **Monitor crash reports** for 48 hours:
  - No unexpected `ClassNotFoundException`, `MethodNotFoundException`,
    or `NullPointerException` spikes.
  - Compare crash rate against baseline (pre-release).

- [ ] **Validate telemetry:**
  - Verify expected events are arriving: capture, annotation, export.
  - Compare event counts to user counts for sanity.

- [ ] **Merge any hotfix branches** back to `main`.

- [ ] **Close the milestone** in the issue tracker.

- [ ] **Archive the release build:**
  - Store AAB, mapping file, SBOM, and release notes in the organisation's
    artifact store (e.g., S3, Artifactory) for compliance retention.

- [ ] **Post-mortem:**
  - If the release required any manual steps not in this checklist, update it.
  - If any CI gate was bypassed, file a ticket to fix the gate.
