# Android Visual QA Companion — MVP plan

**Status:** Product and implementation direction
**Updated:** 29 July 2026
**Platform:** Native Android, Kotlin, Jetpack Compose
**Minimum Android:** 11 / API 30
**Primary job:** Capture a screen, mark the problem, attach comments, and reopen the evidence later.

## Product decision

Build a local-first visual feedback companion with one short loop:

1. Open the page to review in another app, then tap the floating capture control or Quick Settings tile.
2. The app hides the control, captures a clean frozen frame and the visible accessibility tree, then opens the editor.
3. Tap or drag over the problem. The selection snaps to a detected UI entity when possible.
4. Add one or more comments, optionally draw markup, and save.
5. Return to a dashboard where every saved or unfinished capture can be opened.

The companion APK is the MVP. Accessibility metadata is best effort; it must never be presented as exact source/composable identity. The existing Compose SDK can remain in the repository, but it is not part of the MVP promise.

## MVP experience

### 1. First-launch onboarding

Replace the current permission-only landing screen with one focused onboarding page. Do not use a carousel.

The page contains:

- A short value statement: “Review any page, mark what is wrong, and send clear evidence.”
- A screenshot-style visual example showing a marked area and attached comment.
- A three-step explanation: open the target page, capture from Quick Settings, then mark and comment.
- A plain-language privacy note: captures stay on-device until the user shares them; secure windows cannot be captured.
- A setup checklist with live status for:
  - Accessibility capture access.
  - Notifications, when required by the foreground capture service.
- One primary action:
  - “Enable capture access” when setup is incomplete.
  - “Continue to captures” when setup is complete.
- A secondary “Skip setup for now” action; the dashboard remains usable for opening existing evidence and explaining how to capture another app.

On return from Android Settings, refresh the checklist instead of making the user restart onboarding. Store completion with the existing app preference mechanism. The onboarding must not appear again unless the user opens it from Settings.

### 2. Dashboard and history

The post-onboarding destination is **Captures**, not an empty draft screen.

Each history item is an openable card containing:

- Screenshot thumbnail, using the annotated image when available and the original image otherwise.
- App/package label.
- Relative capture time.
- Status: Draft, Needs review, or Saved.
- Number of comments/annotations.

Required behaviors:

- Open any item and restore its screenshot, markup, comments, selected entities, and editor state.
- Show unfinished drafts above completed captures.
- Sort newest first.
- Show a useful empty state explaining that capture starts from the app being reviewed.
- Do not offer a dashboard capture button: this screen is for reviewing captures made in another app.
- Keep capture available from the global accessibility overlay and Quick Settings.
- Support delete from an item overflow menu with confirmation.

Use the existing file-backed draft directories and history index for MVP. Add the smallest repository/read model needed to combine completed history entries with unfinished draft manifests. Do not introduce a new server, account model, search index, or a Room migration solely for this screen.

### 3. Capture entry point

The floating accessibility control is the primary capture affordance because it works from any app. Quick Settings remains a secondary fallback.

Capture behavior:

- One tap captures the current window and immediately opens the frozen-frame editor.
- Hide the control before taking the screenshot.
- Persist the original screenshot and capture metadata before opening the editor.
- Capture the accessibility tree at the same moment and retain it as candidate evidence.
- If the preferred screenshot path fails, show a clear fallback state or manual import action; never silently create a blank report.
- If capture is blocked by `FLAG_SECURE`, explain why and return safely to the source app.
- If the process dies, the dashboard offers the unfinished capture for resumption.

The dashboard must not launch capture; it explains the external-app flow and remains focused on reopening evidence.

### 4. Frozen-frame markup editor

The editor is the core product. It should feel like a lightweight markup board, not a form below an image.

#### Default interaction

Use **Comment** as the default tool:

- Tap a visible entity to select it and show a snapped outline.
- Drag a region to select a larger area; on release, snap the region to the best overlapping accessibility candidate when confidence is adequate.
- If no candidate is suitable, retain the manually drawn region.
- Open a compact anchored comment composer immediately after selection.
- Saving the composer creates a numbered annotation marker and leaves the canvas ready for the next comment.
- Tapping an existing marker opens that comment for editing.

This makes multiple comments natural: select → comment → select → comment. The user should not need to switch to a separate “add annotation” screen.

#### MVP tools

Keep the tool rail small:

- Comment/select with entity snapping.
- Pen/freehand.
- Arrow.
- Text note.
- Eraser/delete selected markup.
- Undo and redo.

Defer highlighter, ellipse, lasso, blur, crop, recording, and comparison mode until the basic comment loop is proven. Redaction remains a separate privacy action, not a tool that competes with commenting during the first pass.

#### Entity selection UX

- Draw the candidate bounds with a subtle selection tint and a stronger outline for the chosen entity.
- Show a small label such as “Button · Submit” when metadata exists.
- Provide “Change target” to cycle through the ranked candidate, its parent, and its child without redrawing the annotation.
- Show “Manual region” when there is no reliable candidate.
- Keep the selected candidate and confidence in the report; do not claim exact composable identity for accessibility-only evidence.

#### Canvas behavior

- Preserve normalized coordinates for markup and selection data.
- Support pinch zoom and pan, while keeping the comment composer reachable.
- Keep handles large enough for touch and stylus input.
- Snap to candidate bounds only within a visible, forgiving threshold; never move a deliberate freehand mark unexpectedly.
- Preserve the clean original image separately from the annotated render.

#### Comment model

