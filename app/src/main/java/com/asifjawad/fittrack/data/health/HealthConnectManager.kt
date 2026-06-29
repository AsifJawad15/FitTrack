package com.asifjawad.fittrack.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(
    private val context: Context
) {
    private val zoneId = ZoneId.systemDefault()

    fun availability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                HealthConnectAvailability.ProviderUpdateRequired
            }
            else -> HealthConnectAvailability.Unavailable
        }
    }

    suspend fun grantedPermissions(): Set<String> {
        if (availability() != HealthConnectAvailability.Available) return emptySet()
        return HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
    }

    suspend fun hasAllPermissions(): Boolean {
        return grantedPermissions().containsAll(PERMISSIONS)
    }

    suspend fun readDailySnapshot(date: LocalDate): HealthConnectDailySnapshot {
        val client = HealthConnectClient.getOrCreate(context)
        val start = date.atStartOfDay(zoneId).toInstant()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val filter = TimeRangeFilter.between(start, end)

        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL
                ),
                timeRangeFilter = filter
            )
        )

        val sleepRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = filter
            )
        ).records
        val exerciseRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = filter
            )
        ).records
        val weightRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = filter
            )
        ).records
        val heightRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = HeightRecord::class,
                timeRangeFilter = filter
            )
        ).records

        return HealthConnectDailySnapshot(
            date = date,
            steps = aggregate[StepsRecord.COUNT_TOTAL],
            sleepMinutes = sleepRecords.sumOf { record ->
                Duration.between(record.startTime, record.endTime).toMinutes()
            },
            exerciseMinutes = exerciseRecords.sumOf { record ->
                Duration.between(record.startTime, record.endTime).toMinutes()
            },
            activeCalories = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories,
            totalCaloriesBurned = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories,
            weightKg = weightRecords.maxByOrNull { it.time }?.weight?.inKilograms,
            heightCm = heightRecords.maxByOrNull { it.time }?.height?.inMeters?.times(100.0),
            isPending = false
        )
    }

    companion object {
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class)
        )
    }
}

enum class HealthConnectAvailability(val label: String) {
    Available("Available"),
    ProviderUpdateRequired("Update required"),
    Unavailable("Unavailable")
}

data class HealthConnectDailySnapshot(
    val date: LocalDate,
    val steps: Long?,
    val sleepMinutes: Long?,
    val exerciseMinutes: Long?,
    val activeCalories: Double?,
    val totalCaloriesBurned: Double?,
    val weightKg: Double?,
    val heightCm: Double?,
    val isPending: Boolean
)
