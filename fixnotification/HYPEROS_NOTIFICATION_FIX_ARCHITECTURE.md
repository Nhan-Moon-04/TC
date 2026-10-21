# BÁO CÁO KỸ THUẬT: KIẾN TRÚC & GIẢI PHÁP TỐI ƯU HÓA THÔNG BÁO VÀ CUỘC GỌI TRÊN XIAOMI HYPEROS

> **Mục đích tài liệu:** Cung cấp tài liệu kỹ thuật toàn diện về nguyên nhân gốc rễ (Root Cause), cơ chế chặn ngầm của Xiaomi HyperOS (HyperOS 1, 2, 3), và kiến trúc giải pháp 7 lớp (7-Layer Deep Fix) kết hợp hệ thống Watchdog tự phục hồi (Self-Healing Watchdog) để AI hoặc kỹ sư hệ thống phân tích, đánh giá và mở rộng.

---

## 1. PHÂN TÍCH NGUYÊN NHÂN GỐC RỄ (ROOT CAUSE ANALYSIS)

Người dùng thiết bị Xiaomi / Redmi chạy HyperOS (đặc biệt là các dòng máy mới như **Xiaomi 15 Pro** chạy HyperOS 2 và **Xiaomi 17 Pro Max** chạy HyperOS 3) thường xuyên gặp tình trạng:
1. Treo máy lâu hoặc tắt màn hình > 10 giây là **không nhận được thông báo** (Telegram, Messenger, Gmail...).
2. **Lỡ cuộc gọi Zalo / VoIP**: Chuông không reo, màn hình không sáng, người gọi chỉ nghe tiếng tút tút, khi người dùng mở khóa máy mới hiện thông báo cuộc gọi nhỡ.
3. Sau khi khởi động lại máy (reboot), toàn bộ cơ chế chạy nền bị hệ thống đóng băng.

### 1.1. Cơ chế đóng băng hạt nhân (Kernel cgroup v2 Freezer: Greezer & Aurogon)
Khác với Android thuần (AOSP) chỉ dùng Doze Mode để hạn chế Network/JobScheduler, Xiaomi tích hợp module độc quyền:
- **Greezer / Aurogon**: Sử dụng cgroup v2 freezer ở tầng Linux Kernel để đóng băng hoàn toàn (`FROZEN`) tiến trình của ứng dụng khi tắt màn hình hoặc chuyển vào background.
- Khi một tiến trình bị rơi vào trạng thái `FROZEN`, CPU ngưng cấp chu kỳ tính toán, toàn bộ socket TCP / TLS bị đình trệ, gói tin TCP Keep-Alive không được phản hồi dẫn đến đứt kết nối với server.

### 1.2. Sự khác biệt giữa FCM và Ứng dụng Non-FCM (Điển hình: Zalo)
- **Ứng dụng dùng Google FCM** (Facebook Messenger, Telegram, Gmail...): Chỉ cần tiến trình `com.google.android.gms` (Google Play Services) sống và giữ kết nối socket tới server Google. Khi có tin nhắn, server gửi tín hiệu High-Priority push đánh thức app.
- **Ứng dụng Non-FCM** (Zalo): Zalo **không dùng FCM** làm kênh chính, mà duy trì một kết nối TCP Socket độc quyền liên tục tới cụm server VNG (`push.zalo.me`). Do đó, nếu tiến trình của Zalo (`:push` hoặc `com.zing.zalo`) bị HyperOS đóng băng hoặc kill, Zalo lập tức rớt mạng và mất toàn bộ tin nhắn/cuộc gọi.

### 1.3. Lỗ hổng bảo mật AppOp 10021 & Cửa sổ nổi của HyperOS
Ngay cả khi Zalo nhận được gói tin cuộc gọi đến qua socket, HyperOS vẫn chặn Zalo hiển thị màn hình nhận cuộc gọi vì:
- **AppOp 10021 (`OP_BACKGROUND_START_ACTIVITY`)**: Xiaomi chặn tất cả ứng dụng bên thứ 3 tự ý mở Activity khi đang ở chế độ nền/tắt màn hình.
- **AppOp `SYSTEM_ALERT_WINDOW`**: Thiếu quyền hiển thị cửa sổ nổi trên màn hình khóa.
=> Kết quả: Zalo rung ngầm nhưng không thể bật giao diện chuông gọi đến, người dùng không hề hay biết có người đang gọi.

