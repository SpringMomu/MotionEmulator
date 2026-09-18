# MotionEmulator

本 fork 的定位插件修复版位于 [`ws-plugin/`](ws-plugin/README.md)。它包含可独立构建的插件及本地 SDK 源码，修复多监听器分发时速度变成 NaN 的问题；仅构建本目录管理端不会包含此修复。

<img src="art/MotionEmulator.svg" width="200">

[English Version](README.md) | 中文文档

Motion Emulator是个模拟连续定位和传感器变化的应用平台。
它支持多种方式，如Xposed和开发者选项。

## 使用场景

如果你是不幸的中国大学生，或许体验过 _校园跑_ 

尽管教职工总希望我们在夕阳下跑阳光长跑，我想做些有创造性的事情来让生活更轻松一点

## 使用方法

要了解最新、最全面的使用方法和注意事项，请参阅[Steve的博客](https://zhufucdev.com/article/G1lNhmtzI5-RQnVmYEbXm)

## 发布与构建

[管理端 1.2.4（OpenStreetMap）与修复插件 1.2.3](https://github.com/SpringMomu/MotionEmulator/releases/tag/motionemulator-v1.2.4)
已配套提供。管理端的插件下载与自身更新均使用本 fork。管理端以原作者稳定版 1.2.2 为基础，
保留旧路线、配置和协议，并与插件直接共用 [SDK 1.0.0 源码](manager-stub/README.md)。

默认地图为 **OpenStreetMap，无需地图 Key**。未配置 Key 的高德或 Google 设置会自动迁移。
已有高德 GCJ-02 路线仅在显示时转换坐标，不改写原路线；新绘制路线保存为 WGS84。
地点搜索使用 Android 系统地理编码服务，也支持直接输入 `纬度, 经度`。
地图需要联网，不提供卫星图层；地名搜索覆盖范围取决于手机系统服务。
遵守 [OSM 瓦片使用政策](https://operations.osmfoundation.org/policies/tiles/)，显示版权归属，
设置独立 User-Agent 并启用本地缓存，不提供批量下载离线地图。

构建使用 JDK 17 或 21、Android SDK 34 和仓库自带 Gradle 8.9。
在不提交的 `local.properties` 中设置 `sdk.dir`，即可使用默认 OSM 配置构建。
只有启用高德或 Google 时，才需要配置自己的 `AMAP_SDK_KEY`、`amap.web.key`、`GCP_MAPS_KEY`；
Android 地图 Key 必须与自己的包名和签名证书匹配。
可选 `server_uri` 用于额外插件目录，留空时仅使用本 fork 的目录。

```shell
./gradlew :app:testDebugUnitTest :app:assembleRelease
cd ws-plugin
./gradlew :stub:testDebugUnitTest :xposed:testDebugUnitTest :app:assembleRelease
```

管理端通用未签名 APK 位于 `app/build/outputs/apk/release/`。发布使用固定的自己的签名，
不要提交签名密钥或密码。原作者签名的安装需要先妥善迁移数据，无法直接覆盖；
之后本 fork 的同签名更新可以正常覆盖安装。

发布时先把 APK 上传到草稿 Release 并校验，随后将包名、版本号、versionCode、最终下载 URL
及 SHA-256 写入根目录 `release.json`，推送源码后在对应提交发布 Release。
插件清单位于 `ws-plugin/release.json`，维护步骤见 [插件说明](ws-plugin/README.md)。

## 营业执照

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

## 特别鸣谢

- [wandergis/coordtransform](https://github.com/wandergis/coordtransform) 的地图坐标系转化算法
