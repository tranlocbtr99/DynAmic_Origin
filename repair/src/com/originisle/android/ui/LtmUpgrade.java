package com.originisle.android.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.HttpsURLConnection;

public final class LtmUpgrade {
    private static final String REPO = "tranlocbtr99/DynAmic_Origin";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String ASSETS = "https://github.com/" + REPO + "/releases/download/";
    private static final String APK_TYPE = "application/vnd.android.package-archive";
    private static final long MAX_APK = 150L * 1024 * 1024;
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static LtmPanels.Sheet active;

    static File apk(Context context) {
        return new File(context.getCacheDir(), "ltmori-update.apk");
    }

    static String version(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException error) {
            return "—";
        }
    }

    static void enqueue(Runnable work) {
        WORK.execute(work);
    }

    static JSONObject newRelease(Context context) throws Exception {
        JSONObject candidate = release();
        String tag = candidate.getString("tag_name");
        if (compareVersions(tag, version(context)) <= 0) return null;
        selectApk(candidate, tag);
        return candidate;
    }

    static String notes(JSONObject release) {
        String body = release.optString("body", "").strip();
        return body.isEmpty() ? "Bản phát hành chưa có ghi chú." : body;
    }

    public static void open(Context context) {
        Activity activity = LtmPanels.activity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (active != null && active.dialog.isShowing()) return;
        LtmPanels.Sheet loading = new LtmPanels.Sheet(activity, "Cập nhật LTMori", "Phiên bản hiện tại · " + version(activity));
        TextView status = loading.text("Đang kiểm tra GitHub…", 16, 0xffffffff);
        loading.body.addView(status);
        loading.button("Đóng", false, loading.dialog::dismiss);
        track(loading);
        loading.show(43);
        WORK.execute(() -> {
            try {
                JSONObject candidate = newRelease(activity);
                post(loading, () -> {
                    if (candidate == null) {
                        status.setText("Bạn đang dùng phiên bản mới nhất.");
                    } else {
                        loading.dialog.dismiss();
                        showRelease(activity, candidate);
                    }
                });
            } catch (Exception error) {
                post(loading, () -> status.setText(userMessage(error)));
            }
        });
    }

    static boolean showRelease(Activity activity, JSONObject release) {
        if (activity.isFinishing() || activity.isDestroyed() || active != null && active.dialog.isShowing()) return false;
        try {
            String tag = release.getString("tag_name");
            JSONObject asset = selectApk(release, tag);
            LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity, "Có gì mới?", "LTMori " + tag);
            sheet.card("Ghi chú phát hành", notes(release));
            TextView status = sheet.text("APK sẽ được xác minh trước khi cài.", 14, 0xffbdc9db);
            sheet.body.addView(status);
            ProgressBar progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            progress.setVisibility(android.view.View.GONE);
            sheet.body.addView(progress);
            Runnable[] action = new Runnable[1];
            Button primary = sheet.button("Cập nhật", true, () -> action[0].run());
            action[0] = () -> download(sheet, status, progress, primary, action, asset, tag);
            sheet.button("Để sau", false, sheet.dialog::dismiss);
            track(sheet);
            sheet.show(73);
            LtmUpdateJob.markSeen(activity, tag);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private static void track(LtmPanels.Sheet sheet) {
        active = sheet;
        sheet.dialog.setOnDismissListener(dialog -> {
            if (active == sheet) active = null;
        });
    }

    private static void download(LtmPanels.Sheet sheet, TextView status, ProgressBar progress,
                                 Button button, Runnable[] action, JSONObject asset, String tag) {
        Activity activity = sheet.activity;
        button.setEnabled(false);
        progress.setVisibility(android.view.View.VISIBLE);
        progress.setIndeterminate(false);
        progress.setProgress(0);
        status.setText("Đang tải APK…");
        WORK.execute(() -> {
            File temp = null;
            try {
                long size = asset.getLong("size");
                if (size <= 0 || size > MAX_APK) throw new IOException("Kích thước APK không hợp lệ.");
                temp = File.createTempFile("ltmori-download-", ".apk", activity.getCacheDir());
                HttpsURLConnection connection = connect(asset.getString("browser_download_url"), false);
                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[32768];
                    long total = 0;
                    int previous = -1;
                    for (int count; (count = in.read(buffer)) != -1;) {
                        total += count;
                        if (total > size) throw new IOException("APK tải về lớn hơn kích thước công bố.");
                        out.write(buffer, 0, count);
                        int percent = (int) (100 * total / size);
                        if (percent != previous) {
                            previous = percent;
                            post(sheet, () -> {
                                progress.setProgress(percent);
                                status.setText("Đang tải APK · " + percent + "%");
                            });
                        }
                    }
                    if (total != size) throw new IOException("APK tải về chưa đầy đủ.");
                } finally {
                    connection.disconnect();
                }
                verify(activity, temp, tag);
                Files.move(temp.toPath(), apk(activity).toPath(), StandardCopyOption.REPLACE_EXISTING);
                post(sheet, () -> {
                    message(status, progress, "APK đã xác minh. Android sẽ yêu cầu xác nhận cài đè.");
                    button.setText("Cài bản cập nhật");
                    action[0] = () -> install(activity, status);
                    button.setEnabled(true);
                    action[0].run();
                });
            } catch (Exception error) {
                post(sheet, () -> {
                    message(status, progress, userMessage(error));
                    button.setEnabled(true);
                });
            } finally {
                if (temp != null) temp.delete();
            }
        });
    }

    private static void install(Activity activity, TextView status) {
        try {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                status.setText("Cho phép cài ứng dụng từ LTMori trong Cài đặt Android, quay lại rồi nhấn Cài bản cập nhật.");
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName())));
                return;
            }
            Uri uri = Uri.parse("content://" + activity.getPackageName() + ".ltmori.updates/release.apk");
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_TYPE);
            intent.setClipData(ClipData.newRawUri("LTMori APK", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (RuntimeException error) {
            status.setText("Không mở được trình cài đặt Android. Kiểm tra quyền cài ứng dụng từ LTMori.");
        }
    }

    private static void verify(Context context, File file, String tag) throws Exception {
        PackageManager pm = context.getPackageManager();
        int flags = PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo current = pm.getPackageInfo(context.getPackageName(), flags);
        PackageInfo update = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        if (update == null || !current.packageName.equals(update.packageName))
            throw new IOException("APK không thuộc mã gói của LTMori đang cài.");
        if (update.versionName == null || update.getLongVersionCode() <= current.getLongVersionCode()
                || compareVersions(update.versionName, tag) != 0)
            throw new IOException("Phiên bản APK không khớp bản phát hành hoặc không mới hơn.");
        if (current.signingInfo == null || update.signingInfo == null
                || !new HashSet<>(Arrays.asList(current.signingInfo.getApkContentsSigners()))
                    .equals(new HashSet<>(Arrays.asList(update.signingInfo.getApkContentsSigners()))))
            throw new IOException("Chữ ký APK không khớp với LTMori đang cài.");
    }

    private static JSONObject release() throws Exception {
        HttpsURLConnection connection = connect(API, true);
        try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            for (int count; (count = in.read(buffer)) != -1;) {
                if (out.size() + count > 2 * 1024 * 1024) throw new IOException("Dữ liệu GitHub quá lớn.");
                out.write(buffer, 0, count);
            }
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    private static JSONObject selectApk(JSONObject release, String tag) throws Exception {
        String plain = tag.replaceFirst("^v", "");
        String primary = "LTMori-" + plain + "-Origin-signed.apk";
        String generic = "LTMori-" + plain + "-signed.apk";
        JSONArray assets = release.getJSONArray("assets");
        JSONObject fallback = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.getJSONObject(i);
            String name = item.optString("name");
            String url = item.optString("browser_download_url");
            if (!url.equals(ASSETS + tag + "/" + name)) continue;
            if (name.equals(primary)) return item;
            if (name.equals(generic)) fallback = item;
        }
        if (fallback != null) return fallback;
        throw new IOException("Bản GitHub này chưa có APK LTMori đã ký phù hợp.");
    }

    private static HttpsURLConnection connect(String address, boolean api) throws Exception {
        for (int redirects = 0; redirects <= 4; redirects++) {
            URL url = new URL(address);
            String host = url.getHost();
            if (!url.getProtocol().equals("https")
                    || !(api ? host.equals("api.github.com") : host.equals("github.com")
                    || host.endsWith(".githubusercontent.com")))
                throw new IOException("Địa chỉ tải cập nhật không hợp lệ.");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "LTMori-Android-Updater");
            if (api) conn.setRequestProperty("Accept", "application/vnd.github+json");
            int code;
            try {
                code = conn.getResponseCode();
            } catch (Exception error) {
                conn.disconnect();
                throw error;
            }
            if (code == 200) return conn;
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (code >= 300 && code < 400 && location != null) {
                address = new URL(url, location).toString();
                continue;
            }
            if (api && code == 404) throw new IOException("GitHub chưa có bản phát hành công khai.");
            if (api && (code == 403 || code == 429)) throw new IOException("GitHub đang giới hạn yêu cầu. Hãy thử lại sau.");
            throw new IOException("GitHub trả về lỗi HTTP " + code + ".");
        }
        throw new IOException("Đường dẫn tải có quá nhiều lần chuyển hướng.");
    }

    static int compareVersions(String left, String right) throws IOException {
        if (!left.matches("v?[0-9]{1,8}(\\.[0-9]{1,8}){0,3}") || !right.matches("v?[0-9]{1,8}(\\.[0-9]{1,8}){0,3}"))
            throw new IOException("Phiên bản GitHub hoặc ứng dụng không hợp lệ.");
        String[] a = left.replaceFirst("^v", "").split("\\.");
        String[] b = right.replaceFirst("^v", "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int order = Integer.compare(i < a.length ? Integer.parseInt(a[i]) : 0,
                    i < b.length ? Integer.parseInt(b[i]) : 0);
            if (order != 0) return order;
        }
        return 0;
    }

    private static void message(TextView status, ProgressBar progress, String text) {
        progress.setVisibility(android.view.View.GONE);
        status.setText(text);
    }

    private static String userMessage(Exception error) {
        if (error instanceof java.net.SocketTimeoutException || error instanceof java.net.UnknownHostException)
            return "Không kết nối được GitHub. Kiểm tra mạng và thử lại.";
        return error.getClass() == IOException.class ? error.getMessage() : "Không xử lý được bản cập nhật. Hãy thử lại.";
    }

    private static void post(LtmPanels.Sheet sheet, Runnable operation) {
        sheet.activity.runOnUiThread(() -> {
            if (sheet.dialog.isShowing() && !sheet.activity.isFinishing() && !sheet.activity.isDestroyed())
                operation.run();
        });
    }
}
