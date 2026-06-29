package com.asifjawad.fittrack.domain.model

import com.asifjawad.fittrack.domain.calculation.FoodCalorieCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionModelsTest {
    @Test
    fun recipeSummaryCalculatesPerServingTotals() {
        val recipe = RecipeSummary(
            id = 1,
            name = "Home chicken curry",
            servings = 4.0,
            totalCalories = 1200.0,
            totalProtein = 96.0,
            totalCarbs = 40.0,
            totalFat = 72.0,
            ingredients = emptyList()
        )

        assertEquals(300.0, recipe.caloriesPerServing, 0.01)
        assertEquals(24.0, recipe.proteinPerServing, 0.01)
        assertEquals(10.0, recipe.carbsPerServing, 0.01)
        assertEquals(18.0, recipe.fatPerServing, 0.01)
    }

    @Test
    fun caloriesForGramsScalesFromPer100gValue() {
        val calories = FoodCalorieCalculator.caloriesForGrams(
            caloriesPer100g = 130.0,
            grams = 250.0
        )

        assertEquals(325.0, calories, 0.01)
    }
}
