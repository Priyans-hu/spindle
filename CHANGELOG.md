# Changelog

## 0.2.0 (2026-11-07)

A redesign around one idea: the album art is the record's center label, and a look is only what
surrounds it.

### Added
- Seek in both looks: tap or drag on a 48dp row, the time shows the target while dragging, one
  seek on release. Apps that cannot seek show a fixed marker; live streams hide the bar.
- Needle drop on Analog: the tonearm swings over, touches down with a small bounce and a haptic
  tick, then tracks the song's progress. Pause lifts it back to its rest.
- Records and the Flux ring spin up and coast down instead of starting and stopping dead.
- Per-look 4x1 widget: Analog follows Day/Night, Flux stays dark. The ring rests when paused,
  the controls step aside when nothing is playing, and it re-themes when you change the look.
- Onboarding help for restricted settings on Android 13+ ("Can't turn it on?").
- Debug-only demo mode: a real media session with three tracks, driven by adb.

### Changed
- New design tokens and type (Jost for Analog, Sora for Flux); every text pair meets WCAG AA.
- Analog follows the system Day/Night setting.
- Nothing playing shows an idle record with the arm parked; a session arriving runs the drop.
- The three-dot menu is now a 48dp **Change look** button with a radio-list sheet.
- Looks cross-fade on switch; label art cross-fades on track change.
- Removed decorative labels and effects that did not reflect real playback.

### Accessibility
- Remove animations (animator duration 0) stops the spin and snaps the arm; haptics still fire.
- TalkBack labels for seek, transport and the look button; 48dp touch targets throughout.

## 0.1.0

First release: Analog (Turntable) and Flux skins and a home-screen widget, driven by the
system media session.
