# Android Visual QA Companion

Capture-first visual feedback for Android: capture a screen, mark the problem, add multiple comments, and reopen the evidence later.

## MVP

- One-page onboarding with clear capture-access setup.
- Dashboard of saved and unfinished captures with thumbnails, status, and comment counts.
- One-tap capture from the accessibility overlay, with Quick Settings as a fallback.
- Frozen-frame editor with entity-aware snapping, touch/stylus markup, and multiple comments per screen.
- Local, reopenable drafts and saved reports.
- Share/export of the local evidence package.
- Material 3 / Material 3 Expressive-aligned Compose UI with dynamic color, adaptive layouts, and accessible controls.

The companion uses accessibility metadata as best-effort evidence. It does not claim exact source or composable identity for arbitrary apps. Captures remain local until the user chooses to share them.

See [`ANDROID_VISUAL_QA_IMPLEMENTATION_PLAN.md`](./ANDROID_VISUAL_QA_IMPLEMENTATION_PLAN.md) for the focused MVP plan.

## Build

```bash
./gradlew assembleDebug
./gradlew test
./gradlew :app:connectedAndroidTest
```

The physical-device capture flow is part of acceptance; a successful build is not sufficient.

## MVP modules

```text
:app                  onboarding, dashboard, capture orchestration, navigation
:annotation           frozen-frame editor and markup state
:capture:*            accessibility and pixel capture adapters
:core:model           persisted evidence/report model
:core:geometry        coordinates, bounds, transforms, hit testing
:core:files           atomic local draft attachments
:matching             candidate ranking and parent/child selection
:report               local report assembly and serialization
:export:share         Android share-sheet export
```

Other modules may remain in the repository as experiments or existing code, but they are not MVP scope or product promises.

## License

Apache License 2.0. See [`LICENSE`](./LICENSE).