### 1.4. Cơ chế dọn dẹp định kỳ của PowerKeeper (`com.miui.powerkeeper`)
Xiaomi cài đặt daemon hệ thống `PowerKeeper`. Daemon này chạy ngầm và định kỳ quét, tự động ghi đè hoặc xóa sạch danh sách miễn trừ trong cài đặt hệ thống (`Settings.System.MILLET_NO_RESTRICT_APP`), khiến các ứng dụng đã fix sau một thời gian lại tiếp tục bị đóng băng.

### 1.5. Chặn quyền Boot Completed (`10007`) & Tự khởi chạy (`10008`)
HyperOS mặc định chặn broadcast `BOOT_COMPLETED` đối với các ứng dụng bên thứ ba trừ khi được cấp quyền MIUI Autostart. Nếu không có quyền này, khi khởi động lại máy (Reboot), các ứng dụng và dịch vụ nền không thể tự động chạy lại.

---

## 2. KIẾN TRÚC GIẢI PHÁP 7 LỚP (7-LAYER DEEP FIX)

Giải pháp áp dụng mô hình phòng thủ theo chiều sâu (Defense-in-Depth), can thiệp trực tiếp vào cả tầng hệ điều hành Android và tầng kernel Linux thông qua Shizuku / ADB shell:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   CÁC TẦNG PHÒNG THỦ (7-LAYER DEEP FIX)               │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 1: Miễn trừ cgroup v2 (MILLET_NO_RESTRICT_APP)                    │
│         settings put system MILLET_NO_RESTRICT_APP "pkg1,pkg2,..."     │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 2: Vô hiệu hóa Limiter hạt nhân (Kernel Freezer Overrides)         │
│         dumpsys greezer IM GMS disable                                 │
│         dumpsys greezer IM APP disable <package>                       │
│         dumpsys greezer LM add <package>                               │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 3: Ma trận AppOps đặc quyền HyperOS                               │
│         10008 (Autostart) | 10007 (Boot) | 10021 (Background Activity)  │
│         10023 (FG Service)| WAKE_LOCK    | SYSTEM_ALERT_WINDOW         │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 4: Miễn trừ Tiết kiệm pin & Standby Bucket (AOSP Doze)            │
│         dumpsys deviceidle whitelist +<package>                        │
│         am set-standby-bucket <package> exempted/active                │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 5: Danh sách trắng mạng nền (Netpolicy Whitelist)                 │
│         cmd netpolicy add restrict-background-whitelist <UID>          │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 6: Bảo vệ chuyên sâu Non-FCM / Zalo VoIP Call                     │
│         make-uid-active | Kích hoạt ZaloService & PushService          │
├────────────────────────────────────────────────────────────────────────┤
│ TẦNG 7: Tối ưu theo phần cứng (Xiaomi 15 Pro vs Xiaomi 17 Pro Max)     │
│         MILLET_FREEZE_MODE 1 | HyperOS 3 Ops (10006, 10062, 10065)     │
└────────────────────────────────────────────────────────────────────────┘
```

### Chi tiết các lệnh thực thi hệ thống:

```bash
# 1. Thêm ứng dụng vào danh sách miễn trừ cgroup của HyperOS
settings --user 0 put system MILLET_NO_RESTRICT_APP "com.google.android.gms,com.zing.zalo,..."

# 2. Can thiệp Greezer (Đóng băng tiến trình tầng nhân)
dumpsys greezer IM GMS disable
dumpsys greezer IM APP disable com.zing.zalo
dumpsys greezer LM add com.google.android.gms
dumpsys greezer LM add com.zing.zalo

