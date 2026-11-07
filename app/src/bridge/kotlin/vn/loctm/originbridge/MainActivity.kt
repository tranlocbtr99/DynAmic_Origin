package vn.loctm.originbridge

import android.Manifest
import android.app.Activity
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.widget.Button
import android.widget.CheckBox
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private var revision = 0
    private var pulse: ObjectAnimator? = null
    private val native get() = packageName == BridgeListener.NATIVE_PACKAGE
    private val policy = """
        1. Phạm vi và mục đích
        LTMori xử lý thông báo trên thiết bị để hiển thị thẻ SuperX của OriginOS. Quyền truy cập thông báo chỉ cần cho tính năng chuyển thẻ và phải được cấp trong Cài đặt Android.

        2. Dữ liệu được xử lý
        Khi bật chuyển thông báo, app có thể đọc tên ứng dụng, tiêu đề, nội dung, biểu tượng, tiến độ và hành động của thông báo. App dùng các dữ liệu này để dựng thẻ và chuyển dữ liệu thẻ đến hệ thống OriginOS trên cùng thiết bị. Bộ lọc ứng dụng cho phép chọn nguồn hiển thị.

        3. Lưu trữ và chia sẻ
        LTMori lưu lựa chọn, trạng thái đồng ý và mã chẩn đoán cục bộ. App không duy trì lịch sử nội dung tin nhắn, không yêu cầu tài khoản, không khai báo quyền Internet và không gửi dữ liệu đến máy chủ của tác giả. Hệ điều hành có thể quản lý thẻ như một thông báo hệ thống.

        4. Màn hình khóa
        Tùy chọn thẻ màn hình khóa mặc định tắt. Khi bật, LTMori dùng tên ứng dụng và thông báo chung, bỏ nội dung riêng tư và hành động. App bỏ qua thông báo nguồn được đánh dấu bí mật.

        5. Quyền kiểm soát
        Bạn có thể tắt chuyển thông báo, thu hồi quyền trong Cài đặt Android, xóa dữ liệu ứng dụng hoặc chọn Rút lại đồng ý trong LTMori. Khi rút lại, app dừng chuyển tiếp, gỡ thẻ của mình và xóa nhật ký chẩn đoán. Quyền Android có thể được thu hồi riêng trong Cài đặt.

        6. Liên hệ
        Designed by LocTM. Liên hệ về dữ liệu và quyền riêng tư qua tranlocbtr99@gmail.com.

        Kết thúc chính sách · Phiên bản 1
    """.trimIndent()
    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun label(value: String, size: Float = 15f, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }
    private fun action(title: String, description: String, click: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply { setColor(0xff242b3d.toInt()); cornerRadius = dp(20).toFloat() }
            addView(label(title, 18f, true))
            addView(label(description, 13f).apply { setTextColor(0xffbdc7d8.toInt()) })
            isFocusable = true
            setOnClickListener {
                if (native && ValueAnimator.areAnimatorsEnabled()) {
                    animate().scaleX(.98f).scaleY(.98f).setDuration(85).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(125).start()
                        click()
                    }.start()
                } else click()
            }
        }
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(0xff101522.toInt()) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(32))
        }
        scroll.addView(root)
        if (native && !BridgeListener.accepted(this)) {
            setContentView(scroll)
            showWelcome()
            return
        }
        if (native) showPreview(animated = false)
        root.addView(label(applicationInfo.loadLabel(packageManager).toString(), 30f, true))
        root.addView(label("OriginOS 6 · Đảo thông báo của hệ thống"))
        action("Cấp quyền thông báo", "Cần để gửi thẻ chẩn đoán") {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
            else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        if (packageName == BridgeListener.NATIVE_PACKAGE) {
            showNativeSettings()
            setContentView(scroll)
            return
        }
        action("Gửi thẻ thử nghiệm", "Gửi thông báo với dữ liệu OEM thử nghiệm") { attempt(false) }
        action("Cập nhật thẻ", "Giữ cùng notification ID, thay nội dung để kiểm tra trùng lặp") { attempt(true) }
        action("Gỡ thẻ", "Hủy thẻ đã gửi, không ảnh hưởng thông báo ứng dụng khác") {
            NativeProbe.clear(this)
            Toast.makeText(this, "Đã yêu cầu gỡ thẻ", Toast.LENGTH_SHORT).show()
        }
        action("Ma trận giao thức · 5 phép thử", "Chọn từng cấu hình để kiểm tra Origin Island gốc") {
            val names = ProtocolMatrix.cases.map { it.id }.toTypedArray()
            android.app.AlertDialog.Builder(this).setTitle("Chọn phép thử OEM")
                .setItems(names) { _, index ->
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
                    } else {
                        val result = ProtocolMatrix.post(this, ProtocolMatrix.cases[index])
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    }
                }.setNegativeButton("Đóng", null).show()
        }
        action("Ghi kết quả quan sát", "Lưu kết quả thực tế sau khi thử trên máy") {
            val cases = ProtocolMatrix.cases.map { it.id }.toTypedArray()
            android.app.AlertDialog.Builder(this).setTitle("Chọn phép thử đã quan sát")
                .setItems(cases) { _, index ->
                    val chosen = ProtocolMatrix.cases[index].id
                    val outcomes = arrayOf("Đảo gốc xuất hiện", "Chỉ khay thông báo", "Không hiển thị", "Có lỗi / khác")
                    android.app.AlertDialog.Builder(this).setTitle("Kết quả: $chosen")
                        .setItems(outcomes) { _, outcome ->
                            val code = arrayOf("NATIVE_VISIBLE", "SHADE_ONLY", "NOT_VISIBLE", "OTHER")[outcome]
                            Diagnostics.record(this, "MATRIX_RESULT_${chosen}_${code}")
                            Toast.makeText(this, "Đã lưu kết quả quan sát", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("Hủy", null).show()
                }.setNegativeButton("Đóng", null).show()
        }
        action("Gỡ thẻ ma trận", "Kết thúc phép thử và hủy thông báo ma trận") {
            ProtocolMatrix.clear(this)
            Toast.makeText(this, "Đã yêu cầu gỡ thẻ ma trận", Toast.LENGTH_SHORT).show()
        }
        action("Kiểm tra hỗ trợ OEM", "Xem quyền thông báo, scene và API OriginOS") {
            android.app.AlertDialog.Builder(this).setTitle("Chẩn đoán OriginOS")
                .setMessage(NativeProbe.environmentReport(this))
                .setPositiveButton("Đóng", null).show()
        }
        action("ADB / Shizuku · hướng dẫn chẩn đoán", "Các lệnh chỉ đọc, không thay đổi quyền hoặc cấu hình hệ thống") {
            val commands = """
                adb shell getprop ro.vivo.os.version
                adb shell getprop ro.build.version.release
                adb shell dumpsys notification
                adb shell dumpsys package $packageName
                adb shell pm list packages | grep -iE 'vivo|superx|island'
            """.trimIndent()
            android.app.AlertDialog.Builder(this)
                .setTitle("Chẩn đoán ADB (chỉ đọc)")
                .setMessage("Chạy từ máy tính có ADB. Shizuku đang chạy không tự cấp quyền shell cho ứng dụng này.\\n\\n" +
                    commands + "\\n\\nLưu ý: dumpsys notification có thể chứa nội dung thông báo cá nhân; hãy xóa dữ liệu riêng tư trước khi chia sẻ.")
                .setPositiveButton("Sao chép") { _, _ ->
                    val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB diagnostics", commands))
                }
                .setNegativeButton("Đóng", null).show()
        }
        action("Xem nhật ký cục bộ", "Chỉ ghi thao tác, không ghi nội dung thông báo") {
            android.app.AlertDialog.Builder(this).setTitle("Nhật ký thử nghiệm")
                .setMessage(Diagnostics.report(this)).setPositiveButton("Đóng", null).show()
        }
        action("Xuất nhật ký", "Chia sẻ bản ghi thao tác qua bảng chia sẻ Android") {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, Diagnostics.report(this@MainActivity))
            }
            startActivity(Intent.createChooser(intent, "Xuất nhật ký"))
        }
        root.addView(label("KẾT QUẢ NGHIỆM THU\nChưa kiểm chứng trên thiết bị. Việc gửi thông báo thành công không chứng minh SystemUI đã hiển thị Origin Island gốc.", 13f))
        setContentView(scroll)
    }
    private fun showPreview(animated: Boolean) {
        val preview = label(if (animated) "•••" else "●   LTMori", 16f, true).apply {
            gravity = android.view.Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), 0xff3e526d.toInt())
            }
        }
        root.addView(preview, LinearLayout.LayoutParams(dp(144), dp(48)).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(18)
        })
        if (animated && ValueAnimator.areAnimatorsEnabled()) {
            pulse = ObjectAnimator.ofFloat(preview, "alpha", .42f, 1f).apply {
                duration = 700
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }
    }

    private fun showWelcome() {
        showPreview(animated = true)
        root.addView(label("Chào mừng đến với LTMori", 30f, true))
        root.addView(label("Đảo thông báo cho OriginOS · Designed by LocTM"))
        val consent = CheckBox(this).apply {
            text = "Tôi đã đọc và đồng ý với chính sách bảo mật"
            isEnabled = false
        }
        val start = Button(this).apply {
            text = "Bắt đầu sử dụng"
            isEnabled = false
            setOnClickListener {
                if (consent.isEnabled && consent.isChecked) {
                    BridgeListener.accept(this@MainActivity)
                    recreate()
                }
            }
        }
        action("Đọc chính sách bảo mật", "Cuộn đến cuối chính sách để mở ô đồng ý") {
            showPolicy {
                consent.isEnabled = true
                consent.requestFocus()
            }
        }
        consent.setOnCheckedChangeListener { _, checked -> start.isEnabled = checked && consent.isEnabled }
        root.addView(consent)
        root.addView(start)
    }

    private fun showPolicy(onRead: (() -> Unit)? = null) {
        val body = ScrollView(this)
        body.addView(label(policy, 16f).apply {
            setTextColor(0xffe4edfa.toInt())
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setLineSpacing(dp(4).toFloat(), 1f)
        })
        val dialog = android.app.AlertDialog.Builder(this).setTitle("Chính sách bảo mật")
            .setView(body)
            .setPositiveButton(if (onRead == null) "Đóng" else "Đã đọc hết") { _, _ -> onRead?.invoke() }
            .show()
        if (onRead != null) {
            val done = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            done.isEnabled = false
            val enableAtEnd = {
                if (body.height > 0 && !body.canScrollVertically(1)) done.isEnabled = true
            }
            body.setOnScrollChangeListener { _, _, _, _, _ -> enableAtEnd() }
            body.post { enableAtEnd() }
        }
    }

    override fun onDestroy() {
        pulse?.cancel()
        super.onDestroy()
    }

    private fun showNativeSettings() {
        action("Quyền đọc thông báo", BridgeListener.connectionStatus(this)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        val idle = BridgeListener.idleEnabled(this)
        action(if (idle) "Tắt đảo chờ" else "Bật đảo chờ",
            "Giữ thẻ LTMori cạnh camera khi rảnh. Cần quyền gửi và đọc thông báo.") {
            if (idle || canPost()) {
                BridgeListener.setIdle(this, !idle)
                recreate()
            }
        }
        val enabled = BridgeListener.enabled(this)
        action(if (enabled) "Tắt chuyển thông báo" else "Bật chuyển thông báo",
            if (enabled) "Đang bật · Nhận thông báo mới và tiến độ" else "Đang tắt · Đảo chờ vẫn có thể hiện") {
            if (enabled || canPost()) {
                BridgeListener.setEnabled(this, !enabled)
                recreate()
            }
        }
        action("Lọc ứng dụng", "Chọn ứng dụng được chuyển lên đảo") {
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = packageManager.queryIntentActivities(launcher, 0)
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != packageName }
                .sortedBy { it.loadLabel(packageManager).toString() }
            val checked = apps.map { BridgeListener.appEnabled(this, it.activityInfo.packageName) }.toBooleanArray()
            android.app.AlertDialog.Builder(this).setTitle("Ứng dụng được phép")
                .setMultiChoiceItems(apps.map { it.loadLabel(packageManager) }.toTypedArray(), checked) { _, index, value ->
                    checked[index] = value
                }
                .setPositiveButton("Lưu") { _, _ ->
                    BridgeListener.setExcludedApps(this, apps.filterIndexed { index, _ -> !checked[index] }
                        .map { it.activityInfo.packageName }.toSet())
                }
                .setNegativeButton("Hủy", null).show()
        }
        val lockscreen = BridgeListener.lockscreen(this)
        action(if (lockscreen) "Tắt thẻ trên màn hình khóa" else "Bật thẻ trên màn hình khóa",
            "Khi bật, thẻ chỉ hiển thị tên ứng dụng để bảo vệ nội dung") {
            BridgeListener.setLockscreen(this, !lockscreen)
            recreate()
        }
        action("Gửi hoặc cập nhật thẻ mẫu", "Dùng chung bộ gửi với thông báo thật") {
            if (canPost()) {
                revision++
                val open = android.app.PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE)
                val card = SuperX.Card(
                    "LTMori", "Thẻ thử $revision", "LTMori",
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_dialog_info),
                    open
                )
                val result = runCatching {
                    SuperX.post(this, BridgeListener.SAMPLE_ID, revision, card, false)
                }.getOrElse { "POST_ERROR_" + it.javaClass.simpleName }
                Diagnostics.record(this, result)
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
            }
        }
        action("Kết thúc thẻ mẫu", "Gửi lệnh kết thúc SuperX") {
            runCatching { SuperX.end(this, BridgeListener.SAMPLE_ID) }
        }
        action("Tuyên bố bảo mật dữ liệu", "Dữ liệu xử lý trên thiết bị · Xem lại chính sách") {
            showPolicy()
        }
        action("Rút lại đồng ý", "Dừng chuyển tiếp và xóa nhật ký chẩn đoán") {
            android.app.AlertDialog.Builder(this).setTitle("Rút lại đồng ý?")
                .setPositiveButton("Rút lại") { _, _ ->
                    BridgeListener.withdraw(this)
                    recreate()
                }.setNegativeButton("Hủy", null).show()
        }
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        action("Designed by LocTM", "LTMori · Phiên bản $version · Ủng hộ tác giả") {
            val number = "4444444790"
            android.app.AlertDialog.Builder(this).setTitle("Ủng hộ tác giả")
                .setMessage("Nếu LTMori hữu ích, bạn có thể ủng hộ qua Techcombank.\n\nTran Minh Loc\n$number")
                .setPositiveButton("Sao chép số tài khoản") { _, _ ->
                    getSystemService(android.content.ClipboardManager::class.java)
                        ?.setPrimaryClip(android.content.ClipData.newPlainText("Techcombank", number))
                }.setNegativeButton("Đóng", null).show()
        }
        action("Xem nhật ký", "Kiểm tra lỗi quyền, kênh và phản hồi từ OriginOS") {
            android.app.AlertDialog.Builder(this).setTitle("Nhật ký SuperX")
                .setMessage(Diagnostics.report(this)).setPositiveButton("Đóng", null).show()
        }
    }

    private fun canPost(): Boolean {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
        return false
    }

    private fun attempt(update: Boolean) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
            return
        }
        revision = if (update) (revision + 1).coerceAtLeast(2) else 1
        NativeProbe.send(this, revision)
        Toast.makeText(this, "Đã gửi yêu cầu; cần quan sát trên thiết bị", Toast.LENGTH_LONG).show()
    }
}
