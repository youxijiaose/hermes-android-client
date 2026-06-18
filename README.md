# Hermes Android Client v6

A native Android client for Hermes Agent API Server with full-featured chat, voice input, file attachments, memory management, skills browser, in-app browser, and OTA updates.

## 🚀 Features

### Core Chat
- ✅ **Real-time streaming** - WebSocket-based real-time message streaming with typing effect
- ✅ **Markdown rendering** - Full Markdown support with code highlighting
- ✅ **Relative timestamps** - "2 minutes ago", "1 hour ago", etc.
- ✅ **Thinking blocks** - Visual display of AI thinking process

### Interaction
- ✅ **Voice input** - Android SpeechRecognizer with Chinese language support
- ✅ **File/Image attachments** - Gallery, camera, and file picker support
- ✅ **Command Approval** - Dangerous commands require manual confirmation

### Management
- ✅ **Session management** - View, create, switch between sessions
- ✅ **Model switching** - Select different AI models
- ✅ **Cron job management** - View, create, enable/disable, delete scheduled tasks
- ✅ **Memory management** - User and Agent memory CRUD operations
- ✅ **Skills browser** - Browse, install, uninstall, and pin skills

### Experience
- ✅ **Multiple themes** - 6 themes: System/Light/Dark/Gold/Midnight/Cyberpunk
- ✅ **Notifications** - Message notifications with notification channel
- ✅ **In-app browser** - WebView with history, bookmarks, private mode
- ✅ **OTA updates** - Automatic update checking and installation

### Advanced
- ✅ **Tool call visualization** - Display tool names and outputs
- ✅ **Connection status** - Real-time connection indicator
- ✅ **Local settings persistence** - Server URL, API key, theme preferences
- ✅ **Material Design 3** - Modern UI with dark/light mode support

## 📋 Requirements

- Android 7.0+ (API 24+)
- Hermes Agent API Server running
- Network access to API Server

## 🛠️ Build

### Prerequisites
- Android Studio (latest version)
- JDK 11+
- Gradle 8.2+

### Build Steps
1. Copy project to computer
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build APK: `Build > Build APK(s)`

### Build Variants
- **Debug** - Debug build with logging
- **Release** - Optimized release build

## 📦 Installation

1. Transfer APK to device
2. Install via MT Manager or file manager
3. Grant required permissions
4. Configure server URL and API key
5. Start chatting!

## 🔧 Configuration

### Server Settings
- **Server URL**: `http://<your-termux-ip>:8080`
- **API Key**: Set in Hermes API Server config

### Permissions Required
- `INTERNET` - Network access
- `RECORD_AUDIO` - Voice input
- `CAMERA` - Image capture
- `READ_MEDIA_IMAGES` - Image selection
- `POST_NOTIFICATIONS` - Notifications
- `REQUEST_INSTALL_PACKAGES` - OTA updates

## 📁 Project Structure

```
app/src/main/
├── java/com/hermes/client/
│   ├── api/
│   │   └── HermesApi.kt          # API client (WebSocket + REST)
│   ├── model/
│   │   ├── Message.kt            # Chat message models
│   │   ├── Memory.kt             # Memory entry models
│   │   ├── Skill.kt              # Skill models
│   │   ├── CronJob.kt            # Cron job models
│   │   └── Update.kt             # Update info models
│   ├── adapter/
│   │   ├── ChatAdapter.kt        # Chat message adapter
│   │   ├── MemoryAdapter.kt      # Memory entry adapter
│   │   ├── SkillsAdapter.kt      # Skill adapter
│   │   ├── SessionsAdapter.kt    # Session adapter
│   │   └── CronAdapter.kt        # Cron job adapter
│   ├── viewmodel/
│   │   ├── MainViewModel.kt      # Main chat ViewModel
│   │   ├── MemoryViewModel.kt    # Memory ViewModel
│   │   ├── SkillsViewModel.kt    # Skills ViewModel
│   │   ├── SessionsViewModel.kt  # Sessions ViewModel
│   │   └── CronViewModel.kt      # Cron ViewModel
│   ├── ui/
│   │   ├── MainActivity.kt       # Main chat interface
│   │   ├── SettingsActivity.kt   # Settings interface
│   │   ├── ApprovalActivity.kt   # Command approval
│   │   ├── SessionsActivity.kt   # Session management
│   │   ├── MemoryActivity.kt     # Memory management
│   │   ├── SkillsActivity.kt     # Skills browser
│   │   ├── BrowserActivity.kt    # In-app browser
│   │   └── CronActivity.kt       # Cron job management
│   └── util/
│       ├── VoiceInputHelper.kt   # Voice input helper
│       ├── FileAttachmentHelper.kt # File attachment helper
│       ├── NotificationHelper.kt # Notification helper
│       ├── ThemeManager.kt       # Theme management
│       ├── MarkdownRenderer.kt   # Markdown renderer
│       ├── TimeUtils.kt          # Time formatting
│       └── UpdateManager.kt      # OTA update manager
├── res/
│   ├── layout/                   # UI layouts
│   ├── values/                   # Strings, colors, themes
│   ├── menu/                     # Menu resources
│   ├── drawable/                 # Drawables
│   └── xml/                      # XML resources (file_paths, etc.)
└── AndroidManifest.xml
```

