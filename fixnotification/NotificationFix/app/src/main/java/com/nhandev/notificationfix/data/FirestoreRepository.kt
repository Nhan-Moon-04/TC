package com.nhandev.notificationfix.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nhandev.notificationfix.domain.model.AppStatus
import com.nhandev.notificationfix.domain.model.FixLogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu dữ liệu fix notification lên Firestore.
 *
 * Cấu trúc:
 * devices/{deviceId}/
 *   ├── deviceInfo   — thông tin máy
 *   └── fixedApps/{packageName} — trạng thái từng app đã fix
 */
@Singleton
class FirestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /** Android ID dùng làm device identifier (không cần login). */
    private val deviceId: String by lazy {
        @SuppressLint("HardwareIds")
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        id ?: "unknown_device"
    }

    private val deviceRef get() = db.collection("devices").document(deviceId)

    // ── Device Info ───────────────────────────────────────────────────────────

    /**
     * Ghi thông tin máy lên Firestore (merge — không overwrite toàn bộ).
     */
    suspend fun saveDeviceInfo(
        brand: String,
        model: String,
        hyperOs: String,
        androidVersion: String,
    ) {
        val data = mapOf(
            "brand"          to brand,
            "model"          to model,
            "hyperOs"        to hyperOs,
            "androidVersion" to androidVersion,
            "lastSeen"       to FieldValue.serverTimestamp(),
            "deviceId"       to deviceId,
        )
        deviceRef.set(data, SetOptions.merge()).await()
    }

    // ── Fixed Apps ────────────────────────────────────────────────────────────

    /**
     * Lưu trạng thái fix của 1 app lên Firestore.
     */
    suspend fun saveAppStatus(label: String, status: AppStatus) {
        val data = mapOf(
            "label"                  to label,
            "packageName"            to status.packageName,
            "wakeLockAllowed"        to status.wakeLockAllowed,
            "runInBackgroundAllowed" to status.runInBackgroundAllowed,
            "dozeWhitelisted"        to status.dozeWhitelisted,
            "notificationsEnabled"   to status.notificationsEnabled,
            "isFullyFixed"           to status.isFullyFixed,
            "updatedAt"              to FieldValue.serverTimestamp(),
        )
        deviceRef
            .collection("fixedApps")
            .document(status.packageName.replace(".", "_")) // Firestore không cho dùng "." trong doc ID
            .set(data, SetOptions.merge())
            .await()
    }

    /**
     * Lưu nhiều app status cùng lúc (batch write).
     */
    suspend fun saveAllAppStatuses(statuses: Map<String, Pair<String, AppStatus>>) {
        val batch = db.batch()
        statuses.forEach { (pkg, pair) ->
            val (label, status) = pair
            val docRef = deviceRef
                .collection("fixedApps")
                .document(pkg.replace(".", "_"))
            batch.set(docRef, mapOf(
                "label"                  to label,
                "packageName"            to status.packageName,
                "wakeLockAllowed"        to status.wakeLockAllowed,
                "runInBackgroundAllowed" to status.runInBackgroundAllowed,
                "dozeWhitelisted"        to status.dozeWhitelisted,
                "isFullyFixed"           to status.isFullyFixed,
                "updatedAt"              to FieldValue.serverTimestamp(),
            ), SetOptions.merge())
        }
        batch.commit().await()
    }

    // ── Fix Logs ──────────────────────────────────────────────────────────────

    /**
     * Ghi 1 log entry vào subcollection logs.
     */
    suspend fun saveFixLog(entry: FixLogEntry) {
        val data = mapOf(
            "packageName" to entry.packageName,
            "appLabel"    to entry.appLabel,
            "action"      to entry.action.name,
            "success"     to entry.success,
            "message"     to entry.message,
            "timestamp"   to FieldValue.serverTimestamp(),
        )
        deviceRef.collection("logs").add(data).await()
    }

    /**
     * Ghi nhiều log cùng lúc (sau 1 lần Fix All).
     */
    suspend fun saveFixLogs(entries: List<FixLogEntry>) {
        val batch = db.batch()
        entries.forEach { entry ->
            val ref = deviceRef.collection("logs").document()
            batch.set(ref, mapOf(
                "packageName" to entry.packageName,
                "appLabel"    to entry.appLabel,
                "action"      to entry.action.name,
                "success"     to entry.success,
                "message"     to entry.message,
                "timestamp"   to FieldValue.serverTimestamp(),
            ))
        }
        batch.commit().await()
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Đọc lại danh sách app đã fix từ Firestore (để restore trên máy mới).
     */
    suspend fun getFixedApps(): List<Map<String, Any>> {
        val snap = deviceRef.collection("fixedApps").get().await()
        return snap.documents.mapNotNull { it.data }
    }
}
