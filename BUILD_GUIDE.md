# 构建指南

## 方式一：Android Studio（推荐）

1. **复制项目到电脑**
   ```bash
   # 在 Termux 上压缩项目
   cd ~
   tar -czf hermes-android-client.tar.gz hermes-android-client/
   
   # 使用 MT 管理器或 adb 传输到电脑
   ```

2. **在 Android Studio 中打开**
   - File > Open > 选择项目文件夹
   - 等待 Gradle 同步完成
   - Build > Build Bundle(s) / APK(s) > Build APK(s)

3. **安装到手机**
   - APK 位于 `app/build/outputs/apk/debug/app-debug.apk`
   - 传输到手机，使用 MT 管理器安装

## 方式二：Termux 命令行构建（高级）

需要安装完整的 Android SDK：

```bash
# 1. 安装 Android SDK
pkg install android-sdk

# 2. 接受许可证
sdkmanager --licenses

# 3. 安装必要的组件
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. 设置环境变量
export ANDROID_HOME=/data/data/com.termux/files/usr/share/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0

# 5. 下载 Gradle wrapper
cd ~/hermes-android-client
./gradlew assembleDebug

# 注意：Termux 构建可能因内存限制失败
```

## 方式三：使用在线构建服务

1. 将项目上传到 GitHub
2. 使用 GitHub Actions 自动构建
3. 下载构建好的 APK

## 项目文件清单

```
hermes-android-client/
├── app/                          # 主应用模块
│   ├── build.gradle.kts          # 模块级构建配置
│   ├── proguard-rules.pro        # ProGuard 规则
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/hermes/client/
│       │   ├── api/HermesApi.kt           # API 客户端
│       │   ├── model/Message.kt           # 数据模型
│       │   ├── adapter/                   # RecyclerView 适配器
│       │   ├── viewmodel/                 # ViewModel
│       │   └── ui/                        # Activities
│       └── res/                           # 资源文件
├── build.gradle.kts              # 项目级构建配置
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── gradle/wrapper/               # Gradle Wrapper
├── README.md                     # 使用说明
└── BUILD_GUIDE.md                # 构建指南
```

## 常见问题

### Q: Gradle 同步失败？
A: 检查网络连接，或配置国内镜像源：
```gradle
// 在 settings.gradle.kts 中添加
dependencyResolutionManagement {
    repositories {
        maven{ url = uri("https://maven.aliyun.com/repository/public") }
        maven{ url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}
```

### Q: 构建内存不足？
A: 在 `gradle.properties` 中增加堆内存：
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### Q: 找不到 SDK？
A: 确保 ANDROID_HOME 环境变量正确设置。
