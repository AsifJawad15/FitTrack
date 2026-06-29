package com.asifjawad.fittrack.domain.model

import java.time.LocalDate

enum class MealType(val label: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner"),
    Snack("Snack")
}

data class WeightLog(
    val id: Long,
    val date: LocalDate,
    val weightKg: Double,
    val note: String?
)

data class WaistLog(
    val id: Long,
    val date: LocalDate,
    val waistCm: Double,
    val note: String?
)

data class FoodItem(
    val id: Long,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val isSeed: Boolean
)

data class MealLog(
    val id: Long,
    val date: LocalDate,
    val mealType: MealType,
    val displayName: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
