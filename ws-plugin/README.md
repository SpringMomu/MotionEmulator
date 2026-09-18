# MotionEmulator 定位插件修复版

这里是可独立构建的 `com.zhufucdev.ws_plugin`，版本为 **1.2.4 / versionCode 5**。
它直接编译 `sdk/` 内的源码，不下载 `com.zhufucdev.me:*` 的旧版二进制。
根目录的 `app/` 仍是管理端；仅重新构建管理端不会把本修复加载进目标应用。

## 修复内容

### 高德坐标偏移（1.2.4）

旧插件将 WGS84 数值装入高德定位对象，并全局改写 Android Location 和
AMapLocation 的经纬度 getter。高德国内地图按 GCJ-02 绘图，因而同一地点会偏离道路；
已交付的历史定位对象也可能在下一帧到来后读出新坐标。

- Android GPS 回调继续输出 WGS84；高德默认国内回调输出 GCJ-02。
- 高德地图绘制的 GCJ-02 路线直接保留原坐标；WGS84 路线只转换一次。
- 尊重每个 AMapLocationClient 的 `setOffset(false)`；海外输出 WGS84。
- `coordType`、`isOffset` 与输出数值保持一致；精度和卫星数写入回调对象。
- 移除全局经纬度、精度、卫星数 getter 改写；回调对象及其副本保留自己的坐标快照。
- 保留插件原有监听接管方式。停止轨迹播放不等于卸载 Hook；本次不承诺停止播放后自动恢复真实定位。