## 📊 Statistics

| Metric | Count |
|--------|-------|
| Kotlin Files | 30 |
| XML Files | 40 |
| Total Resources | 70+ |
| APK Size | ~15MB (with dependencies) |

## 🔄 API Endpoints

| Endpoint | Method | Function |
|----------|--------|----------|
| `/chat` | POST | Send message |
| `/chat/stream` | WebSocket | Stream chat |
| `/sessions` | GET | Get sessions |
| `/models` | GET | Get available models |
| `/approvals/{id}` | POST | Submit approval |
| `/memory` | GET/POST/PATCH/DELETE | Memory CRUD |
| `/memory/stats` | GET | Memory statistics |
| `/skills` | GET | Get skills list |
| `/skills/install` | POST | Install skill |
| `/skills/{id}/uninstall` | POST | Uninstall skill |
| `/skills/{id}/pin` | POST | Toggle pin |
| `/cron` | GET/POST | Get/create cron jobs |
| `/cron/{id}` | DELETE | Delete cron job |
| `/cron/{id}/toggle` | POST | Toggle enabled |
| `/cron/{id}/run` | POST | Run cron job |
| `/update` | GET | Get update info |
| `/health` | GET | Health check |

## 🎨 Themes

| Theme | Description |
|-------|-------------|
| System | Follows system theme |
| Light | Light theme with primary color |
| Dark | Dark theme with primary color |
| Hermes Gold | Gold accent with dark background |
| Midnight | Deep blue with purple accents |
| Cyberpunk | Neon pink/cyan with dark background |

## 📱 Screenshots

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Chat      │  │  Memory     │  │   Skills    │
│  ┌───────┐  │  │  ┌───────┐  │  │  ┌───────┐  │
│  │ Hello │  │  │  │ Entry │  │  │  │ Skill │  │
│  │       │  │  │  │       │  │  │  │       │  │
│  │ AI    │  │  │  │       │  │  │  │       │  │
│  └───────┘  │  │  └───────┘  │  │  └───────┘  │
│  [🎤][📎]   │  │  [+]        │  │  [Install]  │
└─────────────┘  └─────────────┘  └─────────────┘

┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Sessions   │  │   Cron      │  │  Browser    │
│  ┌───────┐  │  │  ┌───────┐  │  │  ┌───────┐  │
│  │ Sess 1│  │  │  │ Job 1 │  │  │  │ URL   │  │
│  │ Sess 2│  │  │  │ Job 2 │  │  │  │       │  │
│  │       │  │  │  │       │  │  │  │ WebView│ │
│  └───────┘  │  │  └───────┘  │  │  └───────┘  │
│  [New]      │  │  [Add]      │  │  [←][→][🔄]│
└─────────────┘  └─────────────┘  └─────────────┘
```

## 🔐 Security

- API key stored in SharedPreferences (encrypted in future versions)
- HTTPS support for API communication
- FileProvider for secure APK installation
- No sensitive data stored in logs

## 📝 Changelog

### v6.0 (Latest)
- ✅ Added in-app browser (WebView)
- ✅ Added OTA update support
- ✅ Added Skills browser
- ✅ Added Memory management
- ✅ Added Cron job management
- ✅ Added voice input
- ✅ Added file attachments
- ✅ Added 6 themes
- ✅ Added notification system
- ✅ Added Markdown rendering
- ✅ Added thinking blocks display

### v5.0
- Added Skills browser
- Added Memory management
- Added Cron job management

### v4.0
- Added Memory management
- Added 6 themes
- Added notification system

### v3.0
- Added Markdown rendering
- Added relative timestamps
- Added thinking blocks

### v2.0
- Added voice input
- Added file attachments
- Added session management

### v1.0
- Initial release
- Basic chat functionality
- WebSocket streaming

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

MIT License - see LICENSE file for details.

## 📧 Support

For issues and questions:
- Open an issue on GitHub
- Check the Hermes Agent documentation: https://hermes-agent.nousresearch.com/docs

---

**Hermes Android Client v6** - A powerful, feature-rich native Android client for Hermes Agent.
