package vn.loctm.originbridge

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Records only local action labels, not notification contents or originating app identities. */
object Diagnostics {
    private const val FILE = "diagnostics.txt"
    fun record(context: Context, event: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        context.openFileOutput(FILE, Context.MODE_APPEND).bufferedWriter().use {
            it.appendLine("$stamp | $event")
        }
    }
    fun report(context: Context): String = try {
        context.openFileInput(FILE).bufferedReader().use { it.readText() }
    } catch (_: Exception) { "Chưa có sự kiện thử nghiệm." }
}
