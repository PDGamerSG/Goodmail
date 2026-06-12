# Swipe-right to mark important — design

Date: 2026-06-12. Approved by user (option: mark this email only, no sender rule).

## Behavior

- Right swipe (start-to-end) on an inbox row marks that one email `IMPORTANT`.
- The row is **not dismissed**: on release past the threshold it snaps back in place;
  the importance dot turns green via the normal Room flow update.
- While dragging right, the background shows green with a star icon (mirrors the
  existing red/delete background shown for left swipe). Left-swipe delete is unchanged.
- Snackbar "Marked as important" with **Undo**; undo restores the email's previous
  importance value (which may be `UNCLASSIFIED` or `NOT_IMPORTANT`).
- Swiping an already-important email is a no-op (no write, no snackbar).

## Components

- `EmailRepository.setImportance(id, importance)` — reads the prior value (kept for
  undo, same single-slot pattern as `undoLastDelete`), then `EmailDao.updateImportance`.
  `undoLastImportanceChange()` restores the remembered value.
- `InboxViewModel.markImportant(id)` / `undoMarkImportant()` — thin wrappers with the
  usual `runCatching` + error message.
- `SwipeableEmailRow` — gains `onMarkImportant`; enables `StartToEnd`, and
  `confirmValueChange` returns `false` for that direction so the row settles back.
- `InboxScreen` — wires the callback, guards the no-op case, shows the undo snackbar.

## Why the manual flag sticks

Classification (`ClassifyEmailUseCase`) only processes `UNCLASSIFIED` emails, and
`refreshInbox` preserves the stored importance on upsert, so a manual `IMPORTANT`
survives refreshes and background sync.

## Testing

JVM unit test for the repository's set/undo logic using an in-memory fake `EmailDao`
(the only new non-glue logic). UI wiring follows the existing delete-swipe pattern.
