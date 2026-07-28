# Android Visual QA Companion

Capture-first visual feedback for Android: trigger a snapshot, freeze the
frame, annotate, and emit a deterministic, schema-versioned report.

## Status

**M0 — Repository, contracts, schema tests.** See [`ANDROID_VISUAL_QA_IMPLEMENTATION_PLAN.md`](./ANDROID_VISUAL_QA_IMPLEMENTATION_PLAN.md) for the full architecture and milestone plan.

## Design at a glance

- **Companion APK** for unmodified apps. Uses an `AccessibilityService` for
  window/component metadata, `takeScreenshotOfWindow()` on Android 14+, and
  sensible fallbacks (display capture, MediaProjection, manual import, text-only).
- **Optional Compose enrichment SDK** for first-party apps. Adds stable
  component IDs, exact bounds, route, design-system tokens, and privacy
  classification. Emits the same report schema.
- **Capture-first workflow** — trigger, hide the trigger, snapshot the
  tree and pixels, validate they describe the same display/window, persist
  an atomic draft, open a full-screen frozen-frame editor.
- **Deterministic report schema** — versioned, `@Serializable`, never
  relying on Kotlin class names.

## Modules (planned)

```
:app                       companion APK orchestration
:core:model                pure-Kotlin evidence/report schema
:core:geometry             coordinate transforms, polygons, hit testing
:core:privacy              classification and redaction rules
:core:database             Room entities, DAO, migrations
:core:files                atomic attachment storage, hashing, encryption
:core:testing              fakes, fixtures, golden report corpus
:capture:api               capture contracts and state machine types
:capture:accessibility     service, node snapshots, overlay trigger
:capture:pixels            screenshot and MediaProjection adapters
:annotation                ink canvas, tools, stroke model, editor state
:matching                  pure deterministic candidate-ranking engine
:report                    report assembler, serializers, Markdown and ZIP
:export:share              Android share/file exports
:export:github             GitHub issue adapter (later)
:export:jira               Jira issue adapter (later)
:export:agent              AI-agent/MCP payload adapter (later)
:sdk:compose-core          platform-neutral SDK contracts
:sdk:compose               VisualFeedbackHost, Modifier.feedbackTarget
:sample:target-compose     deterministic fixture app
:sample:sdk-integration    consumer integration sample
:e2e                       UiAutomator/device integration tests
:benchmark                 macrobenchmark and Baseline Profile
```

## Build

```
./gradlew assembleDebug          # build
./gradlew test                   # pure-JVM unit tests
./gradlew :app:connectedAndroidTest   # device tests
```

## License

Apache License 2.0. See [`LICENSE`](./LICENSE).

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md). All contributors are expected
to follow the [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md).
