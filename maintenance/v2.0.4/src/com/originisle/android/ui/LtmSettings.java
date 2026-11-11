package com.originisle.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.activity.compose.BackHandlerKt;
import androidx.compose.foundation.layout.SizeKt;
import androidx.compose.foundation.lazy.LazyListScope;
import androidx.compose.ui.Modifier;
import androidx.compose.runtime.Composer;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.SnapshotStateKt;
import androidx.compose.ui.viewinterop.AndroidView_androidKt;
import com.originisle.android.island.PlaygroundService;
import com.originisle.android.ui.samples.IdleDot;
import java.util.WeakHashMap;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class LtmSettings {
    private static final int IDLE_ID = 61060;
    private static final int SURFACE = 0xff1c222e;
    private static final int MUTED = 0xffb6c3d6;
    private static final int ACCENT = 0xff9fceff;
    private static final String[] ICONS = {"", "◉", "≋", "☾", "▦"};
    private static final String[] GROUPS = {"", "Thông báo", "Hoạt động trực tiếp", "Giờ yên lặng", "Ứng dụng và nhật ký"};
    private static final String[] SUBTITLES = {"", "Chế độ hiển thị, âm thanh và màn hình khóa", "Sạc pin, chỉ báo riêng tư và thao tác", "Không làm phiền và lịch riêng", "Ứng dụng chuyển tiếp và quản lý thẻ"};
    private static final String[] STICKERS = {"", "🌙", "✨", "💚", "🌿", "☕", "⭐"};
    private static final WeakHashMap<Context, MutableState<Integer>> PAGES = new WeakHashMap<>();

    private static boolean english;

    public static int menu(LazyListScope list, Context context) {
        english = idlePrefs(context).getBoolean("english", false);
        Activity activity = LtmPanels.activity(context);
        Context owner = activity == null ? context : activity;
        MutableState<Integer> state = PAGES.computeIfAbsent(owner,
                key -> SnapshotStateKt.mutableStateOf(0, SnapshotStateKt.structuralEqualityPolicy()));
        int page = state.getValue();
        list.item("ltm-settings-navigation-" + page, null, (scope, composer, changed) -> {
            BackHandlerKt.BackHandler(page != 0, () -> {
                state.setValue(0);
                return Unit.INSTANCE;
            }, composer, 0, 0);
            embed(composer, ctx -> navigation(ctx, state, page), true);
            return Unit.INSTANCE;
        });
        return page;
    }

    public static void footer(LazyListScope list) {
        list.item("ltm-settings-footer", null, (scope, composer, changed) -> {
            embed(composer, context -> {
                LinearLayout layout = column(context);
                TextView copyright = label(context, "Bản quyền © LocTM", 13, 0xffbdc9db);
                copyright.setGravity(Gravity.CENTER);
                layout.addView(copyright);
                TextView privacy = label(context, "Chính sách quyền riêng tư", 14, ACCENT);
                privacy.setGravity(Gravity.CENTER);
                privacy.setMinHeight(dp(context, 48));
                privacy.setOnClickListener(view -> showPrivacy(context));
                layout.addView(privacy);
                return layout;
            }, true);
            return Unit.INSTANCE;
        });
    }

    public static void heart(Composer composer) {
        embed(composer, context -> {
            TextView heart = label(context, "♡  Ủng hộ tác giả", 12, 0xffffbdce);
            heart.setContentDescription(tr("Ủng hộ tác giả"));
            heart.setGravity(Gravity.CENTER);
            heart.setPadding(dp(context, 12), 0, dp(context, 12), 0);
            heart.setHeight(dp(context, 44));
            heart.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff), glass(context), null));
            heart.setOnClickListener(view -> showSupport(context));
            return heart;
        }, false);
    }

    private static void embed(Composer composer, Function1<Context, View> factory, boolean fullWidth) {
        Modifier modifier = fullWidth ? SizeKt.fillMaxWidth(Modifier.Companion, 1f) : Modifier.Companion;
        AndroidView_androidKt.AndroidView(factory, modifier, null, composer, 0, 4);
    }

    private static LinearLayout navigation(Context context, MutableState<Integer> state, int page) {
        LinearLayout layout = column(context);
        if (page != 0) {
            TextView back = label(context, "‹  LTMori", 16, ACCENT);
            back.setGravity(Gravity.CENTER);
            back.setMinHeight(dp(context, 44));
            back.setPadding(dp(context, 16), 0, dp(context, 16), 0);
            back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff), glass(context), null));
            back.setOnClickListener(view -> state.setValue(0));
            layout.addView(back, new LinearLayout.LayoutParams(-2, -2));
            layout.addView(section(context, GROUPS[page]));
            return layout;
        }

        Switch enabled = new Switch(context);
        enabled.setText(tr("Bật Island"));
        enabled.setTextColor(Color.WHITE);
        enabled.setTextSize(16);
        enabled.setMinHeight(dp(context, 54));
        enabled.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        enabled.setThumbTintList(ColorStateList.valueOf(0xfff4f7fc));
        enabled.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[0]},
                new int[]{0xff298a61, 0xff51596a}));
        enabled.setChecked(isIdleActive());
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (checked) showIdle(context);
            else context.startService(new Intent(context, PlaygroundService.class)
                    .setAction("ACTION_CANCEL").putExtra("id", IDLE_ID).putExtra("oi_scene", "NAVIGATION")
                    .putExtra("ltmori_explicit_dismiss", true));
        });

        LinearLayout quick = group(context);
        append(quick, row(context, "●", "Island", "Hiển thị thẻ chờ", 0xff206d52, () -> {
            if (enabled.isChecked()) showIdle(context);
            else enabled.setChecked(true);
        }));
        append(quick, enabled);
        append(quick, row(context, "✦", "Tùy chỉnh thẻ chờ", "Tiêu đề, nội dung và sticker", 0xff694ea1, () -> customize(context)));
        layout.addView(quick);

        layout.addView(section(context, "TRẢI NGHIỆM"));
        LinearLayout experience = group(context);
        int[] colors = {0, 0xff285d99, 0xff8a4b28, 0xff50478e, 0xff335d68};
        for (int i = 1; i < GROUPS.length; i++) {
            int destination = i;
            append(experience, row(context, ICONS[i], GROUPS[i], SUBTITLES[i], colors[i],
                    () -> state.setValue(destination)));
        }
        layout.addView(experience);

        layout.addView(section(context, "HỆ THỐNG"));
        LinearLayout system = group(context);
        append(system, row(context, "⚙", "Quyền và kết nối", "", 0xff536274, () -> permissions(context)));
        append(system, row(context, "↻", "Kiểm tra cập nhật", "", 0xff285d99, () -> LtmUpgrade.open(context)));
        append(system, row(context, "◎", "Ngôn ngữ", english ? "English" : "Tiếng Việt", 0xff694ea1, () -> language(context)));
        append(system, row(context, "ⓘ", "Giới thiệu", LtmUpgrade.version(context), 0xff536274,
                () -> LtmPanels.about(context, (dialog, which) -> {
                    if (!StatusHelpersKt.openAutoStartSettings(context)) appSettings(context);
                })));
        layout.addView(system);
        return layout;
    }

    private static LinearLayout group(Context context) {
        LinearLayout group = column(context);
        group.setBackground(rounded(context, SURFACE, 22));
        group.setClipToOutline(true);
        return group;
    }

    private static void append(LinearLayout group, View row) {
        Context context = group.getContext();
        if (group.getChildCount() > 0) {
            View divider = new View(context);
            divider.setBackgroundColor(0x22d3e3ff);
            LinearLayout.LayoutParams rule = new LinearLayout.LayoutParams(-1, 1);
            rule.setMarginStart(dp(context, 58));
            rule.setMarginEnd(dp(context, 14));
            group.addView(divider, rule);
        }
        group.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private static TextView section(Context context, String text) {
        TextView title = label(context, text, 12, MUTED);
        title.setLetterSpacing(0.08f);
        title.setPadding(dp(context, 15), dp(context, 17), 0, dp(context, 7));
        return title;
    }

    private static View row(Context context, String symbol, String title, String description, int tint, Runnable open) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 14), dp(context, 9), dp(context, 14), dp(context, 9));
        row.setMinimumHeight(dp(context, 52));

        TextView icon = label(context, symbol, 18, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(context, tint, 9));
        row.addView(icon, new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)));

        LinearLayout words = column(context);
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, -2, 1);
        middle.setMarginStart(dp(context, 12));
        middle.setMarginEnd(dp(context, 8));
        words.addView(label(context, title, 16, Color.WHITE));
        if (!description.isEmpty()) words.addView(label(context, description, 12, MUTED));
        row.addView(words, middle);
        row.addView(label(context, "›", 22, MUTED));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26ffffff), null,
                rounded(context, Color.WHITE, 0)));
        row.setFocusable(true);
        row.setOnClickListener(view -> open.run());
        return row;
    }

    private static GradientDrawable rounded(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        return shape;
    }

    private static GradientDrawable glass(Context context) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x9957637e, 0x80232c41});
        shape.setCornerRadius(dp(context, 28));
        shape.setStroke(dp(context, 1), 0x55c1d4fa);
        return shape;
    }

    private static TextView label(Context context, String text, int size, int color) {
        TextView label = new TextView(context);
        label.setText(tr(text));
        label.setTextSize(size);
        label.setTextColor(color);
        return label;
    }

    private static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private static int dp(Context context, int value) {
        return LtmPanels.dp(context, value);
    }

    private static void language(Context context) {
        Activity activity = LtmPanels.activity(context);
        if (activity == null) return;
        LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity, tr("Ngôn ngữ"), "Tiếng Việt / English");
        sheet.button("Tiếng Việt", !english, () -> setLanguage(activity, sheet, false));
        sheet.button("English", english, () -> setLanguage(activity, sheet, true));
        sheet.show(46);
    }

    private static void setLanguage(Activity activity, LtmPanels.Sheet sheet, boolean next) {
        sheet.dialog.dismiss();
        if (english == next) return;
        idlePrefs(activity).edit().putBoolean("english", next).apply();
        english = next;
        activity.recreate();
    }

    private static void permissions(Context context) {
        Activity activity = LtmPanels.activity(context);
        if (activity == null) return;
        LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity, tr("Quyền và kết nối"), "LTMori");
        sheet.button(tr("Truy cập thông báo"), true, () -> openSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        sheet.button(tr("Truy cập dữ liệu sử dụng"), false, () -> openSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS));
        sheet.button(tr("Pin và chạy nền"), false, () -> openSettings(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        sheet.button(tr("Thông tin ứng dụng"), false, () -> appSettings(context));
        sheet.show(58);
    }

    private static void openSettings(Context context, String action) {
        try {
            context.startActivity(new Intent(action));
        } catch (android.content.ActivityNotFoundException unavailable) {
            appSettings(context);
        }
    }

    private static void appSettings(Context context) {
        context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.getPackageName(), null)));
    }

    public static void useSourceIcon(Context context, Intent intent, Bundle card) {
        String source = intent.getStringExtra("source_pkg");
        String name = intent.getStringExtra("source_app");
        if (source == null || name == null || name.isEmpty()) return;
        String baseKey = "notification.superx.baseInfos";
        Bundle base = card.getBundle(baseKey);
        boolean expandedLabel = base != null && base.getInt(baseKey + ".subInfo") == 1
                && isSourceLabel(base.getCharSequence(baseKey + ".subText"), name, source);

        Bundle island = card.getBundle("notification.superx.island");
        String rightKey = "island.superx.rightInfo";
        Bundle right = island == null ? null : island.getBundle(rightKey);
        int template = island == null ? 0 : island.getInt("island.superx.rightTemplate");
        boolean compactLabel = right != null && (template == 4 || template == 5 || template == 6)
                && (isSourceLabel(right.getCharSequence(rightKey + ".content"), name, source)
                || isSourceLabel(right.getCharSequence(rightKey + ".capsuleContent"), name, source));
        if (!expandedLabel && !compactLabel) return;

        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(source);
            Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, 96, 96);
            icon.draw(new Canvas(bitmap));
            Icon appIcon = Icon.createWithBitmap(bitmap);
            if (expandedLabel) {
                base.remove(baseKey + ".subText");
                base.putInt(baseKey + ".subInfo", 3);
                base.putParcelable(baseKey + ".subImage", appIcon);
            }
            if (compactLabel) {
                island.putInt("island.superx.rightTemplate", 4);
                right.remove(rightKey + ".capsuleContent");
                right.putCharSequence(rightKey + ".content", "");
                right.putParcelable(rightKey + ".icon", appIcon);
                String capsuleKey = "notification.superx.capsule";
                Bundle capsule = card.getBundle(capsuleKey);
                if (capsule != null && isSourceLabel(capsule.getCharSequence(capsuleKey + ".content"), name, source)) {
                    capsule.putString(capsuleKey + ".content", "");
                    capsule.putParcelable(capsuleKey + ".icon", appIcon);
                }
            }
        } catch (PackageManager.NameNotFoundException unavailable) {
            // Keep the original label if its app icon is unavailable.
        }
    }

    private static boolean isSourceLabel(CharSequence text, String name, String source) {
        if (text == null) return false;
        String value = text.toString().trim();
        return value.equals(name) || value.equals(source);
    }

    public static String tr(String text) {
        if (!english) return vietnamese(text);
        switch (text) {
            case "♡  Ủng hộ tác giả": return "♡  Support";
            case "Ủng hộ tác giả": return "Support the developer";
            case "Bật Island": return "Enable Island";
            case "Hiển thị thẻ chờ": return "Show idle card";
            case "Tùy chỉnh thẻ chờ": return "Customize idle card";
            case "Tiêu đề, nội dung và sticker": return "Title, message and sticker";
            case "TRẢI NGHIỆM": return "EXPERIENCE";
            case "HỆ THỐNG": return "SYSTEM";
            case "Thông báo": return "Notifications";
            case "Hoạt động trực tiếp": return "Live activities";
            case "Giờ yên lặng": return "Quiet hours";
            case "Ứng dụng và nhật ký": return "Apps and activity";
            case "Chế độ hiển thị, âm thanh và màn hình khóa": return "Display mode, sound and lock screen";
            case "Sạc pin, chỉ báo riêng tư và thao tác": return "Charging, privacy indicators and actions";
            case "Không làm phiền và lịch riêng": return "Do Not Disturb and custom schedule";
            case "Ứng dụng chuyển tiếp và quản lý thẻ": return "Forwarding apps and card management";
            case "Quyền và kết nối": return "Permissions and connection";
            case "Kiểm tra cập nhật": return "Check for updates";
            case "Ngôn ngữ": return "Language";
            case "Giới thiệu": return "About";
            case "Chính sách quyền riêng tư": return "Privacy policy";
            case "Bản quyền © LocTM": return "Copyright © LocTM";
            case "Truy cập thông báo": return "Notification access";
            case "Truy cập dữ liệu sử dụng": return "Usage access";
            case "Pin và chạy nền": return "Battery and background";
            case "Thông tin ứng dụng": return "App info";
            case "Đang chờ": return "Idle";
            case "Tiêu đề": return "Title";
            case "Nội dung": return "Message";
            case "Mặc định": return "Default";
            case "Xem trước": return "Preview";
            case "Lưu": return "Save";
            case "Quay lại": return "Back";
            case "Đóng": return "Close";
            case "Khôi phục mặc định": return "Restore defaults";
            case "Nội dung có thể hiện trên màn hình khóa": return "Content may appear on the lock screen";
            case "Bố cục tham khảo": return "Layout preview";
            case "♡ Ủng hộ tác giả": return "♡ Support the developer";
            case "Ủng hộ qua Techcombank": return "Support via Techcombank";
            case "Làm mới": return "Refresh";
            case "Dừng thẻ": return "Stop cards";
            case "Nội dung trên đảo": return "On the Island";
            case "Đang diễn ra": return "Ongoing";
            case "Tất cả thông báo": return "All notifications";
            case "Áp dụng": return "Apply";
            case "Hủy": return "Cancel";
            case "Nhạc và video": return "Music and video";
            case "Hiển thị điều khiển phát": return "Show playback controls";
            case "Bao gồm tin nhắn và thông báo thường.": return "Includes messages and regular notifications.";
            case "Chỉ hiện hoạt động như cuộc gọi, điều hướng và tải xuống.": return "Shows activities like calls, navigation and downloads.";
            case "Ứng dụng": return "Apps";
            case "Nhật ký": return "Activity";
            case "Quyền truy cập": return "Permissions";
            case "Tự khởi động và khởi động liên kết": return "Auto-start and linked startup";
            case "Thông báo LTMori": return "LTMori notifications";
            case "Giới thiệu LTMori": return "About LTMori";
            case "Phiên bản": return "Version";
            case "Cài đặt tự khởi động": return "Auto-start settings";
            case "Quyền riêng tư": return "Privacy";
            case "Ghi công": return "Credits";
            case "Tự khởi động": return "Auto-start";
            case "Cập nhật LTMori": return "Update LTMori";
            case "Đang kiểm tra GitHub…": return "Checking GitHub…";
            case "Cập nhật": return "Update";
            case "Có gì mới?": return "What's new?";
            case "Bản phát hành chưa có ghi chú.": return "No release notes yet.";
            case "APK sẽ được xác minh trước khi cài.": return "The APK will be verified before installation.";
            case "Đang tải APK…": return "Downloading APK…";
            case "Đang tải APK · ": return "Downloading APK · ";
            case "APK đã xác minh. Android sẽ yêu cầu xác nhận cài đè.": return "APK verified. Android will ask you to confirm the update.";
            case "Phiên bản hiện tại · ": return "Current version · ";
            case "Để sau": return "Later";
            case "Tải lại": return "Retry";
            case "Thông báo và đa phương tiện": return "Notifications and media";
            case "Ứng dụng gần đây": return "Recent apps";
            case "Kết nối mạng": return "Network connections";
            case "Quyền kiểm soát": return "Your controls";
            case "Liên hệ": return "Contact";
            case "Chào mừng đến với LTMori": return "Welcome to LTMori";
            case "Đọc hết chính sách để tiếp tục": return "Read the full policy to continue";
            case "Tôi đã đọc và đồng ý": return "I have read and agree";
            case "Bắt đầu sử dụng": return "Get started";
            case "Thoát": return "Exit";
            default: return text;
        }
    }

    private static String vietnamese(String text) {
        switch (text) {
            case "Hide after": return "Tự ẩn sau";
            case "Non live cards": return "Thẻ thông báo thường";
            case "Never": return "Không bao giờ";
            case "Earbuds": return "Tai nghe";
            case "Battery when they connect": return "Hiện pin khi kết nối";
            case "Skip silent": return "Bỏ qua thông báo im lặng";
            case "Ignore silenced notifications": return "Ẩn thông báo không có âm thanh";
            case "Lock screen": return "Màn hình khóa";
            case "Live card on lockscreen": return "Hiện thẻ trực tiếp trên màn hình khóa";
            case "Cards": return "Thẻ trực tiếp";
            case "Login codes": return "Mã đăng nhập";
            case "Show the code, tap to copy": return "Hiện mã, chạm để sao chép";
            case "Action buttons": return "Nút thao tác";
            case "Charging": return "Sạc pin";
            case "Battery card when you plug in": return "Hiện thẻ pin khi cắm sạc";
            case "Briefly": return "Hiện ngắn";
            case "While charging": return "Trong khi sạc";
            case "Camera & mic": return "Camera và mic";
            case "Follow Do Not Disturb": return "Theo Không làm phiền";
            case "Quiet while DND is on": return "Giảm thông báo khi bật Không làm phiền";
            case "Schedule": return "Lịch riêng";
            case "From ": return "Từ ";
            case "To ": return "Đến ";
            case "Quiet hours": return "Giờ yên lặng";
            case "While quiet, only calls, navigation, timers, login codes, media and charging show.": return "Giờ yên lặng chỉ hiện cuộc gọi, điều hướng, hẹn giờ, mã đăng nhập, đa phương tiện và sạc.";
            case "All on": return "Tất cả đang bật";
            case "All off": return "Tắt tất cả";
            case "All": return "Tất cả";
            case "Also list apps without an icon": return "Hiện cả ứng dụng không có biểu tượng";
            case "Clear search": return "Xóa tìm kiếm";
            case "No apps match \"": return "Không có ứng dụng khớp với \"";
            case "No apps": return "Không có ứng dụng";
            case "Off": return "Tắt";
            case "On": return "Bật";
            case "Search apps": return "Tìm ứng dụng";
            case "System apps": return "Ứng dụng hệ thống";
            case "Turn OFF the apps you don't want cast. Plain chat texts are already filtered out, so a messenger's calls still show even while its messages don't.": return "Tắt các ứng dụng bạn không muốn đưa lên Island. Chế độ chỉ hiện hoạt động vẫn có thể hiển thị cuộc gọi mà bỏ qua tin nhắn thường.";
            case "Cast": return "Đã chuyển tiếp";
            case "Connected ": return "Đã kết nối ";
            case "Couldn't rebind. Reboot, or toggle notification access off and on.": return "Không thể kết nối lại. Hãy khởi động lại máy hoặc tắt rồi bật quyền truy cập thông báo.";
            case "Last event ": return "Hoạt động gần nhất ";
            case "Listener is already connected.": return "Trình lắng nghe đã kết nối.";
            case "Not listening": return "Chưa kết nối";
            case "Notification access isn't granted — turn it on in Settings first.": return "Chưa cấp quyền truy cập thông báo. Hãy bật quyền này trong Cài đặt.";
            case "Rebinding listener… give it a few seconds.": return "Đang kết nối lại trình lắng nghe…";
            case "Reconnect": return "Kết nối lại";
            case "Shown": return "Đã hiện";
            case "Skipped": return "Đã bỏ qua";
            case "Allowed": return "Đã cho phép";
            case "Battery": return "Pin";
            case "Casting may not work": return "Island có thể chưa hoạt động";
            case "Done": return "Xong";
            case "Granted": return "Đã cấp";
            case "Enable \"Origin Isle keep-alive\" under Accessibility. No status-bar icon; it reads nothing.": return "Bật dịch vụ duy trì chạy nền trong mục Trợ năng. Dịch vụ này không đọc nội dung màn hình.";
            case "Keep-alive": return "Duy trì chạy nền";
            case "Not allowed": return "Chưa cho phép";
            case "Not confirmed": return "Chưa xác nhận";
            case "Not granted — required": return "Chưa cấp quyền bắt buộc";
            case "Notification access": return "Truy cập thông báo";
            case "Off — slower to recover after a kill": return "Đang tắt. Kết nối lại có thể chậm hơn.";
            case "On — reconnects casting after a kill": return "Đang bật. Có thể tự kết nối lại khi tiến trình bị dừng.";
            case "Opened — can't be verified, check it's still on": return "Đã mở cài đặt. Hãy kiểm tra tùy chọn vẫn đang bật.";
            case "Redo first-run setup": return "Thiết lập lại";
            case "Restricted — OriginOS may kill it": return "Đang bị giới hạn. OriginOS có thể dừng ứng dụng.";
            case "Setup needs attention": return "Cần kiểm tra thiết lập";
            case "Turn BOTH on, or closing the app from recents kills casting until you reboot.": return "Bật cả hai tùy chọn để duy trì Island khi đóng ứng dụng từ màn hình gần đây.";
            case "Unrestricted": return "Không giới hạn";
            case "Listening": return "Đang lắng nghe";
            case "Connected": return "Đã kết nối";
            case "Checking…": return "Đang kiểm tra…";
            case "Update available: v": return "Có bản mới: v";
            case " — opening GitHub…": return " · Đang mở GitHub…";
            case "No activity yet": return "Chưa có hoạt động";
            case "Clear activity": return "Xóa nhật ký";
            case "Cancel": return "Hủy";
            case "Dismiss": return "Đóng";
            default: return text;
        }
    }

    private static void showSupport(Context context) {
        Activity activity = LtmPanels.activity(context);
        if (activity == null) return;
        LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity, tr("♡ Ủng hộ tác giả"), "LTMori");
        sheet.card(tr("Ủng hộ qua Techcombank"), "Tran Minh Loc\n4444444790");
        sheet.button(tr("Đóng"), true, sheet.dialog::dismiss);
        sheet.show(48);
    }

    private static void showPrivacy(Context context) {
        Activity activity = LtmPanels.activity(context);
        if (activity == null) return;
        LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity, tr("Chính sách quyền riêng tư"), "LTMori");
        LtmPanels.policy(sheet);
        sheet.card("Thẻ chờ", "Tiêu đề, nội dung và sticker đã chọn được lưu trên thiết bị. Thẻ chờ có thể xuất hiện trên màn hình khóa.");
        sheet.button(tr("Đóng"), true, sheet.dialog::dismiss);
        sheet.show(78);
    }

    private static boolean isIdleActive() {
        return PlaygroundService.Companion.getActiveCardIds().contains(IDLE_ID);
    }

    private static void showIdle(Context context) {
        new IdleDot().invoke(context);
    }

    private static SharedPreferences idlePrefs(Context context) {
        return context.getSharedPreferences("ltmori_idle_style", Context.MODE_PRIVATE);
    }

    private static void customize(Context context) {
        Activity activity = LtmPanels.activity(context);
        if (activity == null) return;
        SharedPreferences prefs = idlePrefs(context);
        LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity, tr("Tùy chỉnh thẻ chờ"), tr("Nội dung có thể hiện trên màn hình khóa"));
        EditText title = editor(sheet, "Tiêu đề", prefs.getString("title", "LTMori"), 32);
        EditText body = editor(sheet, "Nội dung", prefs.getString("body", tr("Đang chờ")), 64);
        sheet.body.addView(label(activity, "Sticker", 15, Color.WHITE));
        Spinner stickers = new Spinner(activity);
        String[] choices = STICKERS.clone();
        choices[0] = tr("Mặc định");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stickers.setAdapter(adapter);
        stickers.setSelection(stickerIndex(prefs));
        sheet.body.addView(stickers);
        sheet.button(tr("Xem trước"), false, () -> {
            LtmPanels.Sheet preview = new LtmPanels.Sheet(activity, tr("Xem trước"), tr("Bố cục tham khảo"));
            String sticker = STICKERS[stickers.getSelectedItemPosition()];
            preview.card(sticker + (sticker.isEmpty() ? "" : "  ") + value(title, "LTMori"), value(body, tr("Đang chờ")));
            preview.button(tr("Quay lại"), true, preview.dialog::dismiss);
            preview.show(46);
        });
        sheet.button(tr("Lưu"), true, () -> {
            prefs.edit().putString("title", value(title, "LTMori"))
                    .putString("body", value(body, tr("Đang chờ"))).putInt("sticker", stickers.getSelectedItemPosition()).apply();
            sheet.dialog.dismiss();
            if (isIdleActive()) showIdle(context);
        });
        sheet.button(tr("Khôi phục mặc định"), false, () -> {
            title.setText("LTMori");
            body.setText(tr("Đang chờ"));
            stickers.setSelection(0);
        });
        sheet.show(78);
    }

    private static EditText editor(LtmPanels.Sheet sheet, String label, String value, int limit) {
        sheet.body.addView(label(sheet.activity, label, 15, Color.WHITE));
        EditText editor = new EditText(sheet.activity);
        editor.setTextColor(Color.WHITE);
        editor.setSingleLine();
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        editor.setText(value);
        sheet.body.addView(editor);
        return editor;
    }

    private static String value(EditText editor, String fallback) {
        String value = editor.getText().toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private static int stickerIndex(SharedPreferences prefs) {
        int index = prefs.getInt("sticker", 0);
        return index >= 0 && index < STICKERS.length ? index : 0;
    }

    public static void styleIdle(Context context, Intent intent) {
        if (intent.getIntExtra("id", -1) != IDLE_ID) return;
        SharedPreferences prefs = idlePrefs(context);
        intent.putExtra("title", prefs.getString("title", "LTMori"));
        intent.putExtra("text", prefs.getString("body", prefs.getBoolean("english", false) ? "Idle" : "Đang chờ"));
        int index = stickerIndex(prefs);
        if (index == 0) {
            intent.removeExtra("oi_large_icon");
            return;
        }
        Bitmap image = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(66);
        new Canvas(image).drawText(STICKERS[index], 48, 48 - (paint.ascent() + paint.descent()) / 2, paint);
        intent.putExtra("oi_large_icon", Icon.createWithBitmap(image));
    }
}
