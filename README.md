# FastPhoto (S24 Ultra Edition) 🚀

A blazing-fast photo organizer & cleanup gallery built for high-performance Android devices like the Samsung Galaxy S24 Ultra.

## 🌟 Key UX Features
- **Swipe Up = Delete:** Toss the photo upward and it lands in the app's local trash instantly. No "Are you sure?" prompts per swipe.
- **Swipe Down = Move:** Open the folder picker (or hit a recent-folder chip at the bottom) to move the photo into the album of your choice.
- **Swipe Left / Right = Navigate:** Pure navigation, no MediaStore side-effects.
- **Pending Commit Button (top bar):** A yellow hourglass with a count appears whenever you have pending changes. Tap it to push a single Android approval dialog for all queued deletes at once. When everything is synced, it turns into a green check.
- **Undo Stack:** Last 10 actions can be reverted from the bottom-left "Undo" button.
- **DCIM-aware Move:** Moving a `DCIM/Camera` photo to another album writes into the album's *real* parent (`DCIM/...` or `Pictures/...`) — no more accidental duplicate `Pictures/Camera` folders.

## 🛠 Tech Stack
- **Language:** 100% Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Coroutines + Hilt
- **Storage:** MediaStore API, Room DB (for the undo stack & local trash)

## 📦 Download the Latest APK
Every push to `main` is built by GitHub Actions and published as the *Latest* release. The download URL is stable — bookmark it:

**👉 https://github.com/onurataman/FastPhoto/releases/latest/download/FastPhoto.apk**

Older builds (and the raw artifact ZIP) are also available under the [Actions](https://github.com/onurataman/FastPhoto/actions) tab.

## 🔒 Permissions
| Permission | Why |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | Read your photo library |
| `ACCESS_MEDIA_LOCATION` | Preserve EXIF location when copying |

No internet permission, no telemetry, no cloud. The app runs fully offline.

## 🗂 Versioning
- **1.1.0** — DCIM/Pictures path fix, Pending Commit button, full English UI, GitHub Releases auto-publish.
- **1.0.0** — Initial release: Tinder-style swipe, custom in-app trash, bulk delete.