The existing `AnnotationEvidence.linkedComment` is the simplest compatible basis. Evolve the current single-rectangle state into a list of annotation items, each with:

- Stable annotation ID.
- Tool and geometry.
- Optional linked accessibility/SDK candidate.
- Optional comment text/transcript.
- Creation order and editable status.

Keep the report-level feedback field for an optional overall summary. A saved capture must not flatten multiple comments into one text string.

### 5. Reopen and save behavior

Save is atomic and local:

- Save the editable draft after each meaningful change or when leaving the editor.
- Save the annotated image only as a derived attachment; the original always remains intact.
- Save all annotations, comments, selected candidates, and capture metadata in the report/draft data.
- On dashboard open, load the persisted editor state instead of starting with a blank rectangle.
- Back from the editor prompts only when there are unsaved changes; otherwise it returns immediately.
- A saved capture can be reopened and edited without creating a duplicate history entry.

## Material 3 / 2026 UI direction

Use Material 3 Expressive patterns where supported by the pinned Compose Material 3 version, while keeping the product usable on the stable dependency line. Android Developers currently lists Material 3 stable `1.4.0` and `1.5.0-alpha24` as of 15 July 2026; do not make an alpha-only component a hard MVP dependency. See the [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3).

### Component mapping

| Surface | MVP choice |
|---|---|
| App theme | Material You dynamic light/dark color, with a deterministic fallback below API 31 |
| Onboarding | Hero/task layout, large headline, `primaryContainer` explanation card, one pill-shaped primary button |
| Dashboard top bar | Flexible/large top app bar pattern with “Captures” and an overflow menu |
| Capture action | Accessibility overlay and Quick Settings tile; no dashboard capture FAB |
| History | Large-corner `ElevatedCard`/surface containers, not bordered tables |
| Editor top bar | Back, capture title/status, undo/redo, save |
| Editor tool rail | Compact toggle-button group or expressive button group with clear selected state |
| Comment composer | Anchored bottom sheet or modal bottom sheet with one focused text field and “Add comment” |
| Entity choice | Assist/filter chips or a short bottom sheet for candidate/parent/child choice |
| Loading/capture progress | M3 loading indicator or progress indicator; never an indefinite custom spinner |
| Navigation | Keep MVP to dashboard/editor routes; add a navigation bar only if a second real destination exists |

Use tonal surface hierarchy, large expressive shapes, dynamic color, accessible content descriptions, minimum touch targets, and edge-to-edge insets. Use `MaterialTheme.motionScheme` or spring-based motion for editor selection, sheet, and dashboard list changes. Avoid decorative motion during capture or text entry.

The official [Material 3 in Compose guidance](https://developer.android.com/develop/ui/compose/designsystems/material3) describes Material 3 Expressive as the current evolution of Material 3 and recommends adaptive/accessibility-aware Compose UI. Treat the library version as a dependency choice, not as permission to add every new component.

## Smallest implementation sequence

### Slice A — app shell

- Turn `PermissionDisclosureScreen` into the onboarding page described above.
- Rename the product-facing draft list to `Captures` and implement real history cards.
- Add a small history repository that reads existing report entries and draft manifests.
- Make every card navigate to the correct draft/report.
- Add dashboard empty, loading, failure, and delete states.

### Slice B — reliable capture handoff

- Keep `CaptureOrchestrator` as the single capture path.
- Validate overlay, Quick Settings, screenshot persistence, secure-window handling, and process-death resumption on a physical device.
- Remove placeholder/blank-editor behavior from the product path; keep it only in isolated tests.

### Slice C — multi-comment editor

- Replace the current one-rectangle editor state and callback with a list of annotation items.
- Reuse `MatchingEngine`, `DecisionPolicy`, `ParentNavigator`, and the existing geometry types for snapping and candidate cycling.
- Add the comment composer and the small MVP tool rail.
- Persist/load the full annotation list and comments.
- Render both original and annotated images and keep them independently addressable.

### Slice D — M3 polish and acceptance

- Apply the M3 theme, adaptive layout, surface hierarchy, motion, and accessibility semantics.
- Add Compose UI tests for onboarding, empty/history dashboard, multiple comments, candidate cycling, reopen, and unsaved-back behavior.
- Run the complete capture flow on a physical Android 14–16 device, including an external app, a secure window, rotation, stylus input, and process death.

## MVP acceptance checklist

The MVP is done when a tester can complete this on-device:

1. Install and launch the app.
2. Understand the product from one onboarding page and enable capture access.
3. From another app, tap the floating control once.
4. See a clean frozen screen in the editor, not the overlay.
5. Tap one detected entity, see its bounds snap, add a comment, then add a second comment elsewhere.
6. Draw an arrow and undo/redo it.
7. Save and return to the Captures dashboard.
8. See the thumbnail, app name, timestamp, status, and comment count.
9. Close/relaunch the app, open the capture, and see both comments and markup restored.
10. Start a second capture and confirm both captures remain openable.

Builds, unit tests, and previews support this check; they do not replace it.

## Explicit MVP boundary

Do not add these to the product plan or README:

- External issue-system, agent, or workflow integrations.
- Cloud sync, accounts, multi-tenant workspaces, or automatic upload.
- Continuous recording, autonomous navigation, or target-app actions.
- OCR as a required dependency or AI rewriting of user feedback.
- Source-code mapping claims from visual inference.
- React Native or additional SDKs.
- Screenshot comparison, regression baselines, or enterprise release/MDM work.

Existing extension modules may remain in the checkout while the MVP is developed, but they are not part of the app navigation, acceptance criteria, or public product promise.
