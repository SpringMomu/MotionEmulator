# Stable manager updater

Vendored `update/src/main` from upstream MotionEmulator stable 1.2.2, commit
[`641a80e48fbabf7faabaefde83cf36e1b7472ae0`](https://github.com/zhufucdev/MotionEmulator/tree/641a80e48fbabf7faabaefde83cf36e1b7472ae0/update).
Licensed under [Apache-2.0](../LICENSE).

Local changes: current SDK package imports, a release resolver for the fork's
manager manifest, Compose tooltip API compatibility, and returning after a failed
download to avoid resuming the same continuation twice.

The manager checks `release.json`; its plugin catalog checks `ws-plugin/release.json`.
Both manifests point to APKs in SpringMomu/MotionEmulator GitHub Releases and compare
Android version codes. A failed fork query never falls back to the old plugin.