# 3. Cấp toàn bộ AppOps sống còn
cmd appops set $PKG RUN_IN_BACKGROUND allow
cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow
cmd appops set $PKG WAKE_LOCK allow
cmd appops set $PKG 10007 allow        # OP_BOOT_COMPLETED (Khởi động cùng máy)
cmd appops set $PKG 10008 allow        # OP_AUTO_START (MIUI Autostart)
cmd appops set $PKG 10021 allow        # OP_BACKGROUND_START_ACTIVITY (Bật màn hình cuộc gọi Zalo)
cmd appops set $PKG 10023 allow        # OP_FOREGROUND_SERVICE
cmd appops set $PKG 10053 allow        # OP_AUTOSTART_SWITCH
cmd appops set $PKG SYSTEM_ALERT_WINDOW allow

# 4. Doze Mode & Standby Bucket (Ngăn chặn ngủ đông)
dumpsys deviceidle whitelist +$PKG
am set-inactive $PKG false
am set-standby-bucket $PKG exempted    # Hoặc 'active' nếu ROM không hỗ trợ exempted

# 5. Danh sách trắng lưu lượng mạng ngầm
UID=$(cmd package list packages -U $PKG | grep -oE "uid:[0-9]+" | cut -d: -f2)
cmd netpolicy add restrict-background-whitelist $UID

# 6. Dành riêng cho Xiaomi 17 Pro Max (HyperOS 3):
settings --user 0 put system MILLET_FREEZE_MODE 1 # Chế độ freeze tương thích
cmd appops set $PKG 10006 allow       # Force stop protection
cmd appops set $PKG 10012 allow       # Background process limit exception
cmd appops set $PKG 10016 allow       # Background data restriction
cmd appops set $PKG 10062 allow       # Background network restriction (HyperOS 3)
cmd appops set $PKG 10065 allow       # MIUI Battery saver per-app bypass (HyperOS 3)
```

---

## 3. HỆ THỐNG WATCHDOG CHẠY NGẦM BẤT TỬ (SELF-HEALING WATCHDOG ARCHITECTURE)

Để giải quyết triệt để việc PowerKeeper xóa cài đặt hoặc ứng dụng bị hệ thống kill khi cạn RAM:

```
┌────────────────────────────────────────────────────────────────────────┐
│               KIẾN TRÚC PHỤC HỒI ĐA LỚP (MULTI-LAYER RECOVERY)          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  [1. ContentObserver (Native Android)]                                 │
│      Theo dõi Settings.System.MILLET_NO_RESTRICT_APP                   │
│      => PowerKeeper vừa xóa là tự động ghi đè lại ngay lập tức         │
│                                                                        │
│  [2. Silent Foreground Service (MilletWatcherService)]                 │
│      - NotificationManager.IMPORTANCE_MIN (PRIORITY_MIN)               │
│      - Hoàn toàn KHÔNG hiện biểu tượng/thông báo trên thanh trạng thái │
│      - Vòng lặp Coroutine (45s) kiểm tra nhịp tim socket Zalo/GMS     │
│                                                                        │
│  [3. Cơ chế Hồi sinh khi bị tắt tác vụ (onTaskRemoved)]                │
│      Người dùng vuốt xóa khỏi Đa nhiệm                                 │
│      => onTaskRemoved() bắt sự kiện                                    │
│      => Lên lịch AlarmManager.setExactAndAllowWhileIdle hồi sinh sau 1s│
│                                                                        │
│  [4. Báo thức định kỳ sao lưu (WatchdogAlarmReceiver)]                 │
│      AlarmManager chạy ngầm mỗi 15 phút, đánh thức CPU thức dậy        │
│      rà soát dịch vụ và sửa chữa danh sách nếu bị lỗi                  │
│                                                                        │
│  [5. Khởi động cùng hệ thống (BootReceiver)]                           │
│      Bắt BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, USER_PRESENT           │
│      => Tự chạy ngầm ngay khi thiết bị vừa khởi động nguồn             │
│                                                                        │
│  [6. Linux Shell Daemon độc lập (/data/local/tmp)]                     │
│      Tách biệt hoàn toàn khỏi Android ActivityManager, chạy ở UID shell│
│      với vòng lặp kiểm tra vĩnh viễn (nohup)                           │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.1. Dịch vụ ngầm ẩn hoàn toàn (Silent Foreground Service)
- Thông thường, Android bắt buộc `ForegroundService` phải hiển thị một thông báo liên tục trên thanh thông báo.
- **Giải pháp xử lý:** Khởi tạo `NotificationChannel` với độ ưu tiên thấp nhất:
  ```kotlin
  val channel = NotificationChannel(
      CHANNEL_ID,
      "Bảo Vệ Ngầm Ẩn",
      NotificationManager.IMPORTANCE_MIN // Mức tối thiểu: Không âm thanh, không rung, KHÔNG ICON THANH TRẠNG THÁI
  ).apply {
      setShowBadge(false)
      enableLights(false)
      enableVibration(false)
      lockscreenVisibility = Notification.VISIBILITY_SECRET
  }
  ```
