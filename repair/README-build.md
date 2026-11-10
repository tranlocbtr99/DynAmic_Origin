# LTMori 3.0.0-beta

Bản vá lấy APK LTMori 2.0.3 đã ký làm đầu vào. Download từ release `v2.0.3` trên GitHub.

2.0.3 đã bao gồm toàn bộ bản vá smali của các bản trước (compact UI, notification mode, bung thông báo, IdleDot template, JobScheduler hook). Vì vậy pipeline chỉ còn: giải nén, build lại, thay DEX trợ giúp và tăng version trong `repack.py` — không cần chạy patch smali.

Giải nén bản vá vào thư mục có APK đầu vào, sao cho mã nằm trong thư mục repair. Đặt Apktool 2.12.1 và Android SDK build-tools trong repair/tools. Dùng các lệnh sau từ thư mục chứa APK.

```sh
java -jar repair/tools/apktool.jar d -f -r LTMori-2.0.3-Origin-signed.apk -o repair/decoded
java -jar repair/tools/apktool.jar b -na repair/decoded
mkdir -p repair/compiled repair/dex
javac --release 11 -cp repair/tools/android.jar -d repair/compiled $(find repair/src repair/stubs -name '*.java')
(cd repair/compiled && zip -q ../kotlin-stub.jar kotlin/jvm/functions/Function1.class)
D8 --min-api 34 --lib repair/tools/android.jar --classpath repair/kotlin-stub.jar --output repair/dex $(find repair/compiled/com -name '*.class')
python3 repair/repack.py
```

`D8` là wrapper `/path/to/android-sdk/build-tools/*/d8` (không chạy `java -jar d8.jar`).

`repack.py` đọc versionName `2.0.3`, ghi `3.0.0`, đổi versionCode 53 → 60 và thay `classes3.dex` bằng các lớp trợ giúp mới. Manifest của 2.0.3 đã có provider, JobService và các quyền cần thiết nên không thêm gì.

Dùng apksigner để ký `repair/ltmori-unsigned.apk`:

```sh
apksigner sign \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --ks ltmori-signing.p12 --ks-pass pass:<password> --ks-key-alias ltmori \
  repair/ltmori-unsigned.apk
```

Khoá và mật khẩu được quản lý riêng.

## Script cũ (chỉ dùng cho nhánh 1.3.x)

`patch-smali.py`, `compact-ui.py`, `patch-201.py`, `patch-idledot.py` là các bản vá từng bước cho base 1.3.2/1.3.6. Trên base 2.0.3 chúng đã được áp dụng sẵn; không chạy lại (các mẫu smali sẽ không khớp).

## Phát hành bản tiếp theo

Tạo GitHub Release công khai trong tranlocbtr99/DynAmic_Origin. Dùng tag dạng v3.0.0 hoặc v3.0.0-beta.N và đính kèm LTMori-3.0.0-Origin-signed.apk. Tag phải khớp versionName. APK phải giữ applicationId và chữ ký, đồng thời có versionCode lớn hơn phiên bản đang cài. Kênh ổn định truy vấn release mới nhất; kênh Beta truy vấn toàn bộ release.

## English

Place the 2.0.3 base APK in the repository root and follow the commands above. Use `javac --release 11` (Java 17) and the SDK D8 wrapper for compilation. The 2.0.3 base already contains every smali patch (compact UI, notification mode, message expansion, IdleDot template, JobScheduler hook), so no patch scripts run. Sign the rebuilt APK locally with the existing LTMori key (`--ks-key-alias ltmori`).

Publish stable updates as public GitHub Releases with tag `vX.Y.Z` and signed APK `LTMori-X.Y.Z-Origin-signed.apk`. Publish beta updates as prereleases with tag `vX.Y.Z-beta.N` and the same filename. The beta channel toggle (in About) switches the updater between stable-only and all-releases. Android prompts the user to confirm installation.

The original APK, generated artifacts, tools and signing material are kept outside version control. Check the applicable upstream distribution terms before publishing a rebuilt APK.
