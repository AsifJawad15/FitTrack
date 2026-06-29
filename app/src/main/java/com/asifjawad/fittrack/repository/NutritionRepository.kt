package com.asifjawad.fittrack.repository

import com.asifjawad.fittrack.data.local.FoodDao
import com.asifjawad.fittrack.data.local.FoodItemEntity
import com.asifjawad.fittrack.data.local.MealLogDao
import com.asifjawad.fittrack.data.local.MealLogEntity
import com.asifjawad.fittrack.data.local.SeedData
import com.asifjawad.fittrack.domain.model.FoodItem
import com.asifjawad.fittrack.domain.model.MealLog
import com.asifjawad.fittrack.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class NutritionRepository(
    private val foodDao: FoodDao,
    private val mealDao: MealLogDao
) {
    fun observeFoods(): Flow<List<FoodItem>> {
        return foodDao.observeAll().map { foods -> foods.map { it.toDomain() } }
    }

    fun observeMealsForDate(date: LocalDate): Flow<List<MealLog>> {
        return mealDao.observeByDate(date.toEpochDay()).map { logs ->
            logs.map { entity ->
                MealLog(
                    id = entity.id,
                    date = LocalDate.ofEpochDay(entity.dateEpochDay),
                    mealType = MealType.valueOf(entity.mealType),
                    displayName = entity.displayName,
                    grams = entity.grams,
                    calories = entity.calories,
                    protein = entity.protein,
                    carbs = entity.carbs,
                    fat = entity.fat
                )
            }
        }
    }

    suspend fun seedFoodsIfNeeded() {
        val now = Instant.now().toEpochMilli()
        foodDao.insertAll(SeedData.bangladeshiFoods(now))
    }

    suspend fun addCustomFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double = 0.0,
        carbsPer100g: Double = 0.0,
        fatPer100g: Double = 0.0
    ) {
        foodDao.insert(
            FoodItemEntity(
                name = name.trim(),
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                isSeed = false,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun logMealFromFood(
        date: LocalDate,
        mealType: MealType,
        foodId: Long,
        grams: Double
    ): Boolean {
        val food = foodDao.getById(foodId) ?: return false
        val multiplier = grams / 100.0
        mealDao.insert(
            MealLogEntity(
                dateEpochDay = date.toEpochDay(),
                mealType = mealType.name,
                foodItemId = food.id,
                recipeId = null,
                displayName = food.name,
                grams = grams,
                calories = food.caloriesPer100g * multiplier,
                protein = food.proteinPer100g * multiplier,
                carbs = food.carbsPer100g * multiplier,
                fat = food.fatPer100g * multiplier,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
        return true
    }

    private fun FoodItemEntity.toDomain(): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            isSeed = isSeed
        )
    }
}
