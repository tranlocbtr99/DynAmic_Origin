package com.aikeybridge.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.aikeybridge.R
import com.aikeybridge.guard.BankGuard

/**
 * Quick Settings Tile cho Bank Guard — Bảo vệ ngân hàng.
 * Cho phép xem trạng thái và bật/tắt nhanh cơ chế né ngân hàng ngay trên trung tâm điều khiển.
 */
class BankGuardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        if (!BankGuard.hasUsageAccess(context)) {
            Toast.makeText(context, "Cần cấp quyền 'Truy cập dữ liệu sử dụng' để Bank Guard hoạt động", Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    val pi = android.app.PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE,
                    )
                    startActivityAndCollapse(pi)
                }.onFailure {
                    startActivityAndCollapse(intent)
                }
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        val newEnabled = !BankGuard.isEnabled(context)
        BankGuard.setEnabled(context, newEnabled)
        val msg = if (newEnabled) {
            "Bank Guard: Đã bật tự động bảo vệ khi mở app ngân hàng / VNeID"
        } else {
            "Bank Guard: Đã tắt bảo vệ ngân hàng"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val context = applicationContext
        val hasUsage = BankGuard.hasUsageAccess(context)
        val isEnabled = BankGuard.isEnabled(context)

        tile.label = "Bank Guard"

        if (!hasUsage) {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Chưa cấp quyền"
            }
        } else if (isEnabled) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (BankGuard.bankDisabledByUs) "Đang né ngân hàng" else "Đang bảo vệ"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Đang tắt"
            }
        }

        tile.icon = Icon.createWithResource(context, R.drawable.lc_accessibility)
        tile.updateTile()
    }
}