依据：[高德定位对象](https://a.amap.com/lbs/static/unzip/Android_Location_Doc/com/amap/api/location/AMapLocation.html)、
[坐标偏转选项](https://a.amap.com/lbs/static/unzip/Android_Location_Doc/com/amap/api/location/AMapLocationClientOption.html)。

### 无效速度（1.2.3）

旧版在遍历监听器时反复估速并更新上一点，使第二个及后续监听器可能获得
同一点、同一毫秒的 `0 / 0 = NaN`。

- 每个输入点只读取一次单调时钟、估速一次、更新一次基线。所有监听器接收同一份不可变的位置和速度。
- 使用 `elapsedRealtimeNanos()` 计算间隔；重复或倒退的时间戳不覆盖基线。
- 首点、重启、无效距离及坐标系切换时，速度标记为不可用；有正常时间差的静止点速度为零。
- 无效坐标不分发；非有限值、负速度以及转换成 Float 后的溢出不会写入 Android Location。
- 速度不可用时保留 `Location.hasSpeed() == false`，不设置虚构的正常速度。
- GPS 和 AMap 回调使用各自捕获的位置/速度快照；并发分发与监听器增删不会改变当前帧的数据。
- 修正 GPS `removeUpdates` 获取监听器对象的方式，并在轨迹开始、结束或取消时重置估速基线。

这只修复定位插件。它不会修改目标应用或已经保存的历史运动数据。

## 构建与测试

需要 JDK 17 或 21、Android SDK Platform 34。配置 `ANDROID_HOME`，或在本目录
的 `local.properties` 中设置 `sdk.dir`。插件构建不需要管理端的地图 API Key。

```sh
cd ws-plugin
./gradlew :stub:testDebugUnitTest :xposed:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

Windows 使用 `gradlew.bat`，并建议将仓库放在仅包含 ASCII 字符的路径，
例如 `D:/src/MotionEmulator`。旧版 Android 构建工具在含中文路径下可能无法加载
测试类，仅设置 `android.overridePathCheck=true` 不足以解决该问题。

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。
- Release APK：`app/build/outputs/apk/release/app-release-unsigned.apk`，需要使用自己的证书签名。
- 测试报告：`sdk/stub/build/reports/tests/testDebugUnitTest/` 和 `sdk/xposed/build/reports/tests/testDebugUnitTest/`。

回归测试直接执行生产用的 `LocationDispatcher`，覆盖三个监听器连续 17 帧、
回调耗时、亚毫秒采样、重复/倒退时间、静止、无效数值、Float 溢出、无效坐标、
坐标系切换、停止后重启、无监听器采样、监听器增删、重入及并发回调。

连接测试设备后，可依次运行 `./gradlew :stub:connectedDebugAndroidTest` 和
`./gradlew :xposed:connectedDebugAndroidTest`，验证真实 Android Location 和高德 SDK
的坐标、速度、坐标元数据及复制/Parcel 行为。高德 SDK 仅作为设备测试依赖，
不打包进插件 APK。任务运行独立测试包，不替换设备上已有的定位插件。
坐标单元测试还覆盖 GCJ-02 原点保留、WGS84 转换、海外及每客户端偏转选项。

设备上使用的是插件 APK，包名仍为 `com.zhufucdev.ws_plugin`。
覆盖安装要求签名与原安装版本一致；新证书不能直接覆盖上游签名的插件。
更新插件后，需要让目标应用进程重新启动并确认 LSPosed 加载的是新版本。
本仓库不会提供或提交私钥，也不自动安装或卸载设备上的插件。

## 源码来源与协议基线

- 插件来源：[Xposed-Modules-Repo/com.zhufucdev.ws_plugin](https://github.com/Xposed-Modules-Repo/com.zhufucdev.ws_plugin/tree/bef74436de6af4e86f31bedbbf1203610c6a3e03)，提交 `bef74436de6af4e86f31bedbbf1203610c6a3e03`。
- SDK 来源：[zhufucdev/MotionEmulatorSdk](https://github.com/zhufucdev/MotionEmulatorSdk/tree/0dc23f6619b77c715074c255bdbc4ec8610320bf)，提交 `0dc23f6619b77c715074c255bdbc4ec8610320bf`，版本 1.0.0。
- 使用插件原本声明依赖的 SDK 1.0.0 基线，保持原 1.2.2 插件的通信数据模型。
  不引入 SDK 1.1.x 对轨迹/传感器数据模型的变更；也未升级根目录管理端。
- SDK 保留 [上游 Apache-2.0 许可证](sdk/LICENSE)；插件源码来自从本项目拆出的上游插件，适用项目根目录的 [许可证](../LICENSE)。
- 仅纳入构建插件必需的源码、资源和测试；移除了上游 SDK 的 Maven 发布配置，改为本地 Gradle project 依赖。
- Crunch 保持 1.1.2，Maven 坐标改为仓库迁移后的 `com.github.boxbeam:Crunch:1.1.2`；旧 `Redempt` 坐标的 JitPack JAR 已不可获取。
- 显式安排 KSP 在资源打包前生成 Xposed 入口描述文件，保证首次构建的 APK 也包含 `assets/xposed_init`。

本 fork 的修改集中在定位分发、速度数值检查、轨迹生命周期、回归测试和独立构建接线。

## 管理端的插件下载来源

本 fork 的管理端将 WebSocket 插件的下载和更新检查统一接到
[`release.json`](release.json)，APK 发布在本仓库的
[WebSocket 插件 Release](https://github.com/SpringMomu/MotionEmulator/releases/tag/ws-plugin-v1.2.4)。
版本比较使用 Android `versionCode`；已经安装相同或更高版本时不会提示降级。
原作者目录中的同包名插件会被替换，其他插件仍使用配置的原目录。
本仓库下载源不可用时不会退回旧 WebSocket 插件。

后续发布需要使用同一签名证书签名，先发布可下载的 APK，再更新本文件旁的
`release.json`（包名、版本号、下载地址和校验值）。不要提交签名私钥。
`sha256` 用于发布核对；应用内仍复用原有下载器及 Android 安装签名校验。

已经安装的上游管理端仍然使用它原来的下载入口，必须更新管理端才能采用上述逻辑。
仅安装新插件不会修改管理端的网络请求。
根目录管理端已基于上游稳定版回移修复，`manager-stub/` 与插件共享本地 SDK 1.0.0
源码。配置了有效 Android Key 时默认使用高德，并支持普通/卫星图层；未配置 Key
时使用 OpenStreetMap。地图 Key 仅写入忽略提交的 `local.properties`。
本次坐标修复位于插件的 xposed 模块，管理端 1.2.5 已能通过本目录的发布清单下载更新。
