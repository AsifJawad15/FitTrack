package com.asifjawad.fittrack.domain.model

import java.time.LocalDate

enum class Sex(val label: String) {
    Male("Male"),
    Female("Female")
}

enum class ActivityLevel(
    val label: String,
    val multiplier: Double,
    val description: String
) {
    Sedentary("Desk-based", 1.2, "Mostly sitting, light walking"),
    Light("Light training", 1.375, "Training 1-3 days per week"),
    Moderate("Moderate training", 1.55, "Training 3-5 days per week")
}

data class UserProfile(
    val age: Int,
    val sex: Sex,
    val heightCm: Double,
    val startWeightKg: Double,
    val targetWeightKg: Double,
    val goalDate: LocalDate,
    val activityLevel: ActivityLevel
)

data class UserProfileDraft(
    val age: String = "",
    val sex: Sex = Sex.Male,
    val heightCm: String = "",
    val startWeightKg: String = "",
    val targetWeightKg: String = "",
    val goalDate: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.Light
)