- Kết quả: Dịch vụ duy trì sự sống mức ưu tiên cao (`START_STICKY`) nhưng **người dùng không thấy bất kỳ biểu tượng thông báo nào trên thanh trạng thái**.

---

## 4. CƠ CHẾ DÙNG APP NGÂN HÀNG (TẮT DEVELOPER OPTIONS MÀ VẪN GIỮ FIX)

Một thách thức lớn nhất của người dùng Android tại Việt Nam: **Các ứng dụng ngân hàng (Vietcombank, MB Bank, Techcombank, BIDV, Cake...) phát hiện Tùy chọn nhà phát triển (Developer Options) hoặc Gỡ lỗi USB (USB Debugging) và chặn người dùng mở app.**

### 4.1. Bản chất kỹ thuật: Tại sao tắt Developer Options không làm mất cấu hình?
1. **AppOps & Doze Whitelist lưu vĩnh viễn trong file XML hệ thống:**
   - Khi chạy lệnh cấp quyền qua Shizuku, Android ghi trực tiếp vào cơ sở dữ liệu hệ thống tại `/data/system/users/0/appops.xml` và `/data/system/deviceidle.xml`.
   - **Các quyền này là vĩnh viễn.** Hệ điều hành Android KHÔNG BAO GIỜ thu hồi quyền AppOps hay Doze khi người dùng tắt USB Debugging hay Developer Options.
2. **Quyền sửa cài đặt hệ thống (`android.permission.WRITE_SETTINGS`) độc lập:**
   - Trong quá trình tự cấp quyền, `NotificationFix` đã tự cấp quyền `WRITE_SETTINGS` cho chính nó thông qua `cmd appops set com.nhandev.notificationfix WRITE_SETTINGS allow`.
   - Quyền `WRITE_SETTINGS` này được bảo toàn vĩnh viễn. Khi Developer Options bị tắt, `NotificationFix` **vẫn có toàn quyền đọc và ghi** vào `Settings.System.putString(contentResolver, "MILLET_NO_RESTRICT_APP", ...)`.
3. **Bộ lưu trữ cục bộ các ứng dụng đã chọn (`ProtectedAppsStorage.kt`):**
   - Mọi ứng dụng do người dùng tick chọn (Zalo, Messenger, Telegram, Gmail...) đều được ghi vào `SharedPreferences` nội bộ.
   - Khi không còn Shizuku, `MilletWatcherService` sử dụng chính danh sách đã lưu này kết hợp với quyền `WRITE_SETTINGS` để liên tục tự chữa lành `MILLET_NO_RESTRICT_APP` khi PowerKeeper can thiệp.

### 4.2. Tính an toàn bảo mật đối với App Ngân hàng
- **Không can thiệp Root / Magisk / KernelSU:** Máy hoàn toàn giữ trạng thái Stock ROM nguyên bản.
- **Không can thiệp Xposed / LSPosed / FrHook:** Không có module hook vào bộ nhớ của App Ngân hàng.
- **Không có Debugger:** Sau khi tắt Developer Options và USB Debugging, cờ `Settings.Global.ADB_ENABLED = 0` và `DEVELOPMENT_SETTINGS_ENABLED = 0`. App ngân hàng quét toàn bộ môi trường và xác nhận thiết bị an toàn 100%.

