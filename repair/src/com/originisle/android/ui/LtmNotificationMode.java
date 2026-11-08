package com.originisle.android.ui;

import android.app.Activity;
import android.content.Context;
import java.util.Arrays;
import java.util.List;
import kotlin.jvm.functions.Function1;

public final class LtmNotificationMode {
    private static final String LIVE = "Đang diễn ra";
    private static final String ALL = "Tất cả thông báo";

    public static List<String> labels(boolean all) {
        return Arrays.asList((all ? "○ " : "✓ ") + LIVE, (all ? "✓ " : "○ ") + ALL);
    }

    public static Function1<Integer, Object> confirm(Context context, Function1<Integer, Object> apply) {
        return value -> {
            Activity activity = LtmPanels.activity(context);
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return null;
            boolean all = value == 1;
            LtmPanels.Sheet sheet = new LtmPanels.Sheet(activity,
                    all ? ALL : "Chỉ thông báo đang diễn ra", "Thông báo trên Island");
            sheet.card("Thông báo được đưa lên Island", all
                    ? "Bao gồm cả tin nhắn và thông báo thường. Bộ lọc ứng dụng và thông báo im lặng vẫn được áp dụng."
                    : "Ưu tiên cuộc gọi, điều hướng và tiến trình tải. Tin nhắn thường không được đưa lên Island trong chế độ này.");
            sheet.button("Áp dụng", true, () -> {
                apply.invoke(value);
                sheet.dialog.dismiss();
            });
            sheet.button("Hủy", false, sheet.dialog::dismiss);
            sheet.show(52);
            return null;
        };
    }
}
