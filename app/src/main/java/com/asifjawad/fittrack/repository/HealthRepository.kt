package com.asifjawad.fittrack.repository

import com.asifjawad.fittrack.data.health.HealthConnectAvailability
import com.asifjawad.fittrack.data.health.HealthConnectDailySnapshot
import com.asifjawad.fittrack.data.health.HealthConnectManager
import com.asifjawad.fittrack.data.local.DailySummaryDao
import com.asifjawad.fittrack.data.local.HealthDailySummaryEntity
import com.asifjawad.fittrack.data.local.SyncStateDao
import com.asifjawad.fittrack.data.local.SyncStateEntity
import com.asifjawad.fittrack.domain.model.HealthDailySummary
import com.asifjawad.fittrack.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class HealthRepository(
    private val manager: HealthConnectManager,
    private val dailySummaryDao: DailySummaryDao,
    private val syncStateDao: SyncStateDao
) {
    fun availability(): HealthConnectAvailability = manager.availability()

    suspend fun grantedPermissions(): Set<String> = manager.grantedPermissions()

    fun observeDailySummary(date: LocalDate): Flow<HealthDailySummary?> {
        return dailySummaryDao.observeByDate(date.toEpochDay()).map { entity ->
            entity?.toDomain()
        }
    }

    fun observeSyncState(): Flow<SyncStatus?> {
        return syncStateDao.observeByKey(SYNC_KEY).map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun syncDate(date: LocalDate): Result<HealthDailySummary> {
        if (manager.availability() != HealthConnectAvailability.Available) {
            val status = SyncStatus(
                lastSyncEpochMillis = Instant.now().toEpochMilli(),
                status = "Health Connect unavailable",
                errorMessage = "Install or update Health Connect, then try again."
            )
            syncStateDao.upsert(status.toEntity())
            return Result.failure(IllegalStateException(status.errorMessage))
        }

        val granted = manager.grantedPermissions()
        if (!granted.containsAll(HealthConnectManager.PERMISSIONS)) {
            val missingCount = HealthConnectManager.PERMISSIONS.size - granted.size
            val status = SyncStatus(
                lastSyncEpochMillis = Instant.now().toEpochMilli(),
                status = "Permissions missing",
                errorMessage = "$missingCount Health Connect permissions still need approval."
            )
            syncStateDao.upsert(status.toEntity())
            return Result.failure(IllegalStateException(status.errorMessage))
        }

        return runCatching {
            val snapshot = manager.readDailySnapshot(date)
            val entity = snapshot.toEntity()
            dailySummaryDao.upsert(entity)
            syncStateDao.upsert(
                SyncStatus(
                    lastSyncEpochMillis = entity.lastSyncEpochMillis,
                    status = "Synced ${date}",
                    errorMessage = null
                ).toEntity()
            )
            entity.toDomain()
        }.onFailure { throwable ->
            syncStateDao.upsert(
                SyncStatus(
                    lastSyncEpochMillis = Instant.now().toEpochMilli(),
                    status = "Sync failed",
                    errorMessage = throwable.message ?: "Unknown Health Connect sync error."
                ).toEntity()
            )
        }
    }

    private fun HealthConnectDailySnapshot.toEntity(): HealthDailySummaryEntity {
        return HealthDailySummaryEntity(
            dateEpochDay = date.toEpochDay(),
            sourceLabel = "Health Connect",
            steps = steps,
            sleepMinutes = sleepMinutes,
            exerciseMinutes = exerciseMinutes,
            activeCalories = activeCalories,
            totalCaloriesBurned = totalCaloriesBurned,
            weightKg = weightKg,
            heightCm = heightCm,
            isPending = isPending,
            lastSyncEpochMillis = Instant.now().toEpochMilli()
        )
    }

    private fun HealthDailySummaryEntity.toDomain(): HealthDailySummary {
        return HealthDailySummary(
            date = LocalDate.ofEpochDay(dateEpochDay),
            sourceLabel = sourceLabel,
            steps = steps,
            sleepMinutes = sleepMinutes,
            exerciseMinutes = exerciseMinutes,
            activeCalories = activeCalories,
            totalCaloriesBurned = totalCaloriesBurned,
            weightKg = weightKg,
            heightCm = heightCm,
            isPending = isPending,
            lastSyncEpochMillis = lastSyncEpochMillis
        )
    }

    private fun SyncStateEntity.toDomain(): SyncStatus {
        return SyncStatus(
            lastSyncEpochMillis = lastSyncEpochMillis,
            status = status,
            errorMessage = errorMessage
        )
    }

    private fun SyncStatus.toEntity(): SyncStateEntity {
        return SyncStateEntity(
            key = SYNC_KEY,
            lastSyncEpochMillis = lastSyncEpochMillis,
            status = status,
            errorMessage = errorMessage
        )
    }

    companion object {
        private const val SYNC_KEY = "health_connect"
    }
}
