package com.asifjawad.fittrack.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import com.asifjawad.fittrack.domain.model.UserProfileDraft
import java.time.LocalDate
import java.time.format.DateTimeParseException

class FitTrackViewModel : ViewModel() {
    var uiState by mutableStateOf(FitTrackUiState())
        private set

    fun selectTab(tab: FitTrackTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun updateDraft(transform: UserProfileDraft.() -> UserProfileDraft) {
        uiState = uiState.copy(
            profileDraft = uiState.profileDraft.transform(),
            profileError = null,
            profileSavedMessage = null
        )
    }

    fun saveProfile() {
        val draft = uiState.profileDraft
        val age = draft.age.toIntOrNull()
        val height = draft.heightCm.toDoubleOrNull()
        val startWeight = draft.startWeightKg.toDoubleOrNull()
        val targetWeight = draft.targetWeightKg.toDoubleOrNull()
        val goalDate = try {
            LocalDate.parse(draft.goalDate)
        } catch (_: DateTimeParseException) {
            null
        }

        val error = when {
            age == null || age !in 13..100 -> "Enter an age between 13 and 100."
            height == null || height !in 120.0..230.0 -> "Enter height in cm, for example 180."
            startWeight == null || startWeight !in 35.0..250.0 -> "Enter a realistic start weight in kg."
            targetWeight == null || targetWeight !in 35.0..250.0 -> "Enter a realistic target weight in kg."
            targetWeight >= startWeight -> "Target weight should be lower than start weight for this goal."
            goalDate == null -> "Enter goal date as YYYY-MM-DD."
            goalDate <= LocalDate.now() -> "Goal date must be in the future."
            else -> null
        }

        if (error != null) {
            uiState = uiState.copy(profileError = error)
            return
        }

        uiState = uiState.copy(
            profile = UserProfile(
                age = age!!,
                sex = draft.sex,
                heightCm = height!!,
                startWeightKg = startWeight!!,
                targetWeightKg = targetWeight!!,
                goalDate = goalDate!!,
                activityLevel = draft.activityLevel
            ),
            selectedTab = FitTrackTab.Dashboard,
            profileError = null,
            profileSavedMessage = "Profile saved for this session. Phase 2 will persist it locally."
        )
    }
}

data class FitTrackUiState(
    val selectedTab: FitTrackTab = FitTrackTab.Dashboard,
    val profileDraft: UserProfileDraft = UserProfileDraft(
        age = "",
        heightCm = "180",
        startWeightKg = "90",
        targetWeightKg = "80",
        goalDate = LocalDate.now().plusMonths(3).toString(),
        activityLevel = ActivityLevel.Light
    ),
    val profile: UserProfile? = null,
    val profileError: String? = null,
    val profileSavedMessage: String? = null
)

enum class FitTrackTab(val label: String) {
    Dashboard("Dashboard"),
    Food("Food"),
    Progress("Progress"),
    Health("Health"),
    Settings("Settings")
}