---

## 5. BẢN ĐỒ MÃ NGUỒN DỰ ÁN (PROJECT CODEBASE INVENTORY)

Toàn bộ giải pháp được đóng gói và tích hợp trong dự án mã nguồn mở Android:

| Thành phần | Đường dẫn tập tin | Chức năng kỹ thuật |
| :--- | :--- | :--- |
| **ProtectedAppsStorage** | `.../domain/ProtectedAppsStorage.kt` | Lưu trữ bền vững danh sách app cần bảo vệ để duy trì khi tắt Developer Options |
| **FixEngine** | `.../domain/FixEngine.kt` | Engine thực thi 7 lớp fix qua Shizuku/ADB shell, tự cấp quyền bất tử cho chính mình |
| **DeviceProfileManager** | `.../domain/DeviceProfileManager.kt` | Nhận diện model thiết bị (Xiaomi 15 Pro, 17 Pro Max) và cấu hình phần cứng tối ưu |
| **MilletWatcherService** | `.../domain/MilletWatcherService.kt` | Dịch vụ Foreground chạy ngầm ẩn (`IMPORTANCE_MIN`), theo dõi `ContentObserver` và Zalo Socket |
| **WatchdogAlarmReceiver** | `.../domain/WatchdogAlarmReceiver.kt` | Bộ đánh thức định kỳ 15 phút của `AlarmManager` và phục hồi sau 1s khi app bị vuốt tắt |
| **BootReceiver** | `.../domain/BootReceiver.kt` | Lắng nghe `BOOT_COMPLETED` để tự kích hoạt lại dịch vụ ngay khi khởi động máy |
| **HomeScreen** | `.../ui/screens/HomeScreen.kt` | Giao diện Compose hiện đại: Thẻ tự cấp quyền 1 chạm, Thẻ hướng dẫn & trạng thái App Ngân Hàng |
| **Shell Watchdog Script** | `hyperos_fcm_watchdog.sh` | Shell daemon v4.5 chạy ngầm độc lập trong thư mục `/data/local/tmp` |
| **PowerShell Fix Tool** | `notification_fix_v3.ps1` | Script chạy từ máy tính qua cáp USB ADB hỗ trợ người dùng fix nhanh không cần cài app |
| **File APK Thành Phẩm** | `NotificationFix.apk` | Bản cài đặt APK Android hoàn chỉnh đã biên dịch sẵn |

---

## 6. QUY TRÌNH VẬN HÀNH CHUẨN DÀNH CHO NGƯỜI DÙNG CUỐI

1. **Khởi tạo (Chỉ làm 1 lần):**
   - Mở Shizuku (qua Wi-Fi Debugging hoặc máy tính).
   - Mở ứng dụng **NotificationFix**, app tự nhận diện máy (Xiaomi 15 Pro / 17 Pro Max).
   - Nhấn nút: **`⚡ TỰ CẤP TOÀN BỘ QUYỀN & CHẠY NGẦM NGAY (1 CHẠM)`**.
   - Nhấn nút: **`🚀 TỐI ƯU TOÀN DIỆN CHO THIẾT BỊ`** (hoặc chọn các app Zalo, Telegram, Messenger và bấm Fix).
2. **Kích hoạt Chế độ Ngân Hàng (Bank Mode):**
   - Vào **Cài đặt** điện thoại > **Cài đặt bổ sung** > **Tùy chọn nhà phát triển** > **Gạt TẮT**.
   - Mở các ứng dụng ngân hàng (Vietcombank, MB, Techcombank...) sử dụng bình thường.
3. **Trạng thái duy trì vĩnh viễn:**
   - Dịch vụ `NotificationFix` tiếp tục chạy ngầm ẩn hoàn toàn (không icon, không thông báo).
   - Zalo, Telegram, Messenger nhận thông báo tức thì, cuộc gọi Zalo chuông reo và sáng màn hình bình thường kể cả khi khóa máy qua đêm.
   - Khởi động lại máy (Reboot), hệ thống tự phục hồi 100% không cần thao tác lại.
