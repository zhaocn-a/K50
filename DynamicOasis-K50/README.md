# DynamicOasis-K50

灵动岛 Android 应用 - 适配红米K50（澎湃OS / MIUI 14）

## 项目简介

DynamicOasis 是一个开源的 Android 灵动岛实现项目，本项目针对红米K50（搭载澎湃OS或MIUI 14系统）进行了深度适配，实现了完整的灵动岛状态提示功能。

**红米K50 主要硬件配置：**
- 屏幕：6.67英寸 三星 2K AMOLED（3200×1440），居中挖孔屏
- 前置摄像头：2000万像素，索尼 IMX596，居中设计
- 电池：5500mAh（典型值）
- 快充：67W 有线快充
- CPU：联发科天玑8100
- 尺寸：163.1×76.15×8.48mm，201g

## 功能特性

### 已实现功能

- **灵动岛常驻显示** - 保持前台悬浮窗显示在屏幕顶部（居中位置）
- **充电状态提示** - 充电中/充满/快速充电三种状态
- **低电量提示** - 20%及以下电量自动提醒
- **网络状态提示** - 无网络/网络切换/已连接状态
- **通知信息提示** - 应用通知实时显示
- **闹钟提示** - 临近闹钟提醒
- **手电筒提示** - 开启/关闭状态检测
- **蓝牙连接提示** - 蓝牙设备连接/断开
- **解锁提示** - 屏幕解锁成功提示

### 技术特性

- 使用 Kotlin 语言开发
- 适配澎湃OS/MIUI的通知系统权限
- 系统级状态监听（BroadcastReceiver/JobScheduler）
- 流畅的动画效果
- 低功耗优化
- 居中灵动岛设计（适配K50居中挖孔屏）

## 适配说明（澎湃OS/MIUI 14特殊处理）

### 1. 权限适配

澎湃OS/MIUI对权限管理较为严格，需要额外处理：

```kotlin
// 悬浮窗权限 - 必须用户手动授权
Settings.ACTION_MANAGE_OVERLAY_PERMISSION

// 通知访问权限 - 需使用NotificationListenerService
Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

// 电池优化 - 建议用户关闭以保持后台运行
Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
```

### 2. 前台服务适配

澎湃OS/MIUI对后台服务限制较多，使用前台服务确保稳定性：

```kotlin
// AndroidManifest.xml 配置
<service
    android:name=".service.DynamicIslandService"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="dynamic_island_display" />
</service>
```

### 3. 开机自启适配

澎湃OS的开机自启需要额外引导：

```kotlin
// 在MainActivity中引导用户开启自启
Intent().apply {
    component = ComponentName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity"
    )
}
```

### 4. 省电策略适配

红米K50的省电策略可能会限制后台运行：

- 建议用户将应用加入"无限制"列表
- 使用 WorkManager 替代部分后台任务
- 监听电量变化，适时调整监听频率

## 权限申请说明

### 必要权限

| 权限 | 用途 | 申请方式 |
|------|------|----------|
| SYSTEM_ALERT_WINDOW | 显示悬浮窗 | 跳转到系统设置 |
| POST_NOTIFICATIONS | 发送通知 | 运行时申请 |
| RECEIVE_BOOT_COMPLETED | 开机自启 | AndroidManifest |
| FOREGROUND_SERVICE | 前台服务 | AndroidManifest |
| BATTERY_STATS | 电池状态 | 系统权限 |

### 可选权限

| 权限 | 用途 | 申请方式 |
|------|------|----------|
| ACCESS_NOTIFICATION_POLICY | 通知拦截 | 系统设置 |
| BIND_ACCESSIBILITY_SERVICE | 无障碍服务 | 设置页面 |
| REQUEST_DELETE_PACKAGES | 卸载确认 | 系统权限 |

## K50与K40适配差异

| 项目 | 红米K40 | 红米K50 | 适配说明 |
|------|---------|---------|----------|
| 屏幕类型 | 左侧挖孔 | **居中挖孔** | K50灵动岛位置居中 |
| 屏幕尺寸 | 6.67英寸 | 6.67英寸 | 相同 |
| 屏幕分辨率 | 2400×1080 | **3200×1440 (2K)** | K50清晰度更高 |
| 前置摄像头 | 2000万 | 2000万 (IMX596) | 相同，像素相同 |
| 电池容量 | 4520mAh | **5500mAh** | K50电池更大 |
| 快充功率 | 33W/67W | **67W** | K50统一67W |
| 出厂系统 | 澎湃OS 1.10 | MIUI 13/澎湃OS | 适配逻辑一致 |
| 灵动岛位置 | 偏左 | **居中** | K50无需横向偏移 |

## 编译说明

### 环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 17+
- Android SDK 34
- Kotlin 1.9+

### 编译步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/DynamicOasis-K50.git

# 2. 使用 Android Studio 打开项目

# 3. 同步 Gradle

# 4. 构建调试版本
./gradlew assembleDebug

# 5. 构建发布版本
./gradlew assembleRelease
```

### APK 输出位置

- 调试版本：`app/build/outputs/apk/debug/app-debug.apk`
- 发布版本：`app/build/outputs/apk/release/app-release.apk`

## 项目结构

```
DynamicOasis-K50/
├── app/
│   ├── src/main/
│   │   ├── java/com/dynamicoasis/app/
│   │   │   ├── DynamicOasisApp.kt          # 应用入口
│   │   │   ├── manager/
│   │   │   │   └── IslandStateManager.kt   # 状态管理
│   │   │   ├── receiver/
│   │   │   │   ├── BatteryReceiver.kt      # 电池监听
│   │   │   │   ├── BluetoothReceiver.kt    # 蓝牙监听
│   │   │   │   └── NetworkReceiver.kt       # 网络监听
│   │   │   ├── service/
│   │   │   │   ├── DynamicIslandService.kt # 核心服务
│   │   │   │   └── NotificationInterceptorService.kt
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── SettingsActivity.kt
│   │   │   │   └── OnboardingActivity.kt
│   │   │   └── utils/
│   │   │       ├── PreferencesManager.kt
│   │   │       └── TorchManager.kt
│   │   └── res/
│   │       ├── drawable/
│   │       ├── layout/
│   │       └── values/
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## 许可证

本项目仅供学习交流使用，请勿用于商业用途。

## 联系方式

如有问题或建议，请提交 Issue 或 Pull Request。
