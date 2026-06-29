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
import com.asifjawad.fittrack.domain.model.RecipeIngredientInput
import com.asifjawad.fittrack.domain.model.RecipeSummary
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
            nutritionRepository.observeRecipes().collectLatest { recipes ->
                uiState = uiState.copy(recipes = recipes)
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

    fun updateFoodProtein(protein: String) {
        uiState = uiState.copy(foodProteinInput = protein, foodError = null, foodMessage = null)
    }

    fun updateFoodCarbs(carbs: String) {
        uiState = uiState.copy(foodCarbsInput = carbs, foodError = null, foodMessage = null)
    }

    fun updateFoodFat(fat: String) {
        uiState = uiState.copy(foodFatInput = fat, foodError = null, foodMessage = null)
    }

    fun updateFoodSearch(query: String) {
        uiState = uiState.copy(foodSearchInput = query)
    }

    fun addCustomFood() {
        val name = uiState.foodNameInput.trim()
        val calories = uiState.foodCaloriesInput.toDoubleOrNull()
        val protein = uiState.foodProteinInput.toDoubleOrNull() ?: 0.0
        val carbs = uiState.foodCarbsInput.toDoubleOrNull() ?: 0.0
        val fat = uiState.foodFatInput.toDoubleOrNull() ?: 0.0
        val error = when {
            name.isBlank() -> "Enter a food name."
            calories == null || calories !in 1.0..900.0 -> "Enter calories per 100g (1-900)."
            protein !in 0.0..100.0 -> "Protein per 100g should be 0-100."
            carbs !in 0.0..100.0 -> "Carbs per 100g should be 0-100."
            fat !in 0.0..100.0 -> "Fat per 100g should be 0-100."
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(foodError = error)
            return
        }

        viewModelScope.launch {
            nutritionRepository.addCustomFood(
                name = name,
                caloriesPer100g = calories!!,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat
            )
            uiState = uiState.copy(
                foodNameInput = "",
                foodCaloriesInput = "",
                foodProteinInput = "",
                foodCarbsInput = "",
                foodFatInput = "",
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

    fun updateRecipeName(name: String) {
        uiState = uiState.copy(recipeNameInput = name, recipeError = null, recipeMessage = null)
    }

    fun updateRecipeServings(servings: String) {
        uiState = uiState.copy(recipeServingsInput = servings, recipeError = null, recipeMessage = null)
    }

    fun updateRecipeIngredientGrams(grams: String) {
        uiState = uiState.copy(recipeIngredientGramsInput = grams, recipeError = null, recipeMessage = null)
    }

    fun addRecipeIngredient(foodId: Long) {
        val food = uiState.foods.firstOrNull { it.id == foodId }
        val grams = uiState.recipeIngredientGramsInput.toDoubleOrNull()
        val error = when {
            food == null -> "Food item not found."
            grams == null || grams !in 1.0..5000.0 -> "Enter ingredient grams between 1 and 5000."
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(recipeError = error)
            return
        }

        val multiplier = grams!! / 100.0
        uiState = uiState.copy(
            recipeIngredients = uiState.recipeIngredients + PendingRecipeIngredient(
                foodId = food!!.id,
                name = food.name,
                grams = grams,
                calories = food.caloriesPer100g * multiplier,
                protein = food.proteinPer100g * multiplier,
                carbs = food.carbsPer100g * multiplier,
                fat = food.fatPer100g * multiplier
            ),
            recipeIngredientGramsInput = "",
            recipeError = null,
            recipeMessage = "${food.name} added to recipe."
        )
    }

    fun removeRecipeIngredient(index: Int) {
        uiState = uiState.copy(
            recipeIngredients = uiState.recipeIngredients.filterIndexed { currentIndex, _ ->
                currentIndex != index
            },
            recipeError = null,
            recipeMessage = null
        )
    }

    fun saveRecipe() {
        val name = uiState.recipeNameInput.trim()
        val servings = uiState.recipeServingsInput.toDoubleOrNull()
        val error = when {
            name.isBlank() -> "Enter a recipe name."
            servings == null || servings !in 0.25..100.0 -> "Enter total servings between 0.25 and 100."
            uiState.recipeIngredients.isEmpty() -> "Add at least one ingredient."
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(recipeError = error)
            return
        }

        viewModelScope.launch {
            val success = nutritionRepository.createRecipe(
                name = name,
                servings = servings!!,
                ingredients = uiState.recipeIngredients.map {
                    RecipeIngredientInput(foodId = it.foodId, grams = it.grams)
                }
            )
            uiState = uiState.copy(
                recipeNameInput = if (success) "" else uiState.recipeNameInput,
                recipeServingsInput = if (success) "4" else uiState.recipeServingsInput,
                recipeIngredientGramsInput = "",
                recipeIngredients = if (success) emptyList() else uiState.recipeIngredients,
                recipeError = if (success) null else "Could not save recipe. Check ingredients.",
                recipeMessage = if (success) "Recipe saved locally." else null
            )
        }
    }

    fun updateRecipeLogServings(servings: String) {
        uiState = uiState.copy(recipeLogServingsInput = servings, mealError = null, mealMessage = null)
    }

    fun logRecipe(recipeId: Long) {
        val servings = uiState.recipeLogServingsInput.toDoubleOrNull()
        if (servings == null || servings !in 0.05..20.0) {
            uiState = uiState.copy(mealError = "Enter recipe servings between 0.05 and 20.")
            return
        }

        viewModelScope.launch {
            val success = nutritionRepository.logMealFromRecipe(
                date = today,
                mealType = uiState.selectedMealType,
                recipeId = recipeId,
                servings = servings
            )
            uiState = uiState.copy(
                mealError = if (success) null else "Recipe not found.",
                mealMessage = if (success) "Recipe meal logged for today." else null
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
    val foodProteinInput: String = "",
    val foodCarbsInput: String = "",
    val foodFatInput: String = "",
    val foodSearchInput: String = "",
    val foodError: String? = null,
    val foodMessage: String? = null,
    val mealGramsInput: String = "200",
    val recipeLogServingsInput: String = "1",
    val selectedMealType: MealType = MealType.Lunch,
    val mealError: String? = null,
    val mealMessage: String? = null,
    val recipeNameInput: String = "",
    val recipeServingsInput: String = "4",
    val recipeIngredientGramsInput: String = "",
    val recipeIngredients: List<PendingRecipeIngredient> = emptyList(),
    val recipeError: String? = null,
    val recipeMessage: String? = null,
    val weightInput: String = "",
    val waistInput: String = "",
    val measurementError: String? = null,
    val measurementMessage: String? = null,
    val foods: List<FoodItem> = emptyList(),
    val recipes: List<RecipeSummary> = emptyList(),
    val mealsToday: List<MealLog> = emptyList(),
    val weightLogs: List<WeightLog> = emptyList(),
    val waistLogs: List<WaistLog> = emptyList()
)

data class PendingRecipeIngredient(
    val foodId: Long,
    val name: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

enum class FitTrackTab(val label: String) {
    Dashboard("Dashboard"),
    Food("Food"),
    Progress("Progress"),
    Health("Health"),
    Settings("Settings")
}
