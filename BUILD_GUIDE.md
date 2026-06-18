# Hermes Android Client - 构建指南

## 方法一：Android Studio（推荐）

### 1. 准备环境
- Android Studio Hedgehog 或更高版本
- JDK 17+ 或 JDK 21
- Android SDK (API 24-34)

### 2. 导入项目
```
1. 打开 Android Studio
2. File → Open → 选择 hermes-android-client 文件夹
3. 等待 Gradle 同步完成
4. 点击 "Sync Now"
```

### 3. 构建 APK
```
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. 等待构建完成
3. APK 位置：app/build/outputs/apk/debug/app-debug.apk
```

### 4. 安装到设备
```
1. 连接 Android 设备
2. 运行：Run → Run 'app'
3. 或手动安装 APK
```

---

## 方法二：命令行构建（需要 Android SDK）

### 1. 设置环境变量
```bash
export ANDROID_HOME=/path/to/android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### 2. 接受许可证
```bash
sdkmanager --licenses
```

### 3. 下载依赖
```bash
./gradlew dependencies
```

### 4. 构建 Debug APK
```bash
./gradlew assembleDebug
```

### 5. 构建 Release APK
```bash
./gradlew assembleRelease
```

### 6. 签名 Release APK
```bash
# 生成签名密钥
keytool -genkey -v -keystore hermes-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias hermes

# 使用 jarsigner 签名
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore hermes-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  hermes

# 或使用 apksigner（推荐）
apksigner sign --ks hermes-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 方法三：Termux 构建（不推荐）

Termux 上缺少 Android SDK，需要手动配置：

### 1. 安装 Android SDK 命令行工具
```bash
# 下载 SDK 命令行工具
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip -d ~/android-sdk
```

### 2. 设置环境变量
```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin
```

### 3. 安装必要组件
```bash
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 4. 构建
```bash
./gradlew assembleDebug
```

---

## 构建产物

| 类型 | 路径 | 大小 |
|------|------|------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | ~15MB |
| Release APK | `app/build/outputs/apk/release/app-release.apk` | ~12MB |

---

## 常见问题

### Q: SDK location not found
**A:** 设置 `ANDROID_HOME` 环境变量或在 `local.properties` 中指定 SDK 路径：
```
sdk.dir=/path/to/android/sdk
```

### Q: Gradle sync failed
**A:** 检查网络连接，或配置 Gradle 镜像：
```gradle
// 在 build.gradle 中添加
buildscript {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/google' }
    }
}
```

### Q: 签名失败
**A:** 确保签名密钥存在且密码正确

---

## 快速构建（Android Studio）

```
1. 打开 Android Studio
2. File → Open → 选择项目文件夹
3. 等待 Gradle 同步
4. Build → Build APK
5. 找到 APK：app/build/outputs/apk/debug/
```

---

**构建时间：** ~3-5 分钟（首次）/ ~1-2 分钟（后续）
**APK 大小：** ~12-15MB
