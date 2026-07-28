# M1 Editor — Design Spec

Single source of truth for the M1 frozen-frame annotation editor. The implementation
lanes (`:annotation`, `:app`) must match this document. Deviations need a written
note in the PR.

## Goals

The M1 editor delivers exactly four user-visible things:

1. **Frozen-frame capture.** The user captures a still image of the current screen
   via the trigger (overlay or Quick Settings tile). The editor opens on that
   frozen bitmap. There is no live overlay on top of another app.
2. **Rectangle annotation.** The only annotation tool in M1 is a rectangle drawn
   by tap-drag-release. Pen, freehand, arrows, and erasers are out of scope.
3. **Typed feedback text field.** A single text field below the canvas captures
   the user's sentence-long description. No voice. No AI rewrite.
4. **Review-and-save flow.** After annotating, the user is taken to a Review
   screen that shows the original image, the annotated image, the rectangles, and
   the feedback text. Save persists the report. Export Sheet offers JSON,
   Markdown, image, and share.

## Information architecture

Five screens. Entry conditions and capabilities for each.

### Trigger screen (overlay + Quick Settings tile)

- Entry: user taps the overlay bubble or the Quick Settings tile in another app.
- The trigger is a small floating control, not a full screen. The user sees a
  brief "Capturing…" pill while the state machine runs.
- Single action: cancel (long-press the pill, or system back).

### Draft List

- Entry: app launch, or completion/cancellation of a capture session.
- Shows all drafts and recent reports with status (Draft, Saved, Exported).
- User can open a draft to resume, delete a draft, or trigger a new capture.
- The newly-saved entry highlights for ~2 seconds and fades.

### Editor

- Entry: a fresh draft is available, or the user tapped an existing draft.
- The frozen bitmap fills the canvas with the rectangle tool active by default.
- The user can draw exactly one rectangle, edit the feedback text, and tap Save.
- Cancel returns to Draft List and keeps the draft for later.

### Review

- Entry: Save tapped in Editor.
- Renders the original image, the annotated image, the rectangle summary, and
  the feedback text in read-only form.
- User can tap an Edit button to return to the Editor, or tap Save & Export.

### Export Sheet

- Entry: Save & Export tapped in Review.
- Bottom sheet with destinations: Save locally, Share, Copy as JSON, Copy as
  Markdown, Save annotated image.
- All actions are idempotent. Reopening the sheet after a save state shows a
  "Saved" marker.

## Layout grid

- 8dp base grid. All spacing is a multiple of 8dp.
- 16dp gutters on phone. 24dp gutters on tablet.
- Bottom action bar pinned, 56dp tall, 16dp side padding, 8dp gap between
  primary and secondary buttons.
- Top app bar 64dp, with a Cancel chip on the left and a Save chip on the
  right. Each chip is 48dp tall, 16dp horizontal padding.
- Image fills the middle 60–75% of the viewport vertically. The feedback text
  field occupies the remaining 25–40%, scrolling within its own bounds.

## Editor anatomy

Six regions, top to bottom on phone, with tablet variants noted.

| Region        | Phone                          | Tablet                           |
|---------------|--------------------------------|----------------------------------|
| Status bar    | System, 24dp                   | System, 24dp                     |
| App bar       | 64dp, Cancel + Save chips      | 64dp, Cancel + Save chips        |
| Canvas        | Fills middle 60–75%            | Fills middle 70–80%              |
| Tool palette  | Bottom strip, 56dp, 4 slots    | Left edge, 64dp wide, vertical   |
| Feedback text | 96dp default, scrollable       | 120dp default, scrollable        |
| Save bar      | 56dp, two buttons              | 56dp, two buttons                |

- Minimum touch target: **48dp × 48dp** everywhere. Chips meet this.
- Feedback text field: **minimum visible height of 3 lines** (about 64dp at
  bodyLarge), **maximum 6 lines** before it scrolls.
- Rectangle tool interaction:
  - **Tap-drag-release** draws one rectangle. Lift to commit.
  - **Two fingers** pinch to zoom (range 1.0×–4.0×).
  - **One finger drag** pans the canvas when zoomed in.
  - **Pinch-to-zoom is disabled when only one finger is down.** This prevents
    the pinch from being triggered by a single-finger drag.
  - Drawing a second rectangle replaces the first. M1 keeps at most one
    rectangle per draft.

## State model

The Editor derives every UI state from `CaptureState`. One state, one screen.

