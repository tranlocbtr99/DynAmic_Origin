# Phân tích app tham chiếu: 灵动屿 (Dynamic Isle) v1.5.7

Package: `com.telfdby.yrxyfm` · versionCode 57 · minSdk 26 · targetSdk 31

## 1. Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────┐
│              Dynamic Island App (灵动屿)             │
├─────────────────────────────────────────────────────┤
│  com.stub.StubApp (Application)                     │
│    └── SmartLandServiceImpl (core Island module)    │
│         ├── WidgetModelServiceImpl                 │
│         ├── WallpaperModelServiceImpl              │
│         └── LockWidgetServiceImpl                  │
├─────────────────────────────────────────────────────┤
│  UI Layer                                           │
│    ├── SplashActivity → MainActivity               │
│    ├── NotificationListenerServiceImpl             │
│    ├── WidgetServiceImpl                          │
│    └── 150+ Activities (widget themes, skins, ...) │
├─────────────────────────────────────────────────────┤
│  Island Animation (Lottie JSON + PNG)               │
│    ├── ldl_charging/    → battery charging         │
│    ├── ldl_guide/       → onboarding tutorial     │
│    ├── ldl_headset/     → bluetooth earbuds       │
│    ├── ldl_play_music/  → 6-bar equalizer         │
│    └── ldl_unlock/      → unlock pattern          │
├─────────────────────────────────────────────────────┤
│  Widget System                                      │
│    ├── 6 Widget Providers (2x2, 4x4, 4x6, 1x2,    │
│    │   2x1, 1x1) + WidgetProviderChange            │
│    └── 18+ Broadcast Receivers (media, weather,    │
│        charging, unlock, shortcut, badge, ...)     │
├─────────────────────────────────────────────────────┤
│  Native Libraries                                   │
│    ├── libyrxyfm.so → AES-256-ECB (asset encrypt)  │
│    ├── libpag.so    → PAG rendering engine         │
│    ├── libttmplayer_lite.so → video player         │
│    └── libmmkv.so   → key-value storage            │
├─────────────────────────────────────────────────────┤
│  Third-party SDKs                                   │
│    ├── Pangle (TikTok ads), GDT (Tencent ads)     │
│    ├── Baidu Ads, Umeng analytics                 │
│    ├── Tencent Open SDK (share/login)              │
│    └── JiaGu anti-tamper (libjiagu.so)            │
└─────────────────────────────────────────────────────┘
```

## 2. Island Animation — chi tiết từng trạng thái

### 2.1 Sạc pin (`ldl_charging/data.json`)
- Kích thước: 80×80 px, 60 fps, 120 frames (2s loop)
- Assets: `img_0.png` (thí dụ 36×25), `img_1.png` (ví dụ 386×26)
- Animation: icon pin scale 0→100% (frames 0–80), bar pin static

### 2.2 Hướng dẫn (`ldl_guide/data.json`)
- Kích thước: 840×329 px, 60 fps, 120 frames
- Assets: text image (540×270), dot (40×40)
- Animation: dot di chuyển left→right→left, bar progress scale 47→30→73%

### 2.3 Tai nghe Bluetooth (`ldl_headset/data.json`)
- Kích thước: 80×80 px, 60 fps, 120 frames
- Assets: earbud left (20×34), earbud right (19×34)
- Animation: 2 earbud oscillate y-position antiphase (48→53→48→43→48)

### 2.4 Phát nhạc (`ldl_play_music/data.json`)
- Kích thước: 80×80 px, 60 fps, 120 frames
- Assets: 6 vertical bars (4px wide, heights 13–45px)
- Animation: 6 bars scale Y independently (equalizer effect)

### 2.5 Mở khóa (`ldl_unlock/data.json`)
- Kích thước: 80×80 px, 60 fps, 120 frames
- Assets: lock icon (25×23), key path (350×37)
- Animation: 2 path shapes crossfade, lock icon static

### 2.6 Home Button effect (`home_btn/data.json`)
- Kích thước: 810×162 px, 60 fps, 120 frames
- Assets: light streak (171×304), button body (750×150)
- Animation: light sweep left→right + button pulse 100→108%

## 3. Widget System

### Widget Providers
| Provider | Size | Purpose |
|----------|------|---------|
| WidgetProvider2X2 | 2×2 | Small widgets (clock, weather, shortcut) |
| WidgetProvider4X4 | 4×4 | Large widgets (theme previews, galleries) |
| WidgetProvider4X6 | 4×6 | XL widgets (wallpaper previews) |
| WidgetProvider1X2 | 1×2 | Mini widgets (single action) |
| WidgetProvider2X1 | 2×1 | Narrow widgets (badge, counter) |
| AppShortcutWidgetProvider1X1 | 1×1 | App shortcuts |
| WidgetProviderChange | dynamic | Theme/wallpaper switcher |

### Broadcast Receivers (18+)
| Receiver | Trigger |
|----------|---------|
| MediaSoundReceiver | Media button events |
| SkinSwitchReceiver | Theme skin change |
| ChristmasTreesGrowReceiver | Seasonal animation |
| CalculatorReceiver | Calculator widget |
| WindSpeedReceiver | Weather data update |
| VoiceReceiver | Voice command |
| DynamicPlayReceiver | Media playback state |
| PlanReceiver | Daily plan reminder |
| IconAddCallbackReceiver | Shortcut added |
| PlayWidgetReceiver | Play/pause control |
| CountWidgetReceiver | Counter widget |
| CountDownReceiver | Countdown timer |
| RemoteControlReceiver | Remote control |
| AirConditionerReceiver | AC simulation |
| BadgeReceiver | App badge update |
| AudioKeyReceiver | Hardware audio key |

## 4. So sánh với LTMori

| Feature | 灵动屿 (Dynamic Isle) | LTMori |
|---------|----------------------|--------|
| Island rendering | Lottie JSON (in-app) | SuperX (OriginOS native) |
| Island states | 5 (charging, music, headset, unlock, guide) | Notification-driven |
| Widgets | 7 providers, 18+ receivers | None |
| Wallpapers | Yes | No |
| Themes/skins | Yes (extensive) | No |
| Ads | Pangle, GDT, Baidu | No |
| Update system | Self-hosted API + GitHub | GitHub Releases only |
| Anti-tamper | JiaGu | None |
| Storage | MMKV + SQLite | SharedPreferences |
| Package | com.telfdby.yrxyfm | com.telfdby.yrxyfm |
| Version | 1.5.7 (code 57) | 3.0.0-beta (code 60) |

## 5. Gợi ý triển cập nhật cho LTMori

### Ưu tiên cao
1. **Beta channel** — ✅ Đã triển cập nhật (v3.0.0-beta)
2. **Island state detection** — ✅ Đã triển cập nhật (`LtmIslandState.java`)
3. **Nâng cấp update UX** — Progress bar, retry logic

### Ưu tiên trung bình
4. **Widget nền tảng** — Thêm 1-2 widget đơn giản (notification preview, quick toggle)
5. **Island animation** — Tích hợp Lottie nếu SuperX hỗ trợ custom rendering

### Ưu tiên thấp
6. **Theme/skin system** — Mở rộng giao diện
7. **Wallpaper integration** — Thêm wallpaper picker
8. **Ad monetization** — Nếu cần (khuyên không dùng)

## 6. Rủi ro bảo mật / pháp lý

- **Tất cả tài nguyên 灵动屿** (animation, icon, code) đều thuộc sở hữu của tác giả Trung Quốc — KHÔNG được sao chép trực tiếp
- LTMori phải tự tạo animation assets (PNG, Lottie JSON) cho riêng mình
- Việc tham khảo kiến trúc (architecture pattern) là OK, nhưng tài nguyên (assets) phải tạo mới
- JiaGu anti-tamper không cần thiết cho LTMori (đã có GitHub signature verification)
