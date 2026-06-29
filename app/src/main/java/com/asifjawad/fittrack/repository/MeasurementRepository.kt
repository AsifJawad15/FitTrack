package com.asifjawad.fittrack.repository

import com.asifjawad.fittrack.data.local.WaistLogDao
import com.asifjawad.fittrack.data.local.WaistLogEntity
import com.asifjawad.fittrack.data.local.WeightLogDao
import com.asifjawad.fittrack.data.local.WeightLogEntity
import com.asifjawad.fittrack.domain.model.WaistLog
import com.asifjawad.fittrack.domain.model.WeightLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class MeasurementRepository(
    private val weightDao: WeightLogDao,
    private val waistDao: WaistLogDao
) {
    fun observeWeightLogs(): Flow<List<WeightLog>> {
        return weightDao.observeAll().map { logs ->
            logs.map { entity ->
                WeightLog(
                    id = entity.id,
                    date = LocalDate.ofEpochDay(entity.dateEpochDay),
                    weightKg = entity.weightKg,
                    note = entity.note
                )
            }
        }
    }

    fun observeWaistLogs(): Flow<List<WaistLog>> {
        return waistDao.observeAll().map { logs ->
            logs.map { entity ->
                WaistLog(
                    id = entity.id,
                    date = LocalDate.ofEpochDay(entity.dateEpochDay),
                    waistCm = entity.waistCm,
                    note = entity.note
                )
            }
        }
    }

    suspend fun addWeightLog(date: LocalDate, weightKg: Double, note: String? = null) {
        weightDao.insert(
            WeightLogEntity(
                dateEpochDay = date.toEpochDay(),
                weightKg = weightKg,
                note = note,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun addWaistLog(date: LocalDate, waistCm: Double, note: String? = null) {
        waistDao.insert(
            WaistLogEntity(
                dateEpochDay = date.toEpochDay(),
                waistCm = waistCm,
                note = note,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }
}
