package com.nhandev.notificationfix.domain

import android.content.pm.PackageManager
import com.nhandev.notificationfix.domain.model.PrivilegeMode
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes shell commands via Shizuku (no root needed — uses ADB privilege).
 *
 * Requirements:
 * - Shizuku app installed & running
 * - Permission granted in Shizuku app
 */
@Singleton
class ShizukuHelper @Inject constructor() {

    /** True if Shizuku service is alive. */
    val isAvailable: Boolean
        get() = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    /** True if this app has Shizuku permission. */
    val hasPermission: Boolean
        get() = try {
            isAvailable && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }

    /** Request Shizuku permission — result comes back via onRequestPermissionsResult. */
    fun requestPermission(requestCode: Int = 1001) {
        if (isAvailable) Shizuku.requestPermission(requestCode)
    }

    /**
     * Run a shell command and return stdout.
     * Throws if not available/permitted.
     */
    fun runCommand(command: String): ShellResult {
        if (!hasPermission) return ShellResult(false, "", "Shizuku permission not granted")
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, "/") as Process
            val stdout  = process.inputStream.bufferedReader().readText().trim()
            val stderr  = process.errorStream.bufferedReader().readText().trim()
            val code    = process.waitFor()
            ShellResult(success = code == 0, output = stdout, error = stderr)
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "Unknown error")
        }
    }
}

/**
 * Executes shell commands via root (Magisk / KernelSU).
 * Uses libsu under the hood.
 */
@Singleton
class RootHelper @Inject constructor() {

    val isAvailable: Boolean
        get() = try {
            com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true
        } catch (e: Exception) { false }

    fun runCommand(command: String): ShellResult {
        return try {
            val result = com.topjohnwu.superuser.Shell.cmd(command).exec()
            ShellResult(
                success = result.isSuccess,
                output  = result.out.joinToString("\n").trim(),
                error   = result.err.joinToString("\n").trim(),
            )
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "Unknown error")
        }
    }
}

/** Unified result from a shell command. */
data class ShellResult(
    val success: Boolean,
    val output: String,
    val error: String = "",
)

/**
 * Detects which privilege mode is currently available.
 */
@Singleton
class PrivilegeDetector @Inject constructor(
    private val shizuku: ShizukuHelper,
    private val root: RootHelper,
) {
    fun detect(): PrivilegeMode = when {
        shizuku.hasPermission -> PrivilegeMode.SHIZUKU
        root.isAvailable      -> PrivilegeMode.ROOT
        else                  -> PrivilegeMode.NONE
    }
}
