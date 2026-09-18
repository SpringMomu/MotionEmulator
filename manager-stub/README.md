# Shared manager/plugin SDK

This Gradle module compiles `../ws-plugin/sdk/stub/src/main` directly. The manager
and WebSocket plugin use the same SDK **1.0.0** models, serializers and coordinate
conversion code, including the finite-speed fix. There is no second SDK copy to drift.

The release manager is based on upstream stable 1.2.2, commit
[`641a80e48fbabf7faabaefde83cf36e1b7472ae0`](https://github.com/zhufucdev/MotionEmulator/tree/641a80e48fbabf7faabaefde83cf36e1b7472ae0),
matching the released plugin's protocol and existing trace/preferences format.
The previous development-only SDK 1.1.3 copy has been removed.

See [plugin provenance](../ws-plugin/README.md) and [LICENSE](LICENSE).
