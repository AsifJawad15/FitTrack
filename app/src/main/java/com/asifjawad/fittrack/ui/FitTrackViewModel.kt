package com.asifjawad.fittrack.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.asifjawad.fittrack.FitTrackApplication
import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.FoodItem
import com.asifjawad.fittrack.domain.model.MealLog
import com.asifjawad.fittrack.domain.model.MealType
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import com.asifjawad.fittrack.domain.model.UserProfileDraft
import com.asifjawad.fittrack.domain.model.WaistLog
import com.asifjawad.fittrack.domain.model.WeightLog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

class FitTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FitTrackApplication
    private val profileRepository = app.appContainer.profileRepository
    private val measurementRepository = app.appContainer.measurementRepository
    private val nutritionRepository = app.appContainer.nutritionRepository

    private val today = LocalDate.now()

    var uiState by mutableStateOf(FitTrackUiState())
        private set

    init {
        viewModelScope.launch {
            nutritionRepository.seedFoodsIfNeeded()
        }

        viewModelScope.launch {
            profileRepository.observeProfile().collectLatest { profile ->
                uiState = uiState.copy(
                    profile = profile,
                    profileDraft = profile?.toDraft() ?: uiState.profileDraft
                )
            }
        }

        viewModelScope.launch {
            measurementRepository.observeWeightLogs().collectLatest { logs ->
                uiState = uiState.copy(weightLogs = logs.take(8))
            }
        }

        viewModelScope.launch {
            measurementRepository.observeWaistLogs().collectLatest { logs ->
                uiState = uiState.copy(waistLogs = logs.take(8))
            }
        }

        viewModelScope.launch {
            nutritionRepository.observeFoods().collectLatest { foods ->
                uiState = uiState.copy(foods = foods)
            }
        }

        viewModelScope.launch {
            nutritionRepository.observeMealsForDate(today).collectLatest { meals ->
                uiState = uiState.copy(mealsToday = meals)
            }
        }
    }

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
            goalDate == null -> "Enter goal date as YYYY-MM-DD."
            goalDate <= LocalDate.now() -> "Goal date must be in the future."
            else -> null
        }

        if (error != null) {
            uiState = uiState.copy(profileError = error)
            return
        }

        val profile = UserProfile(
            age = age!!,
            sex = draft.sex,
            heightCm = height!!,
            startWeightKg = startWeight!!,
            targetWeightKg = targetWeight!!,
            goalDate = goalDate!!,
            activityLevel = draft.activityLevel
        )

        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            uiState = uiState.copy(
                selectedTab = FitTrackTab.Dashboard,
                profileError = null,
                profileSavedMessage = "Profile saved locally."
            )
        }
    }

    fun updateFoodName(name: String) {
        uiState = uiState.copy(foodNameInput = name, foodError = null, foodMessage = null)
    }

    fun updateFoodCalories(calories: String) {
        uiState = uiState.copy(foodCaloriesInput = calories, foodError = null, foodMessage = null)
    }

    fun addCustomFood() {
        val name = uiState.foodNameInput.trim()
        val calories = uiState.foodCaloriesInput.toDoubleOrNull()
        val error = when {
            name.isBlank() -> "Enter a food name."
            calories == null || calories !in 1.0..900.0 -> "Enter calories per 100g (1-900)."
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(foodError = error)
            return
        }

        viewModelScope.launch {
            nutritionRepository.addCustomFood(name = name, caloriesPer100g = calories!!)
            uiState = uiState.copy(
                foodNameInput = "",
                foodCaloriesInput = "",
                foodError = null,
                foodMessage = "Food added to local database."
            )
        }
    }

    fun updateMealGrams(value: String) {
        uiState = uiState.copy(mealGramsInput = value, mealError = null, mealMessage = null)
    }

    fun setMealType(mealType: MealType) {
        uiState = uiState.copy(selectedMealType = mealType, mealError = null)
    }

    fun logMeal(foodId: Long) {
        val grams = uiState.mealGramsInput.toDoubleOrNull()
        if (grams == null || grams !in 1.0..2000.0) {
            uiState = uiState.copy(mealError = "Enter grams between 1 and 2000.")
            return
        }

        viewModelScope.launch {
            val success = nutritionRepository.logMealFromFood(
                date = today,
                mealType = uiState.selectedMealType,
                foodId = foodId,
                grams = grams
            )
            uiState = uiState.copy(
                mealError = if (success) null else "Food item not found.",
                mealMessage = if (success) "Meal logged for today." else null
            )
        }
    }

    fun updateWeightInput(value: String) {
        uiState = uiState.copy(weightInput = value, measurementError = null, measurementMessage = null)
    }

    fun updateWaistInput(value: String) {
        uiState = uiState.copy(waistInput = value, measurementError = null, measurementMessage = null)
    }

    fun addWeightLog() {
        val weight = uiState.weightInput.toDoubleOrNull()
        if (weight == null || weight !in 30.0..300.0) {
            uiState = uiState.copy(measurementError = "Enter weight in kg (30-300).")
            return
        }
        viewModelScope.launch {
            measurementRepository.addWeightLog(today, weight)
            uiState = uiState.copy(
                weightInput = "",
                measurementError = null,
                measurementMessage = "Weight saved for today."
            )
        }
    }

    fun addWaistLog() {
        val waist = uiState.waistInput.toDoubleOrNull()
        if (waist == null || waist !in 30.0..200.0) {
            uiState = uiState.copy(measurementError = "Enter waist in cm (30-200).")
            return
        }
        viewModelScope.launch {
            measurementRepository.addWaistLog(today, waist)
            uiState = uiState.copy(
                waistInput = "",
                measurementError = null,
                measurementMessage = "Waist saved for today."
            )
        }
    }

    private fun UserProfile.toDraft(): UserProfileDraft {
        return UserProfileDraft(
            age = age.toString(),
            sex = sex,
            heightCm = heightCm.trimTrailingZero(),
            startWeightKg = startWeightKg.trimTrailingZero(),
            targetWeightKg = targetWeightKg.trimTrailingZero(),
            goalDate = goalDate.toString(),
            activityLevel = activityLevel
        )
    }

    private fun Double.trimTrailingZero(): String {
        return if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            toString()
        }
    }
}

data class FitTrackUiState(
    val selectedTab: FitTrackTab = FitTrackTab.Dashboard,
    val profileDraft: UserProfileDraft = UserProfileDraft(
        age = "",
        heightCm = "",
        startWeightKg = "",
        targetWeightKg = "",
        goalDate = "",
        activityLevel = ActivityLevel.Light
    ),
    val profile: UserProfile? = null,
    val profileError: String? = null,
    val profileSavedMessage: String? = null,
    val foodNameInput: String = "",
    val foodCaloriesInput: String = "",
    val foodError: String? = null,
    val foodMessage: String? = null,
    val mealGramsInput: String = "200",
    val selectedMealType: MealType = MealType.Lunch,
    val mealError: String? = null,
    val mealMessage: String? = null,
    val weightInput: String = "",
    val waistInput: String = "",
    val measurementError: String? = null,
    val measurementMessage: String? = null,
    val foods: List<FoodItem> = emptyList(),
    val mealsToday: List<MealLog> = emptyList(),
    val weightLogs: List<WeightLog> = emptyList(),
    val waistLogs: List<WaistLog> = emptyList()
)

enum class FitTrackTab(val label: String) {
    Dashboard("Dashboard"),
    Food("Food"),
    Progress("Progress"),
    Health("Health"),
    Settings("Settings")
}
