package com.llsit.sipduddy.performance

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.llsit.sipduddy.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class AnimationFrameBenchmark {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun click(label: String) {
        repeat(12) {
            val nodes = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(label).orEmpty()
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                while (current != null) {
                    if (current.isClickable && current.isEnabled && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Thread.sleep(650)
                        return
                    }
                    current = current.parent
                }
            }
            Thread.sleep(200)
        }
        error("Unable to click: $label")
    }
    private fun scroll() {
        fun visit(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
            for (i in 0 until node.childCount) if (visit(node.getChild(i))) return true
            return false
        }
        visit(automation.rootInActiveWindow)
        Thread.sleep(500)
    }
    private fun measure(name: String, action: () -> Unit) {
        shell("dumpsys gfxinfo ${context.packageName} reset")
        action()
        Thread.sleep(500)
        val output = shell("dumpsys gfxinfo ${context.packageName} framestats")
        val prefix = InstrumentationRegistry.getArguments().getString("captureLabel") ?: "baseline"
        File(context.getExternalFilesDir(null), "$prefix-$name.txt").writeText(output)
    }
    @Test fun captureRealVsyncFrames() {
        check(context.packageName.endsWith(".benchmark"))
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(3_000)
        // Warm every destination before measuring; cold start is intentionally excluded.
        click("ประวัติ"); click("ตั้งค่า"); click("วันนี้")
        measure("mascot") { Thread.sleep(8_000) }
        measure("navigation") { repeat(8) { click("ประวัติ"); click("ตั้งค่า"); click("วันนี้") } }
        measure("add-edit-delete") {
            repeat(3) {
                scroll()
                click("บันทึกเครื่องดื่ม")
                click("เติมให้บัดดี้")
                scroll()
                click("แก้ไข")
                click("500 มล.")
                click("บันทึกการแก้ไข")
                click("ลบ")
                click("ลบรายการ")
            }
        }
    }
}