| `CaptureState`              | UI screen                | Notes                                    |
|-----------------------------|--------------------------|------------------------------------------|
| `Idle`                      | Trigger or Draft List    | Trigger overlay is dismissable           |
| `Armed`                     | Trigger overlay          | Awaiting capture-token grant             |
| `SnapshottingContext`       | Capturing… spinner       | Full-screen overlay, no cancel           |
| `CapturingPixels`           | Full-screen progress     | Cancel offered                           |
| `ValidatingFrame`           | Full-screen progress     | Cancel offered                           |
| `PersistingDraft`           | Full-screen progress     | Cancel offered                           |
| `LaunchingEditor`           | Editor (loading)         | Skeleton of canvas, no stroke yet        |
| `Annotating`                | Editor                   | Live rectangle + feedback field          |
| `Reviewing`                 | Review                   | Read-only summary                        |
| `Saving`                    | Progress + cancel        | Pinned bottom bar                        |
| `Exporting`                 | Progress + cancel        | Pinned bottom bar                        |
| `Complete`                  | Draft List               | New entry highlighted for 2s             |
| `Failed(recoverable)`       | Toast + retry            | Returns to Draft List with banner        |
| `Failed(terminal)`          | Draft List with banner   | Banner explains cause, no auto-retry     |
| `Cancelled`                 | Draft List with banner   | Banner offers "Resume"                   |
| `Resuming` (after process death) | Draft List with banner | Banner offers "Resume draft"             |

The editor screen does not have its own sub-states. Rectangle presence,
zoom level, and feedback text live in the draft, not in short-lived UI state.

## Accessibility

- Every tool in the palette has a TalkBack label. The rectangle tool reads
  "Rectangle tool. Drag to draw a box around the issue."
- The canvas has a content description that includes the package name and a
  short description of the captured screen, e.g. "Screenshot of com.example.app
  showing the settings page."
- Dynamic type is respected. Font scale range 0.85–2.0. Layouts reflow with
  measure passes; nothing is clipped at 2.0×.
- High-contrast palette for the editor: rectangle stroke is
  `onPrimary` over a 2dp primary outline. Editor background is a fixed
  neutral so it reads on every device.
- Large-content size respected: tool palette icons grow to 28dp, button labels
  stay full-width.
- Touch targets meet 48dp minimum. Visible focus ring on every interactive
  element when keyboard navigation is active.

## Color palette

M3 expressive defaults with a single accent. Documented as semantic roles,
not hex values, so the implementation can resolve them from the theme.

| Role              | Use                                              |
|-------------------|--------------------------------------------------|
| `surface`         | Page background outside the canvas               |
| `surfaceContainer`| App bar, save bar, tool palette background        |
| `onSurface`       | Default text and icons                           |
| `primary`         | Rectangle stroke, primary action button          |
| `onPrimary`       | Text on primary surface (button label)           |
| `error`           | Terminal-failure banner, failed-state text       |
| `outline`         | Divider lines, canvas border                     |

- Single accent: **purple-40 surface tint**. The editor chrome tints to it;
  the canvas stays neutral so the screenshot reads true.
- **Recommend `dynamicColor = false` for the first release.** Predictable
  appearance on enterprise MDM is more important than per-user theming.
- Dark mode is the same palette shifted to M3 dark roles. Both schemes
  ship in the preview.

## Typography

- System default (Roboto on Android) at M3 type sizes.
- Editor feedback text uses `bodyLarge` (16sp, 24sp line height) so it
  reads on a stylus at arm's length.
- App bar titles use `titleLarge` (22sp).
- Buttons use `labelLarge` (14sp, 20sp line height).
- Captions and metadata use `bodySmall` (12sp, 16sp line height).

## Motion

- Sheet transitions: 200ms `ease-in-out`. The Export Sheet rises and falls.
- Editor canvas crossfade on save: 120ms. The canvas fades to the Review
  screen's annotated preview.
- No bounce. No parallax. No springs. The editor is a workspace, not a
  showpiece. Boring is correct here.

## Edge cases

- **Keyboard up (IME inset).** The save bar pins to the IME top via
  `WindowInsets.ime`. The feedback field stays above the bar so the user
  can see what they typed.
- **Rotation during edit.** Canvas state is preserved in normalized
  coordinates. The draft is re-rendered at the new orientation; an
  indicator chip reads "Rotated — re-draw if needed."
- **Process death.** Drafts auto-recover from app-private storage. On
  next launch, the Draft List shows a Resume banner on the unfinished
  draft.
- **Low memory.** Live canvas effects (blur, parallax) are disabled when
  the system reports low memory. The editor still draws and saves.
- **Multi-window and freeform.** On screens wider than 600dp, the canvas
  is letterboxed. The tool palette moves to the left edge. The spec
  applies; the wireframes in this doc are phone-only.

## Out of scope for M1

Each deferred item is listed with the milestone it lands in, so reviewers
can confirm the M1 spec is honest.

| Feature                                          | Lands in |
|--------------------------------------------------|----------|
| Lasso / freehand selection                       | M2       |
| Pen, highlighter, arrow, eraser                  | M2       |
| Voice feedback and transcription                 | M2       |
| Manual and automatic redaction                   | M3       |
| Multi-select rectangles                          | M2       |
| Component matching (ranked candidates)           | M2       |
| AI enrichment and rewrite                        | M5       |
| Compose SDK enrichment                           | M4       |
| Live in-app overlay drawing                      | Never (by design) |
| Recording / continuous capture                   | Deferred |
| Custom rectangle colors, stroke widths           | M2       |
