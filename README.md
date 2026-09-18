# MotionEmulator

This fork includes a standalone [WebSocket plugin and local SDK](ws-plugin/README.md)
fixing NaN speeds during multi-listener location dispatch. Build `ws-plugin/` to use
the fix; rebuilding the manager application alone does not update the loaded plugin.
<img src="art/MotionEmulator.svg" width="200">

English Version | [中文文档](README_zh.md)

Motion Emulator is an application platform that allows 
you to mock location and sensor data using different methods,
including Xposed and debugging options.

## Scenarios

Trick your fitness app or your favourite game. Make you king of the world.

## Usage

To learn about the latest software and its tricks, refer to
[Steve's Blog](https://zhufucdev.com/article/RTyhZArsyD2JKPbdHEviU).

## Releases and build

[Manager 1.2.4 (OpenStreetMap) and fixed plugin 1.2.3](https://github.com/SpringMomu/MotionEmulator/releases/tag/motionemulator-v1.2.4)
are published together. The manager's download catalog and self-update use this fork.
The manager uses the upstream **stable 1.2.2** UI/data baseline with the fixes applied;
its [SDK models](manager-stub/README.md) are shared with the plugin.

The default map is **OpenStreetMap**, with no API key required. Existing unconfigured
AMap/Google preferences migrate to OSM. Saved GCJ-02 traces are converted for display
without rewriting the saved route. New OSM traces use WGS84. Search uses the Android
system geocoder where available, and accepts `latitude, longitude` without a geocoder.
OSM needs network access, has no satellite layer, and place-search coverage depends
on the device geocoder. Visible attribution, an app-specific User-Agent and local
HTTP-aware caching follow the [OSM tile usage policy](https://operations.osmfoundation.org/policies/tiles/).
No bulk/offline tile download is offered.

Use JDK 17 or 21, Android SDK 34 and the included Gradle 8.9 wrapper. Set `sdk.dir` in
an ignored `local.properties`. Placeholder map keys in `local.defaults.properties`
allow a working OSM build. Only configure your own `AMAP_SDK_KEY`, `amap.web.key` and/or
`GCP_MAPS_KEY` if you want those optional providers; Android keys must match your
package and signing certificate. The optional `server_uri` supplies additional
plugin catalog entries; leave it blank to use only this fork's catalog.

```shell
./gradlew :app:testDebugUnitTest :app:assembleRelease
cd ws-plugin
./gradlew :stub:testDebugUnitTest :xposed:testDebugUnitTest :app:assembleRelease
```

The manager's universal unsigned APK is in `app/build/outputs/apk/release/`.
Sign release APKs with your own persistent identity; never commit keys or passwords.
Upstream-signed installations require a data-preserving migration because Android
cannot overwrite them with this fork's certificate. Subsequent fork updates can be
installed normally with the same fork certificate.

Publishing: upload and verify the APK in a draft release first, update `release.json`
with its package, version code, version name, final URL and SHA-256, push the matching
source commit, then publish the release at that commit. Follow the analogous steps
in [ws-plugin/README.md](ws-plugin/README.md) when changing the plugin.

## License

```
Copyright 2022-2023 zhufucdev

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Special Thanks

- [wandergis/coordtransform](https://github.com/wandergis/coordtransform) for its map coordinate fixing algorithm
