# Termux APK 构建说明

## 当前状态

Termux 上已安装以下工具：
- ✓ aapt2 (Android 资源编译)
- ✓ javac (Java 编译)
- ✓ d8 (Dex 转换器)
- ✓ apksigner (APK 签名)
- ✓ kotlinc (Kotlin 编译)
- ✓ android.jar (Android API 库)
- ✓ kotlin-stdlib (Kotlin 标准库)
- ✓ Gradle 9.5.1 (但版本不兼容 AGP 8.2)

## 问题

1. **Gradle 版本不兼容**: 项目使用 AGP 8.2.0，需要 Gradle 8.2，但 Termux 安装的是 Gradle 9.5.1
2. **Kotlin 编译问题**: kotlinc 在 Termux 上有库加载问题

## 解决方案

### 方法 1: Android Studio（推荐）
在电脑上使用 Android Studio 构建：
1. 从 GitHub 下载项目
2. 用 Android Studio 打开
3. Build → Build APK

### 方法 2: 降级 Gradle
```bash
# 下载 Gradle 8.2
wget https://services.gradle.org/distributions/gradle-8.2-bin.zip
unzip gradle-8.2-bin.zip -d /data/data/com.termux/files/usr/opt/
# 修改 gradle-wrapper.properties 使用本地 Gradle
```

### 方法 3: 手动编译（高级）
需要处理：
1. ViewBinding 生成代码
2. Kotlin 依赖解析
3. DEX 文件合并
4. APK 打包签名

## 建议

由于 Termux 环境限制，**强烈建议在 Android Studio 中构建**。

项目已上传到 GitHub：
https://github.com/youxijiaose/hermes-android-client

在电脑上克隆并构建即可。
