# LTMori 2.0.1

Bản vá lấy APK LTMori 1.3.2 đã ký làm đầu vào. SHA-256 của đầu vào là f85ca4f869dab08817fc447491dc06a32fbedbebe0f73956839a0217309b90ce.

Giải nén bản vá vào thư mục có APK đầu vào, sao cho mã nằm trong thư mục repair. Đặt Apktool 2.12.1, R8 8.9.35, ECJ 3.39.0 và Android SDK 34 android.jar trong repair/tools. Dùng các lệnh sau từ thư mục chứa APK.

```sh
java -jar repair/tools/apktool.jar d -f -r LTMori-1.3.2-Origin-signed.apk -o repair/decoded
python3 repair/patch-smali.py
python3 repair/compact-ui.py
python3 repair/patch-201.py
java -jar repair/tools/apktool.jar b -na repair/decoded
mkdir -p repair/compiled repair/dex
java -jar repair/tools/ecj.jar -11 -proc:none -cp repair/tools/android.jar -d repair/compiled $(find repair/src repair/stubs -name '*.java')
(cd repair/compiled && zip -q ../kotlin-stub.jar kotlin/jvm/functions/Function1.class)
java -cp repair/tools/r8.jar com.android.tools.r8.D8 --min-api 34 --lib repair/tools/android.jar --classpath repair/kotlin-stub.jar --output repair/dex $(find repair/compiled/com -name '*.class')
python3 repair/repack.py
```

Dùng apksigner với --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true để ký repair/ltmori-unsigned.apk bằng khóa LTMori đang dùng. Khóa và mật khẩu được quản lý riêng.

## Phát hành bản tiếp theo

Tạo GitHub Release công khai trong tranlocbtr99/DynAmic_Origin. Dùng tag dạng v2.0.2 và đính kèm LTMori-2.0.2-Origin-signed.apk. Tag phải khớp versionName. APK phải giữ applicationId và chữ ký, đồng thời có versionCode lớn hơn phiên bản đang cài. Nút cập nhật truy vấn release ổn định mới nhất.

## English

Place the exact 1.3.2 base APK in the repository root and follow the commands above. Use the official Android SDK 34 platform JAR for compilation. Sign the rebuilt APK locally with the existing LTMori key. Publish stable updates as public GitHub Releases with a matching version tag and signed APK filename. Increase the version code for every update. Android prompts the user to confirm installation.

The original APK, generated artifacts, tools and signing material are kept outside version control. Check the applicable upstream distribution terms before publishing a rebuilt APK.
