package com.originisle.android.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.res.ColorStateList;

public final class LtmPanels {
    private static final int PANEL = 0xff1d222b;
    private static final int CARD = 0xff282f3a;
    private static final int MUTED = 0xffbdc9db;
    private static final int BLUE = 0xff1673da;

    static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return null;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable rounded(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        return shape;
    }

    static final class Sheet {
        final Activity activity;
        final Dialog dialog;
        final LinearLayout root;
        final ScrollView scroll;
        final LinearLayout body;
        final LinearLayout actions;

        Sheet(Activity activity, String title, String subtitle) {
            this.activity = activity;
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            root = column(activity);
            int pad = dp(activity, 18);
            root.setPadding(pad, pad, pad, pad);
            root.setBackground(rounded(activity, PANEL, 28));
            root.addView(text(title, 24, Color.WHITE));
            root.addView(text(subtitle, 14, MUTED));
            scroll = new ScrollView(activity);
            scroll.setFillViewport(false);
            body = column(activity);
            scroll.addView(body);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
            scrollParams.topMargin = dp(activity, 10);
            root.addView(scroll, scrollParams);
            actions = column(activity);
            root.addView(actions, new LinearLayout.LayoutParams(-1, -2));
            dialog.setContentView(root);
        }

        TextView text(String value, int size, int color) {
            TextView view = new TextView(activity);
            view.setText(value);
            view.setTextSize(size);
            view.setTextColor(color);
            view.setLineSpacing(dp(activity, 3), 1);
            return view;
        }

        void card(String title, String description) {
            LinearLayout card = column(activity);
            int pad = dp(activity, 13);
            card.setPadding(pad, pad, pad, pad);
            card.setBackground(rounded(activity, CARD, 17));
            card.addView(text(title, 16, Color.WHITE));
            card.addView(text(description, 14, MUTED));
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
            layout.bottomMargin = dp(activity, 8);
            body.addView(card, layout);
        }

        Button button(String label, boolean primary, Runnable onClick) {
            Button button = new Button(activity);
            button.setText(label);
            button.setAllCaps(false);
            button.setTextColor(Color.WHITE);
            button.setBackgroundTintList(null);
            button.setBackground(rounded(activity, primary ? BLUE : CARD, 18));
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
            layout.topMargin = dp(activity, 6);
            actions.addView(button, layout);
            button.setOnClickListener(view -> onClick.run());
            return button;
        }

        void show(int heightPercent) {
            dialog.show();
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int width = activity.getWindow().getDecorView().getWidth();
            int height = activity.getWindow().getDecorView().getHeight();
            if (width <= 0 || height <= 0) {
                width = activity.getResources().getDisplayMetrics().widthPixels;
                height = activity.getResources().getDisplayMetrics().heightPixels;
            }
            window.setLayout(Math.min(dp(activity, 430), width - dp(activity, 30)),
                    height * heightPercent / 100);
        }
    }

    private static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private static void policy(Sheet sheet) {
        sheet.card("Thông báo và đa phương tiện",
                "Khi được cấp quyền và bật tính năng, LTMori đọc nội dung và thao tác của thông báo hoặc phiên phát để hiển thị trên Island. Nhật ký có thể chứa tên ứng dụng và tiêu đề thông báo. Việc xử lý diễn ra trên thiết bị.");
        sheet.card("Ứng dụng gần đây",
                "Nếu cấp quyền truy cập dữ liệu sử dụng, LTMori đọc lịch sử mở app khi bạn chạm chấm xanh. Danh sách này không được lưu thêm.");
        sheet.card("Kết nối mạng",
                "GitHub được dùng để kiểm tra và tải cập nhật. Thẻ thể thao có thể gửi tên đội để tra cứu và tải biểu trưng từ TheSportsDB. LTMori không gửi toàn bộ thông báo hay danh sách ứng dụng gần đây đến hai dịch vụ này.");
        sheet.card("Quyền kiểm soát",
                "Bạn có thể tắt tính năng, xóa Nhật ký hoặc thu hồi quyền trong Cài đặt Android. Nội dung hiển thị trên màn hình có thể được người khác nhìn thấy.");
        sheet.card("Liên hệ", "tranlocbtr99@gmail.com");
    }

    public static void welcome(Activity activity, android.content.DialogInterface.OnClickListener accept) {
        Sheet sheet = new Sheet(activity, "Chào mừng đến với LTMori", "Đọc hết chính sách để tiếp tục");
        sheet.dialog.setCancelable(false);
        policy(sheet);
        CheckBox consent = new CheckBox(activity);
        consent.setText("Tôi đã đọc và đồng ý");
        consent.setTextColor(Color.WHITE);
        consent.setButtonTintList(ColorStateList.valueOf(0xff70b4ff));
        consent.setEnabled(false);
        sheet.actions.addView(consent);
        Button start = sheet.button("Bắt đầu sử dụng", true, () -> {
            if (!consent.isEnabled() || !consent.isChecked()) return;
            sheet.dialog.dismiss();
            accept.onClick(null, -1);
        });
        start.setEnabled(false);
        start.setAlpha(0.45f);
        consent.setOnCheckedChangeListener((view, checked) -> {
            start.setEnabled(checked);
            start.setAlpha(checked ? 1 : 0.45f);
        });
        Runnable atEnd = () -> {
            if (sheet.scroll.getHeight() > 0 && !sheet.scroll.canScrollVertically(1))
                consent.setEnabled(true);
        };
        sheet.scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> atEnd.run());
        sheet.scroll.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> atEnd.run());
        sheet.button("Thoát", false, activity::finish);
        sheet.show(82);
    }

    public static void about(Context context, android.content.DialogInterface.OnClickListener autoStart) {
        Activity activity = activity(context);
        if (activity == null) return;
        Sheet sheet = new Sheet(activity, "Giới thiệu LTMori", "Designed by LocTM");
        sheet.card("Phiên bản", LtmUpgrade.version(activity));
        sheet.card("Ủng hộ tác giả", "Techcombank · Tran Minh Loc\n4444444790");
        sheet.card("Quyền riêng tư", "Thông báo và lịch sử mở ứng dụng được xử lý trên thiết bị. Bạn có thể thu hồi quyền trong Cài đặt Android.");
        sheet.card("Tự khởi động", "Bật Tự khởi động và Khởi động liên kết nếu OriginOS có cả hai tùy chọn.");
        sheet.card("Ghi công", "Phát triển dựa trên Origin Isle. LTMori là bản tùy chỉnh độc lập.");
        sheet.button("Chính sách quyền riêng tư", false, () -> {
            Sheet privacy = new Sheet(activity, "Chính sách quyền riêng tư", "LTMori");
            policy(privacy);
            privacy.button("Đóng", true, privacy.dialog::dismiss);
            privacy.show(78);
        });
        sheet.button("Cài đặt tự khởi động", false, () -> autoStart.onClick(null, -3));
        sheet.button("Đóng", true, sheet.dialog::dismiss);
        sheet.show(78);
    }
}
