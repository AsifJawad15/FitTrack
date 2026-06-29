package com.asifjawad.fittrack.repository

import com.asifjawad.fittrack.data.local.UserProfileDao
import com.asifjawad.fittrack.data.local.UserProfileEntity
import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class ProfileRepository(
    private val dao: UserProfileDao
) {
    fun observeProfile(): Flow<UserProfile?> {
        return dao.observeProfile().map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        dao.upsert(
            UserProfileEntity(
                id = 1,
                age = profile.age,
                sex = profile.sex.name,
                heightCm = profile.heightCm,
                startWeightKg = profile.startWeightKg,
                targetWeightKg = profile.targetWeightKg,
                goalDateEpochDay = profile.goalDate.toEpochDay(),
                activityLevel = profile.activityLevel.name,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    private fun UserProfileEntity.toDomain(): UserProfile {
        return UserProfile(
            age = age,
            sex = Sex.valueOf(sex),
            heightCm = heightCm,
            startWeightKg = startWeightKg,
            targetWeightKg = targetWeightKg,
            goalDate = LocalDate.ofEpochDay(goalDateEpochDay),
            activityLevel = ActivityLevel.valueOf(activityLevel)
        )
    }
}
